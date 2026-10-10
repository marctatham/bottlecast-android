package com.bottlecast.app.common.messages.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.bottlecast.app.common.messages.data.IMessageDataSource
import com.bottlecast.app.common.messages.data.MessageFirestoreSource
import com.bottlecast.app.common.messages.data.MessageRepository
import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.threading.DispatcherIO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MessageModule {

    @Provides
    @Singleton
    fun providesFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun providesMessageService(
        firestore: FirebaseFirestore,
        mapper: MessageDataModelMapper
    ): IMessageDataSource {
        return MessageFirestoreSource(firestore, mapper)
    }

    @Provides
    @Singleton
    fun providesMessageRepository(
        messageDataSource: IMessageDataSource,
        @DispatcherIO dispatcher: CoroutineDispatcher
    ): IMessageRepository {
        return MessageRepository(
            messageDataSource,
            dispatcher
        )
    }
}