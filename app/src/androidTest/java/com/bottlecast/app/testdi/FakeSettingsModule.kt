package com.bottlecast.app.testdi

import com.bottlecast.app.common.account.di.AccountModule
import com.bottlecast.app.common.account.domain.IAccountEraser
import com.bottlecast.app.common.account.domain.IAccountLocalData
import com.bottlecast.app.common.reminders.di.RemindersModule
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.settings.di.SettingsModule
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.IThemeApplier
import com.bottlecast.app.fakes.FakeAccountEraser
import com.bottlecast.app.fakes.FakeAccountLocalData
import com.bottlecast.app.fakes.FakeNotificationAccess
import com.bottlecast.app.fakes.FakeReminderScheduler
import com.bottlecast.app.fakes.FakeReminderStore
import com.bottlecast.app.fakes.FakeSettingsStore
import com.bottlecast.app.fakes.FakeThemeApplier
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// the phone's settings live in DataStore, which stays out of instrumented tests
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SettingsModule::class]
)
interface FakeSettingsModule {

    @Binds
    fun bindSettingsStore(fake: FakeSettingsStore): ISettingsStore

    @Binds
    fun bindThemeApplier(fake: FakeThemeApplier): IThemeApplier
}

// the reminder's settings live in DataStore, and its scheduling in WorkManager, which tests don't set up
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RemindersModule::class]
)
interface FakeRemindersModule {

    @Binds
    fun bindReminderStore(fake: FakeReminderStore): IReminderStore

    @Binds
    fun bindReminderScheduler(fake: FakeReminderScheduler): IReminderScheduler

    @Binds
    fun bindNotificationAccess(fake: FakeNotificationAccess): INotificationAccess
}

// deleting an account erases Firestore records, which nothing unfaked may reach
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AccountModule::class]
)
interface FakeAccountModule {

    @Binds
    fun bindAccountEraser(fake: FakeAccountEraser): IAccountEraser

    @Binds
    fun bindAccountLocalData(fake: FakeAccountLocalData): IAccountLocalData
}
