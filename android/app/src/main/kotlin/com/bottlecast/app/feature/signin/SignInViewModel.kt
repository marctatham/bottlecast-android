package com.bottlecast.app.feature.signin

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.R
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.isHoldLive
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.feature.signin.domain.SignInOutcome
import com.bottlecast.app.feature.signin.domain.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Why the sheet is asking, which titles it (spec §3.2). Gifts and the rest add theirs later. */
enum class SignInReason(@param:StringRes val title: Int) {
    Cast(R.string.sign_in_title_cast),
    Keep(R.string.sign_in_title_keep),

    /** A guest's Cast tab in the Chest, which lists only a Google account's bottles. */
    Track(R.string.sign_in_title_track),

    /** A guest's [Continue with Google] on the Me tab or in Settings › Account. */
    Account(R.string.sign_in_title_account),
}

/**
 * The lazy Google sign-in sheet (spec §3.2). It lives as long as the screen that hosts the sheet, so
 * [reset] starts each opening afresh.
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val credentials: IGoogleCredentials,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    authenticationRepository: IAuthenticationRepository,
    connectivityMonitor: IConnectivityMonitor,
    seaRepository: ISeaRepository,
    streakRepository: IStreakRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<SignInUiState> = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[signIn] Couldn't sign in with Google")
        setPhase(SignInPhase.Failed)
    }

    init {
        // A guest holding today's find brings it along (the handover, if linking can't), and its
        // streak too (carried over, if linking can't).
        @OptIn(ExperimentalCoroutinesApi::class)
        val guestCarry = authenticationRepository.user.flatMapLatest { user ->
            val guest = (user as? UserEntity.AuthenticatedUser)?.providerType == AuthProviderType.Anonymous
            if (guest) {
                combine(seaRepository.observeToday(LocalDate.now(clock)), streakRepository.streak) { today, streak ->
                    today.isLiveHold() to (streak?.current ?: 0)
                }
            } else {
                flowOf(false to 0)
            }
        }
        // only the chip and the offline caption hang on this, so a failure here isn't a failed sign-in
        val observeHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.w(throwable, "[init] Stopped following the connection and today's find")
        }
        viewModelScope.launch(observeHandler) {
            combine(connectivityMonitor.isOnline, guestCarry) { online, carry -> !online to carry }
                .collect { (offline, carry) ->
                    _state.update { it.copy(isOffline = offline, carriesTodaysFind = carry.first, carriedStreak = carry.second) }
                }
        }
    }

    /** A new opening of the sheet, with nothing left over from the last one. */
    fun reset() {
        if (_state.value.phase != SignInPhase.Loading) setPhase(SignInPhase.Idle)
    }

    /** "Continue with Google": Google's account picker over [context], an activity, then the sign-in. */
    fun onContinue(context: Context) {
        if (_state.value.phase == SignInPhase.Loading || _state.value.isOffline) return
        setPhase(SignInPhase.Loading)
        viewModelScope.launch(exceptionHandler) {
            val phase = when (val result = credentials.requestIdToken(context)) {
                is GoogleCredentialResult.Token -> SignInPhase.SignedIn(signInWithGoogle(result.idToken))
                // closing the picker leaves the sheet as it was (spec §3.2)
                GoogleCredentialResult.Cancelled -> SignInPhase.Idle
                GoogleCredentialResult.Unavailable -> SignInPhase.Unavailable
                GoogleCredentialResult.Failed -> SignInPhase.Failed
            }
            setPhase(phase)
        }
    }

    private fun setPhase(phase: SignInPhase) = _state.update { it.copy(phase = phase) }

    private fun TodayFind?.isLiveHold(): Boolean =
        this != null && status == FindStatus.Held && message?.isHoldLive(clock.instant()) == true
}

data class SignInUiState(
    val phase: SignInPhase = SignInPhase.Idle,
    /** Offline, the Google button waits (spec §3.2). */
    val isOffline: Boolean = false,
    /** "Today's find comes with you": a guest holding today's find. */
    val carriesTodaysFind: Boolean = false,
    /** The streak a guest brings along, counting today's find, or 0. */
    val carriedStreak: Int = 0,
)

sealed interface SignInPhase {
    data object Idle : SignInPhase

    /** The picker or the sign-in is under way; the sheet can't be closed. */
    data object Loading : SignInPhase

    data object Failed : SignInPhase

    /** No Google account or provider on this phone. */
    data object Unavailable : SignInPhase

    data class SignedIn(val outcome: SignInOutcome) : SignInPhase
}
