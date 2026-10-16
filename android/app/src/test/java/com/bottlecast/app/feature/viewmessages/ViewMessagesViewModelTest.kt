package com.bottlecast.app.feature.viewmessages

import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.ModerationAction
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ViewMessagesViewModel]: the admin's list updates in place after each action.
 */
class ViewMessagesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reported = MessageEntity(id = "reported", message = "Message reported", userId = "writer", reportCount = 2)
    private val other = MessageEntity(id = "other", message = "Message other", userId = "writer", reportCount = 1)

    private val repository = mockk<IMessageRepository>(relaxed = true) {
        coEvery { getReportedMessages() } returns listOf(reported, other)
    }

    @Test
    fun `pulling or removing a bottle updates it in place, without reading the list again`() = runTest {
        // given
        val viewModel = ViewMessagesViewModel(repository)

        // when
        viewModel.moderate("reported", ModerationAction.Pull)
        val pulled = viewModel.state.value
        viewModel.moderate("other", ModerationAction.Remove)

        // then
        Assert.assertEquals(
            MessagesState.MessagesReceived(listOf(reported.copy(status = MessageStatus.UnderReview), other)),
            pulled,
        )
        Assert.assertEquals(
            MessagesState.MessagesReceived(
                listOf(reported.copy(status = MessageStatus.UnderReview), other.copy(status = MessageStatus.Removed)),
            ),
            viewModel.state.value,
        )
        coVerify(exactly = 1) { repository.getReportedMessages() }
    }

    @Test
    fun `a restored bottle leaves the list`() = runTest {
        // given
        val viewModel = ViewMessagesViewModel(repository)

        // when
        viewModel.moderate("reported", ModerationAction.Restore)

        // then
        Assert.assertEquals(MessagesState.MessagesReceived(listOf(other)), viewModel.state.value)
    }

    @Test
    fun `an action that doesn't go through leaves the list as it was, and says so`() = runTest {
        // given
        coEvery { repository.moderate("reported", ModerationAction.Remove) } throws IllegalStateException("offline")
        val viewModel = ViewMessagesViewModel(repository)

        // when
        viewModel.moderate("reported", ModerationAction.Remove)

        // then
        Assert.assertEquals(MessagesState.MessagesReceived(listOf(reported, other), actionFailed = true), viewModel.state.value)
    }
}
