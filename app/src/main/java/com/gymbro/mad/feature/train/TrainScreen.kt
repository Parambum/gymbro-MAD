package com.gymbro.mad.feature.train

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun TrainScreen(
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
    onPickExercise: () -> Unit = {},
    selectedExerciseId: String? = null,
    onExerciseConsumed: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val active by workouts.activeSession.collectAsStateWithLifecycle()
    val currentSession = active
    val sessions by workouts.sessions.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("Workout") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var elapsedNow by remember { mutableStateOf(Instant.now()) }
    var savedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(currentSession?.id) {
        while (currentSession != null) {
            elapsedNow = Instant.now()
            delay(1_000)
        }
    }

    LaunchedEffect(selectedExerciseId, currentSession?.id) {
        val exercise = selectedExerciseId?.let(ExerciseCatalog::exercise)
        if (exercise != null && currentSession != null) {
            workouts.addExercise(currentSession.id, exercise)
        }
        if (selectedExerciseId != null) onExerciseConsumed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (currentSession == null) "Train" else currentSession.title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.ink,
        )

        if (currentSession == null) {
            if (savedMessage) {
                GymBroCard(accent = palette.accent) {
                    Text(
                        "Session saved",
                        color = palette.ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Your completed workout is now in History and Stats.",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Session name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AccentButton(
                text = "Start workout",
                icon = Icons.Filled.PlayArrow,
                onClick = {
                    savedMessage = false
                    scope.launch { workouts.startSession(title.ifBlank { "Workout" }.trim()) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SectionHeader("last session")
            val last = sessions.firstOrNull()
            if (last == null) {
                EmptyState(
                    title = "No previous workouts",
                    body = "Start a session and log completed sets. Your history and stats grow from what you record.",
                )
            } else {
                GymBroCard {
                    Text(last.title, color = palette.ink, style = MaterialTheme.typography.titleMedium)
                    MicroLabel(last.date.toString(), color = palette.muted)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("volume", "${last.volumeKg.roundToInt()}", Modifier.weight(1f), unit = "kg")
                        StatTile("sets", "${last.completedSets}", Modifier.weight(1f))
                    }
                }
            }
        } else {
            val session = checkNotNull(currentSession)
            GymBroCard(accent = palette.accent) {
                MicroLabel("in progress", color = palette.accentInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        "duration",
                        formatDuration(Duration.between(session.startedAt, elapsedNow)),
                        Modifier.weight(1f),
                    )
                    StatTile("volume", "${session.volumeKg.roundToInt()}", Modifier.weight(1f), unit = "kg")
                    StatTile("sets", "${session.completedSets}", Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccentButton(
                    text = "Add exercise",
                    icon = Icons.Filled.Add,
                    onClick = onPickExercise,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = "Finish",
                    onClick = {
                        scope.launch {
                            workouts.finishSession(session.id)
                            savedMessage = true
                        }
                    },
                )
            }
            session.exercises.forEach { exercise ->
                WorkoutExerciseCard(
                    exercise = exercise,
                    session = session,
                    workouts = workouts,
                )
            }
            if (session.exercises.isEmpty()) {
                EmptyState(
                    title = "Add your first exercise",
                    body = "Choose a movement from the exercise catalog to start logging sets.",
                )
            }
            GhostButton(
                text = "Discard workout",
                icon = Icons.Filled.Delete,
                tint = palette.danger,
                onClick = { showDiscardDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val discardSessionId = currentSession?.id
    if (showDiscardDialog && discardSessionId != null) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this workout?") },
            text = { Text("Its exercises and sets will not be added to History.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    scope.launch { workouts.discardSession(discardSessionId) }
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep workout")
                }
            },
        )
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: com.gymbro.mad.data.model.LoggedExercise,
    session: WorkoutSession,
    workouts: WorkoutRepository,
) {
    val palette = LocalPalette.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val group = ExerciseCatalog.group(exercise.groupSlug)
    GymBroCard(accent = group?.accentArgb?.let { androidx.compose.ui.graphics.Color(it.toInt()) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, color = palette.ink, style = MaterialTheme.typography.titleMedium)
                MicroLabel(group?.displayName ?: exercise.groupSlug, color = palette.muted)
            }
            IconButton(onClick = {
                scope.launch { workouts.removeExercise(session.id, exercise.exerciseId) }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove exercise", tint = palette.faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MicroLabel("set", Modifier.weight(0.7f))
            MicroLabel("kg", Modifier.weight(1f))
            MicroLabel("reps", Modifier.weight(1f))
            Spacer(Modifier.width(42.dp))
        }
        exercise.sets.forEachIndexed { index, set ->
            SetLoggerRow(
                set = set,
                index = index + 1,
                canComplete = set.weightKg >= 0.0 && set.reps in 1..30,
                onChange = { updated ->
                    scope.launch { workouts.putSet(session.id, exercise.exerciseId, updated) }
                },
                onDelete = {
                    scope.launch { workouts.deleteSet(session.id, exercise.exerciseId, set.id) }
                },
            )
        }
        GhostButton(
            text = "Add set",
            icon = Icons.Filled.Add,
            onClick = {
                val previous = exercise.sets.lastOrNull()
                scope.launch {
                    workouts.putSet(
                        session.id,
                        exercise.exerciseId,
                        SetEntry(
                            weightKg = previous?.weightKg ?: 0.0,
                            reps = previous?.reps ?: 0,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SetLoggerRow(
    set: SetEntry,
    index: Int,
    canComplete: Boolean,
    onChange: (SetEntry) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    var weightText by remember(set.id) { mutableStateOf(formatWeight(set.weightKg)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicroLabel("$index", Modifier.weight(0.7f))
        OutlinedTextField(
            value = weightText,
            onValueChange = { value ->
                weightText = value
                value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                    onChange(set.copy(weightKg = it))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).height(54.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = repsText,
            onValueChange = { value ->
                repsText = value
                value.toIntOrNull()?.takeIf { it in 0..30 }?.let {
                    onChange(set.copy(reps = it))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(54.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        IconButton(
            onClick = { onChange(set.copy(completed = !set.completed)) },
            enabled = canComplete,
        ) {
            Text(
                text = if (set.completed) "✓" else "○",
                color = if (set.completed) palette.accentInk else palette.faint,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete set", tint = palette.faint)
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
