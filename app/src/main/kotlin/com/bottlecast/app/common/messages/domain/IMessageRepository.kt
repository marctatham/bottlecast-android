package com.bottlecast.app.common.messages.domain

interface IMessageRepository {

    /** Casts a bottle into the sea, and returns it. */
    suspend fun postMessage(message: String, userId: String): MessageEntity

    /** The bottles a moderator should look at: reported, or pulled, most reported first. */
    suspend fun getReportedMessages(): List<MessageEntity>

    suspend fun moderate(messageId: String, action: ModerationAction)
}
