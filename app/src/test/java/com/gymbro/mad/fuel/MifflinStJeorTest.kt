package com.gymbro.mad.fuel

import com.gymbro.mad.data.fuel.ActivityLevel
import com.gymbro.mad.data.fuel.BiologicalSex
import com.gymbro.mad.data.fuel.MifflinStJeor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests verifying Mifflin-St Jeor calculations against hand-calculated examples.
 */
class MifflinStJeorTest {

    private val delta = 0.001

    @Test
    fun `male hand-calculated example - 25yo 75kg 180cm moderate activity`() {
        // Hand calculation:
        // BMR = 10 * 75 + 6.25 * 180 - 5 * 25 + 5
        //     = 750 + 1125 - 125 + 5 = 1755.0
        val bmr = MifflinStJeor.calculateBmr(
            weightKg = 75.0,
            heightCm = 180.0,
            ageYears = 25,
            sex = BiologicalSex.MALE,
        )
        assertEquals(1755.0, bmr, delta)

        // Moderate activity factor: 1.55
        // Daily kcal = 1755.0 * 1.55 = 2720.25
        val dailyKcal = MifflinStJeor.calculateDailyKcal(bmr, ActivityLevel.MODERATELY_ACTIVE)
        assertEquals(2720.25, dailyKcal, delta)

        // Macros unrounded:
        // Protein: 2g/kg * 75kg = 150g -> 600 kcal
        // Fat: 25% of 2720.25 = 680.0625 kcal -> 75.5625 g
        // Carbs: (2720.25 - 600 - 680.0625) / 4 = 1440.1875 / 4 = 360.046875 g
        val unrounded = MifflinStJeor.calculateTargets(
            weightKg = 75.0,
            heightCm = 180.0,
            ageYears = 25,
            sex = BiologicalSex.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            roundToWhole = false,
        )
        assertEquals(2720.25, unrounded.kcal, delta)
        assertEquals(150.0, unrounded.proteinG, delta)
        assertEquals(75.5625, unrounded.fatG, delta)
        assertEquals(360.046875, unrounded.carbsG, delta)

        // Rounded to whole units:
        val rounded = MifflinStJeor.calculateTargets(
            weightKg = 75.0,
            heightCm = 180.0,
            ageYears = 25,
            sex = BiologicalSex.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            roundToWhole = true,
        )
        assertEquals(2720.0, rounded.kcal, delta)
        assertEquals(150.0, rounded.proteinG, delta)
        assertEquals(76.0, rounded.fatG, delta)
        assertEquals(360.0, rounded.carbsG, delta)
    }

    @Test
    fun `female hand-calculated example - 30yo 60kg 165cm light activity`() {
        // Hand calculation:
        // BMR = 10 * 60 + 6.25 * 165 - 5 * 30 - 161
        //     = 600 + 1031.25 - 150 - 161 = 1320.25
        val bmr = MifflinStJeor.calculateBmr(
            weightKg = 60.0,
            heightCm = 165.0,
            ageYears = 30,
            sex = BiologicalSex.FEMALE,
        )
        assertEquals(1320.25, bmr, delta)

        // Light activity factor: 1.375
        // Daily kcal = 1320.25 * 1.375 = 1815.34375
        val dailyKcal = MifflinStJeor.calculateDailyKcal(bmr, ActivityLevel.LIGHTLY_ACTIVE)
        assertEquals(1815.34375, dailyKcal, delta)

        // Macros unrounded:
        // Protein: 2g/kg * 60kg = 120g -> 480 kcal
        // Fat: 25% of 1815.34375 = 453.8359375 kcal -> 50.426215... g
        // Carbs: (1815.34375 - 480 - 453.8359375) / 4 = 881.5078125 / 4 = 220.376953125 g
        val unrounded = MifflinStJeor.calculateTargets(
            weightKg = 60.0,
            heightCm = 165.0,
            ageYears = 30,
            sex = BiologicalSex.FEMALE,
            activityLevel = ActivityLevel.LIGHTLY_ACTIVE,
            roundToWhole = false,
        )
        assertEquals(1815.34375, unrounded.kcal, delta)
        assertEquals(120.0, unrounded.proteinG, delta)
        assertEquals(50.4262, unrounded.fatG, 0.001)
        assertEquals(220.3769, unrounded.carbsG, 0.001)

        // Rounded:
        val rounded = MifflinStJeor.calculateTargets(
            weightKg = 60.0,
            heightCm = 165.0,
            ageYears = 30,
            sex = BiologicalSex.FEMALE,
            activityLevel = ActivityLevel.LIGHTLY_ACTIVE,
            roundToWhole = true,
        )
        assertEquals(1815.0, rounded.kcal, delta)
        assertEquals(120.0, rounded.proteinG, delta)
        assertEquals(50.0, rounded.fatG, delta)
        assertEquals(220.0, rounded.carbsG, delta)
    }

    @Test
    fun `sedentary female hand-calculated example - 20yo 50kg 160cm`() {
        // Hand calculation:
        // BMR = 10 * 50 + 6.25 * 160 - 5 * 20 - 161
        //     = 500 + 1000 - 100 - 161 = 1239.0
        val bmr = MifflinStJeor.calculateBmr(
            weightKg = 50.0,
            heightCm = 160.0,
            ageYears = 20,
            sex = BiologicalSex.FEMALE,
        )
        assertEquals(1239.0, bmr, delta)

        val dailyKcal = MifflinStJeor.calculateDailyKcal(bmr, ActivityLevel.SEDENTARY)
        assertEquals(1486.8, dailyKcal, delta)

        val rounded = MifflinStJeor.calculateTargets(
            weightKg = 50.0,
            heightCm = 160.0,
            ageYears = 20,
            sex = BiologicalSex.FEMALE,
            activityLevel = ActivityLevel.SEDENTARY,
        )
        assertEquals(1487.0, rounded.kcal, delta)
        assertEquals(100.0, rounded.proteinG, delta)
        assertEquals(41.0, rounded.fatG, delta)
        assertEquals(179.0, rounded.carbsG, delta)
    }

    @Test
    fun `carbohydrate target cannot be negative`() {
        // Extreme hypothetical: high weight, very low activity/kcal
        val targets = MifflinStJeor.calculateTargets(
            weightKg = 250.0,
            heightCm = 120.0,
            ageYears = 90,
            sex = BiologicalSex.FEMALE,
            activityLevel = ActivityLevel.SEDENTARY,
            roundToWhole = false,
        )
        assertTrue(targets.carbsG >= 0.0)
    }
}
