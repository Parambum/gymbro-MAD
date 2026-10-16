package com.gymbro.mad.feature.cardio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * PREM OWNS THIS FILE. Ticket P-6.
 *
 * Cardio is reached from the Deck rather than the tab bar - six tabs is the
 * most a phone takes, and lifting is what this app is for.
 */
@Composable
fun CardioScreen(modifier: Modifier = Modifier) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Cardio",
        oneLiner = "Runs, rides and rows, logged after the fact rather than tracked live.",
        owner = Owner.PREM,
        route = Routes.CARDIO,
        whatGoesHere = listOf(
            "A log form: kind (the CardioKind enum), duration, optional distance, " +
                "optional average heart rate.",
            "The list of past activities under it, newest first, with pace where a " +
                "distance was given.",
            "Swipe or long-press to delete, going through CardioRepository.delete.",
            "No live GPS tracking. It needs a foreground service and background " +
                "location, which is a project of its own - if you want it, raise it " +
                "as a new ticket rather than widening this one.",
        ),
        wiredForYou = listOf(
            "Repos.cardio.activities is a StateFlow<List<CardioActivity>>, newest " +
                "first, and log() keeps it sorted.",
            "CardioActivity.paceMinPerKm is computed and returns null when there is " +
                "no distance - show a dash, not a zero.",
            "Durations are java.time.Duration, distances are metres. Convert for " +
                "display only.",
        ),
    )
}
