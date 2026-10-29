package com.gymbro.mad

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.gymbro.mad.data.repo.Repos

/**
 * Application class.
 *
 * Initializes the persisted workout repository and the rest notification
 * channel, which Android requires to exist before a notification is posted.
 */
class GymBroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Repos.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    com.gymbro.mad.feature.train.REST_CHANNEL_ID,
                    "Workout rest timer",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Alerts when a set rest period is over" },
            )
        }
    }
}
