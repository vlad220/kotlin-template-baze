package com.template.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale

/**
 * Геокодер берём из системы (android.location.Geocoder): серверный ключ Яндекса для
 * /taxi/geocode ещё не куплен, а без интернета системный геокодер всё равно вернёт null.
 */
object GeocoderUtils {
    fun addressToCoordinates(context: Context, address: String): Pair<Double, Double>? {
        return try {
            val addresses: List<Address> = Geocoder(context, Locale("ru"))
                .getFromLocationName(address, 1) ?: emptyList()
            addresses.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    fun coordinatesToAddress(context: Context, lat: Double, lon: Double): String? {
        return try {
            val addresses: List<Address> = Geocoder(context, Locale("ru"))
                .getFromLocation(lat, lon, 1) ?: emptyList()
            addresses.firstOrNull()?.let { a ->
                a.getAddressLine(0) ?: a.thoroughfare
            }
        } catch (e: Exception) {
            null
        }
    }

    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun formatDistance(km: Double): String = when {
        km < 1 -> "${(km * 1000).toInt()} м"
        km < 10 -> String.format("%.1f км", km)
        else -> "${km.toInt()} км"
    }
}
