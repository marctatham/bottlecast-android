package com.bottlecast.app.common.reminders.data

import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the next reminder scheduled from the settings: at each start of the app, so a new time zone
 * counts, and whenever the switch, the time or quiet hours change.
 */
@Singleton
class ReminderCoordinator @Inject constructor(
    private val store: IReminderStore,
    private val scheduler: IReminderScheduler,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            runCatching {
                store.settings
                    .map { it.copy(rationaleOffered = false, deniedAt = null) }
                    .distinctUntilChanged()
                    // reminders that are off at start have nothing to cancel, so WorkManager isn't woken for them
                    .dropWhile { !it.enabled }
                    .collect { scheduler.schedule(it) }
            }.onFailure { Timber.e(it, "[start] Stopped scheduling the daily reminder") }
        }
    }
}
