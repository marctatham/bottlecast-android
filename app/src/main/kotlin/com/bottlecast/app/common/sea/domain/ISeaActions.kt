package com.bottlecast.app.common.sea.domain

import com.bottlecast.app.common.messages.domain.MessageEntity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Throw backs and reports wait out an undo window before they're written, and outlive the screen that
 * started them: a throw back's Undo shows on the Shore, after Reveal has gone.
 */
interface ISeaActions {

    /** The action waiting out its undo window, if any. Only one waits at a time. */
    val pending: StateFlow<PendingSeaAction?>

    /** Each action as it's written. */
    val outcomes: SharedFlow<SeaActionOutcome>

    /**
     * Schedules a throw back. [windowMillis] is the undo window; null waits for [commit] or [undo]
     * (TalkBack users get as long as they need), up to a safety limit. Returns the action's id.
     */
    fun throwBack(message: MessageEntity, windowMillis: Long?): String

    /** Schedules a report, and a block of the writer when [blockWriter] is set. Returns the action's id. */
    fun report(message: MessageEntity, reason: ReportReason?, blockWriter: Boolean, windowMillis: Long?): String

    fun undo(actionId: String)

    /** Writes the action now rather than when its window ends. */
    fun commit(actionId: String)
}

sealed interface PendingSeaAction {
    val id: String
    val messageId: String

    data class ThrowBack(override val id: String, override val messageId: String) : PendingSeaAction

    data class Report(override val id: String, override val messageId: String, val blockWriter: Boolean) :
        PendingSeaAction
}

data class SeaActionOutcome(
    val action: PendingSeaAction,
    /** Written while offline, so Firestore sends it once the phone is back online. */
    val queuedOffline: Boolean,
)
