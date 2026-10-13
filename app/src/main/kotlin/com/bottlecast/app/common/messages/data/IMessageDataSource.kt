package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.ModerationAction

interface IMessageDataSource {

    suspend fun postMessage(message: String, userId: String): MessageEntity

    suspend fun getReportedMessages(): List<MessageEntity>

    suspend fun moderate(messageId: String, action: ModerationAction)
}
