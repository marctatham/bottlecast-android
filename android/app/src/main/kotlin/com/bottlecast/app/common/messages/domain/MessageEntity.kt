package com.bottlecast.app.common.messages.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

/** The language of a bottle whose language isn't known yet (BCP 47 "undetermined"). */
const val LANG_UNDETERMINED = "und"

/**
 * A bottle: its text, who cast it, and where it is in the sea.
 *
 * [foundAt] is when its current hold began. A find holds a bottle for [HOLD_DAYS] days, after which
 * it drifts back to the sea; the end is worked out from [foundAt], never stored. [rand] is its place
 * in the sea's random order, which a look at the sea reads through. [keptAt] is when its finder kept
 * it, which takes it out of the sea for good (spec §1.7).
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
    val rand: Double? = null,
    val keptAt: Instant? = null,
)

/** How long a find holds a bottle before it drifts back to the sea (owner decision, 2026-10-13). */
const val HOLD_DAYS = 7L

/** How many reports take a bottle out of the sea. The rules hold the same number. */
const val REPORT_THRESHOLD = 3

/** When the current hold ends and the bottle drifts back, or null when nobody holds it. */
fun MessageEntity.holdEnds(): Instant? =
    foundAt?.takeIf { status == MessageStatus.Found || status == MessageStatus.Orphaned }?.plus(HOLD_DAYS, ChronoUnit.DAYS)

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

    /** Its finder kept it: it's in their chest for good, and never drifts again. */
    Kept("kept"),

    /**
     * Its writer deleted their account while someone held it: it stays with its finder, without the
     * writer's name, until the hold ends, and never drifts again (decided 2026-10-15).
     */
    Orphaned("orphaned"),
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

    /**
     * Clears its reports and puts it back in the sea, or back to its finder while their hold runs, or
     * back to its keeper once kept.
     */
    Restore,
}
