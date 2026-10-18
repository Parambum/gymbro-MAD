package com.gymbro.mad.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.theme.LocalPalette
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// --- UI State & ViewModel ---

sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(val weeks: List<WeekGroup>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

data class WeekGroup(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val sessionCount: Int,
    val totalVolumeKg: Double,
    val items: List<HistoryTimelineItem>
) {
    val headerTitle: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            return "${startDate.format(formatter)} – ${endDate.format(formatter)}"
        }
}

sealed interface HistoryTimelineItem {
    val id: String
    val date: LocalDate

    data class Workout(
        override val id: String,
        override val date: LocalDate,
        val title: String,
        val durationMinutes: Int,
        val volumeKg: Double,
        val completedSets: Int,
        val muscleGroups: List<String>
    ) : HistoryTimelineItem

    data class Cardio(
        override val id: String,
        override val date: LocalDate,
        val kind: String,
        val durationMinutes: Int,
        val distanceKm: Double?,
        val avgHeartRate: Int?
    ) : HistoryTimelineItem
}

class HistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                combine(
                    Repos.workouts.sessions,
                    Repos.cardio.activities
                ) { workouts, cardio ->
                    val allItems = mutableListOf<HistoryTimelineItem>()

                    workouts.mapTo(allItems) { w ->
                        val workoutDate = w.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                        val durationMins = w.duration.toMinutes().toInt()

                        HistoryTimelineItem.Workout(
                            id = w.id,
                            date = workoutDate,
                            title = w.title,
                            durationMinutes = durationMins,
                            volumeKg = w.volumeKg,
                            completedSets = w.completedSets,
                            muscleGroups = w.exercises.map { it.groupSlug }
                        )
                    }

                    cardio.mapTo(allItems) { c ->
                        val cardioDate = c.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                        val durationMins = c.duration.toMinutes().toInt()
                        val distKm = c.distanceMeters?.let { it / 1000.0 }

                        HistoryTimelineItem.Cardio(
                            id = c.id,
                            date = cardioDate,
                            kind = c.kind.displayName,
                            durationMinutes = durationMins,
                            distanceKm = distKm,
                            avgHeartRate = c.avgHeartRate
                        )
                    }

                    val grouped = allItems.groupBy { item ->
                        item.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    }

                    val weekGroups = grouped.map { (monday, itemsList) ->
                        val sunday = monday.plusDays(6)
                        val totalVol = itemsList.filterIsInstance<HistoryTimelineItem.Workout>()
                            .sumOf { it.volumeKg }

                        WeekGroup(
                            startDate = monday,
                            endDate = sunday,
                            sessionCount = itemsList.size,
                            totalVolumeKg = totalVol,
                            items = itemsList.sortedByDescending { it.date }
                        )
                    }.sortedByDescending { it.startDate }

                    HistoryUiState.Success(weekGroups) as HistoryUiState
                }.catch { e ->
                    emit(HistoryUiState.Error(e.localizedMessage ?: "Failed to load history") as HistoryUiState)
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}

// --- Composable Screen ---

@Composable
fun HistoryScreen(
    onOpenSession: (String) -> Unit,
    viewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentAlignment = Alignment.Center
    ) {
        when (val current = state) {
            is HistoryUiState.Loading -> {
                CircularProgressIndicator(color = palette.accent)
            }
            is HistoryUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Could not load history",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted
                    )
                }
            }
            is HistoryUiState.Success -> {
                if (current.weeks.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No workouts logged yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Head over to Train to log your first session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        current.weeks.forEach { week ->
                            item(key = week.startDate.toString()) {
                                WeekHeader(week = week)
                            }
                            items(week.items, key = { it.id }) { item ->
                                when (item) {
                                    is HistoryTimelineItem.Workout -> {
                                        WorkoutHistoryRow(workout = item, onClick = { onOpenSession(item.id) })
                                    }
                                    is HistoryTimelineItem.Cardio -> {
                                        CardioHistoryRow(cardio = item, onClick = { /* no cardio detail route yet */ })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekHeader(week: WeekGroup) {
    val palette = LocalPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = week.headerTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink
                )
                Text(
                    text = "${week.sessionCount} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
            Text(
                text = "${week.totalVolumeKg.toInt()} kg total",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.accent
            )
        }
    }
}

@Composable
fun WorkoutHistoryRow(
    workout: HistoryTimelineItem.Workout,
    onClick: () -> Unit
) {
    val palette = LocalPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink
                    )
                }
                Text(
                    text = workout.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${workout.durationMinutes} mins • ${workout.volumeKg.toInt()} kg • ${workout.completedSets} sets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    workout.muscleGroups.take(4).forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardioHistoryRow(
    cardio: HistoryTimelineItem.Cardio,
    onClick: () -> Unit
) {
    val palette = LocalPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = cardio.kind,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink
                    )
                }
                Text(
                    text = cardio.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val distanceText = cardio.distanceKm?.let { "%.1f km".format(it) } ?: "—"
            val paceOrHrText = cardio.avgHeartRate?.let { "${cardio.durationMinutes} mins • $distanceText • ${it} bpm" }
                ?: "${cardio.durationMinutes} mins • $distanceText"

            Text(
                text = paceOrHrText,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
        }
    }
}