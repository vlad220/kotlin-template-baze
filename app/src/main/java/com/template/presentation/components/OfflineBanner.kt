package com.template.presentation.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.template.util.OfflineNotice

/**
 * Плашка «нет интернета» (экран 29): висит над любым экраном, но экран не блокирует —
 * то, что уже пришло, остаётся перед пользователем.
 */
@Composable
fun OfflineBanner(route: String?) {
    val context = LocalContext.current
    if (!rememberOnline(context)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Нет интернета — не обновится: ${OfflineNotice.whatBreaks(route)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun rememberOnline(context: Context): Boolean {
    val online = remember { mutableStateOf(hasInternet(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online.value = hasInternet(context) }
            override fun onLost(network: Network) { online.value = hasInternet(context) }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return online.value
}

private fun hasInternet(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
