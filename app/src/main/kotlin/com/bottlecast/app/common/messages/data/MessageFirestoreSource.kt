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
            .limit(1)

        // define the query to search down the index
        val searchDownTheIndex: Query = collectionReference
            .whereLessThanOrEqualTo(FieldPath.documentId(), randomIndexId)
            .orderBy(FieldPath.documentId())
            .limit(1)

        var querySnapshot: QuerySnapshot = Tasks.await(searchUpTheIndex.get())
        if (querySnapshot.documents.size == 0) {
            Timber.d("No firestore document found, searching down the index instead")
            querySnapshot = Tasks.await(searchDownTheIndex.get())
        }

        if (querySnapshot.documents.size == 0) {
            Timber.d("No documents found down the index EITHER")
            querySnapshot = Tasks.await(searchDownTheIndex.get())
        }

        val doc: DocumentSnapshot = querySnapshot.documents.first()
        if (doc.exists()) {
            val docData: Map<String, Any> = doc.data!!
            val messageContent: String = docData["messageContent"].toString()
            val userId: String = docData["userId"].toString()
            val dataModel = MessageFirestoreDataModel(messageContent, userId)
            return mapper.mapFrom(dataModel)
        }

        throw IllegalStateException("No message exists found in remoteSource")
    }

    override suspend fun getMessages(): List<MessageEntity> {
        Timber.i("Getting messages")
        val querySnapshot: QuerySnapshot = Tasks.await(collectionReference.get())
        return querySnapshot
            .documents
            .map {
                MessageFirestoreDataModel(
                    it.data!!["messageContent"].toString(),
                    it.data!!["userId"].toString()
                )
            }.map {
                mapper.mapFrom(it)
            }
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
