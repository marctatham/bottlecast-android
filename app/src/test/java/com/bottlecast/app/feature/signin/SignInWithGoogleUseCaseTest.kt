package com.bottlecast.app.feature.signin

import com.bottlecast.app.common.authentication.domain.GoogleLinkResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.sea.domain.Handover
import com.bottlecast.app.common.sea.domain.HandoverResult
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.feature.signin.domain.SignInOutcome
import com.bottlecast.app.feature.signin.domain.SignInWithGoogleUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/** Unit tests for [SignInWithGoogleUseCase]: linking a guest, or switching and bringing today's find. */
class SignInWithGoogleUseCaseTest {

    private val authentication = mockk<IAuthenticationRepository> {
        coEvery { switchToGoogle(any()) } returns GOOGLE_UID
    }
    private val sea = mockk<ISeaRepository>()
    private val seaActions = mockk<ISeaActions> { coEvery { settle() } just Runs }
    private val streak = mockk<IStreakRepository> {
        coEvery { carryable() } returns 0
        coEvery { carry(any(), any()) } just Runs
    }
    private val useCase = SignInWithGoogleUseCase(authentication, sea, seaActions, streak)

    private val handover = Handover("today", "guest", "token")

    @Test
    fun `a guest that links keeps everything, with no handover`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.Linked

        // when
        val outcome = useCase("idToken")

        // then
        Assert.assertEquals(SignInOutcome.Linked, outcome)
        coVerify(exactly = 0) { sea.prepareHandover() }
        coVerify(exactly = 0) { authentication.switchToGoogle(any()) }
        coVerify(exactly = 0) { streak.carry(any(), any()) }
    }

    @Test
    fun `nobody signed in simply signs in`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.SignedIn

        // when
        val outcome = useCase("idToken")

        // then
        Assert.assertEquals(SignInOutcome.SignedIn, outcome)
        coVerify(exactly = 0) { sea.prepareHandover() }
    }

    @Test
    fun `an existing account settles, arms, switches, then claims, in that order`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.AccountExists
        coEvery { sea.prepareHandover() } returns handover
        coEvery { sea.claimHandover(handover, GOOGLE_UID) } returns HandoverResult.HandedOver

        // when
        val outcome = useCase("idToken")

        // then
        Assert.assertEquals(SignInOutcome.HandedOver, outcome)
        coVerifyOrder {
            seaActions.settle()
            sea.prepareHandover()
            authentication.switchToGoogle("idToken")
            sea.claimHandover(handover, GOOGLE_UID)
        }
    }

    @Test
    fun `the guest's streak is noted before the switch, and carried onto the account after it`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.AccountExists
        coEvery { sea.prepareHandover() } returns null
        coEvery { streak.carryable() } returns 3

        // when
        useCase("idToken")

        // then
        coVerifyOrder {
            streak.carryable()
            authentication.switchToGoogle("idToken")
            streak.carry(GOOGLE_UID, 3)
        }
    }

    @Test
    fun `an existing account with no find to bring just switches`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.AccountExists
        coEvery { sea.prepareHandover() } returns null

        // when
        val outcome = useCase("idToken")

        // then
        Assert.assertEquals(SignInOutcome.Switched, outcome)
        coVerify { authentication.switchToGoogle("idToken") }
        coVerify(exactly = 0) { sea.claimHandover(any(), any()) }
    }

    @Test
    fun `an account that keeps its own find today switches without the guest's`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.AccountExists
        coEvery { sea.prepareHandover() } returns handover
        coEvery { sea.claimHandover(handover, GOOGLE_UID) } returns HandoverResult.KeptOwnFind

        // when
        val outcome = useCase("idToken")

        // then
        Assert.assertEquals(SignInOutcome.Switched, outcome)
    }

    @Test
    fun `a switch that fails fails the sign-in, and claims nothing`() = runTest {
        // given
        coEvery { authentication.linkGoogle("idToken") } returns GoogleLinkResult.AccountExists
        coEvery { sea.prepareHandover() } returns handover
        coEvery { authentication.switchToGoogle("idToken") } throws IllegalStateException("network")

        // when
        val failure = runCatching { useCase("idToken") }.exceptionOrNull()

        // then
        Assert.assertTrue(failure is IllegalStateException)
        coVerify(exactly = 0) { sea.claimHandover(any(), any()) }
    }
}

private const val GOOGLE_UID = "gail"
