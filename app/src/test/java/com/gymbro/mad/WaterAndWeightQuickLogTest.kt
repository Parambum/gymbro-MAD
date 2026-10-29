package com.gymbro.mad.feature.fuel

import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.repo.InMemoryFuelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WaterAndWeightQuickLogTest {

    @Test
    fun `water quick log accumulates throughout the day`() = runBlocking {
        val repo = InMemoryFuelRepository()
        val today = LocalDate.now()

        assertEquals(0, repo.waterMlFor(today).first())

        repo.addWater(today, 250)
        assertEquals(250, repo.waterMlFor(today).first())

        repo.addWater(today, 500)
        assertEquals(750, repo.waterMlFor(today).first())

        repo.addWater(today, 250)
        assertEquals(1000, repo.waterMlFor(today).first())
    }

    @Test
    fun `body weight log records once per day and second entry replaces first`() = runBlocking {
        val repo = InMemoryFuelRepository()
        val today = LocalDate.now()

        // First weigh-in
        repo.logWeight(BodyWeightEntry(day = today, kg = 75.2))
        val firstList = repo.weights.value
        assertEquals(1, firstList.size)
        assertEquals(75.2, firstList.first { it.day == today }.kg, 0.01)

        // Second weigh-in on same day replaces the first
        repo.logWeight(BodyWeightEntry(day = today, kg = 74.8))
        val updatedList = repo.weights.value
        assertEquals(1, updatedList.size)
        assertEquals(74.8, updatedList.first { it.day == today }.kg, 0.01)
    }

    @Test
    fun `different days retain their own weigh-ins and are sorted by date`() = runBlocking {
        val repo = InMemoryFuelRepository()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        repo.logWeight(BodyWeightEntry(day = today, kg = 75.0))
        repo.logWeight(BodyWeightEntry(day = yesterday, kg = 75.4))

        val list = repo.weights.value
        assertEquals(2, list.size)
        assertEquals(yesterday, list[0].day)
        assertEquals(75.4, list[0].kg, 0.01)
        assertEquals(today, list[1].day)
        assertEquals(75.0, list[1].kg, 0.01)
    }
}
