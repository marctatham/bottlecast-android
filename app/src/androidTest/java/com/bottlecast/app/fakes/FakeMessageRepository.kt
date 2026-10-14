package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.ModerationAction
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in for the Firestore-backed repository, with nothing to moderate. Casts go to FakeCastCoordinator. */
@Singleton
class FakeMessageRepository @Inject constructor() : IMessageRepository {

    override suspend fun getReportedMessages(): List<MessageEntity> = emptyList()

    override suspend fun moderate(messageId: String, action: ModerationAction) = Unit

    companion object {
        const val SEEDED_MESSAGE: String = FakeSeaRepository.SEEDED_MESSAGE
    }
}
