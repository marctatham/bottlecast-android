package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.data.model.CastTallyFields
import com.bottlecast.app.common.messages.data.model.MessageFields
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.NewCast
import com.bottlecast.app.common.profile.data.model.ProfileFields
import com.bottlecast.app.common.profile.domain.TERMS_VERSION
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.coroutines.resume

class CastFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ICastDataSource {

    private val messages = firestore.collection(MessageFields.COLLECTION)
    private val tallies = firestore.collection(CastTallyFields.COLLECTION)
    private val users = firestore.collection(ProfileFields.COLLECTION)

    override fun observeTally(uid: String): Flow<CastTally?> = callbackFlow {
        val registration = tallies.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) Timber.w(error, "[observeTally] Stopped listening to the cast tally")
            trySend(snapshot?.toTally())
        }
        awaitClose { registration.remove() }
    }

    override fun readTally(uid: String): CastTally? = Tasks.await(tallies.document(uid).get()).toTally()

    override fun writeCast(uid: String, cast: NewCast, rand: Double, tally: TallyWrite): PendingCast {
        val batch = firestore.batch()
        batch.set(
            messages.document(cast.messageId),
            newBottle(cast.messageId, cast.text, uid, rand, cast.lang, cast.penName, cast.sealable),
        )
        val tallyRef = tallies.document(uid)
        when (tally) {
            // the day's first, or the first on this phone that knew of no tally
            is TallyWrite.Start -> batch.set(
                tallyRef,
                mapOf(
                    CastTallyFields.DATE to tally.date.toString(),
                    CastTallyFields.COUNT to 1,
                    CastTallyFields.MESSAGE_ID to cast.messageId,
                ),
            )
            is TallyWrite.Increment -> batch.update(
                tallyRef,
                mapOf(
                    CastTallyFields.DATE to tally.date.toString(),
                    CastTallyFields.COUNT to FieldValue.increment(1),
                    CastTallyFields.MESSAGE_ID to cast.messageId,
                ),
            )
        }
        // the profile as the rules read it after this commit: the Terms, and the pen name it's signed with
        val profile = buildMap {
            if (cast.acceptTerms) {
                put(ProfileFields.TERMS_VERSION, TERMS_VERSION)
                put(ProfileFields.TERMS_ACCEPTED_AT, FieldValue.serverTimestamp())
            }
            cast.penName?.let { put(ProfileFields.PEN_NAME, it) }
        }
        if (profile.isNotEmpty()) batch.set(users.document(uid), profile, SetOptions.merge())

        val commit = batch.commit()
        return PendingCast {
            suspendCancellableCoroutine { continuation ->
                commit.addOnCompleteListener { task ->
                    if (!task.isSuccessful) Timber.e(task.exception, "[writeCast] The server turned the cast down")
                    if (continuation.isActive) continuation.resume(if (task.isSuccessful) CastAck.Confirmed else CastAck.Refused)
                }
            }
        }
    }

    override fun observeCast(messageId: String): Flow<CastStatus> = callbackFlow {
        // with metadata changes, so the moment the server has it shows, not just the local write
        val registration = messages.document(messageId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> Timber.w(error, "[observeCast] Stopped following the cast")
                snapshot == null -> Unit
                // a refused write is undone on the phone, so the bottle is gone
                !snapshot.exists() -> trySend(CastStatus.Refused)
                snapshot.metadata.hasPendingWrites() -> trySend(CastStatus.Queued)
                else -> trySend(CastStatus.Confirmed)
            }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private fun DocumentSnapshot.toTally(): CastTally? {
        if (!exists()) return null
        val date = try {
            getString(CastTallyFields.DATE)?.let(LocalDate::parse)
        } catch (_: DateTimeParseException) {
            null
        } ?: return null
        val count = getLong(CastTallyFields.COUNT)?.toInt() ?: return null
        return CastTally(date, count)
    }
}
