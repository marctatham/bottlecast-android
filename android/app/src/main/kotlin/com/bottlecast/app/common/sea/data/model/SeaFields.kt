package com.bottlecast.app.common.sea.data.model

import java.time.LocalDate

/** finds/{messageId}_{finderUid}: one per finder per bottle, so nobody finds a bottle twice. */
object FindFields {
    const val COLLECTION = "finds"

    const val MESSAGE_ID = "messageId"
    const val FINDER_UID = "finderUid"
    const val FOUND_AT = "foundAt"
    const val DAILY_ID = "dailyId"
    const val STATUS = "status"

    fun id(messageId: String, uid: String) = "${messageId}_$uid"
}

/** daily/{uid}_{yyyy-MM-dd}: the one find each local date allows. Create-only. */
object DailyFields {
    const val COLLECTION = "daily"

    const val UID = "uid"
    const val DATE = "date"
    const val ZONE = "zone"
    const val MESSAGE_ID = "messageId"
    const val FOUND_AT = "foundAt"

    fun id(uid: String, date: LocalDate) = "${uid}_$date"
}

/** reports/{messageId}_{reporterUid}: one per reporter per bottle. Create-only. */
object ReportFields {
    const val COLLECTION = "reports"

    const val MESSAGE_ID = "messageId"
    const val REPORTER_UID = "reporterUid"
    const val REASON = "reason"
    const val BLOCKED_WRITER = "blockedWriter"
    const val CREATED_AT = "createdAt"

    fun id(messageId: String, uid: String) = "${messageId}_$uid"
}

/** users/{uid}/blockedWriters/{writerUid}: the writers whose bottles never reach this account. */
object BlockedWriterFields {
    const val USERS = "users"
    const val COLLECTION = "blockedWriters"

    const val AT = "at"
    const val MESSAGE_ID = "messageId"
}

/**
 * handoffs/{messageId}: a guest's live hold, waiting to be claimed by the Google account it's signing in
 * to (spec §3.2). Nobody can read one; the rules check the claim's token against [TOKEN_HASH].
 */
object HandoffFields {
    const val COLLECTION = "handoffs"

    const val FROM = "from"
    const val DATE = "date"
    const val TOKEN_HASH = "tokenHash"
    const val CREATED_AT = "createdAt"
    const val TOKEN = "token"
    const val CLAIMED_BY = "claimedBy"
    const val CLAIMED_AT = "claimedAt"
}
