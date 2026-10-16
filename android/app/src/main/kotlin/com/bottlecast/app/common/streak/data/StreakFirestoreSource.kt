package com.bottlecast.app.common.streak.data

import com.bottlecast.app.common.sea.data.model.DailyFields
import com.bottlecast.app.common.streak.data.model.StreakDayFields
import com.bottlecast.app.common.streak.domain.StreakDay
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** How long carrying a streak waits for the server, right after signing in. */
private const val CARRY_TIMEOUT_SECONDS = 10L

class StreakFirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : IStreakDataSource {

    private val daily = firestore.collection(DailyFields.COLLECTION)
    private val streakDays = firestore.collection(StreakDayFields.COLLECTION)

    override fun observeFindDays(uid: String, after: LocalDate?): Flow<Read<Set<LocalDate>>> =
        listen(daily.whereEqualTo(DailyFields.UID, uid).after(DailyFields.DATE, after)) { snapshot ->
            snapshot.documents.mapNotNull { it.date(DailyFields.DATE) }.toSet()
        }

    override fun observeMarks(uid: String, after: LocalDate?): Flow<Read<List<StreakDay>>> =
        listen(streakDays.whereEqualTo(StreakDayFields.UID, uid).after(StreakDayFields.DATE, after)) { snapshot ->
            snapshot.documents.mapNotNull { document ->
                val date = document.date(StreakDayFields.DATE) ?: return@mapNotNull null
                StreakDay(
                    date = date,
                    cast = document.getBoolean(StreakDayFields.CAST) == true,
                    calm = document.getBoolean(StreakDayFields.CALM) == true,
                    carried = document.getLong(StreakDayFields.CARRIED)?.toInt() ?: 0,
                )
            }
        }

    override fun markCalm(uid: String, date: LocalDate) {
        streakDays.document(StreakDayFields.id(uid, date))
            .set(mark(uid, date, StreakDayFields.CALM to true), SetOptions.merge())
            .addOnFailureListener { Timber.w(it, "[markCalm] The server turned a calm sea down") }
    }

    override fun carry(uid: String, date: LocalDate, days: Int) {
        Tasks.await(
            streakDays.document(StreakDayFields.id(uid, date)).set(mark(uid, date, StreakDayFields.CARRIED to days), SetOptions.merge()),
            CARRY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    private fun mark(uid: String, date: LocalDate, mark: Pair<String, Any>): Map<String, Any> =
        mapOf(StreakDayFields.UID to uid, StreakDayFields.DATE to date.toString(), mark)

    // Dates are stored as yyyy-MM-dd, which sort as they read.
    private fun Query.after(field: String, date: LocalDate?): Query =
        if (date == null) this else whereGreaterThan(field, date.toString())

    // With metadata changes, so the moment the server confirms what the cache said shows too.
    // A listener that fails sends nothing more, so the streak isn't shown rather than shown wrong.
    private fun <T> listen(query: Query, read: (QuerySnapshot) -> T): Flow<Read<T>> = callbackFlow {
        val registration = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> Timber.w(error, "[listen] Stopped listening to the streak's days")
                snapshot != null -> trySend(Read(read(snapshot), fromServer = !snapshot.metadata.isFromCache))
            }
        }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.date(field: String): LocalDate? = try {
        getString(field)?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
        null
    }
}
