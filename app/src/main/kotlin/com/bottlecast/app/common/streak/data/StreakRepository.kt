package com.bottlecast.app.common.streak.data

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.streak.domain.ICalmSeaLog
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import com.bottlecast.app.common.streak.domain.StreakDay
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.common.streak.domain.foldStreak
import com.bottlecast.app.common.streak.domain.isWorthNoticing
import com.bottlecast.app.common.streak.domain.liveStreak
import com.bottlecast.app.common.threading.ApplicationScope
import com.bottlecast.app.common.threading.DispatcherIO
import com.bottlecast.app.common.time.localDates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How many whole days the checkpoint stays behind today. A cast queued offline can still reach the
 * server a day late, and the rules accept a date that's today anywhere on Earth, so the last days are
 * always read again rather than trusted to the checkpoint.
 */
internal const val CHECKPOINT_LAG_DAYS = 3L

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The streak, folded from the account's find days and streak marks (spec §3.3). The fold up to a few
 * days ago is kept on the phone, so each look reads only the days since; it moves on once the server
 * has confirmed those days.
 */
@Singleton
class StreakRepository @Inject constructor(
    private val source: IStreakDataSource,
    private val checkpoints: IStreakCheckpointStore,
    authenticationRepository: IAuthenticationRepository,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : IStreakRepository, ICalmSeaLog {

    private data class Standing(val uid: String, val today: LocalDate, val streak: Streak)

    // the calm seas already marked, so a look every ten minutes writes once a day
    private val markedCalm = ConcurrentHashMap.newKeySet<Pair<String, LocalDate>>()

    private val uid: Flow<String?> = authenticationRepository.user
        .map { (it as? UserEntity.AuthenticatedUser)?.userId }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val standing: StateFlow<Standing?> = uid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else clock.localDates().flatMapLatest { today -> standingOn(uid, today) }
        }
        .flowOn(dispatcher)
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    override val streak: StateFlow<Streak?> = standing
        .map { it?.streak }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notice: Flow<StreakEvent?> = standing.flatMapLatest { standing ->
        if (standing == null) {
            flowOf(null)
        } else {
            checkpoints.noticeSeenThrough(standing.uid).map { seen ->
                standing.streak.lastEvent?.takeIf { it.isWorthNoticing(standing.streak, standing.today, seen) }
            }
        }
    }.distinctUntilChanged()

    override fun acknowledge(event: StreakEvent) {
        val uid = standing.value?.uid ?: return
        checkpoints.markNoticeSeen(uid, event.date)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun standingOn(uid: String, today: LocalDate): Flow<Standing> = flow { emit(checkpoints.load(uid)) }
        .flatMapLatest { loaded ->
            var checkpoint = loaded
            val after = loaded?.through
            combine(source.observeFindDays(uid, after), source.observeMarks(uid, after)) { finds, marks ->
                val days = streakDays(finds.value, marks.value)
                // only what the server has confirmed moves the checkpoint on: a fresh phone's cache may
                // not have the history yet
                if (finds.fromServer && marks.fromServer) checkpoint = advance(uid, checkpoint, days, today)
                Standing(uid, today, liveStreak(checkpoint, days, today))
            }
        }

    // The checkpoint, folded on to a few days ago and saved, unless it's there already.
    private fun advance(uid: String, checkpoint: StreakCheckpoint?, days: Map<LocalDate, StreakDay>, today: LocalDate): StreakCheckpoint {
        val through = today.minusDays(CHECKPOINT_LAG_DAYS)
        if (checkpoint != null && !checkpoint.through.isBefore(through)) return checkpoint
        return foldStreak(checkpoint, days, through).also { checkpoints.save(uid, it) }
    }

    override fun markCalm(uid: String, date: LocalDate) {
        if (markedCalm.add(uid to date)) source.markCalm(uid, date)
    }

    // the sign-in sheet follows the streak while it's open, so this is what it showed
    override suspend fun carryable(): Int = streak.value?.carryable ?: 0

    override suspend fun carry(uid: String, days: Int) {
        if (days <= 0) return
        withContext(dispatcher) {
            try {
                source.carry(uid, LocalDate.now(clock), days)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // the account may already have carried a longer streak today, which the rules keep
                Timber.w(exception, "[carry] Couldn't carry a streak over")
            }
        }
    }
}

/** The account's days, from its find dates and its marks. */
internal fun streakDays(finds: Set<LocalDate>, marks: List<StreakDay>): Map<LocalDate, StreakDay> {
    val days = marks.associateBy { it.date }.toMutableMap()
    for (date in finds) days[date] = (days[date] ?: StreakDay(date)).copy(found = true)
    return days
}
