package com.gymbro.mad.feature.fuel

import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.Meal
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.repo.FuelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FuelDayCalculationTest {

    private class FakeFuelRepository(
        initialTargets: Macros = Macros.ZERO,
        initialEntries: List<MealEntry> = emptyList(),
    ) : FuelRepository {
        private val _targets = MutableStateFlow(initialTargets)
        private val _entries = MutableStateFlow(initialEntries)

        override val targets: StateFlow<Macros> = _targets.asStateFlow()
        override val weights: StateFlow<List<BodyWeightEntry>> = MutableStateFlow(emptyList())
        override fun waterMlFor(day: LocalDate): Flow<Int> = flowOf(0)

        override fun entriesFor(day: LocalDate): Flow<List<MealEntry>> =
            _entries.map { list -> list.filter { it.day == day } }

        override suspend fun addEntry(entry: MealEntry) {
            _entries.value = _entries.value + entry
        }

        override suspend fun removeEntry(id: String) {
            _entries.value = _entries.value.filterNot { it.id == id }
        }

        override suspend fun setTargets(targets: Macros) {
            _targets.value = targets
        }

        override suspend fun addWater(day: LocalDate, ml: Int) {}
        override suspend fun logWeight(entry: BodyWeightEntry) {}
    }

    @Test
    fun `day totals sum correctly across meals`() = runBlocking {
        val today = LocalDate.now()
        val entry1 = MealEntry(
            foodId = "1",
            name = "Oatmeal",
            grams = 80.0,
            meal = Meal.BREAKFAST,
            day = today,
            macros = Macros(kcal = 300.0, proteinG = 12.0, carbsG = 50.0, fatG = 5.0),
        )
        val entry2 = MealEntry(
            foodId = "2",
            name = "Paneer Curry",
            grams = 150.0,
            meal = Meal.LUNCH,
            day = today,
            macros = Macros(kcal = 450.0, proteinG = 24.0, carbsG = 10.0, fatG = 35.0),
        )

        val entries = listOf(entry1, entry2)
        val dayTotal = entries.fold(Macros.ZERO) { acc, entry -> acc + entry.macros }

        assertEquals(750.0, dayTotal.kcal, 0.01)
        assertEquals(36.0, dayTotal.proteinG, 0.01)
        assertEquals(60.0, dayTotal.carbsG, 0.01)
        assertEquals(40.0, dayTotal.fatG, 0.01)
    }

    @Test
    fun `delete entry removes entry from repository`() = runBlocking {
        val today = LocalDate.now()
        val entry = MealEntry(
            foodId = "1",
            name = "Egg",
            grams = 50.0,
            meal = Meal.BREAKFAST,
            day = today,
            macros = Macros(kcal = 75.0, proteinG = 6.0, carbsG = 0.5, fatG = 5.0),
        )
        val repo = FakeFuelRepository(initialEntries = listOf(entry))

        repo.removeEntry(entry.id)
        val remaining = repo.entriesFor(today).first()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `macros zero indicates no target set`() {
        val target = Macros.ZERO
        assertTrue(target == Macros.ZERO)
        assertEquals(0.0, target.kcal, 0.01)
    }
}
