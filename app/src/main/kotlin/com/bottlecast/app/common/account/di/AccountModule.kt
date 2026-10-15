package com.bottlecast.app.common.account.di

import com.bottlecast.app.common.account.data.AccountFirestoreEraser
import com.bottlecast.app.common.account.data.AccountLocalData
import com.bottlecast.app.common.account.domain.IAccountEraser
import com.bottlecast.app.common.account.domain.IAccountLocalData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    fun providesAccountEraser(eraser: AccountFirestoreEraser): IAccountEraser = eraser

    @Provides
    @Singleton
    fun providesAccountLocalData(localData: AccountLocalData): IAccountLocalData = localData
}
