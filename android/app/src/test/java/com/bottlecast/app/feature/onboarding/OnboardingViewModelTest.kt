package com.bottlecast.app.feature.onboarding

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.preferences.IOnboardingStore
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/** Unit tests for [OnboardingViewModel]. */
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = MutableStateFlow<UserEntity>(UserEntity.UnauthenticatedUser)
    private val store = mockk<IOnboardingStore> { every { markSeen() } returns Job() }
    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@OnboardingViewModelTest.user }

    @Test
    fun `casting asks for Google unless the account has it`() = runTest {
        // given
        val viewModel = OnboardingViewModel(store, auth)
        val signedOut = viewModel.state.value.signInToCast

        // when
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Google, "token", "gail", isAdmin = false)

        // then
        Assert.assertTrue(signedOut)
        Assert.assertFalse(viewModel.state.value.signInToCast)
    }

    @Test
    fun `finishing marks onboarding seen`() = runTest {
        // given
        val viewModel = OnboardingViewModel(store, auth)

        // when
        viewModel.onFinished()

        // then
        verify { store.markSeen() }
    }
}
