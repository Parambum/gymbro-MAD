package com.gymbro.mad.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.ThemeId
import com.gymbro.mad.ui.theme.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ProfileScreen(
    themeId: ThemeId,
    onThemeChange: (ThemeId) -> Unit,
    weightUnit: WeightUnit,
    onWeightUnitChange: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val body = exportJson()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(body) } }
        }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val body = exportCsv()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(body) } }
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader("theme")
        Spacer(Modifier.height(10.dp))
        GymBroCard {
            Text("Three palettes, same app", style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Spacer(Modifier.height(4.dp))
            Text("Choose the palette that feels right. Your choice is saved on this device.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeId.entries.forEach { id ->
                    GymBroChip(label = id.label, selected = id == themeId, onClick = { onThemeChange(id) })
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader("weight unit")
        Spacer(Modifier.height(10.dp))
        GymBroCard {
            Text("Logged weights stay stored in kilograms", style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Spacer(Modifier.height(4.dp))
            Text("Only the values shown to you change when you switch units.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightUnit.entries.forEach { unit ->
                    GymBroChip(label = unit.label, selected = unit == weightUnit, onClick = { onWeightUnitChange(unit) })
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader("your data")
        Spacer(Modifier.height(10.dp))
        GymBroCard {
            Text("Take your training log with you", style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Spacer(Modifier.height(4.dp))
            Text("Export completed workouts and their sets as JSON or CSV.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentButton("Export JSON", onClick = { jsonExport.launch("gymbro-workouts.json") }, modifier = Modifier.weight(1f))
                AccentButton("Export CSV", onClick = { csvExport.launch("gymbro-workouts.csv") }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun exportJson(): String = JSONArray().apply {
    Repos.workouts.sessions.value.forEach { session ->
        put(JSONObject()
            .put("id", session.id).put("title", session.title)
            .put("startedAt", session.startedAt.toString()).put("finishedAt", session.finishedAt?.toString())
            .put("exercises", JSONArray().apply {
                session.exercises.forEach { exercise ->
                    put(JSONObject().put("exerciseId", exercise.exerciseId).put("name", exercise.name).put("group", exercise.groupSlug)
                        .put("sets", JSONArray().apply { exercise.sets.forEach { set ->
                            put(JSONObject().put("id", set.id).put("weightKg", set.weightKg).put("reps", set.reps).put("rpe", set.rpe).put("completed", set.completed))
                        } }))
                }
            }))
    }
}.toString(2)

private fun exportCsv(): String = buildString {
    appendLine("session_id,session_title,started_at,exercise,group,weight_kg,reps,rpe,completed")
    Repos.workouts.sessions.value.forEach { session ->
        session.exercises.forEach { exercise ->
            exercise.sets.forEach { set ->
                appendLine(listOf(session.id, session.title, session.startedAt.toString(), exercise.name, exercise.groupSlug,
                    set.weightKg.toString(), set.reps.toString(), set.rpe?.toString().orEmpty(), set.completed.toString())
                    .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
            }
        }
    }
}