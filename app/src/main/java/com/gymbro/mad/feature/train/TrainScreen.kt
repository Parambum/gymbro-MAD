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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.model.PersonalBest
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
import com.gymbro.mad.ui.theme.LocalWeightUnit
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
    onOpenSummary: (String) -> Unit = {},
) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current
    val trainViewModel: TrainViewModel = viewModel(
        factory = TrainViewModelFactory(workouts),
    )
    val active by trainViewModel.activeSession.collectAsStateWithLifecycle()
    val currentSession = active
    val sessions by trainViewModel.sessions.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("Workout") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showIncompleteFinishDialog by remember { mutableStateOf(false) }
    var elapsedNow by remember { mutableStateOf(Instant.now()) }
    val context = LocalContext.current
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var restEndAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var restTotalSeconds by rememberSaveable { mutableStateOf(90) }
    val trainScrollState = rememberScrollState()

    LaunchedEffect(currentSession?.id) {
        while (currentSession != null) {
            elapsedNow = Instant.now()
            delay(1_000)
        }
    }

    LaunchedEffect(selectedExerciseId, currentSession?.id) {
        val exercise = selectedExerciseId?.let(ExerciseCatalog::exercise)
        if (exercise != null && currentSession != null) {
            trainViewModel.addExercise(currentSession.id, exercise)
            withFrameNanos { }
            trainScrollState.animateScrollTo(trainScrollState.maxValue)
        }
        if (selectedExerciseId != null) onExerciseConsumed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(trainScrollState)
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
                    scope.launch { trainViewModel.start(title.ifBlank { "Workout" }.trim()) }
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
                        StatTile("volume", "${weightUnit.fromKilograms(last.volumeKg).roundToInt()}", Modifier.weight(1f), unit = weightUnit.suffix)
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
                    StatTile("volume", "${weightUnit.fromKilograms(session.volumeKg).roundToInt()}", Modifier.weight(1f), unit = weightUnit.suffix)
                    StatTile("sets", "${session.completedSets}", Modifier.weight(1f))
                }
            }
            if (restEndAt != null) {
                val remaining = ((restEndAt!! - elapsedNow.toEpochMilli()) / 1000L).coerceAtLeast(0).toInt()
                if (remaining > 0) {
                    Text("Rest ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}", color = palette.ink)
                    com.gymbro.mad.ui.components.MeterBar(
                        progress = 1f - remaining / restTotalSeconds.coerceAtLeast(1).toFloat(),
                    )
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
                        if (session.completedSets == 0) {
                            showIncompleteFinishDialog = true
                        } else {
                            scope.launch {
                            trainViewModel.finish(session.id)
                                onOpenSummary(session.id)
                            }
                        }
                    },
                )
            }
            MicroLabel(
                "Only sets marked ✓ are saved when you finish.",
                color = palette.muted,
            )
            session.exercises.forEach { exercise ->
                WorkoutExerciseCard(
                    exercise = exercise,
                    session = session,
                    workouts = workouts,
                    onSetCompleted = { name, seconds ->
                        restTotalSeconds = seconds
                        restEndAt = System.currentTimeMillis() + seconds * 1_000L
                        val startNotification = {
                            scheduleRestNotification(context, name.hashCode(), seconds, name)
                        }
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            val preferences = context.getSharedPreferences("workout_rest", android.content.Context.MODE_PRIVATE)
                            val firstRequest = !preferences.getBoolean("notification_requested", false)
                            preferences.edit().putBoolean("notification_requested", true).apply()
                            if (firstRequest) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            startNotification()
                        } else startNotification()
                    },
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
    if (showIncompleteFinishDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteFinishDialog = false },
            title = { Text("No completed sets yet") },
            text = { Text("Your workout is still here. Mark at least one set complete with the circle beside it, then tap Finish to save the session.") },
            confirmButton = {
                TextButton(onClick = { showIncompleteFinishDialog = false }) {
                    Text("Back to workout")
                }
            },
        )
    }
    if (showDiscardDialog && discardSessionId != null) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this workout?") },
            text = { Text("Its exercises and sets will not be added to History.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    scope.launch { trainViewModel.discard(discardSessionId) }
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
    onSetCompleted: (String, Int) -> Unit,
) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val group = ExerciseCatalog.group(exercise.groupSlug)
    var restSeconds by rememberSaveable(exercise.exerciseId) { mutableStateOf(90) }
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
        ExerciseDemo(exercise.name, group?.accentArgb?.let { androidx.compose.ui.graphics.Color(it.toInt()) } ?: palette.accent)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroLabel("rest seconds")
            OutlinedTextField(
                value = restSeconds.toString(),
                onValueChange = { it.toIntOrNull()?.takeIf { n -> n in 15..600 }?.let { restSeconds = it } },
                modifier = Modifier.width(92.dp).height(54.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MicroLabel("set", Modifier.weight(0.7f))
            MicroLabel(weightUnit.suffix, Modifier.weight(1f))
            MicroLabel("reps", Modifier.weight(1f))
            Spacer(Modifier.width(42.dp))
        }
        exercise.sets.forEachIndexed { index, set ->
            var visible by remember(set.id) { mutableStateOf(true) }
            AnimatedVisibility(visible, exit = shrinkVertically(animationSpec = tween(180)) + fadeOut()) {
                SetLoggerRow(
                    set = set,
                    index = index + 1,
                    canComplete = set.weightKg >= 0.0 && set.reps in 1..30,
                    onChange = { updated ->
                        scope.launch { workouts.putSet(session.id, exercise.exerciseId, updated) }
                        if (!set.completed && updated.completed) onSetCompleted(exercise.name, restSeconds)
                    },
                    onDelete = {
                        visible = false
                        scope.launch {
                            kotlinx.coroutines.delay(180)
                            workouts.deleteSet(session.id, exercise.exerciseId, set.id)
                        }
                    },
                )
            }
        }
        val best = exercise.sets.filter { it.completed && it.reps in 1..10 }.maxByOrNull {
            PersonalBest(exercise.exerciseId, exercise.name, it.weightKg, it.reps, session.date).estimated1RmKg
        }
        if (best != null && best.reps > 0) {
            MicroLabel("estimated 1RM · ${weightUnit.format(PersonalBest(exercise.exerciseId, exercise.name, best.weightKg, best.reps, session.date).estimated1RmKg)} ${weightUnit.suffix}", color = palette.muted)
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
    val weightUnit = LocalWeightUnit.current
    var weightText by remember(set.id, weightUnit) { mutableStateOf(weightUnit.format(set.weightKg)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    var rpeText by remember(set.id) { mutableStateOf(set.rpe?.let(::formatWeight).orEmpty()) }
    val weightValid = weightText.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..1000.0 } == true
    val repsValid = repsText.toIntOrNull()?.let { it in 1..30 } == true
    val rpeValid = rpeText.isBlank() || rpeText.toDoubleOrNull()?.let { it in 1.0..10.0 } == true
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        MicroLabel("$index", Modifier.weight(0.35f))
        OutlinedTextField(
            value = weightText,
            isError = !weightValid,
            onValueChange = { value ->
                weightText = value
                value.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..1000.0 }?.let {
                    onChange(set.copy(weightKg = weightUnit.toKilograms(it)))
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
            isError = !repsValid,
            onValueChange = { value ->
                repsText = value
                value.toIntOrNull()?.takeIf { it in 1..30 }?.let {
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
            enabled = set.completed || (canComplete && weightValid && repsValid && rpeValid),
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
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(32.dp))
        OutlinedTextField(
            value = rpeText,
            isError = !rpeValid,
            onValueChange = { value ->
                rpeText = value
                if (value.isBlank()) onChange(set.copy(rpe = null))
                else value.toDoubleOrNull()?.takeIf { it in 1.0..10.0 }?.let { onChange(set.copy(rpe = it)) }
            },
            label = { Text("RPE") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(92.dp).height(54.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        )
        MicroLabel("optional RPE", Modifier.padding(start = 8.dp))
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
