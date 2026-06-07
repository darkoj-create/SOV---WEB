package com.darko.speleov1.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class TrackingForegroundService : Service() {
    private var trackingHandle: LocationHelper.TrackingHandle? = null
    private var lastQueuedAtMillis: Long = 0L
    private var lastSyncAttemptAtMillis: Long = 0L

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
            maybeQueueFieldTrackingPoint(location)
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
        runCatching { FieldTrackingLiteApi.syncPending(this, limit = 200) }
        runCatching { FieldTrackingLiteApi.stopSession(this) }
        TrackingRuntime.stopSession(keepPoints = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        TrackingNotificationHelper.cancel(this)
        stopSelf()
    }

    private fun maybeQueueFieldTrackingPoint(location: android.location.Location) {
        val state = FieldTrackingLitePrefs.load(this)
        if (!state.active || state.tripId.isBlank() || state.sessionId.isBlank()) return
        val now = System.currentTimeMillis()
        val battery = FieldTrackingLitePrefs.batteryPct(this)
        val intervalMs = when (state.trackingMode) {
            "route" -> when {
                battery in 1..14 -> 120_000L
                battery in 15..29 -> 90_000L
                else -> 20_000L
            }
            else -> when {
                battery in 1..29 -> 120_000L
                else -> 60_000L
            }
        }
        if (lastQueuedAtMillis > 0L && now - lastQueuedAtMillis < intervalMs) return
        FieldTrackingLiteStore.enqueue(this, location, state.sessionId, state.tripId)
        lastQueuedAtMillis = now
        if (lastSyncAttemptAtMillis == 0L || now - lastSyncAttemptAtMillis > 60_000L) {
            lastSyncAttemptAtMillis = now
            Thread {
                runCatching { FieldTrackingLiteApi.syncPending(this, limit = 120) }
                    .onFailure { FieldTrackingLitePrefs.saveSyncResult(this, false, it.message ?: "Tracking sync nije uspio.") }
            }.start()
        }
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

        fun startForFieldTracking(context: Context, tripId: String, tripTitle: String, appVersion: String, trackingMode: String = "lite") {
            val state = FieldTrackingLitePrefs.load(context)
            if (state.sessionId.isBlank() || !state.active || state.tripId != tripId || state.trackingMode != trackingMode) {
                FieldTrackingLiteApi.startSession(context, tripId, tripTitle, appVersion, trackingMode)
            }
            start(context)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply { action = ACTION_STOP }
            runCatching { context.startService(intent) }
            runCatching { context.stopService(Intent(context, TrackingForegroundService::class.java)) }
        }
    }
}
