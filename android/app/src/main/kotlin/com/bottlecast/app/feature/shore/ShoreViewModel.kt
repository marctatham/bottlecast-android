package com.bottlecast.app.feature.shore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.daysLeftUntil
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.holdEnds
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.mayOffer
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.feature.shore.domain.ShoreNotice
import com.bottlecast.app.feature.shore.domain.ShoreNotices
import com.bottlecast.app.feature.shore.domain.Tide
import com.bottlecast.app.feature.shore.domain.tideAt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * The Shore (spec §3.3): state A while today's bottle waits, state B once it's found, until the
 * next tide at local midnight. In state A it picks today's candidate ahead of the tap, which is
 * where a new user's silent guest account is first made.
 */
@HiltViewModel
class ShoreViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val seaRepository: ISeaRepository,
    private val seaActions: ISeaActions,
    private val streakRepository: IStreakRepository,
    private val chestRepository: IChestRepository,
    private val shoreNotices: ShoreNotices,
    private val reminderStore: IReminderStore,
    private val notificationAccess: INotificationAccess,
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<ShoreUiState> = MutableStateFlow(ShoreUiState.BottleWaiting())
    val state: StateFlow<ShoreUiState> = _state.asStateFlow()

    /** A missed day's news, for a one-time snackbar (spec §3.3). */
    val streakNotice: StateFlow<StreakEvent?> = streakRepository.notice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(NOTICE_STOP_MILLIS), null)

    /** A notice another screen left for the Shore. */
    val shoreNotice: StateFlow<ShoreNotice?> = shoreNotices.pending

    // the Shore has no failure screen: if its inputs fail, it still offers today's bottle
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Couldn't follow the Shore's state")
        _state.value = ShoreUiState.BottleWaiting()
    }

    private var prefetch: Job? = null

    // read again each time the Shore comes back, since it can change in Android's settings meanwhile
    private val notificationsAllowed = MutableStateFlow(notificationAccess.allowed())

    // what the Shore offers of the daily reminder (spec §3.16a)
    private data class Reminders(val chipTime: LocalTime?, val offer: Boolean)

    private data class Context(val isAdmin: Boolean, val online: Boolean, val now: ZonedDateTime, val reminders: Reminders)

    // what the Shore shows of the streak and the chest
    private data class Numbers(val streak: Streak?, val counts: ChestCounts?, val awayFinds: Int)

    init {
        chestRepository.refreshCounts()
        viewModelScope.launch(exceptionHandler) {
            // The minute's tick drives the countdown, and a new date finds tomorrow's record. It ticks
            // only while the Shore is watched, so it costs nothing under another screen or in the
            // background, and it starts again from the clock, so a phone that slept through midnight
            // shows the new day at once. Today's listeners carry on meanwhile, so coming back from
            // Reveal shows the find straight away.
            val now = watchedMinuteTicks().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
            val reminders = combine(reminderStore.settings, notificationsAllowed) { settings, allowed ->
                val mayOffer = settings.mayOffer(clock.instant(), allowed)
                Reminders(chipTime = settings.time.takeIf { mayOffer }, offer = mayOffer && !settings.rationaleOffered)
            }
            val context = combine(authenticationRepository.user, connectivityMonitor.isOnline, now, reminders) { user, online, time, reminders ->
                Context(isAdmin = user is UserEntity.AuthenticatedUser && user.isAdmin, online = online, now = time, reminders = reminders)
            }
            val numbers = combine(streakRepository.streak, chestRepository.counts, chestRepository.castFindsSinceViewed) { streak, counts, away ->
                Numbers(streak, counts, away)
            }
            combine(context, todaysFind(now), seaRepository.candidate, seaActions.pending, numbers) { ctx, find, candidate, pending, numbers ->
                stateFor(ctx, find, candidate, pending, numbers)
            }.collect { state ->
                _state.value = state
                if (state is ShoreUiState.BottleWaiting && !state.isOffline) prefetch()
            }
        }
    }

    fun onStreakNoticeShown(event: StreakEvent) = streakRepository.acknowledge(event)

    /** The Shore is on screen again: notifications may have been turned on or off in Android's settings. */
    fun onResume() {
        notificationsAllowed.value = notificationAccess.allowed()
    }

    fun onShoreNoticeShown(notice: ShoreNotice) = shoreNotices.consume(notice)

    /** Takes back a throw back still waiting out its window. */
    fun onUndoThrowBack(actionId: String) = seaActions.undo(actionId)

    /** Throws back at once: the snackbar was swiped away. */
    fun onCommitThrowBack(actionId: String) = seaActions.commit(actionId)

    private fun stateFor(
        context: Context,
        find: TodayFind?,
        candidate: CandidateState,
        pending: PendingSeaAction?,
        numbers: Numbers,
    ): ShoreUiState {
        val streak = numbers.streak?.let { ShoreStreak(days = it.current, shields = it.shields) }
        val stats = numbers.counts?.let { ShoreStats(bottled = it.bottled, found = it.found, kept = it.kept) }
        if (find == null) {
            return ShoreUiState.BottleWaiting(
                isAdmin = context.isAdmin,
                isOffline = !context.online,
                streak = streak,
                stats = stats,
                awayFinds = numbers.awayFinds,
                // offline, the sea can't have been looked at today, so today's bottle waits
                seaCalm = candidate == CandidateState.SeaCalm && context.online,
            )
        }
        // a find that's been thrown back or reported, even one still waiting out its undo, is gone
        // from the Shore, and so is a bottle that can't be read any more
        val showCard = find.status in setOf(FindStatus.Held, FindStatus.Kept) && pending?.messageId != find.messageId
        return ShoreUiState.FoundToday(
            tide = tideAt(context.now),
            todaysFind = find.message?.takeIf { showCard }?.let { message ->
                TodaysFind(
                    messageId = find.messageId,
                    excerpt = message.message,
                    kept = find.status == FindStatus.Kept,
                    daysLeft = message.holdEnds()?.let { daysLeftUntil(it, clock.instant()) },
                )
            },
            reminderTime = context.reminders.chipTime,
            offerReminders = context.reminders.offer,
            pendingThrowBack = (pending as? PendingSeaAction.ThrowBack)?.takeIf { it.messageId == find.messageId },
            isAdmin = context.isAdmin,
            isOffline = !context.online,
            streak = streak,
            stats = stats,
            awayFinds = numbers.awayFinds,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun todaysFind(now: Flow<ZonedDateTime>): Flow<TodayFind?> = now
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { date -> seaRepository.observeToday(date) }

    // The candidate is pinned for the account and the day, so this costs nothing once it's chosen.
    private fun prefetch() {
        if (prefetch?.isActive == true) return
        prefetch = viewModelScope.launch {
            try {
                seaRepository.prefetchCandidate()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.w(exception, "Couldn't look at the sea ahead of the tap")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun watchedMinuteTicks(): Flow<ZonedDateTime> = SharingStarted.WhileSubscribed(TICKS_LINGER_MILLIS)
        .command(_state.subscriptionCount)
        .distinctUntilChanged()
        .flatMapLatest { command -> if (command == SharingCommand.START) minuteTicks() else emptyFlow() }

    // now, then at the start of every minute
    private fun minuteTicks(): Flow<ZonedDateTime> = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            emit(now)
            val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            delay(Duration.between(now, nextMinute).toMillis().coerceAtLeast(1))
        }
    }
}

// how long the ticks carry on once nobody watches, so a rotation doesn't stop them
private const val TICKS_LINGER_MILLIS = 5_000L

private const val NOTICE_STOP_MILLIS = 5_000L

/** The Shore's two states (spec §3.3). A null [streak] or [stats] isn't shown. */
sealed class ShoreUiState {
    abstract val isAdmin: Boolean
    abstract val isOffline: Boolean
    abstract val streak: ShoreStreak?
    abstract val stats: ShoreStats?

    /** Finds of the user's bottles since they last looked in the chest: "While you were away" (G5). */
    abstract val awayFinds: Int

    /** State A: today's bottle is waiting, unless the sea is known to be calm. */
    data class BottleWaiting(
        override val isAdmin: Boolean = false,
        override val isOffline: Boolean = false,
        override val streak: ShoreStreak? = null,
        override val stats: ShoreStats? = null,
        override val awayFinds: Int = 0,
        /** No bottle is drifting that this account could find: the Shore offers casting instead. */
        val seaCalm: Boolean = false,
    ) : ShoreUiState()

    /** State B: today's bottle is found, and the next tide is counting down. */
    data class FoundToday(
        val tide: Tide,
        /** The card straddling the arc; null once the bottle is thrown back, reported, or gone. */
        val todaysFind: TodaysFind?,
        /** The daily reminder's time, shown only while notifications are off. */
        val reminderTime: LocalTime?,
        /** The reminder sheet hasn't been offered yet: it opens once, after the first find (spec §3.16a). */
        val offerReminders: Boolean = false,
        /** A throw back waiting out its window, for the snackbar's Undo. */
        val pendingThrowBack: PendingSeaAction.ThrowBack? = null,
        override val isAdmin: Boolean = false,
        override val isOffline: Boolean = false,
        override val streak: ShoreStreak? = null,
        override val stats: ShoreStats? = null,
        override val awayFinds: Int = 0,
    ) : ShoreUiState()
}

/** The tide streak and its calm-sea shields (spec §3.3, G2). */
data class ShoreStreak(
    val days: Int,
    val shields: Int,
)

/** The Shore's stats. Guests don't cast, so they have no [bottled] count. */
data class ShoreStats(
    val bottled: Int?,
    val found: Int,
    val kept: Int,
)

/** Today's find, as the card straddling the arc shows it; tapping it reopens Reveal. */
data class TodaysFind(
    val messageId: String,
    val excerpt: String,
    val kept: Boolean = false,
    /** Whole days until an unkept find drifts back, or null when that isn't known. */
    val daysLeft: Int? = null,
)
