package com.bottlecast.app

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.preferences.IOnboardingStore
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/** Unit tests for [AppNavigationStateViewModel]'s start destination (spec §3.1). */
class AppNavigationStateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = mockk<IAuthenticationRepository> {
        every { user } returns MutableStateFlow(UserEntity.UnauthenticatedUser)
    }
    private val chest = mockk<IChestRepository> { every { badge } returns MutableStateFlow(false) }
    private val profiles = mockk<IProfileRepository> { every { profile } returns flowOf(null) }

    @Test
    fun `a first launch starts with onboarding`() = runTest {
        // given
        val store = mockk<IOnboardingStore> { every { seen } returns flowOf(false) }

        // when
        val viewModel = AppNavigationStateViewModel(auth, store, chest, profiles)

        // then
        Assert.assertEquals(Onboarding, viewModel.startDestination.value)
    }

    @Test
    fun `once onboarding is seen, the app starts on the Shore`() = runTest {
        // given
        val store = mockk<IOnboardingStore> { every { seen } returns flowOf(true) }

        // when
        val viewModel = AppNavigationStateViewModel(auth, store, chest, profiles)

        // then
        Assert.assertEquals(Shore, viewModel.startDestination.value)
    }

    @Test
    fun `a flag that can't be read starts on the Shore rather than onboarding again`() = runTest {
        // given
        val store = mockk<IOnboardingStore> { every { seen } returns flow { throw IllegalStateException("corrupt") } }

        // when
        val viewModel = AppNavigationStateViewModel(auth, store, chest, profiles)

        // then
        Assert.assertEquals(Shore, viewModel.startDestination.value)
    }
}
