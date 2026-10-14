package com.bottlecast.app.common.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class OnboardingStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
) : IOnboardingStore {

    override val seen: Flow<Boolean> = dataStore.data.map { it[SEEN] == true }

    override fun markSeen(): Job = scope.launch {
        runCatching { dataStore.edit { it[SEEN] = true } }
            .onFailure { Timber.w(it, "[markSeen] Couldn't save that onboarding was seen") }
    }

    private companion object {
        val SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
