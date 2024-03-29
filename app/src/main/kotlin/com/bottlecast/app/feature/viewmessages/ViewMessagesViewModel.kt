package com.bottlecast.app.feature.viewmessages


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
class ViewMessagesViewModel @Inject constructor(
    private val messageRepository: IMessageRepository
) : ViewModel() {

    private val _state: MutableStateFlow<MessagesState> = MutableStateFlow(MessagesState.Loading)
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    init {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception, "There was a problem fetching all messages")
            _state.value = MessagesState.Failure
        }

        viewModelScope.launch(exceptionHandler) {
            _state.value = MessagesState.Loading

            val messages = messageRepository.getMessages()

            _state.value = MessagesState.MessagesReceived(messages)
        }
    }

    fun purgeMessages() = viewModelScope.launch {
        Timber.d("TODO: purge all the messages")
    }
}

sealed class MessagesState {

    data object Loading : MessagesState()

    data class MessagesReceived(val messageEntities: List<MessageEntity>) : MessagesState()

    data object Failure : MessagesState()

}
