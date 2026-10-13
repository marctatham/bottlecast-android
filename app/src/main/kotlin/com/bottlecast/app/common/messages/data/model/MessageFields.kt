package com.bottlecast.app.common.messages.data.model

/**
 * The `messages` collection and its fields. Documents are written as plain maps, never data classes,
 * because firestore.rules checks each document's exact set of keys and R8 renames properties.
 */
object MessageFields {
    const val COLLECTION = "messages"

    const val ID = "id"
    const val MESSAGE_CONTENT = "messageContent"
    const val USER_ID = "userId"
    const val CREATED_AT = "createdAt"
    const val LANG = "lang"
    const val STATUS = "status"
    const val RAND = "rand"
    const val PEN_NAME = "penName"
    const val SEALABLE = "sealable"
    const val REPORT_COUNT = "reportCount"
    const val FIND_COUNT = "findCount"
    const val FOUND_AT = "foundAt"
}
