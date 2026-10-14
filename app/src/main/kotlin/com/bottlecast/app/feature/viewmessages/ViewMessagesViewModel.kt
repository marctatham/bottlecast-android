package com.bottlecast.app.feature.viewmessages


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.ModerationAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** The admin's Moderation list: reported and pulled bottles, to pull, remove or restore. */
@HiltViewModel
class ViewMessagesViewModel @Inject constructor(
    private val messageRepository: IMessageRepository
) : ViewModel() {

    private val _state: MutableStateFlow<MessagesState> = MutableStateFlow(MessagesState.Loading)
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    init {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception, "There was a problem fetching the messages to moderate")
            _state.value = MessagesState.Failure
        }

        viewModelScope.launch(exceptionHandler) {
            _state.value = MessagesState.MessagesReceived(messageRepository.getReportedMessages())
        }
    }

    fun moderate(messageId: String, action: ModerationAction) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception, "There was a problem moderating a message")
            _state.update { (it as? MessagesState.MessagesReceived)?.copy(actionFailed = true) ?: it }
        }

        viewModelScope.launch(exceptionHandler) {
            messageRepository.moderate(messageId, action)
            // what changed is known, so the list is updated in place rather than read again, and a
            // failure always means the bottle didn't change
            _state.update { state ->
                (state as? MessagesState.MessagesReceived)?.let { received ->
                    received.copy(
                        messageEntities = received.messageEntities.mapNotNull { if (it.id == messageId) it.moderated(action) else it },
                        actionFailed = false,
                    )
                } ?: state
            }
        }
    }
}

// A bottle as moderation leaves it, or null once it's no longer one to look at.
private fun MessageEntity.moderated(action: ModerationAction): MessageEntity? = when (action) {
    ModerationAction.Pull -> copy(status = MessageStatus.UnderReview)
    ModerationAction.Remove -> copy(status = MessageStatus.Removed)
    ModerationAction.Restore -> null
}

sealed class MessagesState {

    data object Loading : MessagesState()

    data class MessagesReceived(
        val messageEntities: List<MessageEntity>,
        val actionFailed: Boolean = false,
    ) : MessagesState()

    data object Failure : MessagesState()

}
