package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "tracenet_alerts_channel"
    private const val CHANNEL_NAME = "TraceNet Real-Time Alerts"
    private const val CHANNEL_DESC = "Notifications for urgent security alerts and incidents"
    private const val PREFS_NAME = "tracenet_notifications"
    private const val KEY_COUNTER = "notification_id_counter"

    private fun getNextNotificationId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentId = prefs.getInt(KEY_COUNTER, 0)
        val nextId = if (currentId >= Int.MAX_VALUE - 1) 0 else currentId + 1
        prefs.edit().putInt(KEY_COUNTER, nextId).apply()
        return currentId
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context, 
        title: String, 
        content: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (latitude != null) putExtra("latitude", latitude)
            if (longitude != null) putExtra("longitude", longitude)
            putExtra("navigate_to", "map")
        }

        val notificationId = getNextNotificationId(context)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this exists or use a default
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
