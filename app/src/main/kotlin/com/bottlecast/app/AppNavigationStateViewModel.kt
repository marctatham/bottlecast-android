package com.bottlecast.app

import androidx.lifecycle.ViewModel
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

    fun onNavigateToPostMessage(): BottlingRoute {
        return if (authenticationRepository.user.value is UserEntity.AuthenticatedUser) {
            PostMessage
        } else {
            PostMessageLoginRequired
        }
    }
}
