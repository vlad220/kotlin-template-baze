package com.template.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

private const val FRESH_MS = 60_000L

fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Живые координаты водителя, пока приложение на переднем плане и он на линии.
 * Фоновой службы нет: со свёрнутым приложением точка не обновляется.
 */
class LocationTracker(private val context: Context) {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var last: Location? = null
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun start() {
        val lm = manager ?: return
        if (listener != null || !hasLocationPermission(context)) return
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.time >= (last?.time ?: 0)) last = location
            }

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Метод обязателен для API 26–28")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = l
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (lm.isProviderEnabled(provider)) {
                try {
                    lm.requestLocationUpdates(provider, 10_000L, 0f, l, Looper.getMainLooper())
                } catch (e: SecurityException) {
                }
            }
        }
        if (last == null) last = newestLastKnown(lm)
    }

    fun stop() {
        val l = listener ?: return
        listener = null
        try {
            manager?.removeUpdates(l)
        } catch (e: SecurityException) {
        }
    }

    /** null, если позиция старше минуты: старая точка серверу вреднее, чем её отсутствие. */
    fun current(): Pair<Double, Double>? {
        val loc = last ?: manager?.let { newestLastKnown(it) } ?: return null
        if (System.currentTimeMillis() - loc.time > FRESH_MS) return null
        return loc.latitude to loc.longitude
    }

    @SuppressLint("MissingPermission")
    private fun newestLastKnown(lm: LocationManager): Location? {
        if (!hasLocationPermission(context)) return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }
}
