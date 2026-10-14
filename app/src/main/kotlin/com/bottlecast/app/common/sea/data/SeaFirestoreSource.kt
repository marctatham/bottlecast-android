package com.bottlecast.app.common.sea.data

import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.REPORT_THRESHOLD
import com.bottlecast.app.common.messages.domain.isFindable
import com.bottlecast.app.common.sea.data.model.BlockedWriterFields
import com.bottlecast.app.common.sea.data.model.DailyFields
import com.bottlecast.app.common.sea.data.model.FindFields
import com.bottlecast.app.common.sea.data.model.HandoffFields
import com.bottlecast.app.common.sea.data.model.ReportFields
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ReportReason
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlin.coroutines.resume

// The report counts still in the sea. The rules only allow sea queries that filter on exactly these.
private val COUNTS_IN_THE_SEA: List<Int> = (0 until REPORT_THRESHOLD).toList()

class SeaFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mapper: MessageDataModelMapper,
) : ISeaDataSource {

    private val messages = firestore.collection(MessageFields.COLLECTION)
    private val finds = firestore.collection(FindFields.COLLECTION)
    private val daily = firestore.collection(DailyFields.COLLECTION)
    private val reports = firestore.collection(ReportFields.COLLECTION)
    private val handoffs = firestore.collection(HandoffFields.COLLECTION)

    private fun blockedWritersOf(uid: String) =
        firestore.collection(BlockedWriterFields.USERS).document(uid).collection(BlockedWriterFields.COLLECTION)

    override fun querySea(from: Double, until: Double, after: Double?): List<MessageEntity> {
        val drifting = messages
            .whereEqualTo(MessageFields.STATUS, MessageStatus.Afloat.value)
            .whereIn(MessageFields.REPORT_COUNT, COUNTS_IN_THE_SEA)
            .whereLessThan(MessageFields.RAND, until)
        val ranged = if (after != null) {
            drifting.whereGreaterThan(MessageFields.RAND, after)
        } else {
            drifting.whereGreaterThanOrEqualTo(MessageFields.RAND, from)
        }
        val query = ranged.orderBy(MessageFields.RAND).limit(SEA_BATCH_SIZE.toLong())
        return await(query.get(Source.SERVER)).documents.mapNotNull { it.toMessage() }
    }

    override fun queryExpiredHolds(before: Instant, after: Instant?): List<MessageEntity> {
        val expired = messages
            .whereEqualTo(MessageFields.STATUS, MessageStatus.Found.value)
            .whereIn(MessageFields.REPORT_COUNT, COUNTS_IN_THE_SEA)
            .whereLessThan(MessageFields.FOUND_AT, Timestamp(before))
        val ranged = if (after != null) expired.whereGreaterThan(MessageFields.FOUND_AT, Timestamp(after)) else expired
        val query = ranged.orderBy(MessageFields.FOUND_AT).limit(EXPIRED_BATCH_SIZE.toLong())
        return await(query.get(Source.SERVER)).documents.mapNotNull { it.toMessage() }
    }

    override fun hasFound(messageId: String, uid: String): Boolean =
        await(finds.document(FindFields.id(messageId, uid)).get(Source.SERVER)).exists()

    override fun blockedWriters(uid: String): Set<String> =
        await(blockedWritersOf(uid).get()).documents.map { it.id }.toSet()

    override fun recordFind(messageId: String, uid: String, date: LocalDate, zone: String, now: Instant): FindOutcome {
        val messageRef = messages.document(messageId)
        val findRef = finds.document(FindFields.id(messageId, uid))
        val dailyId = DailyFields.id(uid, date)
        val dailyRef = daily.document(dailyId)

        val task = firestore.runTransaction { transaction ->
            // every read comes before any write, as transactions require
            val dailyRecord = transaction.get(dailyRef)
            if (dailyRecord.exists()) {
                return@runTransaction FindOutcome.AlreadyFound(dailyRecord.getString(DailyFields.MESSAGE_ID).orEmpty())
            }
            if (transaction.get(findRef).exists()) return@runTransaction FindOutcome.Taken
            val message = transaction.get(messageRef).toMessage()
            if (message == null || message.userId == uid || !message.isFindable(now)) {
                return@runTransaction FindOutcome.Taken
            }

            val findCount = message.findCount + 1
            transaction.update(
                messageRef,
                mapOf(
                    MessageFields.STATUS to MessageStatus.Found.value,
                    MessageFields.FIND_COUNT to findCount,
                    MessageFields.FOUND_AT to FieldValue.serverTimestamp(),
                ),
            )
            transaction.set(
                findRef,
                mapOf(
                    FindFields.MESSAGE_ID to messageId,
                    FindFields.FINDER_UID to uid,
                    FindFields.FOUND_AT to FieldValue.serverTimestamp(),
                    FindFields.DAILY_ID to dailyId,
                    FindFields.STATUS to FindStatus.Held.value,
                ),
            )
            transaction.set(
                dailyRef,
                mapOf(
                    DailyFields.UID to uid,
                    DailyFields.DATE to date.toString(),
                    DailyFields.ZONE to zone,
                    DailyFields.MESSAGE_ID to messageId,
                    DailyFields.FOUND_AT to FieldValue.serverTimestamp(),
                ),
            )
            FindOutcome.Found(message.copy(status = MessageStatus.Found, findCount = findCount, foundAt = now))
        }
        return await(task)
    }

    // a bottle that's out of the sea can't be read, which for its finder is the same as gone
    override fun getMessage(messageId: String): MessageEntity? = try {
        await(messages.document(messageId).get()).toMessage()
    } catch (_: SeaRefusedException) {
        null
    }

    override fun observeDaily(uid: String, date: LocalDate): Flow<String?> =
        observe(daily.document(DailyFields.id(uid, date))) { it.getString(DailyFields.MESSAGE_ID) }

    override fun observeFindStatus(messageId: String, uid: String): Flow<FindStatus?> =
        observe(finds.document(FindFields.id(messageId, uid))) { FindStatus.from(it.getString(FindFields.STATUS)) }

    override fun observeMessage(messageId: String): Flow<MessageEntity?> =
        observe(messages.document(messageId)) { it.toMessage() }

    override fun commitThrowBack(messageId: String, uid: String, releaseHold: Boolean) {
        val batch = firestore.batch()
        batch.update(finds.document(FindFields.id(messageId, uid)), FindFields.STATUS, FindStatus.ThrownBack.value)
        if (releaseHold) {
            batch.update(
                messages.document(messageId),
                mapOf(MessageFields.STATUS to MessageStatus.Afloat.value, MessageFields.FOUND_AT to null),
            )
        }
        batch.commit().logFailure("throw back")
    }

    override fun commitReport(messageId: String, uid: String, reason: ReportReason?, blockedWriter: Boolean) {
        val batch = firestore.batch()
        batch.set(
            reports.document(ReportFields.id(messageId, uid)),
            mapOf(
                ReportFields.MESSAGE_ID to messageId,
                ReportFields.REPORTER_UID to uid,
                ReportFields.REASON to reason?.value,
                ReportFields.BLOCKED_WRITER to blockedWriter,
                ReportFields.CREATED_AT to FieldValue.serverTimestamp(),
            ),
        )
        batch.update(messages.document(messageId), MessageFields.REPORT_COUNT, FieldValue.increment(1))
        batch.update(finds.document(FindFields.id(messageId, uid)), FindFields.STATUS, FindStatus.Reported.value)
        batch.commit().logFailure("report")
    }

    // its own write, so a report the server turns down doesn't take the block with it
    override fun blockWriter(uid: String, writerUid: String, messageId: String) {
        blockedWritersOf(uid).document(writerUid)
            .set(mapOf(BlockedWriterFields.AT to FieldValue.serverTimestamp(), BlockedWriterFields.MESSAGE_ID to messageId))
            .logFailure("block")
    }

    override suspend fun awaitPendingWrites() = suspendCancellableCoroutine { continuation ->
        // done either way: a write the server turns down is just as settled
        firestore.waitForPendingWrites().addOnCompleteListener { if (continuation.isActive) continuation.resume(Unit) }
    }

    override fun dailyMessageId(uid: String, date: LocalDate): String? =
        await(daily.document(DailyFields.id(uid, date)).get(Source.SERVER)).getString(DailyFields.MESSAGE_ID)

    override fun findOf(messageId: String, uid: String): FindRecord? {
        val find = await(finds.document(FindFields.id(messageId, uid)).get(Source.SERVER))
        if (!find.exists()) return null
        return FindRecord(FindStatus.from(find.getString(FindFields.STATUS)), find.getTimestamp(FindFields.FOUND_AT)?.toInstant())
    }

    override fun serverMessage(messageId: String): MessageEntity? = try {
        await(messages.document(messageId).get(Source.SERVER)).toMessage()
    } catch (_: SeaRefusedException) {
        null
    }

    override fun armHandoff(messageId: String, fromUid: String, date: LocalDate, tokenHash: ByteArray) {
        awaitServer(
            handoffs.document(messageId).set(
                mapOf(
                    HandoffFields.FROM to fromUid,
                    HandoffFields.DATE to date.toString(),
                    HandoffFields.TOKEN_HASH to Blob.fromBytes(tokenHash),
                    HandoffFields.CREATED_AT to FieldValue.serverTimestamp(),
                    HandoffFields.TOKEN to null,
                    HandoffFields.CLAIMED_BY to null,
                    HandoffFields.CLAIMED_AT to null,
                ),
            ),
        )
    }

    override fun claimHandoff(messageId: String, uid: String, token: String, date: LocalDate, zone: String) {
        val dailyId = DailyFields.id(uid, date)
        val batch = firestore.batch()
        batch.update(
            handoffs.document(messageId),
            mapOf(
                HandoffFields.TOKEN to token,
                HandoffFields.CLAIMED_BY to uid,
                HandoffFields.CLAIMED_AT to FieldValue.serverTimestamp(),
            ),
        )
        batch.update(messages.document(messageId), MessageFields.FOUND_AT, FieldValue.serverTimestamp())
        batch.set(
            finds.document(FindFields.id(messageId, uid)),
            mapOf(
                FindFields.MESSAGE_ID to messageId,
                FindFields.FINDER_UID to uid,
                FindFields.FOUND_AT to FieldValue.serverTimestamp(),
                FindFields.DAILY_ID to dailyId,
                FindFields.STATUS to FindStatus.Held.value,
            ),
        )
        batch.set(
            daily.document(dailyId),
            mapOf(
                DailyFields.UID to uid,
                DailyFields.DATE to date.toString(),
                DailyFields.ZONE to zone,
                DailyFields.MESSAGE_ID to messageId,
                DailyFields.FOUND_AT to FieldValue.serverTimestamp(),
            ),
        )
        awaitServer(batch.commit())
    }

    private fun DocumentSnapshot.toMessage(): MessageEntity? = data?.let { mapper.mapFrom(id, it) }

    // A listener that ends in an error (a bottle pulled from the sea can't be read) reads as gone.
    private fun <T> observe(document: DocumentReference, read: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
        val registration = document.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.w(error, "Stopped listening to ${document.parent.id}")
                trySend(null)
            } else {
                trySend(snapshot?.takeIf { it.exists() }?.let(read))
            }
        }
        awaitClose { registration.remove() }
    }

    // Writes queue while offline and complete once the server accepts them, maybe after a restart.
    private fun <T> Task<T>.logFailure(what: String) {
        addOnFailureListener { Timber.e(it, "The server turned down a $what") }
    }
}

/** How long a handover's write waits for the server, which offline would never answer. */
internal const val SERVER_WRITE_TIMEOUT_SECONDS = 10L

// Waits for a write to reach the server. A write only completes once the server has it, so offline,
// or on a stalled connection, it would wait for good; past the timeout it counts as unavailable, and
// stays queued on the phone.
private fun <T> awaitServer(task: Task<T>): T = try {
    await(task, SERVER_WRITE_TIMEOUT_SECONDS)
} catch (exception: TimeoutException) {
    throw SeaUnavailableException(exception)
}

// Waits for a task, turning Firestore's errors into the sea's own, so the repository needn't know them.
private fun <T> await(task: Task<T>, timeoutSeconds: Long? = null): T = try {
    if (timeoutSeconds == null) Tasks.await(task) else Tasks.await(task, timeoutSeconds, TimeUnit.SECONDS)
} catch (exception: ExecutionException) {
    val cause = exception.cause ?: exception
    throw when ((cause as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> SeaUnavailableException(cause)
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> SeaRefusedException(cause)
        else -> cause
    }
}
