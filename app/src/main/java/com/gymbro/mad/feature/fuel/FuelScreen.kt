package com.gymbro.mad.feature.fuel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * JOHN OWNS THIS FILE. Tickets J-1, J-4, J-5, J-6.
 */
@Composable
fun FuelScreen(
    modifier: Modifier = Modifier,
    onFindFood: () -> Unit = {},
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Fuel",
        oneLiner = "What you ate today, against what you were aiming for.",
        owner = Owner.JOHN,
        route = Routes.FUEL,
        whatGoesHere = listOf(
            "The day at the top: kcal against target as a ring or meter, and three " +
                "smaller meters for protein, carbs and fat.",
            "Four meal sections - breakfast, lunch, dinner, snack - each listing its " +
                "entries with portion and kcal, and an add button that opens search.",
            "A day switcher, defaulting to today. Yesterday must be reachable in one " +
                "tap: people log dinner the next morning.",
            "First run with no targets set: an onboarding sheet asking height, " +
                "weight, age, sex and activity, then Mifflin-St Jeor for a kcal " +
                "target and a 2g/kg protein floor (J-5).",
            "Water and body weight as two quick-log tiles (J-6).",
            "Before targets exist, show intake alone and say there is no target yet. " +
                "Never invent a 2,000 kcal default and present it as the user's own.",
        ),
        wiredForYou = listOf(
            "Repos.fuel.entriesFor(day) is a Flow<List<MealEntry>>; targets, weights " +
                "and waterMlFor(day) are there too.",
            "Macros has plus and scaled, so a portion is foodItem.per100g.scaled" +
                "(grams / 100.0) and a day total is entries.fold(Macros.ZERO).",
            "Macros.ZERO is also how the store says there are no targets - treat it " +
                "as not-set rather than as a target of nothing.",
            "Repos.food.search and byBarcode exist but return nothing yet; J-2 points " +
                "them at the website API, which already serves 13k Indian products " +
                "with barcodes.",
            "Components: MeterBar for the macro bars, GymBroCard, StatTile, " +
                "SectionHeader, EmptyState.",
        ),
    )
}
