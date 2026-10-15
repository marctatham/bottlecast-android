package com.bottlecast.app.feature.composepreview

import androidx.lifecycle.SavedStateHandle
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.CastOutcome
import com.bottlecast.app.common.messages.domain.Draft
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.common.messages.domain.NewCast
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/** Unit tests for [ComposePreviewViewModel]: consent, the Terms and the cast. */
class ComposePreviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saved = Draft("bottle00000000000013", "  If you're reading this, drink some water.  ")
    private val profile = MutableStateFlow<UserProfile?>(UserProfile())
    private val user = MutableStateFlow<UserEntity>(google())
    private val left = MutableStateFlow<Int?>(5)

    private val drafts = mockk<IDraftRepository>(relaxed = true) { coEvery { load() } returns saved }
    private val casts = mockk<ICastCoordinator> { every { castsLeftToday } returns left }
    private val profiles = mockk<IProfileRepository> { every { this@mockk.profile } returns this@ComposePreviewViewModelTest.profile }
    private val languages = mockk<ILanguageDetector> { coEvery { detect(any()) } returns "en" }
    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@ComposePreviewViewModelTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns MutableStateFlow(true) }

    private val settings = mockk<ISettingsStore> { every { this@mockk.settings } returns MutableStateFlow(AppSettings()) }

    private fun viewModel() = ComposePreviewViewModel(drafts, casts, profiles, languages, auth, connectivity, settings, SavedStateHandle())

    private fun ComposePreviewViewModel.ready() = state.value as ComposePreviewUiState.Ready

    @Test
    fun `with no draft, Preview goes back`() = runTest {
        // given
        coEvery { drafts.load() } returns null

        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals(ComposePreviewUiState.NoDraft, viewModel.state.value)
    }

    @Test
    fun `a first cast is sealable, unsigned, and waits for the Terms`() = runTest {
        // when
        val viewModel = viewModel()

        // then
        val ready = viewModel.ready()
        Assert.assertEquals("If you're reading this, drink some water.", ready.text)
        Assert.assertTrue(ready.sealable)
        Assert.assertFalse(ready.signWithPenName)
        Assert.assertTrue(ready.needsTerms)
        Assert.assertFalse(ready.canCast)
    }

    @Test
    fun `a returning caster casts without the Terms`() = runTest {
        // given
        profile.value = UserProfile(termsVersion = 1)

        // when
        val viewModel = viewModel()

        // then
        Assert.assertFalse(viewModel.ready().needsTerms)
        Assert.assertTrue(viewModel.ready().canCast)
    }

    @Test
    fun `a guest signs in first`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", "guest", isAdmin = false)
        val viewModel = viewModel()
        viewModel.onTermsTicked(true)

        // when
        viewModel.onCast()

        // then
        Assert.assertEquals(ComposePreviewEffect.NeedsSignIn, viewModel.effects.first())
        coVerify(exactly = 0) { casts.cast(any()) }
    }

    @Test
    fun `cast it sends the writer's choices, with the Terms on a first cast`() = runTest {
        // given
        profile.value = UserProfile(penName = "Driftwood")
        coEvery { casts.cast(any()) } returns CastOutcome.Cast(queued = false)
        val viewModel = viewModel()
        viewModel.onTermsTicked(true)
        viewModel.onSignWithPenName(true)
        viewModel.onSealable(false)

        // when
        viewModel.onCast()

        // then
        Assert.assertEquals(ComposePreviewEffect.Cast("bottle00000000000013", queued = false, sealable = false), viewModel.effects.first())
        coVerify {
            casts.cast(
                NewCast(
                    messageId = "bottle00000000000013",
                    text = "If you're reading this, drink some water.",
                    lang = "en",
                    penName = "Driftwood",
                    sealable = false,
                    acceptTerms = true,
                ),
            )
        }
    }

    @Test
    fun `a refused cast says the words are saved, and can be tried again`() = runTest {
        // given
        profile.value = UserProfile(termsVersion = 1)
        coEvery { casts.cast(any()) } returns CastOutcome.Refused
        val viewModel = viewModel()

        // when
        viewModel.onCast()

        // then
        Assert.assertEquals(ComposePreviewEffect.CastFailed, viewModel.effects.first())
        Assert.assertFalse(viewModel.ready().casting)
        Assert.assertTrue(viewModel.ready().canCast)
    }

    @Test
    fun `at the day's limit, it waits for tomorrow`() = runTest {
        // given
        profile.value = UserProfile(termsVersion = 1)
        coEvery { casts.cast(any()) } returns CastOutcome.LimitReached
        val viewModel = viewModel()

        // when
        viewModel.onCast()

        // then
        Assert.assertEquals(0, viewModel.ready().castsLeft)
        Assert.assertFalse(viewModel.ready().canCast)
    }

    private fun google() = UserEntity.AuthenticatedUser(AuthProviderType.Google, "token", "gail", isAdmin = false)
}
