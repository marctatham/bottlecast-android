package com.bottlecast.app.common.messages.data.mapper

import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.google.firebase.Timestamp
import javax.inject.Inject

class MessageDataModelMapper @Inject constructor() {

    /**
     * Maps a raw Firestore document to a [MessageEntity], or null when it isn't a readable bottle in
     * the slice 2 shape: no text (the 1.0 release stored R8-obfuscated fields), or no status (an old
     * bottle that hasn't been migrated yet, see scripts/migrate_messages.py)
     */
    fun mapFrom(id: String, document: Map<String, Any?>): MessageEntity? {
        val messageContent = document[MessageFields.MESSAGE_CONTENT] as? String
        if (messageContent.isNullOrBlank()) return null
        val status = MessageStatus.from(document[MessageFields.STATUS] as? String) ?: return null

        return MessageEntity(
            id = id,
            message = messageContent,
            userId = document[MessageFields.USER_ID] as? String ?: "",
            createdAt = (document[MessageFields.CREATED_AT] as? Timestamp)?.toInstant(),
            lang = document[MessageFields.LANG] as? String ?: LANG_UNDETERMINED,
            status = status,
            penName = document[MessageFields.PEN_NAME] as? String,
            sealable = document[MessageFields.SEALABLE] as? Boolean ?: false,
            reportCount = (document[MessageFields.REPORT_COUNT] as? Number)?.toInt() ?: 0,
            findCount = (document[MessageFields.FIND_COUNT] as? Number)?.toInt() ?: 0,
            foundAt = (document[MessageFields.FOUND_AT] as? Timestamp)?.toInstant(),
            rand = (document[MessageFields.RAND] as? Number)?.toDouble(),
        )
    }
}
