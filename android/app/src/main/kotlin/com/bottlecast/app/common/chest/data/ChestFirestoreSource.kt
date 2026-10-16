package com.bottlecast.app.common.chest.data

import com.bottlecast.app.common.chest.data.model.ClaimFields
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.isFindable
import com.bottlecast.app.common.messages.domain.isHoldLive
import com.bottlecast.app.common.sea.data.model.FindFields
import com.bottlecast.app.common.sea.domain.FindStatus
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateQuerySnapshot
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

/** How long counting waits for the server, which offline would never answer. */
private const val COUNT_TIMEOUT_SECONDS = 10L

class ChestFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mapper: MessageDataModelMapper,
) : IChestDataSource {

    private val messages = firestore.collection(MessageFields.COLLECTION)
    private val finds = firestore.collection(FindFields.COLLECTION)
    private val claims = firestore.collection(ClaimFields.COLLECTION)

    override fun keep(messageId: String, uid: String, now: Instant): KeepResult {
        val messageRef = messages.document(messageId)
        val findRef = finds.document(FindFields.id(messageId, uid))
        val task = firestore.runTransaction { transaction ->
            val message = transaction.get(messageRef).toMessage()
            val find = transaction.get(findRef)
            // the live hold must be this account's: a find left behind by a handover has an older foundAt
            val holds = message != null && message.isHoldLive(now) &&
                find.getString(FindFields.STATUS) == FindStatus.Held.value &&
                find.getTimestamp(FindFields.FOUND_AT)?.toInstant() == message.foundAt
            if (!holds) return@runTransaction KeepResult.NotHeld

            transaction.update(
                messageRef,
                mapOf(MessageFields.STATUS to MessageStatus.Kept.value, MessageFields.KEPT_AT to FieldValue.serverTimestamp()),
            )
            transaction.update(findRef, FindFields.STATUS, FindStatus.Kept.value)
            transaction.set(
                claims.document(messageId),
                mapOf(
                    ClaimFields.MESSAGE_ID to messageId,
                    ClaimFields.KEEPER_UID to uid,
                    ClaimFields.STATUS to ClaimFields.STATUS_PENDING_WALLET,
                    ClaimFields.KEPT_AT to FieldValue.serverTimestamp(),
                ),
            )
            KeepResult.Kept
        }
        return when (val outcome = await(task)) {
            is Outcome.Done -> outcome.value
            Outcome.Unavailable -> KeepResult.Offline
            Outcome.Refused -> KeepResult.NotHeld
        }
    }

    override fun pull(messageId: String, uid: String, now: Instant): PullResult {
        val messageRef = messages.document(messageId)
        val task = firestore.runTransaction { transaction ->
            val message = transaction.get(messageRef).toMessage()
            if (message == null || message.userId != uid || !message.isFindable(now)) return@runTransaction PullResult.NotDrifting
            transaction.delete(messageRef)
            PullResult.Pulled
        }
        return when (val outcome = await(task)) {
            is Outcome.Done -> outcome.value
            Outcome.Unavailable -> PullResult.Offline
            Outcome.Refused -> PullResult.NotDrifting
        }
    }

    override fun observeFinds(uid: String, since: Instant): Flow<List<FindOf>> = listen(
        finds.whereEqualTo(FindFields.FINDER_UID, uid)
            .whereGreaterThan(FindFields.FOUND_AT, Timestamp(since))
            .orderBy(FindFields.FOUND_AT, Query.Direction.DESCENDING),
    ) { snapshot -> snapshot.documents.mapNotNull { it.toFind() } }

    override fun observeClaims(uid: String, limit: Int): Flow<List<Claim>> = listen(
        claims.whereEqualTo(ClaimFields.KEEPER_UID, uid)
            .orderBy(ClaimFields.KEPT_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong()),
    ) { snapshot -> snapshot.documents.mapNotNull { it.toClaim() } }

    override fun observeCasts(uid: String, limit: Int): Flow<List<MessageEntity>> = listen(
        messages.whereEqualTo(MessageFields.USER_ID, uid)
            .orderBy(MessageFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong()),
    ) { snapshot -> snapshot.documents.mapNotNull { it.toMessage() } }

    override fun observeMessage(messageId: String): Flow<MessageEntity?> = observe(messages.document(messageId)) { it.toMessage() }

    override fun observeFind(messageId: String, uid: String): Flow<FindOf?> =
        observe(finds.document(FindFields.id(messageId, uid))) { it.toFind() }

    override fun observeClaim(messageId: String): Flow<Claim?> = observe(claims.document(messageId)) { it.toClaim() }

    override fun readMessage(messageId: String): MessageEntity? {
        val document = messages.document(messageId)
        val cached = try {
            Tasks.await(document.get(Source.CACHE)).toMessage()
        } catch (_: ExecutionException) {
            null
        }
        if (cached != null) return cached
        return when (val outcome = await(document.get())) {
            is Outcome.Done -> outcome.value.toMessage()
            // a bottle that's out of the sea can't be read, which is the same as gone here
            Outcome.Refused -> null
            Outcome.Unavailable -> null
        }
    }

    override fun count(uid: String, canCast: Boolean): ChestTotals {
        val found = finds.whereEqualTo(FindFields.FINDER_UID, uid).count()
        if (!canCast) {
            return ChestTotals(bottled = null, found = countOf(found.get(AggregateSource.SERVER)), kept = 0, castFinds = null)
        }
        val findCount = AggregateField.sum(MessageFields.FIND_COUNT)
        val casts = messages.whereEqualTo(MessageFields.USER_ID, uid).aggregate(AggregateField.count(), findCount)
        val kept = claims.whereEqualTo(ClaimFields.KEEPER_UID, uid).count()

        val castTotals = serverAnswer(casts.get(AggregateSource.SERVER))
        return ChestTotals(
            bottled = castTotals.count.toInt(),
            found = countOf(found.get(AggregateSource.SERVER)),
            kept = countOf(kept.get(AggregateSource.SERVER)),
            castFinds = castTotals.getLong(findCount)?.toInt() ?: 0,
        )
    }

    private fun countOf(task: Task<AggregateQuerySnapshot>): Int = serverAnswer(task).count.toInt()

    // Counting only ever asks the server, so offline, or on a stalled connection, it's unavailable.
    private fun <T> serverAnswer(task: Task<T>): T = try {
        Tasks.await(task, COUNT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (exception: TimeoutException) {
        throw ChestUnavailableException(exception)
    } catch (exception: ExecutionException) {
        throw ChestUnavailableException(exception.cause ?: exception)
    }

    // a cast or keep still on its way to the server reads with the phone's estimate of its time
    private fun DocumentSnapshot.toMessage(): MessageEntity? = getData(ESTIMATE)?.let { mapper.mapFrom(id, it) }

    private fun DocumentSnapshot.toFind(): FindOf? {
        if (!exists()) return null
        val messageId = getString(FindFields.MESSAGE_ID) ?: return null
        val foundAt = getTimestamp(FindFields.FOUND_AT)?.toInstant() ?: return null
        return FindOf(messageId, foundAt, FindStatus.from(getString(FindFields.STATUS)))
    }

    private fun DocumentSnapshot.toClaim(): Claim? {
        if (!exists()) return null
        return Claim(getString(ClaimFields.MESSAGE_ID) ?: id, getTimestamp(ClaimFields.KEPT_AT, ESTIMATE)?.toInstant())
    }

    private fun <T> listen(query: Query, read: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            when {
                error != null -> Timber.w(error, "[listen] Stopped listening to part of the chest")
                snapshot != null -> trySend(read(snapshot))
            }
        }
        awaitClose { registration.remove() }
    }

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

    // What became of a task, with Firestore's errors turned into the chest's own, so the repository
    // needn't know them.
    private sealed interface Outcome<out T> {
        data class Done<T>(val value: T) : Outcome<T>

        data object Unavailable : Outcome<Nothing>

        /** The rules turned it down, or a transaction lost its race. */
        data object Refused : Outcome<Nothing>
    }

    private fun <T> await(task: Task<T>): Outcome<T> = try {
        Outcome.Done(Tasks.await(task))
    } catch (exception: ExecutionException) {
        val cause = exception.cause ?: exception
        when ((cause as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> Outcome.Unavailable
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            -> {
                Timber.w(cause, "[await] The server turned a chest write down")
                Outcome.Refused
            }
            else -> throw cause
        }
    }
}
