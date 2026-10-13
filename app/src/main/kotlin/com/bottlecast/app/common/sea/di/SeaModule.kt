package com.bottlecast.app.common.sea.di

import com.bottlecast.app.common.sea.data.ISeaDataSource
import com.bottlecast.app.common.sea.data.SeaActions
import com.bottlecast.app.common.sea.data.SeaFirestoreSource
import com.bottlecast.app.common.sea.data.SeaRepository
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object SeaModule {

    @Provides
    @Singleton
    fun providesSeaDataSource(source: SeaFirestoreSource): ISeaDataSource = source

    @Provides
    @Singleton
    fun providesSeaRepository(repository: SeaRepository): ISeaRepository = repository

    @Provides
    @Singleton
    fun providesSeaActions(actions: SeaActions): ISeaActions = actions

    /** Where in the sea today's look starts; tests pass a seeded one instead. */
    @Provides
    fun providesRandom(): Random = Random.Default
}
