package com.darko.speleov1.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.darko.speleov1.MainActivity
import com.darko.speleov1.R

object TrackingNotificationHelper {
    const val CHANNEL_ID = "live_tracking"
    private const val CHANNEL_NAME = "Live tracking"
    const val NOTIFICATION_ID = 1201

    fun show(context: Context, waitingForGpsFix: Boolean, startedAtMillis: Long? = null) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            buildNotification(context, waitingForGpsFix, startedAtMillis)
        )
    }

    fun buildNotification(context: Context, waitingForGpsFix: Boolean, startedAtMillis: Long? = null): Notification {
        ensureChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TrackingForegroundService::class.java).apply {
            action = "com.darko.speleov1.action.STOP_TRACKING"
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (waitingForGpsFix) "Live tracking radi • tražim GPS fix" else "Live tracking radi"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Speleo app")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setWhen(startedAtMillis ?: System.currentTimeMillis())
            .setUsesChronometer(startedAtMillis != null && !waitingForGpsFix)
            .build()
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = "Prikazuje da je live tracking aktivan"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
