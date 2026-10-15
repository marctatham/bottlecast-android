package com.bottlecast.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.IChestRepository
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
    chestRepository: IChestRepository,
) : ViewModel() {

    /** The Chest tab's dot: a bottle not kept yet, or a cast found, since the Chest was last viewed (spec §2). */
    val chestBadge: StateFlow<Boolean> = chestRepository.badge

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

    /** Casting needs Google (spec §2, flow 3): anyone else sees the sign-in sheet first. */
    fun needsSignInToCast(): Boolean {
        return authenticationRepository.user.value.googleUid == null
    }
}
