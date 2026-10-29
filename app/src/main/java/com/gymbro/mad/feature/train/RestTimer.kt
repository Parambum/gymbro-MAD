package com.gymbro.mad.feature.train

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gymbro.mad.R

internal const val REST_CHANNEL_ID = "workout_rest"

internal fun scheduleRestNotification(context: Context, key: Int, durationSeconds: Int, exercise: String) {
    val intent = Intent(context, RestFinishedReceiver::class.java).apply {
        putExtra("key", key)
        putExtra("exercise", exercise)
    }
    val pending = PendingIntent.getBroadcast(context, key, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + durationSeconds.coerceAtLeast(1) * 1_000L,
        pending,
    )
}

class RestFinishedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notification = NotificationCompat.Builder(context, REST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest is over")
            .setContentText("Ready for your next set of ${intent.getStringExtra("exercise") ?: "the exercise"}?")
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(intent.getIntExtra("key", 0), notification)
        }
    }
}
