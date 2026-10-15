package com.bottlecast.app.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/**
 * The Me tab (spec §3.12): the pen name and its initial, when the account began casting, and its
 * numbers. A guest sees only what it can have, its finds and its streak.
 */
@HiltViewModel
class MeViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    profileRepository: IProfileRepository,
    streakRepository: IStreakRepository,
    private val chestRepository: IChestRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<MeUiState> = MutableStateFlow(MeUiState.Loading)
    val state: StateFlow<MeUiState> = _state.asStateFlow()

    // the numbers fail quietly: the tab still shows who's here, with nothing known of their numbers
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[observe] Couldn't follow the Me tab's numbers")
        _state.value = (_state.value as? MeUiState.Showing)?.let { it.copy(stats = MeStats(guest = it.stats.guest)) }
            ?: stateFor(authenticationRepository.user.value, profile = null, streak = null, counts = null)
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            @OptIn(ExperimentalCoroutinesApi::class)
            val profile = authenticationRepository.user.flatMapLatest { user ->
                if (user.googleUid == null) flowOf(user to null) else profileRepository.profile.map { user to it }
            }
            combine(profile, streakRepository.streak, chestRepository.counts) { (user, profile), streak, counts ->
                stateFor(user, profile, streak, counts)
            }.collect { _state.value = it }
        }
    }

    /** The tab is shown again: its numbers may have moved on meanwhile. */
    fun onResume() = chestRepository.refreshCounts()

    private fun stateFor(user: UserEntity, profile: UserProfile?, streak: Streak?, counts: ChestCounts?): MeUiState {
        val google = user.googleUid != null
        val identity = when {
            !google -> MeIdentity.Guest
            profile?.penName != null -> MeIdentity.PenName(profile.penName)
            else -> MeIdentity.Anonymous
        }
        val createdAt = (user as? UserEntity.AuthenticatedUser)?.createdAt
        return MeUiState.Showing(
            identity = identity,
            castingSince = createdAt?.takeIf { google }?.let { YearMonth.from(it.atZone(clock.zone)) },
            stats = MeStats(
                guest = !google,
                bottled = counts?.bottled,
                found = counts?.found,
                kept = counts?.kept,
                streak = streak?.current,
                best = streak?.best,
                shields = streak?.shields,
            ),
        )
    }
}

sealed class MeUiState {
    data object Loading : MeUiState()

    data class Showing(
        val identity: MeIdentity,
        /** The month the Google account was made, "Casting since Oct 2026"; null for guests. */
        val castingSince: YearMonth?,
        val stats: MeStats,
    ) : MeUiState()
}

/** Whose tab it is: the heading is the pen name, never the Google name (decided 2026-10-15). */
sealed interface MeIdentity {
    /** "Guest sailor", with a nudge to sign in. */
    data object Guest : MeIdentity

    /** A Google account without a pen name: "Anonymous sailor", with [Set a pen name]. */
    data object Anonymous : MeIdentity

    data class PenName(val name: String) : MeIdentity
}

/** The stats card's numbers, each null while it isn't known, such as offline. A guest has no casts or keeps. */
data class MeStats(
    val guest: Boolean,
    val bottled: Int? = null,
    val found: Int? = null,
    val kept: Int? = null,
    val streak: Int? = null,
    val best: Int? = null,
    val shields: Int? = null,
)
