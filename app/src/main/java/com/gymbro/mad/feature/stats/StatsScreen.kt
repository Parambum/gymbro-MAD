package com.gymbro.mad.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * ARNAV OWNS THIS FILE. Tickets A-2, A-3.
 */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Stats",
        oneLiner = "Volume, trend and personal bests, computed from what was logged.",
        owner = Owner.ARNAV,
        route = Routes.STATS,
        whatGoesHere = listOf(
            "Weekly volume as a bar chart, eight weeks, including the weeks with no " +
                "training - a gap has to look like a gap.",
            "Volume per muscle group as a ranked bar list, each bar in that group's " +
                "accent colour.",
            "A personal-best table: exercise, best set, estimated 1RM, date.",
            "Body-weight trend as a line chart once there are two weigh-ins (J-6 " +
                "feeds it).",
            "Charts drawn on Canvas - no chart library. Axis labels in the mono " +
                "micro style, grid lines at 12% ink, and a value label on the bar " +
                "being touched.",
            "Under about three sessions there is nothing worth charting: say so and " +
                "show the numbers instead of a two-bar chart.",
        ),
        wiredForYou = listOf(
            "Repos.analytics gives muscleVolume(days), weeklyVolume(weeks) and " +
                "personalBests() as Flows, all derived from the logged sessions.",
            "weeklyVolume already pads empty weeks, so the bar chart can trust its " +
                "list to be continuous and in order.",
            "PersonalBest.estimated1RmKg is Epley and is only honest to about ten " +
                "reps - mark anything above that, or show the raw set.",
            "ExerciseCatalog.group(slug) gives the display name and accent for a " +
                "group slug.",
        ),
    )
}
