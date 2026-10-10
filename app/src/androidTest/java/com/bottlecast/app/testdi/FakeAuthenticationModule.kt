package com.bottlecast.app.testdi

import com.bottlecast.app.common.authentication.di.AuthenticationModule
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.fakes.FakeAuthenticationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// keeps instrumented tests away from Firebase Auth
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthenticationModule::class]
)
interface FakeAuthenticationModule {

    @Binds
    fun bindAuthenticationRepository(
        fakeRepository: FakeAuthenticationRepository
    ): IAuthenticationRepository
}
