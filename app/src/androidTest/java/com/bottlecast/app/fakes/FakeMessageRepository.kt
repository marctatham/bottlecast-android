package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the Firestore-backed repository, seeded with a single message.
 */
@Singleton
class FakeMessageRepository @Inject constructor() : IMessageRepository {

    private val messages: MutableList<MessageEntity> = mutableListOf(
        MessageEntity(message = "Ahoy! This bottle washed up from the test sea.", userId = "fakeUserId")
    )

    var getMessageCalls: Int = 0
        private set

    override suspend fun getMessage(): MessageEntity {
        getMessageCalls++
        return messages.first()
    }

    override suspend fun getMessages(): List<MessageEntity> = messages.toList()

    override suspend fun postMessage(message: MessageEntity) {
        messages += message
    }
}
