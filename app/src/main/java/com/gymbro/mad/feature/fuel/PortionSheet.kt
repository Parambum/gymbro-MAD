package com.gymbro.mad.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.Meal
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.model.newId
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.NumericStyle
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Standard portion options for quick selection in the portion sheet.
 */
data class QuickPortion(val label: String, val grams: Double)

val CommonPortions = listOf(
    QuickPortion("100 g", 100.0),
    QuickPortion("50 g", 50.0),
    QuickPortion("150 g (1 katori)", 150.0),
    QuickPortion("200 g (1 cup/bowl)", 200.0),
    QuickPortion("250 g", 250.0),
)

/**
 * Returns the default meal suggested based on the user's current device hour.
 */
fun defaultMealForTime(time: LocalTime = LocalTime.now()): Meal {
    return when (time.hour) {
        in 5..10 -> Meal.BREAKFAST
        in 11..15 -> Meal.LUNCH
        in 16..18 -> Meal.SNACK
        in 19..23 -> Meal.DINNER
        else -> Meal.SNACK
    }
}

/**
 * Ticket J-4: Portion Sheet for Adding or Editing a Food Entry.
 *
 * Requirements:
 * - Portion in grams or millilitres, with macros updating live (`foodItem.per100g.scaled(grams / 100.0)`).
 * - Common portion chips (100g, 50g, 1 katori, 1 cup).
 * - Meal selector (Breakfast, Lunch, Dinner, Snack) defaulting to current time of day.
 * - Editing an existing entry reuses the same sheet.
 * - Macros stored on the entry so modifying a food later never silently rewrites past logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortionSheet(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onSaveEntry: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
    initialMeal: Meal? = null,
    initialGrams: Double = 100.0,
    targetDay: LocalDate = LocalDate.now(),
    existingEntryId: String? = null,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMeal by rememberSaveable(foodItem.id, initialMeal) {
        mutableStateOf(initialMeal ?: defaultMealForTime())
    }
    var gramsInput by rememberSaveable {
        mutableStateOf(initialGrams.roundToInt().toString())
    }

    val currentGrams = gramsInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    // Live macro calculation per requirement: foodItem.per100g.scaled(grams / 100.0)
    val scaledMacros = foodItem.per100g.scaled(currentGrams / 100.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            // ── Top Header ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existingEntryId != null) "Edit Entry" else "Log Food",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                    Text(
                        text = foodItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                    )
                    foodItem.brand?.let {
                        MicroLabel("BRAND: $it", color = palette.muted)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.muted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Live Macro Card ────────────────────────────────────
            GymBroCard(accent = palette.accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "${scaledMacros.kcal.roundToInt()}",
                            style = NumericStyle,
                            color = palette.ink,
                        )
                        MicroLabel("CALORIES FOR PORTION", color = palette.muted)
                    }
                    Text(
                        text = "${currentGrams.roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        color = palette.accent,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(
                        label = "PROTEIN",
                        value = String.format("%.1f", scaledMacros.proteinG),
                        unit = "g",
                        valueColor = palette.accent,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "CARBS",
                        value = String.format("%.1f", scaledMacros.carbsG),
                        unit = "g",
                        valueColor = palette.info,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "FAT",
                        value = String.format("%.1f", scaledMacros.fatG),
                        unit = "g",
                        valueColor = palette.warn,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Meal Selector ──────────────────────────────────────
            SectionHeader("meal")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Meal.entries.forEach { meal ->
                    GymBroChip(
                        label = meal.displayName,
                        selected = selectedMeal == meal,
                        onClick = { selectedMeal = meal },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Portion Input (Grams / ml) ─────────────────────────
            SectionHeader("portion (grams)")
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = gramsInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        gramsInput = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (g or ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = palette.accent,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // ── Quick Portion Chips ────────────────────────────────
            MicroLabel("QUICK PORTIONS", color = palette.faint)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CommonPortions.forEach { portion ->
                    val isSelected = (currentGrams - portion.grams).let { it >= -0.5 && it <= 0.5 }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.accent.copy(alpha = 0.16f) else palette.raised)
                            .clickable { gramsInput = portion.grams.roundToInt().toString() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = portion.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) palette.accent else palette.muted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Save Button ────────────────────────────────────────
            AccentButton(
                text = if (existingEntryId != null) "Update Entry" else "Add to ${selectedMeal.displayName}",
                icon = Icons.Default.Check,
                onClick = {
                    if (currentGrams > 0.0) {
                        val entry = MealEntry(
                            id = existingEntryId ?: newId(),
                            foodId = foodItem.id,
                            name = foodItem.name,
                            grams = currentGrams,
                            meal = selectedMeal,
                            day = targetDay,
                            macros = scaledMacros,
                        )
                        onSaveEntry(entry)
                    }
                },
                enabled = currentGrams > 0.0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
