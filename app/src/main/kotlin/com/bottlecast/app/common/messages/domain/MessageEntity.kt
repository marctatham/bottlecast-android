package com.bottlecast.app.common.messages.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

/** The language of a bottle whose language isn't known yet (BCP 47 "undetermined"). */
const val LANG_UNDETERMINED = "und"

/**
 * A bottle: its text, who cast it, and where it is in the sea.
 *
 * [foundAt] is when its current hold began. A find holds a bottle for [HOLD_DAYS] days, after which
 * it drifts back to the sea; the end is worked out from [foundAt], never stored.
 */
data class MessageEntity(
    val id: String,
    val message: String,
    val userId: String,
    val createdAt: Instant? = null,
    val lang: String = LANG_UNDETERMINED,
    val status: MessageStatus = MessageStatus.Afloat,
    val penName: String? = null,
    val sealable: Boolean = false,
    val reportCount: Int = 0,
    val findCount: Int = 0,
    val foundAt: Instant? = null,
)

/** How long a find holds a bottle before it drifts back to the sea (owner decision, 2026-10-13). */
const val HOLD_DAYS = 7L

/** How many reports take a bottle out of the sea. The rules hold the same number. */
const val REPORT_THRESHOLD = 3

/** When the current hold ends and the bottle drifts back, or null when nobody holds it. */
fun MessageEntity.holdEnds(): Instant? =
    foundAt?.takeIf { status == MessageStatus.Found }?.plus(HOLD_DAYS, ChronoUnit.DAYS)

fun MessageEntity.isHoldLive(now: Instant): Boolean = holdEnds()?.let { now.isBefore(it) } == true

/** Whether someone could find this bottle now, as firestore.rules decides it (with the server's clock). */
fun MessageEntity.isFindable(now: Instant): Boolean = reportCount < REPORT_THRESHOLD && when (status) {
    MessageStatus.Afloat -> true
    MessageStatus.Found -> holdEnds()?.let { !now.isBefore(it) } == true
    else -> false
}

enum class MessageStatus(val value: String) {
    Afloat("afloat"),
    Found("found"),
    UnderReview("under_review"),
    Removed("removed");

    companion object {
        fun from(value: String?): MessageStatus? = entries.firstOrNull { it.value == value }
    }
}

enum class ModerationAction {
    /** Takes the bottle out of the sea while it's looked at. */
    Pull,

    /** Takes the bottle out of the sea for good. */
    Remove,

    /** Puts the bottle back in the sea, with its reports cleared. */
    Restore,
}
