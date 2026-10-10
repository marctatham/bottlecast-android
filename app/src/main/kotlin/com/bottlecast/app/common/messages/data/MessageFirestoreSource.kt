package com.bottlecast.app.common.messages.data

import android.annotation.SuppressLint
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.util.Util.autoId
import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.data.model.MessageFirestoreDataModel
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber
import java.lang.IllegalStateException
import javax.inject.Inject

private const val COLLECTION = "messages"

// how many documents to fetch per random search, so that malformed ones can be skipped over
private const val RANDOM_SEARCH_BATCH = 10L

class MessageFirestoreSource @Inject constructor(
    firestore: FirebaseFirestore,
    private val mapper: MessageDataModelMapper,
) : IMessageDataSource {

    private val collectionReference: CollectionReference = firestore.collection(COLLECTION)

    override suspend fun getMessage(): MessageEntity {

        // generate a random ID within the index
        val randomIndexId = getRandomIndexId()

        // define the query to search up the index
        val searchUpTheIndex: Query = collectionReference
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), randomIndexId)
            .orderBy(FieldPath.documentId())
            .limit(RANDOM_SEARCH_BATCH)

        // define the query to wrap around to the start of the index
        val searchFromTheStart: Query = collectionReference
            .orderBy(FieldPath.documentId())
            .limit(RANDOM_SEARCH_BATCH)

        findFirstMessage(searchUpTheIndex)?.let { return it }

        Timber.d("No message found up the index, searching from the start instead")
        return findFirstMessage(searchFromTheStart)
            ?: throw IllegalStateException("No message exists found in remoteSource")
    }

    // the first document of the query that holds a readable message, skipping any malformed ones
    private fun findFirstMessage(query: Query): MessageEntity? {
        val querySnapshot: QuerySnapshot = Tasks.await(query.get())
        return querySnapshot.documents.firstNotNullOfOrNull { mapMessage(it) }
    }

    private fun mapMessage(doc: DocumentSnapshot): MessageEntity? {
        val message: MessageEntity? = doc.data?.let { mapper.mapFrom(it) }
        if (message == null) {
            Timber.w("Skipping firestore document ${doc.id} as it holds no readable message")
        }
        return message
    }

    override suspend fun getMessages(): List<MessageEntity> {
        Timber.i("Getting messages")
        val querySnapshot: QuerySnapshot = Tasks.await(collectionReference.get())
        return querySnapshot
            .documents
            .mapNotNull { mapMessage(it) }
    }

    override suspend fun postMessage(message: MessageEntity) {
        val addTask: Task<DocumentReference> = collectionReference.add(
            MessageFirestoreDataModel(
                message.message,
                message.userId
            )
        )

        Tasks.await(addTask)
        Timber.i("New message posted")
    }

    @SuppressLint("RestrictedApi") // this is actually the recommended way of retrieving random documents from firestore
    private fun getRandomIndexId() = autoId()
}
