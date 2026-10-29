package com.gymbro.mad.feature.fuel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.Meal
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.repo.FuelRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.MeterBar
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.MicroStyle
import com.gymbro.mad.ui.theme.NumericStyle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Fuel Screen: tracks daily calories, macronutrient targets, and meal logs.
 * Covers:
 * - Ticket J-5: Targets Onboarding & Mifflin-St Jeor calculation.
 * - Ticket J-1: Fuel day screen: macro rings/meters, meal sections, day switcher.
 * - Ticket J-4: Portion editing sheet on entry click.
 * - Ticket J-6: Water (+250ml, +500ml) & body-weight quick log tiles.
 */
@Composable
fun FuelScreen(
    modifier: Modifier = Modifier,
    fuel: FuelRepository = Repos.fuel,
    onFindFood: (meal: Meal?) -> Unit = {},
) {
    val palette = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val targets by fuel.targets.collectAsStateWithLifecycle()
    val entries by remember(selectedDay) { fuel.entriesFor(selectedDay) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val waterMl by remember(selectedDay) { fuel.waterMlFor(selectedDay) }
        .collectAsStateWithLifecycle(initialValue = 0)
    val weights by fuel.weights.collectAsStateWithLifecycle()
    val currentWeightEntry = weights.find { it.day == selectedDay }

    var showOnboardingSheet by rememberSaveable { mutableStateOf(false) }
    var hasSkippedOnboarding by rememberSaveable { mutableStateOf(false) }
    var entryBeingEdited by remember { mutableStateOf<MealEntry?>(null) }
    var showWeightSheet by rememberSaveable { mutableStateOf(false) }

    entryBeingEdited?.let { entry ->
        val per100g = if (entry.grams > 0) {
            entry.macros.scaled(100.0 / entry.grams)
        } else {
            entry.macros
        }
        val foodItem = FoodItem(
            id = entry.foodId,
            name = entry.name,
            per100g = per100g,
        )
        PortionSheet(
            foodItem = foodItem,
            initialMeal = entry.meal,
            initialGrams = entry.grams,
            targetDay = entry.day,
            existingEntryId = entry.id,
            onDismiss = { entryBeingEdited = null },
            onSaveEntry = { updated ->
                coroutineScope.launch {
                    fuel.removeEntry(entry.id)
                    fuel.addEntry(updated)
                    entryBeingEdited = null
                }
            },
        )
    }

    if (showWeightSheet) {
        WeightLoggingSheet(
            currentKg = currentWeightEntry?.kg,
            day = selectedDay,
            onDismiss = { showWeightSheet = false },
            onSaveWeight = { kg ->
                coroutineScope.launch {
                    fuel.logWeight(BodyWeightEntry(day = selectedDay, kg = kg))
                    showWeightSheet = false
                }
            },
        )
    }

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

    val dayTotal = entries.fold(Macros.ZERO) { acc, entry -> acc + entry.macros }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 28.dp),
    ) {
        // ── Top Title & Subtitle ───────────────────────────────────
        Text(
            text = "Fuel",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
        )
        MicroLabel(
            text = "DAILY NUTRITION & TARGETS",
            color = palette.faint,
            modifier = Modifier.padding(start = 2.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ── Day Switcher ───────────────────────────────────────────
        DaySwitcherBar(
            selectedDay = selectedDay,
            onDaySelected = { selectedDay = it },
        )

        Spacer(Modifier.height(16.dp))

        // ── Intake vs Targets Header ───────────────────────────────
        NutritionHeaderCard(
            dayTotal = dayTotal,
            targets = targets,
            hasTargets = hasTargets,
            onOpenTargets = { showOnboardingSheet = true },
        )

        Spacer(Modifier.height(16.dp))

        // ── Ticket J-6: Water and Body-Weight Quick Log Tiles ─────────
        // Visible on the day screen without scrolling to the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WaterTile(
                waterMl = waterMl,
                onAddWater = { ml ->
                    coroutineScope.launch {
                        fuel.addWater(selectedDay, ml)
                    }
                },
                modifier = Modifier.weight(1f),
            )

            WeighInTile(
                weightEntry = currentWeightEntry,
                onOpenWeighIn = { showWeightSheet = true },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Four Meal Sections ─────────────────────────────────────
        Meal.entries.forEach { meal ->
            val mealEntries = entries.filter { it.meal == meal }
            MealSection(
                meal = meal,
                entries = mealEntries,
                onAddFood = { onFindFood(meal) },
                onEditEntry = { entryBeingEdited = it },
                onDeleteEntry = { entryId ->
                    coroutineScope.launch {
                        fuel.removeEntry(entryId)
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Water quick-log tile with +250ml and +500ml buttons (Ticket J-6).
 */
@Composable
private fun WaterTile(
    waterMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    GymBroCard(
        modifier = modifier,
        accent = palette.info,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$waterMl",
                        style = NumericStyle,
                        color = palette.info,
                    )
                    Text(
                        text = " ml",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.muted,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                MicroLabel("WATER LOGGED", color = palette.muted)
            }
            Icon(
                imageVector = Icons.Default.Opacity,
                contentDescription = null,
                tint = palette.info,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QuickWaterButton(
                label = "+250 ml",
                onClick = { onAddWater(250) },
                modifier = Modifier.weight(1f),
            )
            QuickWaterButton(
                label = "+500 ml",
                onClick = { onAddWater(500) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickWaterButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.raised)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MicroStyle,
            fontWeight = FontWeight.Bold,
            color = palette.info,
        )
    }
}

/**
 * Body-weight tile showing today's weigh-in (Ticket J-6).
 */
@Composable
private fun WeighInTile(
    weightEntry: BodyWeightEntry?,
    onOpenWeighIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    GymBroCard(
        modifier = modifier,
        accent = palette.accent,
        onClick = onOpenWeighIn,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (weightEntry != null) {
                        Text(
                            text = String.format("%.1f", weightEntry.kg),
                            style = NumericStyle,
                            color = palette.accent,
                        )
                        Text(
                            text = " kg",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = palette.muted,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    } else {
                        Text(
                            text = "--",
                            style = NumericStyle,
                            color = palette.faint,
                        )
                    }
                }
                MicroLabel(
                    text = if (weightEntry != null) "BODY WEIGHT" else "NO WEIGH-IN",
                    color = palette.muted,
                )
            }
            Icon(
                imageVector = Icons.Default.Scale,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.raised)
                .clickable(onClick = onOpenWeighIn)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (weightEntry != null) "UPDATE WEIGHT" else "LOG WEIGHT",
                style = MicroStyle,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
        }
    }
}

/**
 * Bottom sheet to enter or update daily body weight (stored in kg).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightLoggingSheet(
    currentKg: Double?,
    day: LocalDate,
    onDismiss: () -> Unit,
    onSaveWeight: (Double) -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kgInput by rememberSaveable {
        mutableStateOf(currentKg?.let { String.format("%.1f", it) } ?: "")
    }

    val parsedKg = kgInput.toDoubleOrNull()
    val isValid = parsedKg != null && parsedKg in 20.0..350.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Daily Weigh-in",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                    MicroLabel(
                        text = day.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        color = palette.muted,
                    )
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

            OutlinedTextField(
                value = kgInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        kgInput = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (kg)") },
                placeholder = { Text("e.g. 74.5") },
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

            Spacer(Modifier.height(6.dp))
            Text(
                text = "One weigh-in per day: logging a second value replaces the earlier entry.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.faint,
            )

            Spacer(Modifier.height(20.dp))

            AccentButton(
                text = "Save Weigh-in",
                icon = Icons.Default.Scale,
                enabled = isValid,
                onClick = {
                    if (parsedKg != null && isValid) {
                        onSaveWeight(parsedKg)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Day switcher component allowing moving back/forward or tapping "Today".
 */
@Composable
private fun DaySwitcherBar(
    selectedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
) {
    val palette = LocalPalette.current
    val today = LocalDate.now()
    val isToday = selectedDay == today
    val isYesterday = selectedDay == today.minusDays(1)

    val label = when {
        isToday -> "Today, ${selectedDay.format(DateTimeFormatter.ofPattern("d MMM"))}"
        isYesterday -> "Yesterday, ${selectedDay.format(DateTimeFormatter.ofPattern("d MMM"))}"
        else -> selectedDay.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }

    GymBroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onDaySelected(selectedDay.minusDays(1)) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day",
                    tint = palette.ink,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDaySelected(today) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) palette.accent else palette.ink,
                )
                if (!isToday) {
                    MicroLabel("TAP FOR TODAY", color = palette.muted)
                }
            }

            IconButton(
                onClick = { onDaySelected(selectedDay.plusDays(1)) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = palette.ink,
                )
            }
        }
    }
}

/**
 * Header card showing calories consumed vs target, and three progress meters.
 * Honest: if no target is configured, displays consumed macros cleanly without a fake default.
 */
@Composable
private fun NutritionHeaderCard(
    dayTotal: Macros,
    targets: Macros,
    hasTargets: Boolean,
    onOpenTargets: () -> Unit,
) {
    val palette = LocalPalette.current

    GymBroCard(accent = if (hasTargets) palette.accent else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${dayTotal.kcal.roundToInt()}",
                        style = NumericStyle,
                        color = palette.ink,
                    )
                    if (hasTargets) {
                        Text(
                            text = " / ${targets.kcal.roundToInt()} kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = palette.muted,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        )
                    } else {
                        Text(
                            text = " kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = palette.muted,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        )
                    }
                }
                MicroLabel(
                    text = if (hasTargets) "CONSUMED VS TARGET" else "TOTAL INTAKE (NO TARGET SET)",
                    color = palette.muted,
                )
            }

            GhostButton(
                text = if (hasTargets) "Edit" else "Set Target",
                icon = if (hasTargets) Icons.Default.Edit else Icons.Default.LocalFireDepartment,
                onClick = onOpenTargets,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── 3 Stat Tiles for Protein, Carbs, Fat ───────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MacroStatTile(
                label = "PROTEIN",
                consumed = dayTotal.proteinG.roundToInt(),
                target = if (hasTargets) targets.proteinG.roundToInt() else null,
                valueColor = palette.accent,
                modifier = Modifier.weight(1f),
            )
            MacroStatTile(
                label = "CARBS",
                consumed = dayTotal.carbsG.roundToInt(),
                target = if (hasTargets) targets.carbsG.roundToInt() else null,
                valueColor = palette.info,
                modifier = Modifier.weight(1f),
            )
            MacroStatTile(
                label = "FAT",
                consumed = dayTotal.fatG.roundToInt(),
                target = if (hasTargets) targets.fatG.roundToInt() else null,
                valueColor = palette.warn,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Meters ─────────────────────────────────────────────
        if (hasTargets) {
            Spacer(Modifier.height(16.dp))
            val pProgress = (dayTotal.proteinG / targets.proteinG.coerceAtLeast(1.0)).toFloat()
            val cProgress = (dayTotal.carbsG / targets.carbsG.coerceAtLeast(1.0)).toFloat()
            val fProgress = (dayTotal.fatG / targets.fatG.coerceAtLeast(1.0)).toFloat()

            MacroMeterRow(label = "PROTEIN", progress = pProgress, color = palette.accent)
            Spacer(Modifier.height(8.dp))
            MacroMeterRow(label = "CARBS", progress = cProgress, color = palette.info)
            Spacer(Modifier.height(8.dp))
            MacroMeterRow(label = "FAT", progress = fProgress, color = palette.warn)
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Set your body stats with Mifflin-St Jeor to calculate personalized macro rings and calorie deficit/surplus.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.faint,
            )
        }
    }
}

@Composable
private fun MacroStatTile(
    label: String,
    consumed: Int,
    target: Int?,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.raised)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "$consumed",
                style = NumericStyle.copy(fontSize = 18.sp),
                color = valueColor,
            )
            if (target != null) {
                Text(
                    text = " / ${target}g",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = palette.muted,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                )
            } else {
                Text(
                    text = " g",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = palette.faint,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        MicroLabel(label)
    }
}

@Composable
private fun MacroMeterRow(
    label: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MicroLabel(label, color = palette.faint)
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MicroStyle,
                color = if (progress >= 1f) palette.ink else palette.muted,
            )
        }
        Spacer(Modifier.height(4.dp))
        MeterBar(progress = progress, color = color, height = 6.dp)
    }
}

/**
 * Meal section for Breakfast, Lunch, Dinner, Snack with per-meal empty state and swipe-to-delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSection(
    meal: Meal,
    entries: List<MealEntry>,
    onAddFood: () -> Unit,
    onEditEntry: (MealEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val mealKcal = entries.sumOf { it.macros.kcal }.roundToInt()

    SectionHeader(
        title = "${meal.displayName} ${if (mealKcal > 0) "• $mealKcal kcal" else ""}".trim(),
        trailing = {
            GhostButton(
                text = "Add",
                icon = Icons.Default.Add,
                onClick = onAddFood,
            )
        },
    )

    Spacer(Modifier.height(8.dp))

    if (entries.isEmpty()) {
        // Honest empty state per meal, not one for the whole day
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.raised.copy(alpha = 0.5f))
                .padding(vertical = 16.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = palette.faint,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "No food logged for ${meal.displayName.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { entry ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            onDeleteEntry(entry.id)
                            true
                        } else {
                            false
                        }
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.danger.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete entry",
                                tint = palette.danger,
                            )
                        }
                    },
                    content = {
                        MealEntryRow(
                            entry = entry,
                            onClick = { onEditEntry(entry) },
                            onDelete = { onDeleteEntry(entry.id) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current

    GymBroCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${entry.grams.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.muted,
                    )
                    Text(text = "•", color = palette.border)
                    Text(
                        text = "P: ${entry.macros.proteinG.roundToInt()}g  C: ${entry.macros.carbsG.roundToInt()}g  F: ${entry.macros.fatG.roundToInt()}g",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.faint,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${entry.macros.kcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = palette.faint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
