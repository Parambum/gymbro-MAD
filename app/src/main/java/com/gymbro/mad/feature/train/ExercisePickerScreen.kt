package com.gymbro.mad.feature.train

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * PREM OWNS THIS FILE. Ticket P-1.
 */
@Composable
fun ExercisePickerScreen(
    modifier: Modifier = Modifier,
    onPicked: (exerciseId: String) -> Unit = {},
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Pick an exercise",
        oneLiner = "Search and filter the catalog, then hand an exercise back to the session.",
        owner = Owner.PREM,
        route = Routes.EXERCISE_PICKER,
        whatGoesHere = listOf(
            "A search field at the top, filtering as the user types.",
            "A row of muscle-group chips below it; selecting one narrows the list " +
                "and selecting it again clears the filter.",
            "The results as a plain list - name, and the group as a coloured dot " +
                "using MuscleGroup.accentArgb.",
            "Tapping a result calls onPicked(exerciseId) and pops back to Train.",
            "Recently used movements first once A-5 lands persistence; until then, " +
                "catalog order is the honest default.",
        ),
        wiredForYou = listOf(
            "ExerciseCatalog.search(query, groupSlug) does the filtering already.",
            "ExerciseCatalog.groups gives the 11 chips with their accent colours.",
            "The id handed back is stable and group-prefixed - chest-flat-barbell-" +
                "bench-press - so two groups can share a movement name.",
            "Components: GymBroChip for the filters, GymBroCard for rows.",
        ),
    )
}
