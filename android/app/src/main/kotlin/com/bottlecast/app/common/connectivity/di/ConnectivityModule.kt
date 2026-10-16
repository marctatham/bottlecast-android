package com.bottlecast.app.common.connectivity.di

import android.content.Context
import android.net.ConnectivityManager
import com.bottlecast.app.common.connectivity.ConnectivityMonitor
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {

    @Provides
    @Singleton
    fun providesConnectivityMonitor(@ApplicationContext context: Context): IConnectivityMonitor =
        ConnectivityMonitor(context.getSystemService(ConnectivityManager::class.java))
}
