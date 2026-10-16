package com.gymbro.mad.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * ARNAV OWNS THIS FILE. Ticket A-1.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onOpenSession: (sessionId: String) -> Unit = {},
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "History",
        oneLiner = "Every session logged, newest first, grouped by week.",
        owner = Owner.ARNAV,
        route = Routes.HISTORY,
        whatGoesHere = listOf(
            "A week header - date range, number of sessions, total volume - with the " +
                "sessions of that week under it.",
            "One row per session: title, date, duration, volume, set count, and a " +
                "dot per muscle group trained in its accent colour.",
            "Tapping a row opens the session detail route.",
            "Cardio activities belong in the same timeline, marked so they read as " +
                "cardio rather than lifting.",
            "An empty state for a brand-new install that says the first session has " +
                "yet to be logged, and points at Train.",
        ),
        wiredForYou = listOf(
            "Repos.workouts.sessions is already newest-first; Repos.cardio." +
                "activities is the other half of the timeline.",
            "WorkoutSession.date, volumeKg, completedSets and duration are computed.",
            "Routes.sessionDetail(id) builds the detail route; the argument is read " +
                "in GymBroApp.kt and handed to the screen.",
            "Grouping by week: session.date.with(TemporalAdjusters.previousOrSame" +
                "(MONDAY)) is what DerivedAnalyticsRepository uses, so the week " +
                "boundaries match the charts.",
        ),
    )
}
