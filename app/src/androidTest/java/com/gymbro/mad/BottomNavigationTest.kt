package com.gymbro.mad

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class BottomNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun eachBottomTabNavigatesAndShowsItsSelectedState() {
        listOf("deck", "train", "fuel", "body", "stats", "history").forEach { tab ->
            compose.onNodeWithTag("bottom_tab_$tab").performClick().assertIsSelected()
        }
    }
}
