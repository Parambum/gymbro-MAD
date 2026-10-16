package com.gymbro.mad.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.UnbuiltScreen
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.ThemeId

/**
 * ARNAV OWNS THIS FILE. Ticket A-6.
 *
 * The theme picker below is real and working - it is the one piece of this
 * screen the base needed for itself, and it doubles as a worked example of a
 * settings row: state is hoisted, the screen only reports the choice.
 */
@Composable
fun ProfileScreen(
    themeId: ThemeId,
    onThemeChange: (ThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SectionHeader("theme")
            Spacer(Modifier.height(10.dp))
            GymBroCard {
                Text(
                    text = "Three palettes, same app",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Carried over from the website. Every colour on screen " +
                        "resolves through the palette, so nothing is left behind " +
                        "when you switch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeId.entries.forEach { id ->
                        GymBroChip(
                            label = id.label,
                            selected = id == themeId,
                            onClick = { onThemeChange(id) },
                        )
                    }
                }
            }
        }

        UnbuiltScreen(
            title = "Profile",
            oneLiner = "Who is lifting, in what units, with which fonts.",
            owner = Owner.ARNAV,
            route = Routes.PROFILE,
            whatGoesHere = listOf(
                "Name and a weight unit switch - kilograms or pounds. Store kg and " +
                    "convert for display; a mixed-unit store is a silently wrong PR.",
                "The theme choice above, persisted so it survives a restart. It is " +
                    "held in GymBroApp state today and resets on relaunch.",
                "Bundle Space Grotesk and IBM Plex through downloadable fonts, then " +
                    "point Type.kt at them - the type scale is already in place.",
                "A sign-in section, if and only if the app ever talks to the website " +
                    "account system. Leave it out until then rather than shipping a " +
                    "button that does nothing.",
                "An export: the logged sessions as JSON or CSV, so a user can leave " +
                    "with their own data.",
            ),
            wiredForYou = listOf(
                "ThemeId and paletteFor(id) are the whole theme system; the picker " +
                    "above is the working example.",
                "GymBroApp.kt holds themeId in rememberSaveable - move it into " +
                    "DataStore and read it there instead.",
                "Weights are kilograms everywhere inside the app by convention " +
                    "(see Models.kt).",
            ),
        )
    }
}
