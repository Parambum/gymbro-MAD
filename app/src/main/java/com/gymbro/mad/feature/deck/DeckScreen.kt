package com.gymbro.mad.feature.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.model.WeekVolume
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.AnalyticsRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Roadmap
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MeterBar
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.LocalWeightUnit
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The Deck: where the app opens.
 *
 * This screen is also the worked example for the other five. It reads every
 * number from the repositories, it never invents one, and where there is
 * nothing to show it says so in a sentence that tells the user what to do
 * next. Copy the shape of this file, not just its components.
 */
@Composable
fun DeckScreen(
    modifier: Modifier = Modifier,
    workouts: WorkoutRepository = Repos.workouts,
    analytics: AnalyticsRepository = Repos.analytics,
    onOpenTrain: () -> Unit = {},
    onOpenFuel: () -> Unit = {},
    onOpenBody: () -> Unit = {},
    onOpenCardio: () -> Unit = {},
) {
    val palette = LocalPalette.current

    val active by workouts.activeSession.collectAsStateWithLifecycle()
    val sessions by workouts.sessions.collectAsStateWithLifecycle()
    val weeks by analytics.weeklyVolume(weeks = 1)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val thisWeek = weeks.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 28.dp),
    ) {
        Text(
            text = greeting(),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
        )
        MicroLabel(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
            color = palette.faint,
        )

        Spacer(Modifier.height(20.dp))
        SessionCard(active = active, last = sessions.firstOrNull(), onOpenTrain = onOpenTrain)

        Spacer(Modifier.height(22.dp))
        SectionHeader("this week")
        Spacer(Modifier.height(10.dp))
        WeekRow(thisWeek)

        Spacer(Modifier.height(22.dp))
        SectionHeader("jump to")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Train", Icons.Filled.FitnessCenter, Modifier.weight(1f), onOpenTrain)
            QuickAction("Fuel", Icons.Filled.LocalFireDepartment, Modifier.weight(1f), onOpenFuel)
            QuickAction("Body", Icons.Filled.AccessibilityNew, Modifier.weight(1f), onOpenBody)
            QuickAction(
                label = "Cardio",
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                modifier = Modifier.weight(1f),
                onClick = onOpenCardio,
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("build status")
        Spacer(Modifier.height(10.dp))
        BuildStatusCard()
    }
}

@Composable
private fun SessionCard(
    active: WorkoutSession?,
    last: WorkoutSession?,
    onOpenTrain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current

    if (active != null) {
        GymBroCard(modifier = modifier, accent = palette.accent) {
            MicroLabel("in progress", color = palette.accentInk)
            Spacer(Modifier.height(6.dp))
            Text(
                text = active.title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("elapsed", formatDuration(active.duration), Modifier.weight(1f))
                StatTile("sets", "${active.completedSets}", Modifier.weight(1f))
                StatTile(
                    label = "volume",
                    value = "${weightUnit.fromKilograms(active.volumeKg).roundToInt()}",
                    unit = weightUnit.suffix,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            AccentButton(
                text = "Back to the session",
                onClick = onOpenTrain,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    GymBroCard(modifier = modifier) {
        if (last == null) {
            // First run. There is nothing to summarise and nothing to fake.
            Text(
                text = "Nothing logged yet",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your first session will show up here, and on the body map as " +
                    "soon as it has a completed set in it.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
        } else {
            MicroLabel("last session")
            Spacer(Modifier.height(6.dp))
            Text(
                text = last.title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = last.date.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                style = MaterialTheme.typography.bodySmall,
                color = palette.faint,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("sets", "${last.completedSets}", Modifier.weight(1f))
                StatTile(
                    label = "volume",
                    value = "${weightUnit.fromKilograms(last.volumeKg).roundToInt()}",
                    unit = weightUnit.suffix,
                    modifier = Modifier.weight(1f),
                )
                StatTile("time", formatDuration(last.duration), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))
        AccentButton(
            text = "Start a session",
            onClick = onOpenTrain,
            icon = Icons.Filled.FitnessCenter,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeekRow(week: WeekVolume?, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val weightUnit = LocalWeightUnit.current

    if (week == null || week.sessions == 0) {
        GymBroCard(modifier = modifier) {
            Text(
                text = "No sessions this week",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Volume, sets and the weekly chart all read from logged " +
                    "sessions, so this fills itself in once you train.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
        }
        return
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("sessions", "${week.sessions}", Modifier.weight(1f))
        StatTile(
            label = "volume",
            value = "${weightUnit.fromKilograms(week.volumeKg).roundToInt()}",
            unit = weightUnit.suffix,
            modifier = Modifier.weight(1f),
            valueColor = palette.accentInk,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GhostButton(text = label, onClick = onClick, icon = icon, modifier = modifier)
}

/**
 * Where the team is. It reads [Roadmap], which the unbuilt screens also
 * read, so there is one list of work in the whole project and the Deck cannot
 * disagree with the tab you are looking at.
 */
@Composable
private fun BuildStatusCard(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current

    GymBroCard(modifier = modifier) {
        Row {
            Text(
                text = "${(Roadmap.progress * 100).roundToInt()}% built",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${Roadmap.hoursLeft}h left",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = palette.faint,
            )
        }
        Spacer(Modifier.height(10.dp))
        MeterBar(progress = Roadmap.progress, color = palette.accent)
        Spacer(Modifier.height(14.dp))

        Owner.entries.forEach { owner ->
            val tickets = Roadmap.forOwner(owner)
            val left = tickets.filterNot { it.done }.sumOf { it.hours }
            val share = (Roadmap.shareOf(owner) * 100).roundToInt()
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = owner.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (left == 0) palette.faint else palette.ink,
                    modifier = Modifier.width(72.dp),
                )
                Text(
                    text = owner.area,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.faint,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (left == 0) "done" else "$left h",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = if (left == 0) palette.accentInk else palette.muted,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$share%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = palette.faint,
                )
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Still up"
}

/** `1h 12m` for anything over an hour, `12m` under it. */
private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
