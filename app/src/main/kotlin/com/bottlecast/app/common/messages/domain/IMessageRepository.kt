package com.bottlecast.app.common.messages.domain

/** Moderation's side of the messages. Casting goes through [ICastCoordinator]. */
interface IMessageRepository {

    /** The bottles a moderator should look at: reported, or pulled, most reported first. */
    suspend fun getReportedMessages(): List<MessageEntity>

    suspend fun moderate(messageId: String, action: ModerationAction)
}
