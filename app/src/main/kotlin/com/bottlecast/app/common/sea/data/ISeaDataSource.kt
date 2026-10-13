package com.bottlecast.app.common.sea.data

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ReportReason
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** How many drifting bottles one look at the sea reads, so a short page means the range ran out. */
const val SEA_BATCH_SIZE = 10

/** Firestore reads and writes for the sea. The blocking calls run on the caller's dispatcher. */
interface ISeaDataSource {

    /** Drifting bottles with fewer than 3 reports, from [fromRand] up, or below it when [wrap] is set. */
    fun querySea(fromRand: Double, wrap: Boolean): List<MessageEntity>

    /** Found bottles whose hold began before [before], so they've drifted back. */
    fun queryExpiredHolds(before: Instant): List<MessageEntity>

    fun hasFound(messageId: String, uid: String): Boolean

    fun blockedWriters(uid: String): Set<String>

    /** One transaction: holds the bottle and creates the finder's find and today's daily record. */
    fun recordFind(messageId: String, uid: String, date: LocalDate, zone: String, now: Instant): FindOutcome

    fun getMessage(messageId: String): MessageEntity?

    /** The message id of the account's find on [date], or null while there's none. */
    fun observeDaily(uid: String, date: LocalDate): Flow<String?>

    fun observeFindStatus(messageId: String, uid: String): Flow<FindStatus?>

    /** The bottle, or null once it can't be read. */
    fun observeMessage(messageId: String): Flow<MessageEntity?>

    // These write without waiting for the server, so they queue while offline.

    /** Marks the find thrown back, and releases the bottle to the sea unless its hold has expired. */
    fun commitThrowBack(messageId: String, uid: String, releaseHold: Boolean)

    fun commitReport(messageId: String, uid: String, reason: ReportReason?, blockedWriter: Boolean)

    fun blockWriter(uid: String, writerUid: String, messageId: String)
}

sealed interface FindOutcome {
    data class Found(val message: MessageEntity) : FindOutcome

    /** The daily record already names [messageId]. */
    data class AlreadyFound(val messageId: String) : FindOutcome

    /** The bottle can't be found now: someone holds it, it's out of the sea, or this account found it before. */
    data object Taken : FindOutcome
}

/** The phone couldn't reach Firestore. */
class SeaUnavailableException(cause: Throwable) : Exception(cause)

/** Firestore turned a write down: the rules refused it, or a transaction lost its race. */
class SeaRefusedException(cause: Throwable) : Exception(cause)
