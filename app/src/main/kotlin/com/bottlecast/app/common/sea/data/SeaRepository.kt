package com.bottlecast.app.common.sea.data

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.REPORT_THRESHOLD
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.random.Random

// how many candidates to try at the pop when others take them first
private const val MAX_FIND_ATTEMPTS = 3

// a calm sea is looked at again after this, in case someone has cast since
private val SEA_CALM_RECHECK: Duration = Duration.ofMinutes(10)

// an hour's margin, so a hold the server still thinks is live isn't picked
private val EXPIRY_MARGIN: Duration = Duration.ofHours(1)

class SeaRepository @Inject constructor(
    private val source: ISeaDataSource,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    private val random: Random,
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

    private val mutex = Mutex()
    private var pin: Pin? = null
    private var blocked: Pair<String, Set<String>>? = null
    private val justFound = ConcurrentHashMap<String, MessageEntity>()

    private val _candidate = MutableStateFlow<CandidateState>(CandidateState.Unknown)
    override val candidate: StateFlow<CandidateState> = _candidate.asStateFlow()

    override suspend fun prefetchCandidate(): CandidateState = withContext(dispatcher) {
        mutex.withLock {
            val uid = authenticationRepository.ensureSignedIn()
            val date = LocalDate.now(clock)
            currentPin(uid, date)?.let { return@withLock it.state }
            pick(uid, date, exclude = emptySet()).state
        }
    }

    // The pin, unless it's for another account or day, or a calm sea that's due another look.
    private fun currentPin(uid: String, date: LocalDate): Pin? = pin?.takeIf {
        it.uid == uid && it.date == date &&
            !(it.state is CandidateState.SeaCalm && Duration.between(it.pinnedAt, clock.instant()) >= SEA_CALM_RECHECK)
    }

    private fun pick(uid: String, date: LocalDate, exclude: Set<String>): Pin {
        val blockedWriters = blockedWritersOf(uid)
        val fromRand = random.nextDouble()
        val upward = source.querySea(fromRand, wrap = false)
        val drifting = if (upward.size < SEA_BATCH_SIZE) upward + source.querySea(fromRand, wrap = true) else upward
        var pool = seaCandidates(drifting, uid, blockedWriters, exclude)
        if (pool.isEmpty()) {
            val expiredBefore = clock.instant().minus(Duration.ofDays(HOLD_DAYS)).minus(EXPIRY_MARGIN)
            pool = seaCandidates(source.queryExpiredHolds(expiredBefore), uid, blockedWriters, exclude)
        }

        // A bottle this account found before can never be found again, so skip to one it hasn't.
        val unseen = pool.dropWhile { source.hasFound(it.id, uid) }
        val chosen = unseen.firstOrNull()
        val state = if (chosen == null) CandidateState.SeaCalm else CandidateState.Available(chosen)
        return Pin(uid, date, state, clock.instant(), unseen.drop(1)).also {
            pin = it
            _candidate.value = state
        }
    }

    private fun blockedWritersOf(uid: String): Set<String> =
        blocked?.takeIf { it.first == uid }?.second
            ?: source.blockedWriters(uid).also { blocked = uid to it }

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
                        TodayFind(date, messageId, message ?: justFound[messageId], status ?: FindStatus.Held)
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
                is FindOutcome.Found -> {
                    justFound[outcome.message.id] = outcome.message
                    return FindResult.Found(outcome.message)
                }

                is FindOutcome.AlreadyFound -> return if (outcome.messageId == candidate.id) {
                    FindResult.Found(justFound[candidate.id] ?: candidate)
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

    override fun cachedMessage(messageId: String): MessageEntity? = justFound[messageId]
        ?: (pin?.state as? CandidateState.Available)?.message?.takeIf { it.id == messageId }

    override suspend fun getMessage(messageId: String): MessageEntity? =
        cachedMessage(messageId) ?: withContext(dispatcher) { source.getMessage(messageId) }

}

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

