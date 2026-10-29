package com.gymbro.mad.feature.cardio

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gymbro.mad.MainActivity
import com.gymbro.mad.R
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.CardioKind
import com.gymbro.mad.data.model.RoutePoint
import com.gymbro.mad.data.repo.Repos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsTrackingSnapshot(
    val isActive: Boolean = false,
    val kind: CardioKind? = null,
    val startedAt: Instant? = null,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val route: List<RoutePoint> = emptyList(),
    val error: String? = null,
)

/** Records a user-started route in a location foreground service. */
class GpsTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val points = mutableListOf<RoutePoint>()
    private var distanceMeters = 0.0
    private var startedAt: Instant? = null
    private var startedElapsedMs = 0L
    private var kind: CardioKind = CardioKind.RUN
    private var locationListener: LocationListener? = null
    private var finishing = false

    private val clock = object : Runnable {
        override fun run() {
            if (startedAt == null) return
            updateSnapshot()
            updateNotification()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> finishTracking()
            ACTION_START -> {
                val requestedKind = intent.getStringExtra(EXTRA_KIND)?.let { name ->
                    CardioKind.entries.firstOrNull { it.name == name }
                }
                beginTracking(requestedKind ?: CardioKind.RUN)
            }
        }
        return START_NOT_STICKY
    }

    private fun beginTracking(activityKind: CardioKind) {
        if (startedAt != null) return
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            mutableState.value = GpsTrackingSnapshot(error = "Allow location access to start GPS tracking.")
            stopSelf()
            return
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mutableState.value = GpsTrackingSnapshot(error = "Turn on Location in Android settings, then try again.")
            stopSelf()
            return
        }

        kind = activityKind
        finishing = false
        startedAt = Instant.now()
        startedElapsedMs = SystemClock.elapsedRealtime()
        points.clear()
        distanceMeters = 0.0
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_METERS) return
                val previous = points.lastOrNull()
                val point = RoutePoint(location.latitude, location.longitude, Instant.ofEpochMilli(location.time))
                if (previous != null) {
                    distanceMeters += distanceBetweenMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
                }
                points += point
                updateSnapshot()
                updateNotification()
            }

            @Deprecated("Deprecated by Android; callbacks are delivered through onLocationChanged(Location)")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) {
                mutableState.value = mutableState.value.copy(error = "GPS signal was turned off. Your route is still being recorded when available.")
            }
        }
        locationListener = listener
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL_MS,
                LOCATION_MIN_DISTANCE_METERS,
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            startedAt = null
            mutableState.value = GpsTrackingSnapshot(error = "Location permission was not available. Allow it and try again.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        } catch (_: IllegalArgumentException) {
            startedAt = null
            mutableState.value = GpsTrackingSnapshot(error = "GPS is unavailable on this device.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        mutableState.value = GpsTrackingSnapshot(isActive = true, kind = kind, startedAt = startedAt)
        handler.post(clock)
    }

    private fun finishTracking() {
        val start = startedAt
        if (start != null) {
            if (finishing) return
            finishing = true
            locationListener?.let { listener -> runCatching { locationManager.removeUpdates(listener) } }
            locationListener = null
            handler.removeCallbacks(clock)
            val elapsedSeconds = ((SystemClock.elapsedRealtime() - startedElapsedMs) / 1_000L).coerceAtLeast(1L)
            val activity = CardioActivity(
                kind = kind,
                startedAt = start,
                duration = Duration.ofSeconds(elapsedSeconds),
                distanceMeters = distanceMeters.takeIf { it > 0.0 },
                route = points.toList(),
            )
            startedAt = null
            serviceScope.launch {
                try {
                    Repos.cardio.log(activity)
                    mutableState.value = GpsTrackingSnapshot()
                } catch (_: Exception) {
                    mutableState.value = GpsTrackingSnapshot(error = "The GPS activity could not be saved. Please try manual logging.")
                } finally {
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateSnapshot() {
        val start = startedAt ?: return
        mutableState.value = GpsTrackingSnapshot(
            isActive = true,
            kind = kind,
            startedAt = start,
            elapsedSeconds = ((SystemClock.elapsedRealtime() - startedElapsedMs) / 1_000L).coerceAtLeast(0L),
            distanceMeters = distanceMeters,
            route = points.toList(),
            error = mutableState.value.error,
        )
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID,
            Intent(this, GpsTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snapshot = mutableState.value
        val mins = snapshot.elapsedSeconds / 60
        val secs = snapshot.elapsedSeconds % 60
        return NotificationCompat.Builder(this, CARDIO_TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${kind.displayName} in progress")
            .setContentText("$mins:${secs.toString().padStart(2, '0')} · ${(snapshot.distanceMeters / 1000.0).formatKm()} km")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Stop and save", stopIntent)
            .build()
    }

    private fun updateNotification() {
        if (startedAt == null) return
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        locationListener?.let { listener -> runCatching { locationManager.removeUpdates(listener) } }
        handler.removeCallbacks(clock)
        if (startedAt != null) {
            startedAt = null
            mutableState.value = GpsTrackingSnapshot(error = "GPS tracking stopped before the activity was saved. Start a new route in Cardio.")
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CARDIO_TRACKING_CHANNEL_ID = "cardio_tracking"
        private const val ACTION_START = "com.gymbro.mad.cardio.START_GPS"
        private const val ACTION_STOP = "com.gymbro.mad.cardio.STOP_GPS"
        private const val EXTRA_KIND = "kind"
        private const val NOTIFICATION_ID = 4102
        private const val LOCATION_INTERVAL_MS = 1_000L
        private const val LOCATION_MIN_DISTANCE_METERS = 2f
        private const val MAX_ACCURACY_METERS = 50f
        private val mutableState = MutableStateFlow(GpsTrackingSnapshot())
        val state = mutableState.asStateFlow()

        fun start(context: android.content.Context, kind: CardioKind) {
            val intent = Intent(context, GpsTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_KIND, kind.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, GpsTrackingService::class.java).setAction(ACTION_STOP))
        }

        private fun distanceBetweenMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = (sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
                ).coerceIn(0.0, 1.0)
            return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}

private fun Double.formatKm(): String = "%.2f".format(this)
