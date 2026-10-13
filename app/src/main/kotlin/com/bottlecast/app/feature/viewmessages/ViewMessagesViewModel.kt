package com.bottlecast.app.feature.viewmessages


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
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
            _state.value = MessagesState.MessagesReceived(messageRepository.getReportedMessages())
        }
    }
}

sealed class MessagesState {

    data object Loading : MessagesState()

    data class MessagesReceived(
        val messageEntities: List<MessageEntity>,
        val actionFailed: Boolean = false,
    ) : MessagesState()

    data object Failure : MessagesState()

}
