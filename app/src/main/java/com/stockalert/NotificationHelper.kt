package com.stockalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID   = "stock_price_alerts"
    private const val CHANNEL_NAME = "Stock Price Alerts"

    fun createChannel(context: Context) {
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH       // heads-up popup
        ).apply {
            description      = "Scheduled NSE stock price alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            enableLights(true)
            // Default system notification sound
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttr)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Delete old (soundless) channel and recreate — channel settings are immutable once set
        nm.deleteNotificationChannel(CHANNEL_ID)
        nm.createNotificationChannel(channel)
    }

    /**
     * Posts a rich push notification with all stock prices.
     * Plays the default notification sound + vibrates.
     */
    fun postPriceAlert(
        context: Context,
        timeLabel: String,
        lines: List<String>,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title    = "📈 Stock Alert — $timeLabel IST"
        val bodyText = lines.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: "Prices updated")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle(title)
                    .setSummaryText("${lines.size} stocks • NSE")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            // Sound + vibrate (belt-and-suspenders alongside channel settings)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        nm.notify(notificationId, notification)
    }
}
