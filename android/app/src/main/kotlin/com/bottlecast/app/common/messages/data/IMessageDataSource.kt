package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.ModerationAction

interface IMessageDataSource {

    suspend fun getReportedMessages(): List<MessageEntity>

    suspend fun moderate(messageId: String, action: ModerationAction)
}
