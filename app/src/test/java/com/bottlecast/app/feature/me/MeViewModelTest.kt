package com.bottlecast.app.feature.me

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

/** Unit tests for [MeViewModel]: who the tab is for, since when, and its numbers. */
class MeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-10-15T09:00:00Z"))
    private val user = MutableStateFlow<UserEntity>(google())
    private val profile = MutableStateFlow<UserProfile?>(UserProfile(penName = "Driftwood"))
    private val streak = MutableStateFlow<Streak?>(Streak(current = 6, best = 14, shields = 1, foundToday = true))
    private val counts = MutableStateFlow<ChestCounts?>(ChestCounts(bottled = 12, found = 30, kept = 9))

    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@MeViewModelTest.user }
    private val profiles = mockk<IProfileRepository> { every { this@mockk.profile } returns this@MeViewModelTest.profile }
    private val streaks = mockk<IStreakRepository> { every { this@mockk.streak } returns this@MeViewModelTest.streak }
    private val chest = mockk<IChestRepository>(relaxed = true) { every { this@mockk.counts } returns this@MeViewModelTest.counts }

    private fun viewModel() = MeViewModel(auth, profiles, streaks, chest, clock)

    @Test
    fun `a Google account is headed by its pen name, with when it began and all its numbers`() = runTest {
        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals(
            MeUiState.Showing(
                identity = MeIdentity.PenName("Driftwood"),
                castingSince = YearMonth.of(2026, 10),
                stats = MeStats(guest = false, bottled = 12, found = 30, kept = 9, streak = 6, best = 14, shields = 1),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `without a pen name it's an anonymous sailor`() = runTest {
        // given
        profile.value = UserProfile()

        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals(MeIdentity.Anonymous, (viewModel.state.value as MeUiState.Showing).identity)
    }

    @Test
    fun `a guest is a guest sailor, with no casting date and only what a guest can have`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false, createdAt = clock.instant())

        // when
        val viewModel = viewModel()

        // then
        val showing = viewModel.state.value as MeUiState.Showing
        Assert.assertEquals(MeIdentity.Guest, showing.identity)
        Assert.assertNull(showing.castingSince)
        Assert.assertTrue(showing.stats.guest)
    }

    @Test
    fun `numbers not known yet stay empty, and coming back asks for them again`() = runTest {
        // given
        counts.value = null
        val viewModel = viewModel()

        // when
        viewModel.onResume()

        // then
        Assert.assertEquals(MeStats(guest = false, streak = 6, best = 14, shields = 1), (viewModel.state.value as MeUiState.Showing).stats)
        verify { chest.refreshCounts() }
    }

    private fun google() = UserEntity.AuthenticatedUser(
        AuthProviderType.Google,
        "jwt",
        "gail",
        isAdmin = false,
        createdAt = Instant.parse("2026-10-02T12:00:00Z"),
        googleName = "Gail Storm",
    )
}
