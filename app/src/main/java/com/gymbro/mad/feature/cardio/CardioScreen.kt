package com.gymbro.mad.feature.cardio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.CardioKind
import com.gymbro.mad.data.model.RoutePoint
import com.gymbro.mad.data.repo.CardioRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CardioScreen(modifier: Modifier = Modifier, cardio: CardioRepository = Repos.cardio) {
    val palette = LocalPalette.current
    val activities by cardio.activities.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var kind by remember { mutableStateOf(CardioKind.RUN) }
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    val tracking by GpsTrackingService.state.collectAsStateWithLifecycle()
    val requestLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val allowed = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) GpsTrackingService.start(context, kind)
        else gpsError = "Location permission was declined. You can still log cardio manually."
    }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 16.dp)) {
        Text("Cardio", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardioKind.entries.forEach { option ->
                GymBroChip(option.displayName, kind == option, { kind = option })
            }
        }
        if (tracking.isActive) {
            GymBroCard(accent = palette.accent) {
                Text("${tracking.kind?.displayName ?: "Cardio"} GPS tracking", color = palette.ink, style = MaterialTheme.typography.titleMedium)
                val gpsDistance = if (tracking.route.isEmpty()) {
                    "Waiting for GPS signal"
                } else {
                    "${(tracking.distanceMeters / 1000.0).pretty()} km"
                }
                Text("${tracking.elapsedSeconds / 60}:${(tracking.elapsedSeconds % 60).toString().padStart(2, '0')} · $gpsDistance", color = palette.muted)
                tracking.error?.let { Text(it, color = palette.danger) }
                if (tracking.route.size > 1) RoutePreview(tracking.route, Modifier.fillMaxWidth().height(120.dp))
                AccentButton("Stop and save GPS activity", onClick = { GpsTrackingService.stop(context) }, Modifier.fillMaxWidth())
            }
        } else {
            AccentButton(
                "Start GPS tracking",
                onClick = {
                    gpsError = null
                    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasLocation) GpsTrackingService.start(context, kind)
                    else requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("GPS starts only when you tap here. It keeps recording in the background with an ongoing notification.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            (gpsError ?: tracking.error)?.let { Text(it, color = palette.danger) }
        }
        OutlinedTextField(duration, { duration = it.filter(Char::isDigit); error = null }, Modifier.fillMaxWidth(), label = { Text("Duration (minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(distance, { distance = it.filter { c -> c.isDigit() || c == '.' }; error = null }, Modifier.fillMaxWidth(), label = { Text("Distance in km (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(heartRate, { heartRate = it.filter(Char::isDigit); error = null }, Modifier.fillMaxWidth(), label = { Text("Average heart rate (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        error?.let { Text(it, color = palette.danger) }
        AccentButton("Log activity manually", onClick = {
            val minutes = duration.toLongOrNull()
            val km = distance.takeIf(String::isNotBlank)?.toDoubleOrNull()
            val bpm = heartRate.takeIf(String::isNotBlank)?.toIntOrNull()
            when {
                minutes == null || minutes !in 1L..1440L -> error = "Enter a duration between 1 and 1440 minutes."
                distance.isNotBlank() && (km == null || km <= 0 || km > 1000) -> error = "Enter a valid distance in kilometres."
                heartRate.isNotBlank() && (bpm == null || bpm !in 30..250) -> error = "Enter a valid heart rate, or leave it blank."
                else -> scope.launch {
                    cardio.log(CardioActivity(kind = kind, startedAt = Instant.now(), duration = Duration.ofMinutes(minutes), distanceMeters = km?.times(1000), avgHeartRate = bpm))
                    duration = ""; distance = ""; heartRate = ""
                }
            }
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SectionHeader("past activities")
        if (activities.isEmpty()) {
            EmptyState("No cardio logged", "Log a run, walk, ride, row or swim here. Activities are sorted newest first.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities, key = CardioActivity::id) { activity ->
                    GymBroCard(modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { deleteId = activity.id })) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(activity.kind.displayName, color = palette.ink, style = MaterialTheme.typography.titleMedium)
                            MicroLabel(activity.startedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString())
                        }
                        Text("${activity.duration.toMinutes()} min · ${activity.distanceMeters?.let { "${(it / 1000).pretty()} km" } ?: "—"} · ${activity.avgHeartRate?.let { "$it bpm" } ?: "—"}", color = palette.muted)
                        MicroLabel("pace ${activity.paceMinPerKm?.let { "${it.toInt()} min ${(it * 60).roundToInt() % 60} sec/km" } ?: "—"}")
                        if (activity.route.size > 1) {
                            MicroLabel("GPS route · ${activity.route.size} points", color = palette.muted)
                            RoutePreview(activity.route, Modifier.fillMaxWidth().height(96.dp))
                        }
                    }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete activity?") }, text = { Text("This cardio entry will be removed from your log.") }, confirmButton = {
            TextButton(onClick = { deleteId = null; scope.launch { cardio.delete(id) } }) { Text("Delete") }
        }, dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } })
    }
}

private fun Double.pretty() = if (this % 1.0 == 0.0) roundToInt().toString() else "%.1f".format(this)

@Composable
private fun RoutePreview(points: List<RoutePoint>, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)
        val inset = 12.dp.toPx()
        val width = (size.width - inset * 2).coerceAtLeast(1f)
        val height = (size.height - inset * 2).coerceAtLeast(1f)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = inset + ((point.longitude - minLon) / lonRange).toFloat() * width
            val y = inset + (1f - ((point.latitude - minLat) / latRange).toFloat()) * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, palette.accent, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        val first = points.first()
        val last = points.last()
        val startX = inset + ((first.longitude - minLon) / lonRange).toFloat() * width
        val startY = inset + (1f - ((first.latitude - minLat) / latRange).toFloat()) * height
        val endX = inset + ((last.longitude - minLon) / lonRange).toFloat() * width
        val endY = inset + (1f - ((last.latitude - minLat) / latRange).toFloat()) * height
        drawCircle(palette.ink, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(startX, startY))
        drawCircle(palette.accent, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(endX, endY))
    }
}
