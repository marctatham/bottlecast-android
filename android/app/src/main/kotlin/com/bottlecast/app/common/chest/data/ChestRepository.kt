package com.bottlecast.app.common.chest.data

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.ChestPage
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.threading.ApplicationScope
import com.bottlecast.app.common.threading.DispatcherIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val STOP_TIMEOUT_MILLIS = 5_000L

// how long a keep waits for an account just signed in to show up
private const val SIGNED_IN_WAIT_MILLIS = 5_000L

// counting again within this long of the last count only repeats it
private val RECOUNT_AFTER: Duration = Duration.ofSeconds(30)

// a day past the hold, so a find whose hold is ending still comes through the query
private const val FINDS_LOOKBACK_DAYS = HOLD_DAYS + 1

/**
 * The chest, read straight from Firestore: kept bottles from the account's claims, unkept ones from its
 * finds of the last week, and casts from its own bottles. The counts come from the server alone, so
 * they're kept here, per account, and asked for again only when something changed.
 */
@Singleton
class ChestRepository @Inject constructor(
    private val source: IChestDataSource,
    private val seenStore: IChestSeenStore,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : IChestRepository {

    private data class Account(val uid: String, val canCast: Boolean)

    private data class Counted(val uid: String, val totals: ChestTotals, val at: Instant)

    private val account: Flow<Account?> = authenticationRepository.user
        .map { user -> (user as? UserEntity.AuthenticatedUser)?.let { Account(it.userId, user.googleUid != null) } }
        .distinctUntilChanged()

    private val currentAccount: Account?
        get() = (authenticationRepository.user.value as? UserEntity.AuthenticatedUser)
            ?.let { Account(it.userId, authenticationRepository.user.value.googleUid != null) }

    private val counted = MutableStateFlow<Counted?>(null)
    private val countMutex = Mutex()

    override suspend fun keep(messageId: String): KeepResult {
        // Firebase tells the app about an account just signed in a moment after the sign-in returns,
        // so a keep straight after the sign-in sheet waits a little for the Google account
        val uid = withTimeoutOrNull(SIGNED_IN_WAIT_MILLIS) { authenticationRepository.user.first { it.googleUid != null } }
            ?.googleUid ?: return KeepResult.NotHeld
        if (!connectivityMonitor.isOnline.value) return KeepResult.Offline
        return withContext(dispatcher) {
            try {
                source.keep(messageId, uid, clock.instant())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "[keep] Couldn't keep a bottle")
                KeepResult.Failed
            }
        }.also { if (it == KeepResult.Kept) refreshCounts(force = true) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notKept: StateFlow<List<FoundBottle>?> = account
        .flatMapLatest { account ->
            if (account == null) return@flatMapLatest flowOf(null)
            val since = clock.instant().minus(FINDS_LOOKBACK_DAYS, ChronoUnit.DAYS)
            source.observeFinds(account.uid, since).mapLatest { finds ->
                val now = clock.instant()
                finds
                    .filter { it.status == FindStatus.Held && it.foundAt.plus(HOLD_DAYS, ChronoUnit.DAYS).isAfter(now) }
                    // a bottle that can't be read any more is out of the sea, and out of reach here
                    .mapNotNull { find -> source.readMessage(find.messageId)?.let { FoundBottle(find.messageId, it, find.foundAt, null) } }
            }
        }
        .flowOn(dispatcher)
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun kept(limit: Int): Flow<ChestPage<FoundBottle>> = account
        .flatMapLatest { account ->
            if (account == null || !account.canCast) return@flatMapLatest flowOf(ChestPage(emptyList(), hasMore = false))
            source.observeClaims(account.uid, limit).mapLatest { claims ->
                val bottles = claims.map { claim ->
                    val message = source.readMessage(claim.messageId)
                    FoundBottle(claim.messageId, message, message?.foundAt, claim.keptAt ?: message?.keptAt ?: clock.instant())
                }
                ChestPage(bottles, hasMore = claims.size >= limit)
            }
        }
        .flowOn(dispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun cast(limit: Int): Flow<ChestPage<MessageEntity>> = account
        .flatMapLatest { account ->
            if (account == null || !account.canCast) return@flatMapLatest flowOf(ChestPage(emptyList(), hasMore = false))
            source.observeCasts(account.uid, limit).map { ChestPage(it, hasMore = it.size >= limit) }
        }
        .flowOn(dispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun item(messageId: String): Flow<ChestItem?> = account
        .flatMapLatest { account ->
            if (account == null) return@flatMapLatest flowOf(null)
            val claim = if (account.canCast) source.observeClaim(messageId) else flowOf(null)
            combine(source.observeMessage(messageId), source.observeFind(messageId, account.uid), claim) { message, find, kept ->
                val now = clock.instant()
                when {
                    message != null && message.userId == account.uid -> ChestItem.Cast(message)
                    kept != null -> ChestItem.Found(
                        FoundBottle(messageId, message, message?.foundAt ?: find?.foundAt, kept.keptAt ?: message?.keptAt ?: now),
                    )
                    message != null && find != null && find.status == FindStatus.Held &&
                        find.foundAt.plus(HOLD_DAYS, ChronoUnit.DAYS).isAfter(now) ->
                        ChestItem.Found(FoundBottle(messageId, message, find.foundAt, keptAt = null))
                    else -> null
                }
            }
        }
        .flowOn(dispatcher)

    override suspend fun pull(messageId: String): PullResult {
        val uid = currentAccount?.takeIf { it.canCast }?.uid ?: return PullResult.NotDrifting
        if (!connectivityMonitor.isOnline.value) return PullResult.Offline
        return withContext(dispatcher) {
            try {
                source.pull(messageId, uid, clock.instant())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "[pull] Couldn't pull a bottle from the sea")
                PullResult.Failed
            }
        }.also { if (it == PullResult.Pulled) refreshCounts(force = true) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val counts: StateFlow<ChestCounts?> = account
        // a new account counts afresh; the same one again only once its last count is stale
        .onEach { if (it != null) refreshCounts() }
        .flatMapLatest { account -> counted.map { counted -> counted?.takeIf { it.uid == account?.uid }?.totals } }
        .map { totals -> totals?.let { ChestCounts(bottled = it.bottled, found = it.found, kept = it.kept) } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    override fun refreshCounts() = refreshCounts(force = false)

    private fun refreshCounts(force: Boolean) {
        scope.launch(dispatcher) {
            countMutex.withLock {
                val account = currentAccount ?: return@withLock
                val last = counted.value
                val fresh = last != null && last.uid == account.uid &&
                    Duration.between(last.at, clock.instant()) < RECOUNT_AFTER
                if (fresh && !force) return@withLock
                val totals = try {
                    source.count(account.uid, account.canCast)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.w(exception, "[refreshCounts] Couldn't count the chest")
                    return@withLock
                }
                counted.value = Counted(account.uid, totals, clock.instant())
                // the first count this phone knows is the baseline, so old finds aren't news
                val seen = seenStore.seen(account.uid).first()
                if (seen?.castFinds == null && totals.castFinds != null) {
                    seenStore.save(account.uid, ChestSeen(seen?.viewedAt ?: Instant.EPOCH, totals.castFinds))
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val news: Flow<Pair<Boolean, Int>> = account.flatMapLatest { account ->
        if (account == null) return@flatMapLatest flowOf(false to 0)
        combine(notKept, seenStore.seen(account.uid), counted) { notKept, seen, counted ->
            val castFinds = counted?.takeIf { it.uid == account.uid }?.totals?.castFinds
            val foundSince = if (castFinds != null && seen?.castFinds != null) (castFinds - seen.castFinds).coerceAtLeast(0) else 0
            val viewedAt = seen?.viewedAt ?: Instant.EPOCH
            val unseenNotKept = notKept.orEmpty().any { it.foundAt?.isAfter(viewedAt) == true }
            (unseenNotKept || foundSince > 0) to foundSince
        }
    }

    override val badge: StateFlow<Boolean> = news
        .map { it.first }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    override val castFindsSinceViewed: StateFlow<Int> = news
        .map { it.second }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    override fun markViewed() {
        val uid = currentAccount?.uid ?: return
        scope.launch(dispatcher) {
            val castFinds = counted.value?.takeIf { it.uid == uid }?.totals?.castFinds ?: seenStore.seen(uid).first()?.castFinds
            seenStore.save(uid, ChestSeen(clock.instant(), castFinds))
        }
    }
}
