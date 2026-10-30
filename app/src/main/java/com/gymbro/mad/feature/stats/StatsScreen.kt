package com.gymbro.mad.feature.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.model.PersonalBest
import com.gymbro.mad.data.model.WeekVolume
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.AnalyticsRepository
import com.gymbro.mad.data.repo.FuelRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MeterBar
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.ProgressionChart
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.components.WeeklyVolumeChart
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.LocalWeightUnit
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
    analytics: AnalyticsRepository = Repos.analytics,
    fuel: FuelRepository = Repos.fuel,
) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current
    val sessions by workouts.sessions.collectAsStateWithLifecycle()
    val bests by analytics.personalBests().collectAsStateWithLifecycle(initialValue = emptyList())
    val weights by fuel.weights.collectAsStateWithLifecycle()
    var windowWeeks by remember { mutableIntStateOf(8) }
    val windowBests = bests.filter { it.achievedOn >= cutoffDate(windowWeeks) }
    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    var selectedProgressIndex by remember { mutableIntStateOf(0) }
    var selectedBodyWeightIndex by remember { mutableIntStateOf(0) }

    val weeks by analytics.weeklyVolume(windowWeeks)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val muscleVolumes by analytics.muscleVolume(windowWeeks * 7)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val today = LocalDate.now()
    val cutoff = today.minusDays(windowWeeks * 7L - 1)
    val recentSessions = sessions.filter { it.date >= cutoff }
    val volume = weeks.sumOf(WeekVolume::volumeKg)
    val totalDuration = recentSessions.sumOf { it.duration.seconds }
    val averageWorkouts = recentSessions.size.toDouble() / windowWeeks
    val selectedWeek = weeks.getOrNull(
        selectedWeekIndex.coerceIn(0, (weeks.size - 1).coerceAtLeast(0)),
    )

    val exerciseIds = sessions
        .sortedByDescending { it.startedAt }
        .flatMap { session -> session.exercises.map { it.exerciseId } }
        .distinct()
        .filter { id ->
            sessions.count { session ->
                session.exercises.any { exercise ->
                    exercise.exerciseId == id && exercise.sets.any { it.completed }
                }
            } >= 2
        }
        .take(12)
    val activeExerciseId = selectedExerciseId?.takeIf { it in exerciseIds } ?: exerciseIds.firstOrNull()
    val progression = activeExerciseId?.let { id ->
        sessions.sortedBy { it.startedAt }.mapNotNull { session ->
            val exercise = session.exercises.firstOrNull { it.exerciseId == id } ?: return@mapNotNull null
            val bestSet = exercise.sets.filter { it.completed }.maxByOrNull { it.weightKg }
                ?: return@mapNotNull null
            ProgressPoint(session, bestSet.weightKg, bestSet.reps)
        }
    }.orEmpty()
    val selectedProgress = progression.getOrNull(
        selectedProgressIndex.coerceIn(0, (progression.size - 1).coerceAtLeast(0)),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Your training, in numbers.",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.ink,
        )
        MicroLabel(
            text = "Based on completed sets in your logged sessions",
            color = palette.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 8, 12).forEach { weeksCount ->
                GymBroChip(
                    label = "$weeksCount weeks",
                    selected = windowWeeks == weeksCount,
                    onClick = {
                        windowWeeks = weeksCount
                        selectedWeekIndex = 0
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("all-time workouts", "${sessions.size}", Modifier.weight(1f))
            StatTile(
                "weekly average",
                "%.1f".format(averageWorkouts),
                Modifier.weight(1f),
                unit = "sessions",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("window volume", "${weightUnit.fromKilograms(volume).roundToInt()}", Modifier.weight(1f), unit = weightUnit.suffix)
            StatTile("window time", formatDuration(totalDuration), Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("weekly volume")
        if (recentSessions.size < 3) {
            EmptyState(
                title = "A little more training makes a trend",
                body = "You've logged ${recentSessions.size} sessions in this window. " +
                    "The chart appears after three; these totals already reflect your real sessions.",
            )
            WeekVolumeSummary(weeks, weightUnit)
        } else if (weeks.isNotEmpty()) {
            selectedWeek?.let {
                MicroLabel(
                    text = "${it.weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} · " +
                        "${weightUnit.fromKilograms(it.volumeKg).roundToInt()} ${weightUnit.suffix} · ${it.sessions} sessions",
                    color = palette.accentInk,
                )
            }
            WeeklyVolumeChart(
                weeks = weeks,
                selectedIndex = selectedWeekIndex.coerceIn(0, weeks.lastIndex),
                onSelect = { selectedWeekIndex = it },
            )
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("volume by muscle")
        if (muscleVolumes.isEmpty()) {
            EmptyState(
                title = "No completed sets in this window",
                body = "Log a workout with completed sets and the muscle breakdown will appear here.",
            )
        } else {
            val maxMuscleVolume = muscleVolumes.maxOf { it.volumeKg }.coerceAtLeast(1.0)
            muscleVolumes.forEach { volumeByMuscle ->
                val group = ExerciseCatalog.group(volumeByMuscle.groupSlug)
                val accent = group?.accentArgb?.let { Color(it.toInt()) } ?: palette.accent
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            group?.displayName ?: volumeByMuscle.groupSlug,
                            color = palette.ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        MicroLabel(
                            "${weightUnit.fromKilograms(volumeByMuscle.volumeKg).roundToInt()} ${weightUnit.suffix} · ${volumeByMuscle.sets} sets",
                            color = palette.muted,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    MeterBar(
                        progress = (volumeByMuscle.volumeKg / maxMuscleVolume).toFloat(),
                        color = accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("exercise progression")
        if (progression.size < 2) {
            EmptyState(
                title = "Progress shows up with repeat sessions",
                body = "Log the same exercise in at least two sessions to see its heaviest completed set over time.",
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                exerciseIds.forEach { id ->
                    val exercise = ExerciseCatalog.exercise(id)
                    GymBroChip(
                        label = exercise?.name ?: id,
                        selected = activeExerciseId == id,
                        onClick = {
                            selectedExerciseId = id
                            selectedProgressIndex = progression.lastIndex
                        },
                    )
                }
            }
            selectedProgress?.let { point ->
                MicroLabel(
                    text = "${point.session.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · " +
                        "${weightUnit.format(point.weightKg)} ${weightUnit.suffix} × ${point.reps}",
                    color = palette.accentInk,
                )
                ProgressionChart(
                    values = progression.map { weightUnit.fromKilograms(it.weightKg) },
                    selectedIndex = selectedProgressIndex.coerceIn(0, progression.lastIndex),
                    onSelect = { selectedProgressIndex = it },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("personal bests")
        if (windowBests.isEmpty()) {
            EmptyState(
                title = "No personal bests yet",
                body = "Complete a set in Train. Your heaviest set per exercise will be listed here.",
            )
        } else {
            windowBests.forEach { best -> PersonalBestCard(best) }
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader("body weight")
        if (weights.size < 2) {
            EmptyState(
                title = "No weight trend yet",
                body = if (weights.isEmpty()) {
                    "Add weigh-ins in Fuel. Once there are two, this chart will show the actual trend."
                } else {
                    "One weigh-in is recorded. Add another on a different day to show a trend."
                },
            )
        } else {
            val orderedWeights = weights.sortedBy { it.day }
            val selectedWeightIndex = selectedBodyWeightIndex.coerceIn(orderedWeights.indices)
            val selectedWeight = orderedWeights[selectedWeightIndex]
            MicroLabel(
                text = "${selectedWeight.day.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · " +
                    "${weightUnit.format(selectedWeight.kg)} ${weightUnit.suffix}",
                color = palette.accentInk,
            )
            ProgressionChart(
                values = orderedWeights.map { weightUnit.fromKilograms(it.kg) },
                selectedIndex = selectedWeightIndex,
                onSelect = { selectedBodyWeightIndex = it },
            )
        }
    }
}

private data class ProgressPoint(
    val session: WorkoutSession,
    val weightKg: Double,
    val reps: Int,
)

private fun cutoffDate(windowWeeks: Int): LocalDate =
    LocalDate.now().minusDays(windowWeeks * 7L - 1)

@Composable
private fun WeekVolumeSummary(weeks: List<WeekVolume>, weightUnit: com.gymbro.mad.ui.theme.WeightUnit) {
    val palette = LocalPalette.current
    weeks.takeLast(4).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MicroLabel(
                week.weekStart.format(DateTimeFormatter.ofPattern("d MMM")),
                color = palette.muted,
            )
            MicroLabel(
                "${weightUnit.fromKilograms(week.volumeKg).roundToInt()} ${weightUnit.suffix} · ${week.sessions} sessions",
                color = palette.faint,
            )
        }
    }
}

@Composable
private fun PersonalBestCard(best: PersonalBest) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current
    GymBroCard(accent = palette.accent2) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(
                    best.exerciseName,
                    color = palette.ink,
                    style = MaterialTheme.typography.titleSmall,
                )
                MicroLabel(
                    "${best.achievedOn.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · " +
                        "${weightUnit.format(best.weightKg)} ${weightUnit.suffix} × ${best.reps}",
                    color = palette.muted,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    if (best.reps <= 10) "${weightUnit.format(best.estimated1RmKg)} ${weightUnit.suffix}"
                    else "${weightUnit.format(best.weightKg)} ${weightUnit.suffix}",
                    color = palette.accentInk,
                    style = MaterialTheme.typography.titleMedium,
                )
                MicroLabel(
                    if (best.reps <= 10) "est. 1RM" else "actual · >10 reps",
                    color = palette.faint,
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val duration = Duration.ofSeconds(seconds.coerceAtLeast(0))
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
