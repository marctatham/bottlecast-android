package com.bottlecast.app.common.reminders.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderScheduler
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderDecision
import com.bottlecast.app.common.reminders.domain.decideReminder
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.streak.domain.IStreakRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime

// how long the reminder waits to learn the streak for its text
private const val STREAK_WAIT_MILLIS = 5_000L

/**
 * The daily reminder (spec §3.16b): posts "A new bottle washed ashore" at the user's time, held back
 * by quiet hours, and skipped when today's bottle is found or the app is on screen. Then it schedules
 * the next one.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val store: IReminderStore,
    private val scheduler: IReminderScheduler,
    private val access: INotificationAccess,
    private val seaRepository: ISeaRepository,
    private val streakRepository: IStreakRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = store.settings.first()
        try {
            val now = ZonedDateTime.now(clock)
            var decision = decideReminder(now, settings, access.allowed(), foundToday = false, inForeground = inForeground())
            // reading today's record costs a look at the server, so only when it would go out otherwise
            if (decision == ReminderDecision.Post && seaRepository.hasFoundToday()) decision = ReminderDecision.FoundToday
            if (decision == ReminderDecision.Post) {
                val streak = withTimeoutOrNull(STREAK_WAIT_MILLIS) { streakRepository.streak.filterNotNull().first() }
                notifier.postDaily(streak?.current ?: 0)
            }
            Timber.i("[doWork] Daily reminder: $decision")
        } finally {
            scheduler.schedule(settings)
        }
        return Result.success()
    }

    private suspend fun inForeground(): Boolean = withContext(Dispatchers.Main) {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}
