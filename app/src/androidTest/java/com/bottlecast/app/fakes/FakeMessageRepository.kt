package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.ModerationAction
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the Firestore-backed repository: records casts, and has nothing to moderate.
 */
@Singleton
class FakeMessageRepository @Inject constructor() : IMessageRepository {

    private val _cast = CopyOnWriteArrayList<MessageEntity>()
    val cast: List<MessageEntity> get() = _cast.toList()

    override suspend fun postMessage(message: String, userId: String): MessageEntity =
        MessageEntity(id = "cast${_cast.size}", message = message, userId = userId).also { _cast += it }

    override suspend fun getReportedMessages(): List<MessageEntity> = emptyList()

    override suspend fun moderate(messageId: String, action: ModerationAction) = Unit

    companion object {
        const val SEEDED_MESSAGE: String = FakeSeaRepository.SEEDED_MESSAGE
    }
}
