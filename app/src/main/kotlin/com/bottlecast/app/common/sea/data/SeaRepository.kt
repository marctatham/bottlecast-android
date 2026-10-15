package com.bottlecast.app.common.sea.data

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.REPORT_THRESHOLD
import com.bottlecast.app.common.messages.domain.isHoldLive
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.Handover
import com.bottlecast.app.common.sea.domain.HandoverResult
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.common.streak.domain.ICalmSeaLog
import com.bottlecast.app.common.threading.DispatcherIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.random.Random

// how many candidates to try at the pop when others take them first
private const val MAX_FIND_ATTEMPTS = 3

// a calm sea is looked at again after this, in case someone has cast since
private val SEA_CALM_RECHECK: Duration = Duration.ofMinutes(10)

// an hour's margin, so a hold the server still thinks is live isn't picked
private val EXPIRY_MARGIN: Duration = Duration.ofHours(1)

// How far one look reads on through each part of the sea before calling it calm. A look stops as soon
// as it has today's candidate and a fallback for each other attempt at the pop.
private const val SEA_PAGES = 2
private const val EXPIRED_PAGES = 2

class SeaRepository @Inject constructor(
    private val source: ISeaDataSource,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    private val random: Random,
    private val secureRandom: SecureRandom,
    private val calmSeaLog: ICalmSeaLog,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : ISeaRepository {

    // Today's candidate, and the rest of the pool to fall back on if someone else takes it first.
    private data class Pin(
        val uid: String,
        val date: LocalDate,
        val state: CandidateState,
        val pinnedAt: Instant,
        val fallbacks: List<MessageEntity>,
    )

    // The writers an account had blocked when the day's look at the sea began. A block always comes
    // with a report, which needs the day's find, so a new block can't matter until the next day's look.
    private data class Blocked(val uid: String, val date: LocalDate, val writers: Set<String>)

    private val mutex = Mutex()
    private var pin: Pin? = null
    private var blocked: Blocked? = null
    private val justFound = ConcurrentHashMap<String, MessageEntity>()

    // Bottles each account is known to have found. A find is forever, so only a yes is kept.
    private val foundBefore = ConcurrentHashMap<String, MutableSet<String>>()

    private val _candidate = MutableStateFlow<CandidateState>(CandidateState.Unknown)
    override val candidate: StateFlow<CandidateState> = _candidate.asStateFlow()

    override suspend fun prefetchCandidate(): CandidateState = withContext(dispatcher) {
        mutex.withLock {
            val uid = authenticationRepository.ensureSignedIn()
            val date = LocalDate.now(clock)
            currentPin(uid, date)?.let { return@withLock it.state }
            // another account's or day's candidate says nothing about this one's
            if (pin?.let { it.uid != uid || it.date != date } == true) _candidate.value = CandidateState.Unknown
            pick(uid, date, exclude = emptySet()).state
        }
    }

    // The pin, unless it's for another account or day, or a calm sea that's due another look.
    private fun currentPin(uid: String, date: LocalDate): Pin? = pin?.takeIf {
        it.uid == uid && it.date == date &&
            !(it.state is CandidateState.SeaCalm && Duration.between(it.pinnedAt, clock.instant()) >= SEA_CALM_RECHECK)
    }

    // Reads on through the sea, from a random point up, then round from the bottom to it, then the holds
    // that have ended, until it has today's candidate and its fallbacks or the pages run out.
    private fun pick(uid: String, date: LocalDate, exclude: Set<String>): Pin {
        val blockedWriters = blocked?.takeIf { it.uid == uid && it.date == date }?.writers ?: source.blockedWriters(uid)
        val look = Look(uid, blockedWriters, exclude)
        val fromRand = random.nextDouble()
        look.readOn(SEA_PAGES, SEA_BATCH_SIZE, MessageEntity::rand) { after -> source.querySea(fromRand, 1.0, after) }
        look.readOn(SEA_PAGES, SEA_BATCH_SIZE, MessageEntity::rand) { after -> source.querySea(0.0, fromRand, after) }
        val expiredBefore = clock.instant().minus(Duration.ofDays(HOLD_DAYS)).minus(EXPIRY_MARGIN)
        look.readOn(EXPIRED_PAGES, EXPIRED_BATCH_SIZE, MessageEntity::foundAt) { after -> source.queryExpiredHolds(expiredBefore, after) }

        val chosen = look.unseen.firstOrNull()
        val state = if (chosen == null) CandidateState.SeaCalm else CandidateState.Available(chosen)
        // the server said so, and a calm day never breaks a streak (spec §3.3); a look that ran out
        // after others took its candidates at the pop isn't a calm sea
        if (chosen == null && exclude.isEmpty()) calmSeaLog.markCalm(uid, date)
        blocked = Blocked(uid, date, blockedWriters)
        return Pin(uid, date, state, clock.instant(), look.unseen.drop(1)).also {
            pin = it
            _candidate.value = state
        }
    }

    // One look at the sea: the bottles it has passed, and the ones this account could find.
    private inner class Look(val uid: String, val blockedWriters: Set<String>, exclude: Set<String>) {
        val unseen = mutableListOf<MessageEntity>()
        private val passed = exclude.toMutableSet()
        private val full get() = unseen.size >= MAX_FIND_ATTEMPTS

        fun <C : Any> readOn(maxPages: Int, pageSize: Int, cursor: (MessageEntity) -> C?, read: (after: C?) -> List<MessageEntity>) {
            var after: C? = null
            repeat(maxPages) {
                if (full) return
                val page = read(after)
                for (bottle in seaCandidates(page, uid, blockedWriters, passed)) {
                    passed += bottle.id
                    // a bottle this account found before can never be found again
                    if (!hasFound(bottle.id, uid)) unseen += bottle
                    if (full) return
                }
                if (page.size < pageSize) return
                after = cursor(page.last()) ?: return
            }
        }
    }

    private fun hasFound(messageId: String, uid: String): Boolean {
        val known = foundBefore.getOrPut(uid) { ConcurrentHashMap.newKeySet() }
        if (messageId in known) return true
        return source.hasFound(messageId, uid).also { if (it) known += messageId }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeToday(date: LocalDate): Flow<TodayFind?> = authenticationRepository.user
        .map { (it as? UserEntity.AuthenticatedUser)?.userId }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(null)
            source.observeDaily(uid, date).flatMapLatest { messageId ->
                if (messageId == null) {
                    flowOf(null)
                } else {
                    combine(source.observeFindStatus(messageId, uid), source.observeMessage(messageId)) { status, message ->
                        // a bottle that can't be read any more, such as one a moderator pulled, is gone
                        // here too, so Reveal can't show it from memory
                        if (message == null) justFound.remove(messageId)
                        TodayFind(date, messageId, message, status ?: FindStatus.Held)
                    }
                }
            }
        }

    override suspend fun findTodaysBottle(): FindResult = withContext(dispatcher) {
        if (!connectivityMonitor.isOnline.value) return@withContext FindResult.Offline
        mutex.withLock {
            try {
                find(authenticationRepository.ensureSignedIn())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Couldn't record today's find")
                if (exception is SeaUnavailableException) FindResult.Offline else FindResult.Failed
            }
        }
    }

    private fun find(uid: String): FindResult {
        val date = LocalDate.now(clock)
        var current = currentPin(uid, date) ?: pick(uid, date, exclude = emptySet())
        val tried = mutableSetOf<String>()

        repeat(MAX_FIND_ATTEMPTS) {
            val candidate = (current.state as? CandidateState.Available)?.message ?: return FindResult.SeaCalm
            tried += candidate.id
            val outcome = try {
                source.recordFind(candidate.id, uid, date, clock.zone.id, clock.instant())
            } catch (exception: SeaRefusedException) {
                // The rules turned it down, or the transaction lost a race: someone took this one.
                Timber.w(exception, "A candidate couldn't be found")
                FindOutcome.Taken
            }
            when (outcome) {
                is FindOutcome.Found -> return FindResult.Found(remember(uid, outcome.message))

                // An earlier attempt went through though its answer was lost, so the hold is today's.
                // The candidate is from before the find, so it's held from now, or a throw back would
                // think the hold over and leave the bottle out of the sea.
                is FindOutcome.AlreadyFound -> return if (outcome.messageId == candidate.id) {
                    FindResult.Found(justFound[candidate.id] ?: remember(uid, candidate.heldFrom(clock.instant())))
                } else {
                    FindResult.AlreadyFound(outcome.messageId)
                }

                FindOutcome.Taken -> current = nextCandidate(current, tried)
            }
        }
        return FindResult.Failed
    }

    // The next fallback from the same look at the sea, or a fresh look when they've run out.
    private fun nextCandidate(current: Pin, tried: Set<String>): Pin {
        val next = current.fallbacks.firstOrNull()
        if (next == null) return pick(current.uid, current.date, exclude = tried)
        return current.copy(state = CandidateState.Available(next), fallbacks = current.fallbacks.drop(1)).also {
            pin = it
            _candidate.value = it.state
        }
    }

    // A just-found bottle, for Reveal's first frame, and a find this account won't try again.
    private fun remember(uid: String, found: MessageEntity): MessageEntity {
        justFound[found.id] = found
        foundBefore.getOrPut(uid) { ConcurrentHashMap.newKeySet() } += found.id
        return found
    }

    override fun cachedMessage(messageId: String): MessageEntity? = justFound[messageId]

    override suspend fun getMessage(messageId: String): MessageEntity? =
        cachedMessage(messageId) ?: withContext(dispatcher) { source.getMessage(messageId) }

    // Neither handover step takes the sea's lock: a look for the new account may hold it a while.

    override suspend fun prepareHandover(): Handover? = withContext(dispatcher) {
        val guest = authenticationRepository.user.value as? UserEntity.AuthenticatedUser
        if (guest == null || guest.providerType != AuthProviderType.Anonymous) return@withContext null
        try {
            val date = LocalDate.now(clock)
            val messageId = source.dailyMessageId(guest.userId, date) ?: return@withContext null
            val find = source.findOf(messageId, guest.userId)
            val message = source.serverMessage(messageId)
            // only a hold the guest still has: not thrown back, reported or run out
            if (find?.status != FindStatus.Held || message?.foundAt == null || find.foundAt != message.foundAt ||
                !message.isHoldLive(clock.instant())
            ) {
                return@withContext null
            }
            val token = newHandoverToken()
            source.armHandoff(messageId, guest.userId, date, sha256(token))
            Handover(messageId, guest.userId, token)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.w(exception, "[prepareHandover] Couldn't arm a handover of today's find")
            null
        }
    }

    override suspend fun claimHandover(handover: Handover, uid: String): HandoverResult = withContext(dispatcher) {
        if (uid == handover.fromUid) {
            Timber.w("[claimHandover] The guest can't hand its find to itself")
            return@withContext HandoverResult.Failed
        }
        try {
            val date = LocalDate.now(clock)
            if (source.dailyMessageId(uid, date) != null) return@withContext HandoverResult.KeptOwnFind
            if (hasFound(handover.messageId, uid)) return@withContext HandoverResult.Failed
            source.claimHandoff(handover.messageId, uid, handover.token, date, clock.zone.id)
            // held from now, as the claim left it, so a throw back sees a live hold
            justFound[handover.messageId]?.let { justFound[handover.messageId] = it.copy(foundAt = clock.instant()) }
            foundBefore.getOrPut(uid) { ConcurrentHashMap.newKeySet() } += handover.messageId
            HandoverResult.HandedOver
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.w(exception, "[claimHandover] Couldn't take over the guest's find")
            HandoverResult.Failed
        }
    }

    // 32 random bytes, which only this phone knows until the claim presents them
    private fun newHandoverToken(): String {
        val bytes = ByteArray(HANDOVER_TOKEN_BYTES).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

}

private const val HANDOVER_TOKEN_BYTES = 32

/** The commitment a handoff carries: the SHA-256 of the token's UTF-8, which firestore.rules checks. */
internal fun sha256(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))

/** A bottle from before its find, as the find left it: held from [now]. */
private fun MessageEntity.heldFrom(now: Instant): MessageEntity =
    copy(status = MessageStatus.Found, findCount = findCount + 1, foundAt = now)

/**
 * The bottles this account could find, in the order they came: never its own, a blocked writer's, one
 * already tried, or one with enough reports to be out of the sea.
 */
internal fun seaCandidates(
    pool: List<MessageEntity>,
    uid: String,
    blockedWriters: Set<String>,
    exclude: Set<String>,
): List<MessageEntity> = pool
    .distinctBy { it.id }
    .filter { it.userId != uid && it.userId !in blockedWriters && it.id !in exclude && it.reportCount < REPORT_THRESHOLD }

