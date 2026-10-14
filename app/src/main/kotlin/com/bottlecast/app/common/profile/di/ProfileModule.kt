package com.bottlecast.app.common.profile.di

import com.bottlecast.app.common.profile.data.IProfileDataSource
import com.bottlecast.app.common.profile.data.ProfileFirestoreSource
import com.bottlecast.app.common.profile.data.ProfileRepository
import com.bottlecast.app.common.profile.domain.IProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun providesProfileDataSource(source: ProfileFirestoreSource): IProfileDataSource = source

    @Provides
    @Singleton
    fun providesProfileRepository(repository: ProfileRepository): IProfileRepository = repository
}
