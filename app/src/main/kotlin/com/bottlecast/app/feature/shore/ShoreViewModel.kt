package com.bottlecast.app.feature.shore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.feature.shore.domain.Tide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ShoreViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
) : ViewModel() {

    private val _state: MutableStateFlow<ShoreUiState> = MutableStateFlow(ShoreUiState.BottleWaiting())
    val state: StateFlow<ShoreUiState> = _state.asStateFlow()

    // the Shore has no failure screen: if its inputs fail, it still offers today's bottle
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Couldn't follow the Shore's state")
        _state.value = ShoreUiState.BottleWaiting()
    }

    init {
        // finds, streaks and stats aren't recorded until slices 2 and 4, so until then the Shore
        // always shows today's bottle waiting, without a streak pill or stats
        viewModelScope.launch(exceptionHandler) {
            combine(authenticationRepository.user, connectivityMonitor.isOnline) { user, isOnline ->
                ShoreUiState.BottleWaiting(
                    isAdmin = user is UserEntity.AuthenticatedUser && user.isAdmin,
                    isOffline = !isOnline,
                )
            }.collect { _state.value = it }
        }
    }
}

/** The Shore's two states (spec §3.3). A null [streak] or [stats] isn't shown. */
sealed class ShoreUiState {
    abstract val isAdmin: Boolean
    abstract val isOffline: Boolean
    abstract val streak: ShoreStreak?
    abstract val stats: ShoreStats?

    /** State A: today's bottle is waiting. */
    data class BottleWaiting(
        override val isAdmin: Boolean = false,
        override val isOffline: Boolean = false,
        override val streak: ShoreStreak? = null,
        override val stats: ShoreStats? = null,
    ) : ShoreUiState()

    /** State B: today's bottle is found, and the next tide is counting down. */
    data class FoundToday(
        val tide: Tide,
        val todaysFind: TodaysFind,
        /** The daily reminder's time, shown only while notifications are off. */
        val reminderTime: LocalTime?,
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

/** Today's find, as the card straddling the arc shows it. */
data class TodaysFind(
    val excerpt: String,
    val isKept: Boolean,
    /** Until an unkept find drifts back to the sea (G3). */
    val daysLeft: Int,
)
