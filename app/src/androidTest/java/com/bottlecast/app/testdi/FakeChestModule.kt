package com.bottlecast.app.testdi

import com.bottlecast.app.common.chest.di.ChestModule
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.fakes.FakeChestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firestore's chest
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ChestModule::class]
)
interface FakeChestModule {

    @Binds
    fun bindChestRepository(fakeRepository: FakeChestRepository): IChestRepository
}
