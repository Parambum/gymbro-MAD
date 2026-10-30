package com.gymbro.mad.feature.train

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymbro.mad.data.model.PersonalBest
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.LocalWeightUnit
import java.time.Duration
import kotlin.math.roundToInt

@Composable
fun WorkoutSummaryScreen(
    sessionId: String,
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
    onDone: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current
    val session = workouts.sessions.value.firstOrNull { it.id == sessionId }
    if (session == null) {
        EmptyState("Summary unavailable", "This workout is no longer in your history.", modifier.fillMaxSize())
        return
    }
    val previous = workouts.sessions.value
        .filter { it.id != session.id && it.title.equals(session.title, ignoreCase = true) && it.startedAt < session.startedAt }
        .maxByOrNull { it.startedAt }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Session complete", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
        Text(session.title, style = MaterialTheme.typography.titleMedium, color = palette.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("volume", weightUnit.fromKilograms(session.volumeKg).roundToInt().toString(), Modifier.weight(1f), unit = weightUnit.suffix)
            StatTile("sets", session.completedSets.toString(), Modifier.weight(1f))
            StatTile("duration", durationLabel(session.duration), Modifier.weight(1f))
        }
        SectionHeader("volume by muscle group")
        val groups = session.exercises.filter { it.completedSets > 0 }.groupBy { it.groupSlug }
        if (groups.isEmpty()) EmptyState("No completed sets", "No muscle-group volume was recorded.")
        groups.forEach { (slug, exercises) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ExerciseCatalog.group(slug)?.displayName ?: slug, color = palette.ink)
                Text("${weightUnit.fromKilograms(exercises.sumOf { it.volumeKg }).roundToInt()} ${weightUnit.suffix}", color = palette.muted)
            }
        }
        SectionHeader("estimated 1RM")
        val estimates = session.exercises.mapNotNull { exercise ->
            exercise.sets.filter { it.completed && it.reps in 1..10 }
                .maxByOrNull { PersonalBest(exercise.exerciseId, exercise.name, it.weightKg, it.reps, session.date).estimated1RmKg }
                ?.let { exercise to it }
        }
        if (estimates.isEmpty()) Text("No sets at 10 reps or fewer to estimate.", color = palette.muted)
        estimates.forEach { (exercise, set) ->
            GymBroCard {
                Text(exercise.name, color = palette.ink, style = MaterialTheme.typography.titleSmall)
                val estimated = PersonalBest(exercise.exerciseId, exercise.name, set.weightKg, set.reps, session.date).estimated1RmKg
                MicroLabel("${weightUnit.format(set.weightKg)} ${weightUnit.suffix} × ${set.reps} · estimated 1RM ${weightUnit.format(estimated)} ${weightUnit.suffix}", color = palette.muted)
            }
        }
        SectionHeader("personal bests")
        val personalBests = session.exercises.mapNotNull { exercise ->
            val set = exercise.sets.filter { it.completed }.maxByOrNull { it.weightKg } ?: return@mapNotNull null
            val previousBest = workouts.sessions.value.asSequence()
                .filter { it.id != session.id && it.startedAt < session.startedAt }
                .flatMap { it.exercises.asSequence() }
                .filter { it.exerciseId == exercise.exerciseId }
                .flatMap { it.sets.asSequence() }
                .filter { it.completed }
                .maxOfOrNull { it.weightKg }
            if (previousBest == null || set.weightKg > previousBest) exercise to set else null
        }
        if (personalBests.isEmpty()) Text("No personal best weight was set this time.", color = palette.muted)
        personalBests.forEach { (exercise, set) ->
            Text("${exercise.name}: ${weightUnit.format(set.weightKg)} ${weightUnit.suffix} × ${set.reps}", color = palette.ink)
        }
        previous?.let {
            val change = if (it.volumeKg > 0) (((session.volumeKg - it.volumeKg) / it.volumeKg * 1000).roundToInt() / 10.0) else null
            if (change != null && change != 0.0) Text(
                "${if (change > 0) "+" else ""}${"%.1f".format(change)}% volume vs your previous ${session.title} session",
                color = palette.muted,
            ) else if (change == null && session.volumeKg > it.volumeKg) {
                Text("Volume increased from 0 kg in your previous ${session.title} session.", color = palette.muted)
            }
        }
        Spacer(Modifier.height(4.dp))
        AccentButton("Done", onDone, Modifier.fillMaxWidth())
    }
}

private fun Double.pretty() = if (this % 1.0 == 0.0) roundToInt().toString() else "%.1f".format(this)
private fun durationLabel(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(0)
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) "${hours}h ${remainder}m" else "${remainder}m"
}
