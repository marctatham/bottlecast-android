package com.bottlecast.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.preferences.IOnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppNavigationStateViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    onboardingStore: IOnboardingStore,
) : ViewModel() {

    /**
     * Where the app starts: onboarding until it's been seen (spec §3.1), then the Shore. Null until
     * the flag is read, which the first frame waits for. It's read once, since a NavHost whose start
     * changes builds a new graph.
     */
    val startDestination: StateFlow<BottlingRoute?> = flow {
        val seen = runCatching { onboardingStore.seen.first() }
            .onFailure { Timber.w(it, "[startDestination] Couldn't read whether onboarding was seen") }
            .getOrDefault(true)
        emit(if (seen) Shore else Onboarding)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * The destination the last screen view was sent for. It outlives a rotation, so the restored
     * screen isn't counted twice, but not the process: after the system kills the app, the screen
     * it comes back to starts a new session and is counted again.
     */
    var lastTrackedDestinationId: Int? = null

    // Guests have an account too (from today's first prefetch), so only Google skips the sign-in
    // sheet, where a guest can still choose to carry on as one until slice 3.
    fun onNavigateToPostMessage(): BottlingRoute {
        val user = authenticationRepository.user.value
        return if (user is UserEntity.AuthenticatedUser && user.providerType == AuthProviderType.Google) {
            PostMessage
        } else {
            PostMessageLoginRequired
        }
    }
}
