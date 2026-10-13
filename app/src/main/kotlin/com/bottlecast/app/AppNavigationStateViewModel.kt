package com.bottlecast.app

import androidx.lifecycle.ViewModel
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationStateViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
) : ViewModel() {

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
