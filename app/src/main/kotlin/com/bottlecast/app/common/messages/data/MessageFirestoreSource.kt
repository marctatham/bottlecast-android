package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.ModerationAction
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.random.Random

class MessageFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mapper: MessageDataModelMapper,
) : IMessageDataSource {

    private val messages: CollectionReference = firestore.collection(MessageFields.COLLECTION)

    override suspend fun postMessage(message: String, userId: String): MessageEntity {
        val document = messages.document()
        Tasks.await(
            document.set(newBottle(document.id, message, userId, Random.nextDouble(), LANG_UNDETERMINED, penName = null, sealable = false))
        )
        Timber.i("New message posted")
        return MessageEntity(id = document.id, message = message, userId = userId)
    }

    override suspend fun getReportedMessages(): List<MessageEntity> {
        val reported = messages
            .whereGreaterThanOrEqualTo(MessageFields.REPORT_COUNT, 1)
            .orderBy(MessageFields.REPORT_COUNT, Query.Direction.DESCENDING)
        val pulled = messages.whereEqualTo(MessageFields.STATUS, MessageStatus.UnderReview.value)

        val documents = Tasks.await(reported.get()).documents + Tasks.await(pulled.get()).documents
        return documents
            .distinctBy { it.id }
            .mapNotNull { mapMessage(it) }
            .sortedByDescending { it.reportCount }
    }

    override suspend fun moderate(messageId: String, action: ModerationAction) {
        val document = messages.document(messageId)
        when (action) {
            ModerationAction.Pull -> Tasks.await(document.update(MessageFields.STATUS, MessageStatus.UnderReview.value))
            ModerationAction.Remove -> Tasks.await(document.update(MessageFields.STATUS, MessageStatus.Removed.value))
            ModerationAction.Restore -> restore(document)
        }
        Timber.i("Moderated a message: $action")
    }

    // Back to the sea with its reports cleared, unless a finder's hold still runs: then back to that
    // finder for the rest of it, so the bottle never has two holders (firestore.rules insists).
    private fun restore(document: DocumentReference) {
        Tasks.await(
            firestore.runTransaction { transaction ->
                val holdStarted = transaction.get(document).getTimestamp(MessageFields.FOUND_AT)?.toInstant()
                val holdRunning = holdStarted != null && Instant.now().isBefore(holdStarted.plus(HOLD_DAYS, ChronoUnit.DAYS))
                val changes: Map<String, Any?> = if (holdRunning) {
                    mapOf(MessageFields.STATUS to MessageStatus.Found.value, MessageFields.REPORT_COUNT to 0)
                } else {
                    mapOf(
                        MessageFields.STATUS to MessageStatus.Afloat.value,
                        MessageFields.FOUND_AT to null,
                        MessageFields.REPORT_COUNT to 0,
                    )
                }
                transaction.update(document, changes)
            }
        )
    }

    private fun mapMessage(document: DocumentSnapshot): MessageEntity? {
        val message = document.data?.let { mapper.mapFrom(document.id, it) }
        if (message == null) {
            Timber.w("Skipping firestore document ${document.id} as it holds no readable message")
        }
        return message
    }
}

/**
 * A new bottle, with exactly the keys firestore.rules accepts for a cast: the writer's words, language
 * and choices from Preview (spec §3.4b), afloat and unfound.
 */
internal fun newBottle(
    id: String,
    message: String,
    userId: String,
    rand: Double,
    lang: String,
    penName: String?,
    sealable: Boolean,
): Map<String, Any?> = mapOf(
    MessageFields.ID to id,
    MessageFields.MESSAGE_CONTENT to message,
    MessageFields.USER_ID to userId,
    MessageFields.CREATED_AT to FieldValue.serverTimestamp(),
    MessageFields.LANG to lang,
    MessageFields.STATUS to MessageStatus.Afloat.value,
    MessageFields.RAND to rand,
    MessageFields.PEN_NAME to penName,
    MessageFields.SEALABLE to sealable,
    MessageFields.REPORT_COUNT to 0,
    MessageFields.FIND_COUNT to 0,
    MessageFields.FOUND_AT to null,
)
