package com.bottlecast.app.feature.shore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.TodayFind
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
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
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<ShoreUiState> = MutableStateFlow(ShoreUiState.BottleWaiting())
    val state: StateFlow<ShoreUiState> = _state.asStateFlow()

    // the Shore has no failure screen: if its inputs fail, it still offers today's bottle
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Couldn't follow the Shore's state")
        _state.value = ShoreUiState.BottleWaiting()
    }

    private var prefetch: Job? = null

    private data class Context(val isAdmin: Boolean, val online: Boolean, val now: ZonedDateTime)

    init {
        viewModelScope.launch(exceptionHandler) {
            // the minute's tick drives the countdown, and a new date finds tomorrow's record
            val now = minuteTicks().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
            val context = combine(authenticationRepository.user, connectivityMonitor.isOnline, now) { user, online, time ->
                Context(isAdmin = user is UserEntity.AuthenticatedUser && user.isAdmin, online = online, now = time)
            }
            combine(context, todaysFind(now), seaRepository.candidate, seaActions.pending) { ctx, find, candidate, pending ->
                stateFor(ctx, find, candidate, pending)
            }.collect { state ->
                _state.value = state
                if (state is ShoreUiState.BottleWaiting && !state.isOffline) prefetch()
            }
        }
    }

    /** Takes back a throw back still waiting out its window. */
    fun onUndoThrowBack(actionId: String) = seaActions.undo(actionId)

    /** Throws back at once: the snackbar was swiped away. */
    fun onCommitThrowBack(actionId: String) = seaActions.commit(actionId)

    private fun stateFor(
        context: Context,
        find: TodayFind?,
        candidate: CandidateState,
        pending: PendingSeaAction?,
    ): ShoreUiState {
        if (find == null) {
            return ShoreUiState.BottleWaiting(
                isAdmin = context.isAdmin,
                isOffline = !context.online,
                seaCalm = candidate == CandidateState.SeaCalm,
            )
        }
        // a find that's been thrown back or reported, even one still waiting out its undo, is gone
        // from the Shore, and so is a bottle that can't be read any more
        val showCard = find.status == FindStatus.Held && find.message != null && pending?.messageId != find.messageId
        return ShoreUiState.FoundToday(
            tide = tideAt(context.now),
            todaysFind = if (showCard) TodaysFind(find.messageId, find.message!!.message) else null,
            reminderTime = null,
            pendingThrowBack = (pending as? PendingSeaAction.ThrowBack)?.takeIf { it.messageId == find.messageId },
            isAdmin = context.isAdmin,
            isOffline = !context.online,
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

/** The Shore's two states (spec §3.3). A null [streak] or [stats] isn't shown. */
sealed class ShoreUiState {
    abstract val isAdmin: Boolean
    abstract val isOffline: Boolean
    abstract val streak: ShoreStreak?
    abstract val stats: ShoreStats?

    /** State A: today's bottle is waiting, unless the sea is known to be calm. */
    data class BottleWaiting(
        override val isAdmin: Boolean = false,
        override val isOffline: Boolean = false,
        override val streak: ShoreStreak? = null,
        override val stats: ShoreStats? = null,
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
        /** A throw back waiting out its window, for the snackbar's Undo. */
        val pendingThrowBack: PendingSeaAction.ThrowBack? = null,
        override val isAdmin: Boolean = false,
        override val isOffline: Boolean = false,
        override val streak: ShoreStreak? = null,
        override val stats: ShoreStats? = null,
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
)
