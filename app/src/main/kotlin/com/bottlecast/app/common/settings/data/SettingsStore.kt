package com.bottlecast.app.common.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.ThemeChoice
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
import javax.inject.Inject

/** The phone's settings, in the app's DataStore. A file that can't be read gives the defaults. */
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : ISettingsStore {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val defaults = AppSettings()
            AppSettings(
                theme = preferences[THEME]?.let { name -> ThemeChoice.entries.firstOrNull { it.name == name } } ?: defaults.theme,
                reduceMotion = preferences[REDUCE_MOTION] ?: defaults.reduceMotion,
                haptics = preferences[HAPTICS] ?: defaults.haptics,
                shakeToUncork = preferences[SHAKE_TO_UNCORK] ?: defaults.shakeToUncork,
                signWithPenName = preferences[SIGN_WITH_PEN_NAME] ?: defaults.signWithPenName,
            )
        }
        .distinctUntilChanged()

    override fun setTheme(theme: ThemeChoice): Job = edit { it[THEME] = theme.name }

    override fun setReduceMotion(reduce: Boolean): Job = edit { it[REDUCE_MOTION] = reduce }

    override fun setHaptics(on: Boolean): Job = edit { it[HAPTICS] = on }

    override fun setShakeToUncork(on: Boolean): Job = edit { it[SHAKE_TO_UNCORK] = on }

    override fun setSignWithPenName(sign: Boolean): Job = edit { it[SIGN_WITH_PEN_NAME] = sign }

    private fun edit(change: (MutablePreferences) -> Unit): Job = scope.launch {
        runCatching { dataStore.edit(change) }.onFailure { Timber.w(it, "[edit] Couldn't save a setting") }
    }

    private companion object {
        val THEME = stringPreferencesKey("settings_theme")
        val REDUCE_MOTION = booleanPreferencesKey("settings_reduce_motion")
        val HAPTICS = booleanPreferencesKey("settings_haptics")
        val SHAKE_TO_UNCORK = booleanPreferencesKey("settings_shake_to_uncork")
        val SIGN_WITH_PEN_NAME = booleanPreferencesKey("settings_sign_with_pen_name")
    }
}
