package com.bottlecast.app.testdi

import com.bottlecast.app.common.streak.di.StreakModule
import com.bottlecast.app.common.streak.domain.ICalmSeaLog
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.fakes.FakeStreakRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firestore's streak days
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StreakModule::class]
)
interface FakeStreakModule {

    @Binds
    fun bindStreakRepository(fakeRepository: FakeStreakRepository): IStreakRepository

    @Binds
    fun bindCalmSeaLog(fakeRepository: FakeStreakRepository): ICalmSeaLog
}
