package com.bottlecast.app.testdi

import com.bottlecast.app.common.firestore.di.FirestoreModule
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/**
 * Instrumented tests never reach Firestore: every module that binds something backed by it is
 * faked, and this fails loudly if one isn't, rather than quietly using the debug project.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FirestoreModule::class]
)
object NoFirestoreModule {

    @Provides
    fun providesFirestore(): FirebaseFirestore =
        error("Instrumented tests never reach Firestore: fake the module that asked for it")
}
