package com.gymbro.mad.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.LoggedExercise
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SessionDetailScreen(
    sessionId: String,
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
) {
    val sessions by workouts.sessions.collectAsStateWithLifecycle()
    val activeSession by workouts.activeSession.collectAsStateWithLifecycle()
    val session = (sessions + listOfNotNull(activeSession)).firstOrNull { it.id == sessionId }

    if (session == null) {
        EmptyState(
            title = "Session not found",
            body = "This workout may have been removed. Your other sessions are still in History.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val palette = LocalPalette.current
    val recordSets = sessionRecordSets(session, sessions)
    val muscleVolumes = session.exercises
        .groupBy { it.groupSlug }
        .mapValues { (_, exercises) ->
            exercises.sumOf { it.volumeKg } to exercises.sumOf { it.completedSets }
        }
        .filterValues { it.second > 0 }
        .toList()
        .sortedByDescending { it.second.first }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        Text(
            text = session.title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.ink,
        )
        Spacer(Modifier.height(4.dp))
        MicroLabel(
            text = session.startedAt.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · h:mm a")),
            color = palette.muted,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("duration", formatDuration(session.duration), Modifier.weight(1f))
            StatTile("volume", "${session.volumeKg.roundToInt()}", Modifier.weight(1f), unit = "kg")
            StatTile("sets", "${session.completedSets}", Modifier.weight(1f))
        }

        if (muscleVolumes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("volume by muscle")
            Spacer(Modifier.height(10.dp))
            muscleVolumes.forEach { (slug, totals) ->
                val group = ExerciseCatalog.group(slug)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group?.displayName ?: slug,
                        color = palette.muted,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${totals.first.roundToInt()} kg · ${totals.second} sets",
                        color = group?.accentArgb?.let { Color(it.toInt()) } ?: palette.ink,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("exercises")
        Spacer(Modifier.height(10.dp))
        if (session.exercises.isEmpty()) {
            EmptyState(
                title = "No exercises in this session",
                body = "This session has no exercise entries.",
            )
        } else {
            session.exercises.forEach { exercise ->
                ExerciseDetailCard(
                    exercise = exercise,
                    recordSetIds = recordSets[exercise.exerciseId].orEmpty(),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ExerciseDetailCard(exercise: LoggedExercise, recordSetIds: Set<String>) {
    val palette = LocalPalette.current
    val group = ExerciseCatalog.group(exercise.groupSlug)
    GymBroCard(
        accent = group?.accentArgb?.let { Color(it.toInt()) } ?: palette.accent,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    color = palette.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                MicroLabel(group?.displayName ?: exercise.groupSlug, color = palette.muted)
            }
            Text(
                "${exercise.volumeKg.roundToInt()} kg",
                color = palette.accentInk,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (exercise.sets.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            MicroLabel("no sets recorded", color = palette.faint)
        } else {
            Spacer(Modifier.height(10.dp))
            exercise.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(palette.raised)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MicroLabel("set ${index + 1}", modifier = Modifier.weight(1f))
                    Text(
                        text = if (set.completed) {
                            "${formatWeight(set.weightKg)} kg × ${set.reps}"
                        } else {
                            "Skipped"
                        },
                        color = if (set.completed) palette.ink else palette.faint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    set.rpe?.let {
                        Text(
                            text = " · RPE ${formatWeight(it)}",
                            color = palette.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (set.id in recordSetIds) {
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = "Personal record",
                            tint = palette.accent2,
                        )
                    }
                }
            }
        }
    }
}

private fun sessionRecordSets(
    session: WorkoutSession,
    allSessions: List<WorkoutSession>,
): Map<String, Set<String>> = session.exercises.associate { exercise ->
    val previousBest = allSessions
        .filter { it.startedAt < session.startedAt }
        .flatMap { it.exercises }
        .filter { it.exerciseId == exercise.exerciseId }
        .flatMap { it.sets }
        .filter { it.completed }
        .maxOfOrNull { it.weightKg } ?: Double.NEGATIVE_INFINITY
    var bestSoFar = previousBest
    val records = buildSet {
        exercise.sets.filter { it.completed }.forEach { set ->
            if (set.weightKg > bestSoFar) {
                add(set.id)
                bestSoFar = set.weightKg
            }
        }
    }
    exercise.exerciseId to records
}

private fun formatDuration(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
