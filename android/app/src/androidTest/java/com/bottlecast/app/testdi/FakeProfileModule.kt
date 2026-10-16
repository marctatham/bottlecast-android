package com.bottlecast.app.testdi

import com.bottlecast.app.common.profile.di.ProfileModule
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.fakes.FakeProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from the profiles in Firestore
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ProfileModule::class]
)
interface FakeProfileModule {

    @Binds
    fun bindProfileRepository(fakeRepository: FakeProfileRepository): IProfileRepository
}
