package com.gymbro.mad.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymbro.mad.data.fuel.ActivityLevel
import com.gymbro.mad.data.fuel.BiologicalSex
import com.gymbro.mad.data.fuel.MifflinStJeor
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.ui.components.AccentButton
import com.gymbro.mad.ui.components.GhostButton
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.components.StatTile
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.NumericStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsOnboardingSheet(
    onDismiss: () -> Unit,
    onSaveTargets: (Macros) -> Unit,
    modifier: Modifier = Modifier,
    initialSex: BiologicalSex = BiologicalSex.MALE,
    initialActivityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    isInitialOnboarding: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = LocalPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.ink,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.border),
            )
        },
        modifier = modifier,
    ) {
        TargetsOnboardingContent(
            onDismiss = onDismiss,
            onSaveTargets = onSaveTargets,
            initialSex = initialSex,
            initialActivityLevel = initialActivityLevel,
            isInitialOnboarding = isInitialOnboarding,
        )
    }
}

@Composable
fun TargetsOnboardingContent(
    onDismiss: () -> Unit,
    onSaveTargets: (Macros) -> Unit,
    modifier: Modifier = Modifier,
    initialSex: BiologicalSex = BiologicalSex.MALE,
    initialActivityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    isInitialOnboarding: Boolean = true,
) {
    val palette = LocalPalette.current

    var sex by rememberSaveable { mutableStateOf(initialSex) }
    var activityLevel by rememberSaveable { mutableStateOf(initialActivityLevel) }
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }

    val heightCm = heightText.toDoubleOrNull()
    val weightKg = weightText.toDoubleOrNull()
    val ageYears = ageText.toIntOrNull()

    val isValid = heightCm != null && heightCm > 0 &&
        weightKg != null && weightKg > 0 &&
        ageYears != null && ageYears > 0

    val previewTargets = if (isValid) {
        MifflinStJeor.calculateTargets(
            weightKg = weightKg!!,
            heightCm = heightCm!!,
            ageYears = ageYears!!,
            sex = sex,
            activityLevel = activityLevel,
        )
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = if (isInitialOnboarding) "Set Nutrition Targets" else "Update Nutrition Targets",
            style = MaterialTheme.typography.titleLarge,
            color = palette.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Mifflin-St Jeor formula calculates your BMR and sets daily calorie and macro goals tailored to your body.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )

        Spacer(Modifier.height(18.dp))
        MicroLabel("BIOLOGICAL SEX")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BiologicalSex.entries.forEach { item ->
                GymBroChip(
                    label = item.displayName,
                    selected = sex == item,
                    onClick = { sex = item },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TargetsInputField(
                label = "HEIGHT",
                value = heightText,
                onValueChange = { heightText = it },
                unit = "cm",
                placeholder = "175",
                modifier = Modifier.weight(1f),
            )
            TargetsInputField(
                label = "WEIGHT",
                value = weightText,
                onValueChange = { weightText = it },
                unit = "kg",
                placeholder = "75.0",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))
        TargetsInputField(
            label = "AGE",
            value = ageText,
            onValueChange = { ageText = it },
            unit = "years",
            placeholder = "25",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        MicroLabel("ACTIVITY LEVEL")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityLevel.entries.forEach { level ->
                GymBroChip(
                    label = level.displayName,
                    selected = activityLevel == level,
                    onClick = { activityLevel = level },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${activityLevel.description} (${activityLevel.factor}x)",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )

        Spacer(Modifier.height(20.dp))
        SectionHeader("calculated targets")
        Spacer(Modifier.height(10.dp))
        GymBroCard(accent = if (isValid) palette.accent else null) {
            if (isValid && previewTargets != null) {
                val bmr = MifflinStJeor.calculateBmr(weightKg!!, heightCm!!, ageYears!!, sex)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${previewTargets.kcal.roundToInt()}",
                            style = NumericStyle,
                            color = palette.accent,
                        )
                        MicroLabel("KCAL / DAY")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${bmr.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = palette.muted,
                        )
                        MicroLabel("BASE BMR")
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(
                        label = "PROTEIN",
                        value = "${previewTargets.proteinG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "CARBS",
                        value = "${previewTargets.carbsG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "FAT",
                        value = "${previewTargets.fatG.roundToInt()}",
                        unit = "g",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Protein ~2g/kg • Fat 25% kcal • Carbs remainder",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.faint,
                )
            } else {
                Text(
                    text = "Enter height, weight, and age above to calculate your personalized daily targets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        AccentButton(
            text = if (isInitialOnboarding) "Save Targets" else "Update Targets",
            onClick = {
                if (previewTargets != null) {
                    onSaveTargets(previewTargets)
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        GhostButton(
            text = if (isInitialOnboarding) "Skip for Now" else "Cancel",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TargetsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.raised)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        MicroLabel(label)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        onValueChange(input)
                    }
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = palette.ink,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = palette.faint,
                            ),
                        )
                    }
                    innerTextField()
                },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = palette.faint,
            )
        }
    }
}
