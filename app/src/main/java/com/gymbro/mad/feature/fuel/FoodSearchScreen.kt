package com.gymbro.mad.feature.fuel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.UnbuiltScreen

/**
 * JOHN OWNS THIS FILE. Tickets J-2, J-3.
 */
@Composable
fun FoodSearchScreen(
    modifier: Modifier = Modifier,
    onPicked: (foodId: String) -> Unit = {},
) {
    UnbuiltScreen(
        modifier = modifier,
        title = "Find a food",
        oneLiner = "Search the product database, or scan the barcode on the packet.",
        owner = Owner.JOHN,
        route = Routes.FOOD_SEARCH,
        whatGoesHere = listOf(
            "A search field with a debounce - roughly 300ms - so typing does not " +
                "fire a request per keystroke.",
            "Results showing name, brand and kcal per 100g, tapping through to a " +
                "portion sheet (J-4).",
            "A scan button that opens the camera and reads a barcode, then looks it " +
                "up with byBarcode (J-3).",
            "Ask for the camera permission when the user taps scan, not on open, and " +
                "handle the refusal: search still works without a camera.",
            "A no-results state that says so plainly, including when the network is " +
                "down - a failed request must not look like an empty cupboard.",
        ),
        wiredForYou = listOf(
            "FoodRepository is the seam: implement search and byBarcode against the " +
                "website API and swap it into Repos.food. No screen changes.",
            "FoodItem carries macros per 100g plus an optional barcode.",
            "CameraX and ML Kit barcode scanning are not in libs.versions.toml yet - " +
                "add them in the J-3 commit so the dependency arrives with its use.",
        ),
    )
}
