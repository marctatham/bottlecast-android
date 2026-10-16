package com.bottlecast.app.common.messages.data.model

/** castTally/{uid}: today's casts, counted with each one, at most 5 a local date (firestore.rules). */
object CastTallyFields {
    const val COLLECTION = "castTally"

    const val DATE = "date"
    const val COUNT = "count"
    const val MESSAGE_ID = "messageId"
}
