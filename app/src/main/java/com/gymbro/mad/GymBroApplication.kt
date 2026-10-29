package com.gymbro.mad

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.feature.cardio.GpsTrackingService

/**
 * Application class.
 *
 * Initializes local repositories and notification channels before features
 * post workout-rest or GPS-tracking notifications.
 */
class GymBroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Repos.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifications = getSystemService(NotificationManager::class.java)
            notifications.createNotificationChannel(
                NotificationChannel(
                    GpsTrackingService.CARDIO_TRACKING_CHANNEL_ID,
                    "Cardio GPS tracking",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Ongoing GPS recording for your cardio activity" },
            )
            notifications.createNotificationChannel(
                NotificationChannel(
                    com.gymbro.mad.feature.train.REST_CHANNEL_ID,
                    "Workout rest timer",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Alerts when a set rest period is over" },
            )
        }
    }
}
