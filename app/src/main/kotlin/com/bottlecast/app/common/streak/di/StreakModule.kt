package com.bottlecast.app.common.streak.di

import com.bottlecast.app.common.streak.data.IStreakCheckpointStore
import com.bottlecast.app.common.streak.data.IStreakDataSource
import com.bottlecast.app.common.streak.data.StreakCheckpointStore
import com.bottlecast.app.common.streak.data.StreakFirestoreSource
import com.bottlecast.app.common.streak.data.StreakRepository
import com.bottlecast.app.common.streak.domain.ICalmSeaLog
import com.bottlecast.app.common.streak.domain.IStreakRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreakModule {

    @Provides
    @Singleton
    fun providesStreakDataSource(source: StreakFirestoreSource): IStreakDataSource = source

    @Provides
    @Singleton
    fun providesStreakCheckpointStore(store: StreakCheckpointStore): IStreakCheckpointStore = store

    @Provides
    @Singleton
    fun providesStreakRepository(repository: StreakRepository): IStreakRepository = repository

    @Provides
    @Singleton
    fun providesCalmSeaLog(repository: StreakRepository): ICalmSeaLog = repository
}
