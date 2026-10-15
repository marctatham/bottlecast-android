package com.bottlecast.app.common.account

import android.content.Context
import com.bottlecast.app.common.account.domain.DeleteAccountResult
import com.bottlecast.app.common.account.domain.DeleteAccountUseCase
import com.bottlecast.app.common.account.domain.IAccountEraser
import com.bottlecast.app.common.account.domain.IAccountLocalData
import com.bottlecast.app.common.account.domain.SignOutUseCase
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.sea.domain.ISeaActions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/** Unit tests for [SignOutUseCase] and [DeleteAccountUseCase]. */
class AccountUseCasesTest {

    private val context = mockk<Context>()
    private val user = MutableStateFlow<UserEntity>(UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "gail", isAdmin = false))
    private val online = MutableStateFlow(true)
    private val auth = mockk<IAuthenticationRepository>(relaxed = true) {
        every { this@mockk.user } returns this@AccountUseCasesTest.user
        coEvery { reauthenticateWithGoogle(any()) } returns true
    }
    private val credentials = mockk<IGoogleCredentials>(relaxed = true) {
        coEvery { requestIdToken(any()) } returns GoogleCredentialResult.Token("token")
    }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }
    private val seaActions = mockk<ISeaActions>(relaxed = true)
    private val eraser = mockk<IAccountEraser>(relaxed = true)
    private val localData = mockk<IAccountLocalData>(relaxed = true)

    private val deleteAccount = DeleteAccountUseCase(auth, credentials, connectivity, seaActions, eraser, localData)

    @Test
    fun `signing out writes what's waiting first, then forgets the account`() = runTest {
        // given
        val signOut = SignOutUseCase(auth, seaActions, credentials)

        // when
        signOut()

        // then
        coVerifyOrder {
            seaActions.settle()
            auth.signOut()
            credentials.forgetAccount()
        }
    }

    @Test
    fun `deleting signs in again, writes what's waiting, erases the records, then the account, then what the phone kept`() = runTest {
        // when
        val result = deleteAccount(context)

        // then
        Assert.assertEquals(DeleteAccountResult.Deleted, result)
        coVerifyOrder {
            credentials.requestIdToken(context)
            auth.reauthenticateWithGoogle("token")
            seaActions.settle()
            eraser.erase("gail")
            auth.deleteUser()
            localData.clear()
            credentials.forgetAccount()
        }
    }

    @Test
    fun `offline, nothing is asked or changed`() = runTest {
        // given
        online.value = false

        // when
        val result = deleteAccount(context)

        // then
        Assert.assertEquals(DeleteAccountResult.Offline, result)
        coVerify(exactly = 0) { credentials.requestIdToken(any()) }
        coVerify(exactly = 0) { eraser.erase(any()) }
    }

    @Test
    fun `a closed picker or another account changes nothing`() = runTest {
        // given
        coEvery { credentials.requestIdToken(any()) } returns GoogleCredentialResult.Cancelled
        val cancelled = deleteAccount(context)
        coEvery { credentials.requestIdToken(any()) } returns GoogleCredentialResult.Token("other")
        coEvery { auth.reauthenticateWithGoogle("other") } returns false

        // when
        val wrong = deleteAccount(context)

        // then
        Assert.assertEquals(DeleteAccountResult.Cancelled, cancelled)
        Assert.assertEquals(DeleteAccountResult.WrongAccount, wrong)
        coVerify(exactly = 0) { eraser.erase(any()) }
        coVerify(exactly = 0) { auth.deleteUser() }
    }

    @Test
    fun `when the erasing stops part way, the account stays, to try again`() = runTest {
        // given
        coEvery { eraser.erase(any()) } throws IllegalStateException("unreachable")

        // when
        val result = deleteAccount(context)

        // then
        Assert.assertEquals(DeleteAccountResult.Failed, result)
        coVerify(exactly = 0) { auth.deleteUser() }
        coVerify(exactly = 0) { localData.clear() }
    }

    @Test
    fun `a guest has no account to delete`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)

        // when
        val result = deleteAccount(context)

        // then
        Assert.assertEquals(DeleteAccountResult.Failed, result)
        coVerify(exactly = 0) { credentials.requestIdToken(any()) }
    }
}
