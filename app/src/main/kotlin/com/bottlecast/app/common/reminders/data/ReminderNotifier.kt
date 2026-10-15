package com.bottlecast.app.common.reminders.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bottlecast.app.MainActivity
import com.bottlecast.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The "Daily bottle" channel and its one notification (spec §3.16b). */
internal const val DAILY_CHANNEL = "daily_bottle"
private const val DAILY_NOTIFICATION_ID = 1
private const val OPEN_SHORE_REQUEST = 1
private const val FIND_IT_REQUEST = 2

/** What a notification asks MainActivity to open. */
const val EXTRA_OPEN = "com.bottlecast.app.OPEN"
const val OPEN_SHORE = "shore"
const val OPEN_DISCOVER = "discover"

class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** "A new bottle washed ashore", with [streakDays] in its text when there's a streak to keep going. */
    fun postDaily(streakDays: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel()
        val text = if (streakDays > 0) {
            context.resources.getQuantityString(R.plurals.notification_daily_text_streak, streakDays, streakDays)
        } else {
            context.getString(R.string.notification_daily_text)
        }
        val notification = NotificationCompat.Builder(context, DAILY_CHANNEL)
            .setSmallIcon(R.drawable.ic_glyph_bottle)
            .setColor(context.getColor(R.color.notification_accent))
            .setContentTitle(context.getString(R.string.notification_daily_title))
            .setContentText(text)
            .setAutoCancel(true)
            // a tap opens the Shore; Find it goes straight to Discover (spec §2, flow 7)
            .setContentIntent(openApp(OPEN_SHORE_REQUEST, OPEN_SHORE))
            .addAction(0, context.getString(R.string.notification_daily_find), openApp(FIND_IT_REQUEST, OPEN_DISCOVER))
            .build()
        NotificationManagerCompat.from(context).notify(DAILY_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            DAILY_CHANNEL,
            context.getString(R.string.notification_channel_daily),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_daily_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun openApp(request: Int, open: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN, open)
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
