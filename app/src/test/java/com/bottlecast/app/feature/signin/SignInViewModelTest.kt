package com.bottlecast.app.feature.signin

import android.content.Context
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.feature.signin.domain.SignInOutcome
import com.bottlecast.app.feature.signin.domain.SignInWithGoogleUseCase
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Unit tests for [SignInViewModel]: the sheet's states, and the chip for a guest holding today's find. */
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-10-14T09:00:00Z"))
    private val user = MutableStateFlow<UserEntity>(guest())
    private val online = MutableStateFlow(true)
    private val today = MutableStateFlow<TodayFind?>(null)
    private val context = mockk<Context>()

    private val credentials = mockk<IGoogleCredentials>()
    private val signIn = mockk<SignInWithGoogleUseCase>()
    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@SignInViewModelTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }
    private val sea = mockk<ISeaRepository> { every { observeToday(any()) } returns today }

    private fun viewModel() = SignInViewModel(credentials, signIn, auth, connectivity, sea, clock)

    @Test
    fun `a sign-in goes from loading to signed in, with how it went`() = runTest {
        // given
        val picked = CompletableDeferred<GoogleCredentialResult>()
        coEvery { credentials.requestIdToken(context) } coAnswers { picked.await() }
        coEvery { signIn("idToken") } returns SignInOutcome.Linked
        val viewModel = viewModel()

        // when
        viewModel.onContinue(context)
        val whilePicking = viewModel.state.value.phase
        picked.complete(GoogleCredentialResult.Token("idToken"))

        // then
        Assert.assertEquals(SignInPhase.Loading, whilePicking)
        Assert.assertEquals(SignInPhase.SignedIn(SignInOutcome.Linked), viewModel.state.value.phase)
    }

    @Test
    fun `closing the picker leaves the sheet as it was, with no error`() = runTest {
        // given
        coEvery { credentials.requestIdToken(context) } returns GoogleCredentialResult.Cancelled
        val viewModel = viewModel()

        // when
        viewModel.onContinue(context)

        // then
        Assert.assertEquals(SignInPhase.Idle, viewModel.state.value.phase)
        coVerify(exactly = 0) { signIn(any()) }
    }

    @Test
    fun `no Google account on the phone, or a failure, says so`() = runTest {
        // given
        coEvery { credentials.requestIdToken(context) } returns GoogleCredentialResult.Unavailable
        val viewModel = viewModel()

        // when
        viewModel.onContinue(context)
        val unavailable = viewModel.state.value.phase
        coEvery { credentials.requestIdToken(context) } returns GoogleCredentialResult.Token("idToken")
        coEvery { signIn("idToken") } throws IllegalStateException("network")
        viewModel.onContinue(context)

        // then
        Assert.assertEquals(SignInPhase.Unavailable, unavailable)
        Assert.assertEquals(SignInPhase.Failed, viewModel.state.value.phase)
    }

    @Test
    fun `offline, the button waits`() = runTest {
        // given
        online.value = false
        val viewModel = viewModel()

        // when
        viewModel.onContinue(context)

        // then
        Assert.assertTrue(viewModel.state.value.isOffline)
        Assert.assertEquals(SignInPhase.Idle, viewModel.state.value.phase)
        coVerify(exactly = 0) { credentials.requestIdToken(any()) }
    }

    @Test
    fun `a guest holding today's find is told it comes along`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        today.value = TodayFind(LocalDate.now(clock), "today", held(), FindStatus.Held)
        val holding = viewModel.state.value.carriesTodaysFind
        today.value = TodayFind(LocalDate.now(clock), "today", held(), FindStatus.ThrownBack)
        val thrownBack = viewModel.state.value.carriesTodaysFind

        // then
        Assert.assertTrue(holding)
        Assert.assertFalse(thrownBack)
    }

    @Test
    fun `opening the sheet again starts afresh`() = runTest {
        // given
        coEvery { credentials.requestIdToken(context) } returns GoogleCredentialResult.Failed
        val viewModel = viewModel()
        viewModel.onContinue(context)

        // when
        viewModel.reset()

        // then
        Assert.assertEquals(SignInPhase.Idle, viewModel.state.value.phase)
    }

    private fun guest() = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", "guest", isAdmin = false)

    private fun held() = MessageEntity(
        id = "today", message = "Hello", userId = "writer",
        status = MessageStatus.Found, foundAt = clock.instant().minus(Duration.ofHours(1)),
    )
}
