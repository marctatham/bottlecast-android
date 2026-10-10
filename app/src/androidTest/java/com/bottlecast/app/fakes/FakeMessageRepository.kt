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
        MessageEntity(message = SEEDED_MESSAGE, userId = "fakeUserId")
    )

    override suspend fun getMessage(): MessageEntity = messages.first()

    override suspend fun getMessages(): List<MessageEntity> = messages.toList()

    override suspend fun postMessage(message: MessageEntity) {
        messages += message
    }

    companion object {
        const val SEEDED_MESSAGE: String = "Ahoy! This bottle washed up from the test sea."
    }
}
