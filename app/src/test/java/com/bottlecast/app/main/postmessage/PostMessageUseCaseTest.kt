package com.bottlecast.app.main.postmessage

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.feature.postmessage.domain.PostMessageResult
import com.bottlecast.app.feature.postmessage.domain.PostMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [PostMessageUseCase].
 */
class PostMessageUseCaseTest {

    @Test
    fun `when user has not authenticated, they should not be able to post`() = runTest {
        // given
        val messageRepo = mockk<IMessageRepository>()
        val authRepo = mockk<IAuthenticationRepository>()
        val useCase = PostMessageUseCase(authRepo, messageRepo)
        every { authRepo.user.value } returns UserEntity.UnauthenticatedUser

        // when
        val result: PostMessageResult = useCase.postMessage("This should not be allowed")

        // then
        Assert.assertEquals(PostMessageResult.Unauthenticated, result)
    }


    @Test
    fun `when user has authenticated, they are able to post anything they want`() = runTest {
        // given
        val messageRepo = mockk<IMessageRepository>()
        val authRepo = mockk<IAuthenticationRepository>()
        val useCase = PostMessageUseCase(authRepo, messageRepo)
        val authenticatedUser =
            UserEntity.AuthenticatedUser(
                providerType = AuthProviderType.Google,
                jwtToken = "myFakeToken",
                userId = "myFakeUserId",
                isAdmin = false
            )
        every { authRepo.user.value } returns authenticatedUser
        val posted = MessageEntity(id = "new", message = "I should be allowed to post my message", userId = "myFakeUserId")
        coEvery { messageRepo.postMessage(any(), any()) } returns posted

        // when
        val result: PostMessageResult =
            useCase.postMessage("  I should be allowed to post my message  ")

        // then
        coVerify(exactly = 1) { messageRepo.postMessage("I should be allowed to post my message", "myFakeUserId") }
        Assert.assertEquals(PostMessageResult.Success(posted), result)
    }
}