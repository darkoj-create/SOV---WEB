package com.darko.speleov1.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class TrackingForegroundService : Service() {
    private var trackingHandle: LocationHelper.TrackingHandle? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTrackingAndSelf()
            else -> startTrackingIfNeeded()
        }
        return START_STICKY
    }

    private fun startTrackingIfNeeded() {
        if (trackingHandle != null) {
            startForeground(
                TrackingNotificationHelper.NOTIFICATION_ID,
                TrackingNotificationHelper.buildNotification(this, TrackingRuntime.state.value.waitingForGpsFix, TrackingRuntime.state.value.startedAtMillis)
            )
            return
        }
        if (!TrackingRuntime.state.value.active) {
            TrackingRuntime.startSession()
        }
        startForeground(
            TrackingNotificationHelper.NOTIFICATION_ID,
            TrackingNotificationHelper.buildNotification(this, waitingForGpsFix = true, startedAtMillis = TrackingRuntime.state.value.startedAtMillis)
        )
        LocationHelper.bootstrapLastKnownLocation(this) { location ->
            TrackingRuntime.bootstrapLocation(location)
            NotificationManagerCompat.from(this).notify(
                TrackingNotificationHelper.NOTIFICATION_ID,
                TrackingNotificationHelper.buildNotification(this, waitingForGpsFix = TrackingRuntime.state.value.waitingForGpsFix, startedAtMillis = TrackingRuntime.state.value.startedAtMillis)
            )
        }
        trackingHandle = LocationHelper.startLocationUpdates(
            context = this,
            minTimeMs = 1000L,
            minDistanceM = 2f,
            mode = LocationHelper.LocationMode.GPS_ONLY,
        ) { location ->
            TrackingRuntime.onLocation(location)
            NotificationManagerCompat.from(this).notify(
                TrackingNotificationHelper.NOTIFICATION_ID,
                TrackingNotificationHelper.buildNotification(this, waitingForGpsFix = TrackingRuntime.state.value.waitingForGpsFix, startedAtMillis = TrackingRuntime.state.value.startedAtMillis)
            )
        }
        if (trackingHandle == null) {
            TrackingRuntime.stopSession(keepPoints = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopTrackingAndSelf() {
        LocationHelper.stopLocationUpdates(trackingHandle)
        trackingHandle = null
        TrackingRuntime.stopSession(keepPoints = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        TrackingNotificationHelper.cancel(this)
        stopSelf()
    }

    override fun onDestroy() {
        LocationHelper.stopLocationUpdates(trackingHandle)
        trackingHandle = null
        if (TrackingRuntime.state.value.active) {
            TrackingRuntime.stopSession(keepPoints = true)
        }
        TrackingNotificationHelper.cancel(this)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.darko.speleov1.action.START_TRACKING"
        private const val ACTION_STOP = "com.darko.speleov1.action.STOP_TRACKING"

        fun start(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply { action = ACTION_START }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply { action = ACTION_STOP }
            runCatching { context.startService(intent) }
            runCatching { context.stopService(Intent(context, TrackingForegroundService::class.java)) }
        }
    }
}
