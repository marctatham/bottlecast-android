package com.bottlecast.app.feature.shore

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ShoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = MutableStateFlow<UserEntity>(UserEntity.UnauthenticatedUser)
    private val isOnline = MutableStateFlow(true)
    private val authenticationRepository = mockk<IAuthenticationRepository> { every { user } returns this@ShoreViewModelTest.user }
    private val connectivityMonitor = mockk<IConnectivityMonitor> { every { isOnline } returns this@ShoreViewModelTest.isOnline }

    @Test
    fun `today's bottle waits, with no streak or stats until they're recorded`() = runTest {
        // given
        val viewModel = ShoreViewModel(authenticationRepository, connectivityMonitor)

        // when
        val state = viewModel.state.value

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(isAdmin = false, isOffline = false, streak = null, stats = null), state)
    }

    @Test
    fun `an admin is offered moderation`() = runTest {
        // given
        val viewModel = ShoreViewModel(authenticationRepository, connectivityMonitor)

        // when
        user.value = UserEntity.AuthenticatedUser(
            providerType = AuthProviderType.Google,
            jwtToken = "token",
            userId = "admin",
            isAdmin = true,
        )

        // then
        Assert.assertTrue(viewModel.state.value.isAdmin)
    }

    @Test
    fun `a signed-in user who isn't an admin isn't offered moderation`() = runTest {
        // given
        val viewModel = ShoreViewModel(authenticationRepository, connectivityMonitor)

        // when
        user.value = UserEntity.AuthenticatedUser(
            providerType = AuthProviderType.Anonymous,
            jwtToken = "token",
            userId = "guest",
            isAdmin = false,
        )

        // then
        Assert.assertFalse(viewModel.state.value.isAdmin)
    }

    @Test
    fun `losing the connection shows the offline state, and getting it back clears it`() = runTest {
        // given
        val viewModel = ShoreViewModel(authenticationRepository, connectivityMonitor)

        // when
        isOnline.value = false
        val offline = viewModel.state.value.isOffline
        isOnline.value = true
        val online = viewModel.state.value.isOffline

        // then
        Assert.assertTrue(offline)
        Assert.assertFalse(online)
    }

    @Test
    fun `a failure still offers today's bottle`() = runTest {
        // given
        every { connectivityMonitor.isOnline } throws IllegalStateException("no network service")

        // when
        val viewModel = ShoreViewModel(authenticationRepository, connectivityMonitor)

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(), viewModel.state.value)
    }
}
