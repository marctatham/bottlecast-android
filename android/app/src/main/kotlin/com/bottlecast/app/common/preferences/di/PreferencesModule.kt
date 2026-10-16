package com.bottlecast.app.common.preferences.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.bottlecast.app.common.preferences.IOnboardingStore
import com.bottlecast.app.common.preferences.OnboardingStore
import com.bottlecast.app.common.threading.DispatcherIO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    /**
     * The app's one preferences file: the onboarding flag and the Compose draft. Only one DataStore
     * may own a file, so this is the only one. A corrupt file starts again empty rather than crashing.
     */
    @Provides
    @Singleton
    fun providesPreferences(
        @ApplicationContext context: Context,
        @DispatcherIO dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + dispatcher),
        produceFile = { context.preferencesDataStoreFile(FILE) },
    )

    @Provides
    @Singleton
    fun providesOnboardingStore(store: OnboardingStore): IOnboardingStore = store

    private const val FILE = "bottlecast"
}
