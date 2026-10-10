package com.bottlecast.app.testdi

import com.bottlecast.app.common.messages.di.MessageModule
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.fakes.FakeMessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firestore
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MessageModule::class]
)
interface FakeMessageModule {

    @Binds
    fun bindMessageRepository(
        fakeRepository: FakeMessageRepository
    ): IMessageRepository
}
