package com.bottlecast.app.common.chest.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/** Keeps one account's at a time, in the app's DataStore, as uid|viewedAt|castFinds. */
class ChestSeenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : IChestSeenStore {

    override fun seen(uid: String): Flow<ChestSeen?> = dataStore.data.map { preferences ->
        val fields = preferences[SEEN]?.split(SEPARATOR) ?: return@map null
        if (fields.size != 3 || fields[0] != uid) return@map null
        val viewedAt = fields[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@map null
        ChestSeen(viewedAt, fields[2].toIntOrNull())
    }

    override fun save(uid: String, seen: ChestSeen): Job = scope.launch {
        val line = listOf(uid, seen.viewedAt.toEpochMilli(), seen.castFinds ?: "").joinToString(SEPARATOR)
        runCatching { dataStore.edit { it[SEEN] = line } }
            .onFailure { Timber.w(it, "[save] Couldn't save when the chest was seen") }
    }

    override fun clear(): Job = scope.launch {
        runCatching { dataStore.edit { it.remove(SEEN) } }
            .onFailure { Timber.w(it, "[clear] Couldn't forget when the chest was seen") }
    }

    private companion object {
        val SEEN = stringPreferencesKey("chest_seen")
        const val SEPARATOR = "|"
    }
}
