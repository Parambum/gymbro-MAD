package com.gymbro.mad.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.CardioRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
    cardio: CardioRepository = Repos.cardio,
    onOpenSession: (String) -> Unit = {},
) {
    val palette = LocalPalette.current
    val sessions by workouts.sessions.collectAsStateWithLifecycle()
    val activities by cardio.activities.collectAsStateWithLifecycle()
    var windowDays by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val visibleSessions = sessions.filter { session ->
        when {
            selectedDay != null -> session.date == selectedDay
            windowDays != null -> session.date >= today.minusDays(windowDays!!.toLong() - 1)
            else -> true
        }
    }
    val visibleActivities = activities.filter { activity ->
        val day = activity.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        when {
            selectedDay != null -> day == selectedDay
            windowDays != null -> day >= today.minusDays(windowDays!!.toLong() - 1)
            else -> true
        }
    }
    val weeks = historyRows(visibleSessions, visibleActivities)

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = "Every session, accounted for.",
                        color = palette.ink,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    MicroLabel(
                        text = "${visibleSessions.size} strength sessions · " +
                            "${visibleActivities.size} cardio activities",
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "All", 30 to "30 days", 90 to "90 days").forEach { (days, label) ->
                            GymBroChip(
                                label = label,
                                selected = selectedDay == null && windowDays == days,
                                onClick = {
                                    windowDays = days
                                    selectedDay = null
                                },
                            )
                        }
                        GymBroChip(
                            label = selectedDay?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "Pick day",
                            selected = selectedDay != null,
                            onClick = { showDatePicker = true },
                        )
                    }
                }
            }

            if (weeks.isEmpty()) {
                item {
                    EmptyState(
                        title = "No training in this view",
                        body = if (selectedDay != null || windowDays != null) {
                            "Try a wider date range, or log a session from Train."
                        } else {
                            "Start a workout from Train. Finished sessions will appear here with their sets and totals."
                        },
                        icon = Icons.Filled.CalendarMonth,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                weeks.forEach { week ->
                    item(key = "week-${week.weekStart}") {
                        WeekHeader(week, Modifier.padding(horizontal = 16.dp))
                    }
                    items(
                        items = week.items,
                        key = HistoryItem::key,
                    ) { activity ->
                        when (activity) {
                            is HistoryItem.Strength -> WorkoutHistoryCard(
                                session = activity.session,
                                onClick = { onOpenSession(activity.session.id) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            is HistoryItem.Cardio -> CardioHistoryCard(
                                activity = activity.activity,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDay
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDay = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    windowDays = null
                    showDatePicker = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private data class WeekHistory(
    val weekStart: LocalDate,
    val items: List<HistoryItem>,
)

private sealed interface HistoryItem {
    val key: String
    val timestamp: Instant

    data class Strength(val session: WorkoutSession) : HistoryItem {
        override val key: String = "workout-${session.id}"
        override val timestamp: Instant = session.startedAt
    }

    data class Cardio(val activity: CardioActivity) : HistoryItem {
        override val key: String = "cardio-${activity.id}"
        override val timestamp: Instant = activity.startedAt
    }
}

private fun historyRows(
    sessions: List<WorkoutSession>,
    activities: List<CardioActivity>,
): List<WeekHistory> {
    val groupedSessions = sessions.groupBy {
        it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val groupedActivities = activities.groupBy {
        it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    return (groupedSessions.keys + groupedActivities.keys)
        .distinct()
        .sortedDescending()
        .map { week ->
            val weekItems = groupedSessions[week].orEmpty().map(HistoryItem::Strength) +
                groupedActivities[week].orEmpty().map(HistoryItem::Cardio)
            WeekHistory(
                weekStart = week,
                items = weekItems.sortedByDescending(HistoryItem::timestamp),
            )
        }
}

@Composable
private fun WeekHeader(week: WeekHistory, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val formatter = DateTimeFormatter.ofPattern("d MMM")
    val strengthSessions = week.items.filterIsInstance<HistoryItem.Strength>().map { it.session }
    val volume = strengthSessions.sumOf { it.volumeKg }
    Column(modifier.padding(top = 10.dp)) {
        SectionHeader(
            title = "${week.weekStart.format(formatter)} — ${week.weekStart.plusDays(6).format(formatter)}",
        )
        Spacer(Modifier.height(6.dp))
        MicroLabel(
            text = "${week.items.size} activities · " +
                "${volume.roundToInt()} kg strength volume",
            color = palette.faint,
        )
    }
}

@Composable
private fun WorkoutHistoryCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    GymBroCard(
        modifier = modifier,
        accent = palette.accent,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = palette.accentInk)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    session.title,
                    color = palette.ink,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                Text(
                    session.startedAt.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")),
                    color = palette.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
            MuscleDots(session = session)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("duration", formatDuration(session), Modifier.weight(1f))
            StatTile("volume", "${session.volumeKg.roundToInt()}", Modifier.weight(1f), unit = "kg")
            StatTile("sets", "${session.completedSets}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MuscleDots(session: WorkoutSession) {
    val groups = session.exercises
        .filter { it.completedSets > 0 }
        .map { it.groupSlug }
        .distinct()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        groups.take(5).forEach { slug ->
            val color = ExerciseCatalog.group(slug)?.accentArgb?.let { Color(it.toInt()) }
                ?: LocalPalette.current.faint
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        if (groups.size > 5) {
            Text("+${groups.size - 5}", color = LocalPalette.current.faint)
        }
    }
}

@Composable
private fun CardioHistoryCard(activity: CardioActivity, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    GymBroCard(modifier = modifier, accent = palette.info) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = palette.info,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    activity.kind.displayName,
                    color = palette.ink,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                Text(
                    activity.startedAt.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")),
                    color = palette.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
            MicroLabel("cardio", color = palette.info)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("duration", formatDuration(activity.duration.seconds), Modifier.weight(1f))
            StatTile(
                label = "distance",
                value = activity.distanceMeters?.let { "%.1f".format(it / 1000.0) } ?: "—",
                modifier = Modifier.weight(1f),
                unit = if (activity.distanceMeters != null) "km" else null,
            )
            StatTile(
                label = "avg. heart rate",
                value = activity.avgHeartRate?.toString() ?: "—",
                modifier = Modifier.weight(1f),
                unit = if (activity.avgHeartRate != null) "bpm" else null,
            )
        }
    }
}

private fun formatDuration(session: WorkoutSession): String = formatDuration(session.duration.seconds)

private fun formatDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
