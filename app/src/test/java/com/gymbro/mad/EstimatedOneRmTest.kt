package com.gymbro.mad

import com.gymbro.mad.data.model.PersonalBest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EstimatedOneRmTest {
    @Test
    fun epleyEstimatesMatchKnownValues() {
        val date = LocalDate.of(2026, 1, 1)
        assertEquals(100.0, PersonalBest("bench", "Bench", 100.0, 1, date).estimated1RmKg, 0.0001)
        assertEquals(110.0, PersonalBest("bench", "Bench", 100.0, 3, date).estimated1RmKg, 0.0001)
        assertEquals(120.0, PersonalBest("squat", "Squat", 100.0, 6, date).estimated1RmKg, 0.0001)
    }
}
