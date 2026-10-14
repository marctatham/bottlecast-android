package com.bottlecast.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.preferences.IOnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** How onboarding ended, for analytics' `onboarding_finished`. */
enum class OnboardingExit(val analyticsValue: String) {
    Skip("skip"),
    FindFirst("find"),
    CastInstead("cast"),
}

/**
 * First-launch onboarding (spec §3.1). It never creates an account: the guest comes with the first
 * look at the sea, on the Shore.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingStore: IOnboardingStore,
    authenticationRepository: IAuthenticationRepository,
) : ViewModel() {

    private val _state: MutableStateFlow<OnboardingUiState> = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[init] Couldn't follow the account")
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            authenticationRepository.user.collect { user ->
                _state.value = OnboardingUiState(signInToCast = user.googleUid == null)
            }
        }
    }

    /** Onboarding is done, however it ended, so it never shows again. */
    fun onFinished() {
        onboardingStore.markSeen()
    }
}

data class OnboardingUiState(
    /** "I'd rather cast one" asks for Google first (spec §2, flow 1). */
    val signInToCast: Boolean = true,
)
