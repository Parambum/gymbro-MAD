package com.gymbro.mad.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * ARNAV OWNS THIS FILE. Ticket A-1.
 *
 * [sessionId] is the nav argument, already parsed in GymBroApp.kt. Look it up
 * with Repos.workouts.session(id), and handle null: a deep link can outlive
 * the session it points at.
 */
@Composable
fun SessionDetailScreen(
    sessionId: String,
    modifier: Modifier = Modifier,
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Session",
        oneLiner = "One session in full: every exercise, every set, what it added up to.",
        owner = Owner.ARNAV,
        route = Routes.SESSION_DETAIL,
        whatGoesHere = listOf(
            "A header with the title, date, duration, total volume and set count.",
            "Each exercise as a card with its sets listed - weight, reps, RPE - and " +
                "incomplete sets shown as skipped rather than hidden.",
            "Volume per muscle group for this session, as a short bar list.",
            "Any PR set in this session, flagged on the set that earned it.",
            "A not-found state for an id that no longer exists: say the session is " +
                "gone and offer a way back to History.",
        ),
        wiredForYou = listOf(
            "Repos.workouts.session(sessionId) returns the session or null.",
            "LoggedExercise.volumeKg and completedSets are computed per exercise.",
            "The id arrives as a route argument - do not pass the whole session " +
                "object through navigation.",
        ),
    )
}
