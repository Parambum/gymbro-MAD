package com.gymbro.mad.feature.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.repo.FuelRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.EmptyState
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MeterBar
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.NumericStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fuel Screen: tracks daily calories, macronutrient targets, and meals.
 * Ticket J-5: Targets Onboarding & Mifflin-St Jeor calculation.
 */
@Composable
fun FuelScreen(
    modifier: Modifier = Modifier,
    fuel: FuelRepository = Repos.fuel,
    onFindFood: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    val targets by fuel.targets.collectAsStateWithLifecycle()

    var showOnboardingSheet by rememberSaveable { mutableStateOf(false) }
    var hasSkippedOnboarding by rememberSaveable { mutableStateOf(false) }

    val hasTargets = targets != Macros.ZERO
    val shouldShowInitialSheet = !hasTargets && !hasSkippedOnboarding

    if (showOnboardingSheet || shouldShowInitialSheet) {
        TargetsOnboardingSheet(
            onDismiss = {
                hasSkippedOnboarding = true
                showOnboardingSheet = false
            },
            onSaveTargets = { newTargets ->
                coroutineScope.launch {
                    fuel.setTargets(newTargets)
                    hasSkippedOnboarding = true
                    showOnboardingSheet = false
                }
            },
            isInitialOnboarding = !hasTargets,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Fuel",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
        )
        MicroLabel(
            text = "DAILY NUTRITION & TARGETS",
            color = palette.faint,
        )

        Spacer(Modifier.height(20.dp))
        SectionHeader("daily targets")
        Spacer(Modifier.height(10.dp))

        if (hasTargets) {
            GymBroCard(accent = palette.accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${targets.kcal.roundToInt()}",
                            style = NumericStyle,
                            color = palette.accent,
                        )
                        MicroLabel("DAILY KCAL TARGET", color = palette.muted)
                    }
                    GhostButton(
                        text = "Update",
                        icon = Icons.Default.Edit,
                        onClick = { showOnboardingSheet = true },
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(
                        label = "PROTEIN",
                        value = "${targets.proteinG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "CARBS",
                        value = "${targets.carbsG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "FAT",
                        value = "${targets.fatG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(14.dp))
                MicroLabel("MACRO SPLIT", color = palette.faint)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val totalGrams = targets.proteinG + targets.carbsG + targets.fatG
                    if (totalGrams > 0) {
                        val pWeight = (targets.proteinG / totalGrams).toFloat().coerceAtLeast(0.05f)
                        val cWeight = (targets.carbsG / totalGrams).toFloat().coerceAtLeast(0.05f)
                        val fWeight = (targets.fatG / totalGrams).toFloat().coerceAtLeast(0.05f)

                        MeterBar(progress = 1f, color = palette.accent, modifier = Modifier.weight(pWeight))
                        MeterBar(progress = 1f, color = palette.info, modifier = Modifier.weight(cWeight))
                        MeterBar(progress = 1f, color = palette.warn, modifier = Modifier.weight(fWeight))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Mifflin-St Jeor: ~2g/kg protein floor, 25% fat, remainder carbs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.faint,
                )
            }
        } else {
            GymBroCard {
                Text(
                    text = "No targets set yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Calculate your calorie and macro targets using the Mifflin-St Jeor equation based on your height, weight, age, and activity level.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                Spacer(Modifier.height(14.dp))
                AccentButton(
                    text = "Set Up Targets",
                    icon = Icons.Default.LocalFireDepartment,
                    onClick = { showOnboardingSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("food log")
        Spacer(Modifier.height(10.dp))
        EmptyState(
            title = "Nothing logged today",
            body = "Search for foods to log your meals and track calories and macros against your targets.",
            icon = Icons.Default.Search,
            actionLabel = "Search Foods",
            onAction = onFindFood,
        )
    }
}
