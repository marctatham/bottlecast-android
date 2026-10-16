package com.bottlecast.app.common.account.data

import com.bottlecast.app.common.account.data.model.DeletionFields
import com.bottlecast.app.common.account.domain.BottleErasure
import com.bottlecast.app.common.account.domain.IAccountEraser
import com.bottlecast.app.common.account.domain.OwnBottle
import com.bottlecast.app.common.account.domain.erasureOf
import com.bottlecast.app.common.chest.data.model.ClaimFields
import com.bottlecast.app.common.messages.data.model.CastTallyFields
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.profile.data.model.ProfileFields
import com.bottlecast.app.common.sea.data.model.BlockedWriterFields
import com.bottlecast.app.common.sea.data.model.DailyFields
import com.bottlecast.app.common.sea.data.model.FindFields
import com.bottlecast.app.common.streak.data.model.StreakDayFields
import com.bottlecast.app.common.threading.DispatcherIO
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Clock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Erases an account from Firestore in the order firestore.rules allows (decided 2026-10-15):
 * 1. the tombstone, `deletions/{uid}`, which stops the account finding or casting;
 * 2. its bottles: one someone holds or kept is anonymised (a held one becomes orphaned), the rest are deleted;
 * 3. its finds, unsealed claims, daily records, streak days, cast tally and blocked writers;
 * 4. its profile.
 *
 * Reports and handoffs it made stay: the rules don't let anyone but an admin remove them.
 */
class AccountFirestoreEraser @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val clock: Clock,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : IAccountEraser {

    override suspend fun erase(uid: String) = withContext(dispatcher) {
        val tombstone = firestore.collection(DeletionFields.COLLECTION).document(uid)
        // the rules let it be made once, and never changed
        if (!awaitServer(tombstone.get(Source.SERVER)).exists()) {
            awaitServer(tombstone.set(mapOf(DeletionFields.CREATED_AT to FieldValue.serverTimestamp())))
        }

        eraseBottles(uid)

        deleteAll(firestore.collection(FindFields.COLLECTION).whereEqualTo(FindFields.FINDER_UID, uid))
        deleteAll(
            firestore.collection(ClaimFields.COLLECTION)
                .whereEqualTo(ClaimFields.KEEPER_UID, uid)
                .whereEqualTo(ClaimFields.STATUS, ClaimFields.STATUS_PENDING_WALLET),
        )
        deleteAll(firestore.collection(DailyFields.COLLECTION).whereEqualTo(DailyFields.UID, uid))
        deleteAll(firestore.collection(StreakDayFields.COLLECTION).whereEqualTo(StreakDayFields.UID, uid))
        deleteIfThere(firestore.collection(CastTallyFields.COLLECTION).document(uid))
        val profile = firestore.collection(ProfileFields.COLLECTION).document(uid)
        deleteAll(profile.collection(BlockedWriterFields.COLLECTION), batchSize = PLAIN_BATCH)
        deleteIfThere(profile)
        Timber.i("[erase] The account's records are gone")
    }

    private fun eraseBottles(uid: String) {
        val bottles = awaitServer(
            firestore.collection(MessageFields.COLLECTION).whereEqualTo(MessageFields.USER_ID, uid).get(Source.SERVER),
        ).documents
        val now = clock.instant()
        val (anonymise, delete) = bottles.partition { erasureOf(it.toOwnBottle(), now) == BottleErasure.Anonymise }
        // anonymising makes no access call in the rules, so it goes in big batches; a delete reads the tombstone
        anonymise.chunked(PLAIN_BATCH).forEach { page -> commitOrEach(page, BottleErasure.Anonymise) }
        delete.chunked(GATED_BATCH).forEach { page -> commitOrEach(page, BottleErasure.Delete) }
        Timber.i("[eraseBottles] Anonymised ${anonymise.size} bottles and deleted ${delete.size}")
    }

    // A hold that ends just as this runs can be judged differently by the server's clock, which turns
    // the whole batch down. Then each bottle goes on its own, trying the other way when one is refused.
    private fun commitOrEach(page: List<DocumentSnapshot>, erasure: BottleErasure) {
        val batch = firestore.batch()
        page.forEach { batch.erase(it, erasure) }
        try {
            awaitServer(batch.commit())
        } catch (exception: RefusedException) {
            Timber.w(exception, "[commitOrEach] A batch was turned down, so its bottles go one at a time")
            page.forEach { snapshot ->
                try {
                    awaitServer(firestore.batch().apply { erase(snapshot, erasure) }.commit())
                } catch (refused: RefusedException) {
                    val other = if (erasure == BottleErasure.Delete) BottleErasure.Anonymise else BottleErasure.Delete
                    awaitServer(firestore.batch().apply { erase(snapshot, other) }.commit())
                }
            }
        }
    }

    private fun WriteBatch.erase(snapshot: DocumentSnapshot, erasure: BottleErasure) {
        when (erasure) {
            BottleErasure.Delete -> delete(snapshot.reference)
            BottleErasure.Anonymise -> {
                val status = snapshot.getString(MessageFields.STATUS)
                update(
                    snapshot.reference,
                    mapOf(
                        MessageFields.USER_ID to null,
                        MessageFields.PEN_NAME to null,
                        MessageFields.STATUS to if (status == MessageStatus.Found.value) MessageStatus.Orphaned.value else status,
                    ),
                )
            }
        }
    }

    private fun deleteAll(query: Query, batchSize: Int = GATED_BATCH) {
        val documents = awaitServer(query.get(Source.SERVER)).documents
        documents.chunked(batchSize).forEach { page ->
            awaitServer(firestore.batch().apply { page.forEach { delete(it.reference) } }.commit())
        }
    }

    private fun deleteIfThere(document: DocumentReference) {
        if (awaitServer(document.get(Source.SERVER)).exists()) awaitServer(document.delete())
    }

    // Waits for the server. A timeout means it couldn't be reached, which the caller reports as such.
    private fun <T> awaitServer(task: Task<T>): T = try {
        Tasks.await(task, SERVER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (exception: ExecutionException) {
        val cause = exception.cause ?: exception
        if ((cause as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            throw RefusedException(cause)
        }
        throw cause
    }

    private class RefusedException(cause: Throwable) : Exception(cause)

    private companion object {
        // a batch whose rules read the tombstone stays under the rules' access-call limit
        const val GATED_BATCH = 10
        const val PLAIN_BATCH = 400
        const val SERVER_TIMEOUT_SECONDS = 20L
    }
}

private fun DocumentSnapshot.toOwnBottle() = OwnBottle(
    id = id,
    status = getString(MessageFields.STATUS),
    foundAt = getTimestamp(MessageFields.FOUND_AT)?.toInstant(),
    kept = get(MessageFields.KEPT_AT) != null,
)
