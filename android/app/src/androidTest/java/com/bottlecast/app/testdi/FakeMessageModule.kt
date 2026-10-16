package com.bottlecast.app.testdi

import com.bottlecast.app.common.messages.di.MessageModule
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.fakes.FakeCastCoordinator
import com.bottlecast.app.fakes.FakeDraftRepository
import com.bottlecast.app.fakes.FakeLanguageDetector
import com.bottlecast.app.fakes.FakeMessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firestore (NoFirestoreModule refuses it anyway)
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

    @Binds
    fun bindDraftRepository(fakeRepository: FakeDraftRepository): IDraftRepository

    @Binds
    fun bindCastCoordinator(fakeCoordinator: FakeCastCoordinator): ICastCoordinator

    @Binds
    fun bindLanguageDetector(fakeDetector: FakeLanguageDetector): ILanguageDetector
}
