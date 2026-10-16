package com.bottlecast.app.common.random.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object RandomModule {

    /** Where in the sea today's look starts, a new bottle's id and suggested pen names; tests pass a seeded one. */
    @Provides
    fun providesRandom(): Random = Random.Default

    /** Secrets, such as the token that hands today's find over to a Google account (spec §3.2). */
    @Provides
    fun providesSecureRandom(): SecureRandom = SecureRandom()
}
