package com.gymbro.mad.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes

/**
 * The six tabs.
 *
 * Same six sections as the website, with one swap: the site has Cardio in the
 * bar and no body map, while the phone has Body in the bar and reaches Cardio
 * from the Deck. Seven tabs do not fit a phone - at six the labels are already
 * at their smallest, and a seventh would either truncate or push the bar into
 * two rows.
 *
 * Each tab names its owner, which is what lets an unbuilt screen tell you
 * whose it is without a second list to keep in step.
 */
enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val owner: Owner,
) {
    DECK(Routes.DECK, "Deck", Icons.Filled.SpaceDashboard, Owner.ADITYA),
    TRAIN(Routes.TRAIN, "Train", Icons.Filled.FitnessCenter, Owner.PREM),
    FUEL(Routes.FUEL, "Fuel", Icons.Filled.LocalFireDepartment, Owner.JOHN),
    BODY(Routes.BODY, "Body", Icons.Filled.AccessibilityNew, Owner.ADITYA),
    STATS(Routes.STATS, "Stats", Icons.Filled.BarChart, Owner.ARNAV),
    HISTORY(Routes.HISTORY, "History", Icons.Filled.CalendarMonth, Owner.ARNAV);

    companion object {
        fun ofRoute(route: String?): Tab? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Screens that are not tabs. They keep the bottom bar visible but add a back
 * arrow, because a logger or a search that hides navigation traps the user on
 * a screen they did not mean to open.
 */
val InnerRoutes = setOf(
    Routes.CARDIO,
    Routes.PROFILE,
    Routes.EXERCISE_PICKER,
    Routes.FOOD_SEARCH,
    Routes.SESSION_DETAIL,
)

/** Title shown in the top bar for anything that is not a tab. */
fun innerRouteTitle(route: String?): String? = when (route) {
    Routes.CARDIO -> "Cardio"
    Routes.PROFILE -> "Profile"
    Routes.EXERCISE_PICKER -> "Pick an exercise"
    Routes.FOOD_SEARCH -> "Find a food"
    Routes.SESSION_DETAIL -> "Session"
    else -> null
}
