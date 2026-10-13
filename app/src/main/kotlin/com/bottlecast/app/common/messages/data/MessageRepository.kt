package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.ModerationAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

class MessageRepository(
    private val messageDataSource: IMessageDataSource,
    private val dispatcherNetwork: CoroutineDispatcher
) : IMessageRepository {

    override suspend fun postMessage(message: String, userId: String): MessageEntity {
        Timber.i("[postMessage] Posting message to datasource")
        return withContext(dispatcherNetwork) {
            messageDataSource.postMessage(message, userId).also {
                Timber.i("[postMessage] Message posted successfully")
            }
        }
    }

    override suspend fun getReportedMessages(): List<MessageEntity> {
        Timber.v("[getReportedMessages] Retrieving the messages to moderate")
        return withContext(dispatcherNetwork) {
            messageDataSource.getReportedMessages()
        }
    }

    override suspend fun moderate(messageId: String, action: ModerationAction) {
        withContext(dispatcherNetwork) {
            messageDataSource.moderate(messageId, action)
        }
    }
}
