package com.bottlecast.app.common.streak.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Bump when the fold's rules change, so every phone folds its history again under the new ones.
 */
internal const val FOLD_VERSION = 1

/** Keeps one account's checkpoint at a time, in the app's DataStore, as a short line of fields. */
class StreakCheckpointStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : IStreakCheckpointStore {

    override suspend fun load(uid: String): StreakCheckpoint? = try {
        dataStore.data.first()[CHECKPOINT]?.let { decode(uid, it) }
    } catch (exception: java.io.IOException) {
        Timber.w(exception, "[load] Couldn't read the streak's checkpoint")
        null
    }

    override fun save(uid: String, checkpoint: StreakCheckpoint): Job = scope.launch {
        runCatching { dataStore.edit { it[CHECKPOINT] = encode(uid, checkpoint) } }
            .onFailure { Timber.w(it, "[save] Couldn't save the streak's checkpoint") }
    }

    override fun noticeSeenThrough(uid: String): Flow<LocalDate?> = dataStore.data.map { preferences ->
        preferences[NOTICE_SEEN]?.split(SEPARATOR)?.takeIf { it.size == 2 && it[0] == uid }?.let { parseDate(it[1]) }
    }

    override fun markNoticeSeen(uid: String, date: LocalDate): Job = scope.launch {
        runCatching { dataStore.edit { it[NOTICE_SEEN] = listOf(uid, date).joinToString(SEPARATOR) } }
            .onFailure { Timber.w(it, "[markNoticeSeen] Couldn't save that the streak's news was seen") }
    }

    private companion object {
        val CHECKPOINT = stringPreferencesKey("streak_checkpoint")
        val NOTICE_SEEN = stringPreferencesKey("streak_notice_seen")
    }
}

private const val SEPARATOR = "|"
private const val SHIELD_USED = "shield"
private const val RESTED = "rested"

// version|uid|through|current|best|shields|event|eventDate|eventDays
internal fun encode(uid: String, checkpoint: StreakCheckpoint): String {
    val event = checkpoint.lastEvent
    return listOf(
        FOLD_VERSION,
        uid,
        checkpoint.through,
        checkpoint.current,
        checkpoint.best,
        checkpoint.shields,
        when (event) {
            is StreakEvent.ShieldUsed -> SHIELD_USED
            is StreakEvent.Rested -> RESTED
            null -> ""
        },
        event?.date ?: "",
        (event as? StreakEvent.ShieldUsed)?.days ?: "",
    ).joinToString(SEPARATOR)
}

internal fun decode(uid: String, line: String): StreakCheckpoint? {
    val fields = line.split(SEPARATOR)
    if (fields.size != 9 || fields[0] != FOLD_VERSION.toString() || fields[1] != uid) return null
    val through = parseDate(fields[2]) ?: return null
    val event = when (fields[6]) {
        SHIELD_USED -> parseDate(fields[7])?.let { date -> fields[8].toIntOrNull()?.let { StreakEvent.ShieldUsed(date, it) } }
        RESTED -> parseDate(fields[7])?.let { StreakEvent.Rested(it) }
        else -> null
    }
    return StreakCheckpoint(
        through = through,
        current = fields[3].toIntOrNull() ?: return null,
        best = fields[4].toIntOrNull() ?: return null,
        shields = fields[5].toIntOrNull() ?: return null,
        lastEvent = event,
    )
}

private fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text)
} catch (_: DateTimeParseException) {
    null
}
