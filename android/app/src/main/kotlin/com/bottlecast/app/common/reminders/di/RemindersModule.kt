package com.bottlecast.app.common.reminders.di

import com.bottlecast.app.common.reminders.data.NotificationAccess
import com.bottlecast.app.common.reminders.data.ReminderStore
import com.bottlecast.app.common.reminders.data.WorkReminderScheduler
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.IReminderStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemindersModule {

    @Provides
    @Singleton
    fun providesReminderStore(store: ReminderStore): IReminderStore = store

    @Provides
    @Singleton
    fun providesReminderScheduler(scheduler: WorkReminderScheduler): IReminderScheduler = scheduler

    @Provides
    @Singleton
    fun providesNotificationAccess(access: NotificationAccess): INotificationAccess = access
}
