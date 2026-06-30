package com.darko.speleov1.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationHelper {
    enum class LocationMode { ANY, GPS_ONLY }

    data class TrackingHandle(
        val manager: LocationManager?,
        val listeners: List<LocationListener>,
        val mode: LocationMode,
        val fusedClient: FusedLocationProviderClient? = null,
        val fusedCallback: LocationCallback? = null,
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isGpsProviderEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    }

    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    fun getLastKnownGpsLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    }

    fun bootstrapLastKnownLocation(context: Context, onLocation: (Location) -> Unit) {
        if (!hasLocationPermission(context)) return
        getLastKnownLocation(context)?.let(onLocation)
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull() ?: return
        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) onLocation(location)
            }
        }
    }

    fun getRecentNavigationBootstrapLocation(context: Context, maxAgeMs: Long = 15_000L): Location? {
        if (!hasLocationPermission(context)) return null
        val now = System.currentTimeMillis()
        val candidates = buildList {
            getLastKnownGpsLocation(context)?.let { add(it) }
            getLastKnownLocation(context)?.let { add(it) }
        }
        return candidates
            .distinctBy { Triple(it.provider ?: "", it.latitude, it.longitude) }
            .filter { location ->
                val ageMs = (now - location.time).coerceAtLeast(0L)
                ageMs <= maxAgeMs && isGoodNavigationFix(location)
            }
            .maxByOrNull { it.time }
    }

    fun isGoodNavigationFix(location: Location?): Boolean {
        location ?: return false
        val acc = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
        return location.provider == LocationManager.GPS_PROVIDER || acc <= 25f
    }

    fun startLocationUpdates(
        context: Context,
        minTimeMs: Long = 2000L,
        minDistanceM: Float = 5f,
        mode: LocationMode = LocationMode.ANY,
        onLocation: (Location) -> Unit
    ): TrackingHandle? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val providers = when (mode) {
            LocationMode.GPS_ONLY -> buildList {
                if (manager != null && runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.GPS_PROVIDER)
                }
            }
            LocationMode.ANY -> buildList {
                if (manager != null && runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) add(LocationManager.GPS_PROVIDER)
                if (manager != null && runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) add(LocationManager.NETWORK_PROVIDER)
                if (isEmpty()) add(LocationManager.PASSIVE_PROVIDER)
            }
        }
        val listeners = providers.mapNotNull { provider ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) { onLocation(location) }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            val ok = runCatching { manager?.requestLocationUpdates(provider, minTimeMs, minDistanceM, listener) }.isSuccess
            if (ok) listener else null
        }

        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()
        val fusedCallback = if (fusedClient != null) {
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach(onLocation)
                }
            }.also { callback ->
                val priority = if (mode == LocationMode.GPS_ONLY) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                val request = LocationRequest.Builder(priority, minTimeMs)
                    .setMinUpdateIntervalMillis((minTimeMs / 2).coerceAtLeast(500L))
                    .setWaitForAccurateLocation(false)
                    .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    .setMinUpdateDistanceMeters(minDistanceM)
                    .build()
                runCatching { fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper()) }
            }
        } else null

        if (listeners.isEmpty() && fusedCallback == null) return null
        return TrackingHandle(manager, listeners, mode, fusedClient, fusedCallback)
    }

    fun stopLocationUpdates(handle: TrackingHandle?) {
        val resolved = handle ?: return
        resolved.listeners.forEach { listener ->
            runCatching { resolved.manager?.removeUpdates(listener) }
        }
        resolved.fusedClient?.let { client ->
            resolved.fusedCallback?.let { callback -> runCatching { client.removeLocationUpdates(callback) } }
        }
    }
}
