package com.bottlecast.app.common.firestore.di

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firestore, for every data source that reads or writes it. Instrumented tests replace this module
 * with one that refuses, so a class they don't fake can never reach the debug project.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun providesFirestore(): FirebaseFirestore = Firebase.firestore
}
