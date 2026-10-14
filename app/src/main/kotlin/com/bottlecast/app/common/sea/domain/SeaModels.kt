package com.bottlecast.app.common.sea.domain

import com.bottlecast.app.common.messages.domain.MessageEntity
import java.time.LocalDate

/** What today's prefetch found in the sea. */
sealed interface CandidateState {

    /** Nothing has looked yet, or the account or the day changed. */
    data object Unknown : CandidateState

    data class Available(val message: MessageEntity) : CandidateState

    /** No bottle is drifting that this account could find. */
    data object SeaCalm : CandidateState
}

/**
 * Today's find, for Shore B. [message] is null when the bottle can't be read any more, such as after
 * a moderator pulls it.
 */
data class TodayFind(
    val date: LocalDate,
    val messageId: String,
    val message: MessageEntity?,
    val status: FindStatus,
)

enum class FindStatus(val value: String) {
    Held("held"),
    ThrownBack("thrown_back"),
    Reported("reported");

    companion object {
        fun from(value: String?): FindStatus? = entries.firstOrNull { it.value == value }
    }
}

sealed interface FindResult {

    data class Found(val message: MessageEntity) : FindResult

    /** Today already has a find, of another bottle: on another device, or a moment ago. */
    data class AlreadyFound(val messageId: String) : FindResult

    data object SeaCalm : FindResult

    data object Offline : FindResult

    data object Failed : FindResult
}

/**
 * A guest's live hold on today's bottle, armed to hand over to the Google account it's signing in to
 * (spec §3.2). Only this phone knows [token], which the claim presents.
 */
class Handover(val messageId: String, val fromUid: String, val token: String) {
    // the token stays out of logs
    override fun toString(): String = "Handover(messageId=$messageId)"
}

enum class HandoverResult {
    /** The Google account holds today's find now. */
    HandedOver,

    /** The Google account already has a find today, which stays. */
    KeptOwnFind,

    /** It couldn't move: that account found the bottle before, or the server turned the claim down. */
    Failed,
}

/** Why someone reported a bottle, as firestore.rules spells it. */
enum class ReportReason(val value: String) {
    Hurtful("hurtful"),
    Sexual("sexual"),
    PersonalInfo("personal_info"),
    Spam("spam"),
    Other("other"),
}
