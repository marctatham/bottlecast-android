package com.bottlecast.app.common.settings.di

import com.bottlecast.app.common.settings.data.SettingsStore
import com.bottlecast.app.common.settings.data.UiModeThemeApplier
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.IThemeApplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun providesSettingsStore(store: SettingsStore): ISettingsStore = store

    @Provides
    @Singleton
    fun providesThemeApplier(applier: UiModeThemeApplier): IThemeApplier = applier
}
