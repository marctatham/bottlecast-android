package com.bottlecast.app.testdi

import com.bottlecast.app.common.sea.di.SeaModule
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.fakes.FakeSeaActions
import com.bottlecast.app.fakes.FakeSeaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firestore's sea
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SeaModule::class]
)
interface FakeSeaModule {

    @Binds
    fun bindSeaRepository(fakeRepository: FakeSeaRepository): ISeaRepository

    @Binds
    fun bindSeaActions(fakeActions: FakeSeaActions): ISeaActions
}
