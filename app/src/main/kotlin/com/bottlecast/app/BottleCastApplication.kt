package com.bottlecast.app

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bottlecast.app.common.performance.CrashReportingTree
import com.bottlecast.app.common.reminders.data.ReminderCoordinator
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant
import javax.inject.Inject

@HiltAndroidApp
class BottleCastApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminders: ReminderCoordinator

    // WorkManager starts on demand with Hilt's factory, so the daily reminder's worker can be injected
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // setup the appropriate Timber tree based on whether or not application is debuggable
        val isDebuggableBuild = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggableBuild) {
            plant(DebugTree())
        } else {
            plant(CrashReportingTree())
        }

        reminders.start()
    }
}
