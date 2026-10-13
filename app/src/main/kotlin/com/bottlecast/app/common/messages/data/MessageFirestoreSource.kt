package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.ModerationAction
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

class MessageFirestoreSource @Inject constructor(
    firestore: FirebaseFirestore,
    private val mapper: MessageDataModelMapper,
) : IMessageDataSource {

    private val messages: CollectionReference = firestore.collection(MessageFields.COLLECTION)

    override suspend fun postMessage(message: String, userId: String): MessageEntity {
        val document = messages.document()
        Tasks.await(document.set(newBottle(document.id, message, userId, Random.nextDouble())))
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
        val changes: Map<String, Any?> = when (action) {
            ModerationAction.Pull -> mapOf(MessageFields.STATUS to MessageStatus.UnderReview.value)
            ModerationAction.Remove -> mapOf(MessageFields.STATUS to MessageStatus.Removed.value)
            ModerationAction.Restore -> mapOf(
                MessageFields.STATUS to MessageStatus.Afloat.value,
                MessageFields.FOUND_AT to null,
                MessageFields.REPORT_COUNT to 0,
            )
        }
        Tasks.await(messages.document(messageId).update(changes))
        Timber.i("Moderated a message: $action")
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
 * A new bottle, with exactly the keys firestore.rules accepts for a cast. Until slice 3's Preview lets
 * writers choose, a bottle has no pen name and can't be sealed, and its language isn't detected.
 */
internal fun newBottle(id: String, message: String, userId: String, rand: Double): Map<String, Any?> = mapOf(
    MessageFields.ID to id,
    MessageFields.MESSAGE_CONTENT to message,
    MessageFields.USER_ID to userId,
    MessageFields.CREATED_AT to FieldValue.serverTimestamp(),
    MessageFields.LANG to LANG_UNDETERMINED,
    MessageFields.STATUS to MessageStatus.Afloat.value,
    MessageFields.RAND to rand,
    MessageFields.PEN_NAME to null,
    MessageFields.SEALABLE to false,
    MessageFields.REPORT_COUNT to 0,
    MessageFields.FIND_COUNT to 0,
    MessageFields.FOUND_AT to null,
)
