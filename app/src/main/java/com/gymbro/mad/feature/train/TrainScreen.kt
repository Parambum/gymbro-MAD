package com.gymbro.mad.feature.train

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * PREM OWNS THIS FILE. Tickets P-2, P-3, P-4, P-5, P-7.
 *
 * Replace the [UnbuiltScreen] call with the real screen. Keep the brief in the
 * commit message rather than here, and delete this comment when the screen
 * exists - a doc comment describing work that is finished is worse than none.
 */
@Composable
fun TrainScreen(
    modifier: Modifier = Modifier,
    onPickExercise: () -> Unit = {},
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Train",
        oneLiner = "Logging a lifting session, set by set. The heart of the app.",
        owner = Owner.PREM,
        route = Routes.TRAIN,
        whatGoesHere = listOf(
            "No session running: a Start button, and the last session as a card so " +
                "the user can see what they did and repeat it.",
            "Session running: title, elapsed time, running volume, and the list of " +
                "exercises added so far.",
            "One row per set - weight, reps, optional RPE, and a tick that marks it " +
                "complete. Only completed sets count towards volume.",
            "Add exercise opens the picker (P-1) and appends to the running session.",
            "A rest timer that keeps counting with the screen off, and posts a " +
                "notification when the rest is up (P-4 declares POST_NOTIFICATIONS).",
            "Finish shows a summary - volume, sets, PRs hit, duration - then returns " +
                "to the Deck (P-5).",
        ),
        wiredForYou = listOf(
            "Repos.workouts.activeSession is a StateFlow<WorkoutSession?>; collect it " +
                "with collectAsStateWithLifecycle().",
            "startSession, addExercise, putSet, deleteSet, finishSession and " +
                "discardSession all exist. finishSession already throws away a " +
                "session with nothing completed in it, so do not special-case that.",
            "ExerciseCatalog.search(query, groupSlug) covers all 191 movements.",
            "WorkoutSession.volumeKg, completedSets and duration are computed - read " +
                "them, never recalculate them in the UI.",
            "PersonalBest.estimated1RmKg is the Epley formula, ready for P-7.",
            "Components: GymBroCard, StatTile, AccentButton, GhostButton, " +
                "GymBroChip, EmptyState, MeterBar, SectionHeader.",
        ),
    )
}
