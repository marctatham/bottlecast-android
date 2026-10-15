package com.bottlecast.app.common.chest.di

import com.bottlecast.app.common.chest.data.ChestFirestoreSource
import com.bottlecast.app.common.chest.data.ChestRepository
import com.bottlecast.app.common.chest.data.ChestSeenStore
import com.bottlecast.app.common.chest.data.IChestDataSource
import com.bottlecast.app.common.chest.data.IChestSeenStore
import com.bottlecast.app.common.chest.domain.IChestRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChestModule {

    @Provides
    @Singleton
    fun providesChestDataSource(source: ChestFirestoreSource): IChestDataSource = source

    @Provides
    @Singleton
    fun providesChestSeenStore(store: ChestSeenStore): IChestSeenStore = store

    @Provides
    @Singleton
    fun providesChestRepository(repository: ChestRepository): IChestRepository = repository
}
