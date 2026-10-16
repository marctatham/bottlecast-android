package com.bottlecast.app.common.reminders.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

/** The reminder's settings in the app's DataStore, times as minutes of the day. */
class ReminderStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : IReminderStore {

    override val settings: Flow<ReminderSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val defaults = ReminderSettings()
            ReminderSettings(
                enabled = preferences[ENABLED] ?: defaults.enabled,
                time = preferences[TIME]?.let(::timeOf) ?: defaults.time,
                quietStart = preferences[QUIET_START]?.let(::timeOf) ?: defaults.quietStart,
                quietEnd = preferences[QUIET_END]?.let(::timeOf) ?: defaults.quietEnd,
                rationaleOffered = preferences[RATIONALE_OFFERED] ?: defaults.rationaleOffered,
                deniedAt = preferences[DENIED_AT]?.let(Instant::ofEpochMilli),
            )
        }
        .distinctUntilChanged()

    override fun setEnabled(enabled: Boolean): Job = edit { it[ENABLED] = enabled }

    override fun setTime(time: LocalTime): Job = edit { it[TIME] = minutesOf(time) }

    override fun setQuietHours(start: LocalTime, end: LocalTime): Job = edit {
        it[QUIET_START] = minutesOf(start)
        it[QUIET_END] = minutesOf(end)
    }

    override fun markRationaleOffered(): Job = edit { it[RATIONALE_OFFERED] = true }

    override fun markDenied(at: Instant?): Job = edit {
        if (at == null) it.remove(DENIED_AT) else it[DENIED_AT] = at.toEpochMilli()
    }

    private fun edit(change: (MutablePreferences) -> Unit): Job = scope.launch {
        runCatching { dataStore.edit(change) }.onFailure { Timber.w(it, "[edit] Couldn't save the reminder's settings") }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("reminder_enabled")
        val TIME = intPreferencesKey("reminder_minute_of_day")
        val QUIET_START = intPreferencesKey("reminder_quiet_start_minute")
        val QUIET_END = intPreferencesKey("reminder_quiet_end_minute")
        val RATIONALE_OFFERED = booleanPreferencesKey("reminder_rationale_offered")
        val DENIED_AT = longPreferencesKey("reminder_permission_denied_at")
    }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

private fun minutesOf(time: LocalTime): Int = time.hour * MINUTES_PER_HOUR + time.minute

private fun timeOf(minutes: Int): LocalTime? =
    minutes.takeIf { it in 0 until MINUTES_PER_DAY }?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
