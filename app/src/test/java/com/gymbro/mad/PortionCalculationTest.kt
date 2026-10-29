package com.gymbro.mad.feature.fuel

import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalTime

class PortionCalculationTest {

    @Test
    fun `scaled macros calculate correctly based on grams`() {
        // per 100g: 200 kcal, 20g protein, 10g carbs, 8g fat
        val food = FoodItem(
            id = "test_food",
            name = "Test Food",
            per100g = Macros(kcal = 200.0, proteinG = 20.0, carbsG = 10.0, fatG = 8.0),
        )

        // Test 150g (1.5x)
        val grams150 = 150.0
        val scaled150 = food.per100g.scaled(grams150 / 100.0)
        assertEquals(300.0, scaled150.kcal, 0.01)
        assertEquals(30.0, scaled150.proteinG, 0.01)
        assertEquals(15.0, scaled150.carbsG, 0.01)
        assertEquals(12.0, scaled150.fatG, 0.01)

        // Test 50g (0.5x)
        val grams50 = 50.0
        val scaled50 = food.per100g.scaled(grams50 / 100.0)
        assertEquals(100.0, scaled50.kcal, 0.01)
        assertEquals(10.0, scaled50.proteinG, 0.01)
        assertEquals(5.0, scaled50.carbsG, 0.01)
        assertEquals(4.0, scaled50.fatG, 0.01)
    }

    @Test
    fun `defaultMealForTime picks appropriate meal based on hour`() {
        assertEquals(Meal.BREAKFAST, defaultMealForTime(LocalTime.of(8, 30)))
        assertEquals(Meal.LUNCH, defaultMealForTime(LocalTime.of(13, 0)))
        assertEquals(Meal.SNACK, defaultMealForTime(LocalTime.of(16, 45)))
        assertEquals(Meal.DINNER, defaultMealForTime(LocalTime.of(20, 15)))
    }

    @Test
    fun `common portions are non-empty and have positive grams`() {
        assertNotNull(CommonPortions)
        org.junit.Assert.assertTrue(CommonPortions.isNotEmpty())
        CommonPortions.forEach { portion ->
            org.junit.Assert.assertTrue(portion.grams > 0.0)
        }
    }
}
