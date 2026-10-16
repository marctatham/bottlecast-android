package com.bottlecast.app.common.reminders.data

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationAccess @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : INotificationAccess {

    override fun allowed(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        // a channel that doesn't exist yet is made, on, with the first reminder
        val channel = manager.getNotificationChannelCompat(DAILY_CHANNEL)
        return manager.areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    override fun permissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
