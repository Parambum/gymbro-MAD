package com.gymbro.mad.feature.train

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.Exercise
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.theme.LocalPalette

@Composable
fun ExercisePickerScreen(
    modifier: Modifier = Modifier,
    onPicked: (exerciseId: String) -> Unit = {},
) {
    val palette = LocalPalette.current
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    val results = ExerciseCatalog.search(query, selectedGroup)

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 12.dp)) {
        Text(
            "Choose a movement",
            color = palette.ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search exercises") },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GymBroChip(
                label = "All",
                selected = selectedGroup == null,
                onClick = { selectedGroup = null },
            )
            ExerciseCatalog.groups.forEach { group ->
                GymBroChip(
                    label = group.displayName,
                    selected = selectedGroup == group.slug,
                    onClick = {
                        selectedGroup = if (selectedGroup == group.slug) null else group.slug
                    },
                    accent = Color(group.accentArgb.toInt()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty()) {
            EmptyState(
                title = "No matching exercises",
                body = if (query.isBlank()) {
                    "No movements are available for this muscle group."
                } else {
                    "Nothing matched “$query”. Try another name or group."
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = Exercise::id) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onPicked(exercise.id) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val group = ExerciseCatalog.group(exercise.groupSlug)
    GymBroCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(group?.accentArgb?.let { Color(it.toInt()) } ?: palette.faint),
            )
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(exercise.name, color = palette.ink, style = MaterialTheme.typography.titleSmall)
                MicroLabel(group?.displayName ?: exercise.groupSlug, color = palette.muted)
            }
        }
    }
}
