package com.template.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.template.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * Локальные уведомления о заявках и о статусе поездки. Фоновый push (FCM) не подключали —
 * это внешний сервис, поэтому уведомление приходит только пока процесс приложения жив.
 */
object Notifier {
    private const val CHANNEL = "taxi_events"
    private const val CHANNEL_NAME = "Заявки и поездки"
    private val nextId = AtomicInteger(1)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun enabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** С 33-го Android уведомляет только после явного согласия; до него просить нечего. */
    fun needsConsent(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !enabled(context)

    fun show(context: Context, title: String, text: String) {
        if (!enabled(context)) return
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val tap = open?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        NotificationManagerCompat.from(context).notify(
            nextId.getAndIncrement(),
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_taxi)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(tap)
                .build()
        )
    }
}
