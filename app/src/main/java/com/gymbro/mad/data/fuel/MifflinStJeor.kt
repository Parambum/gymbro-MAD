package com.gymbro.mad.data.fuel

import com.gymbro.mad.data.model.Macros
import kotlin.math.round

/**
 * Biological sex for the Mifflin-St Jeor equation.
 */
enum class BiologicalSex(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
}

/**
 * Physical activity level multipliers (Physical Activity Level - PAL).
 * Standard multipliers for total daily energy expenditure (TDEE).
 */
enum class ActivityLevel(
    val displayName: String,
    val factor: Double,
    val description: String,
) {
    SEDENTARY("Sedentary", 1.2, "Little or no exercise"),
    LIGHTLY_ACTIVE("Light", 1.375, "Light exercise 1-3 days/week"),
    MODERATELY_ACTIVE("Moderate", 1.55, "Moderate exercise 3-5 days/week"),
    VERY_ACTIVE("Active", 1.725, "Hard exercise 6-7 days/week"),
    EXTRA_ACTIVE("Extra Active", 1.9, "Intense training or physical job"),
}

/**
 * Mifflin-St Jeor calculations for BMR, daily caloric expenditure, and macronutrient targets.
 * Free of Compose and Android dependencies.
 */
object MifflinStJeor {

    /**
     * Calculates Basal Metabolic Rate (BMR) in kcal using the Mifflin-St Jeor formula:
     * - Men: 10 * weight (kg) + 6.25 * height (cm) - 5 * age (years) + 5
     * - Women: 10 * weight (kg) + 6.25 * height (cm) - 5 * age (years) - 161
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
    ): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            BiologicalSex.MALE -> base + 5.0
            BiologicalSex.FEMALE -> base - 161.0
        }
    }

    /**
     * Calculates total daily energy expenditure (TDEE) / daily kcal target
     * by multiplying BMR with the activity factor.
     */
    fun calculateDailyKcal(
        bmr: Double,
        activityLevel: ActivityLevel,
    ): Double = bmr * activityLevel.factor

    /**
     * Calculates daily macro targets:
     * - Daily kcal: BMR * activity factor
     * - Protein: ~2g per kg body weight (4 kcal per gram)
     * - Fat: ~25% of daily kcal (9 kcal per gram)
     * - Carbs: remainder of kcal (4 kcal per gram)
     *
     * @param roundToWhole when true, rounds kcal, protein, carbs, and fat to whole integers.
     */
    fun calculateTargets(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        activityLevel: ActivityLevel,
        roundToWhole: Boolean = true,
    ): Macros {
        val bmr = calculateBmr(
            weightKg = weightKg,
            heightCm = heightCm,
            ageYears = ageYears,
            sex = sex,
        )
        val dailyKcal = calculateDailyKcal(bmr, activityLevel)
        val proteinG = 2.0 * weightKg
        val proteinKcal = proteinG * 4.0
        val fatKcal = dailyKcal * 0.25
        val fatG = fatKcal / 9.0
        val carbsKcal = (dailyKcal - proteinKcal - fatKcal).coerceAtLeast(0.0)
        val carbsG = carbsKcal / 4.0

        return if (roundToWhole) {
            Macros(
                kcal = round(dailyKcal),
                proteinG = round(proteinG),
                carbsG = round(carbsG),
                fatG = round(fatG),
            )
        } else {
            Macros(
                kcal = dailyKcal,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
            )
        }
    }
}
