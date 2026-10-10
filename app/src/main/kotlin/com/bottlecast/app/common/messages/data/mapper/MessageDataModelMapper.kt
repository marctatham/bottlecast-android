package com.bottlecast.app.common.messages.data.mapper

import com.bottlecast.app.common.messages.domain.MessageEntity
import javax.inject.Inject

// must match the property names of MessageFirestoreDataModel, which Firestore writes them as
private const val FIELD_MESSAGE_CONTENT = "messageContent"
private const val FIELD_USER_ID = "userId"

class MessageDataModelMapper @Inject constructor() {

    /**
     * Maps a raw Firestore document to a [MessageEntity], or null when the document holds no
     * readable message (e.g. posts from the 1.0 release, which R8 stored under obfuscated fields)
     */
    fun mapFrom(document: Map<String, Any?>): MessageEntity? {
        val messageContent = document[FIELD_MESSAGE_CONTENT] as? String
        if (messageContent.isNullOrBlank()) return null

        val userId = document[FIELD_USER_ID] as? String ?: ""
        return MessageEntity(messageContent, userId)
    }
}
