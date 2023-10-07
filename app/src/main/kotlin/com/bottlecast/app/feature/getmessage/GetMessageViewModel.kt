package com.bottlecast.app.feature.getmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GetMessageViewModel @Inject constructor(
    private val messageRepository: IMessageRepository
) : ViewModel() {

    private val _state: MutableStateFlow<MessageUiState> = MutableStateFlow(MessageUiState.PlayingAnimation)
    val state: StateFlow<MessageUiState> = _state.asStateFlow()

    init {
        getNewMessage()
    }

    private fun getNewMessage() {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception, "There was a problem retrieving the message")
            _state.value = MessageUiState.Failure
        }

        viewModelScope.launch(exceptionHandler) {
            _state.value = MessageUiState.PlayingAnimation

            val messageEntity = messageRepository.getMessage()

            _state.value = MessageUiState.MessageReceived(messageEntity)
        }
    }
}

sealed class MessageUiState {

    data object PlayingAnimation : MessageUiState()

    data class MessageReceived(val messageEntity: MessageEntity) : MessageUiState()

    data object Failure : MessageUiState()
}
