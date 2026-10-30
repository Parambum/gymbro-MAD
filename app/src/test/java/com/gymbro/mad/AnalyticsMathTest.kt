package com.gymbro.mad

import com.gymbro.mad.data.model.MuscleVolume
import com.gymbro.mad.data.repo.normaliseMuscleVolume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsMathTest {
    @Test
    fun `body map intensity is relative to highest group`() {
        val values = normaliseMuscleVolume(listOf(
            MuscleVolume("chest", 600.0, 10),
            MuscleVolume("back", 300.0, 5),
            MuscleVolume("quads", 0.0, 0),
        ))
        assertEquals(1f, values["chest"]!!, 0.0001f)
        assertEquals(0.5f, values["back"]!!, 0.0001f)
        assertEquals(0f, values["quads"]!!, 0.0001f)
    }

    @Test
    fun `empty and zero-volume windows stay unlit`() {
        assertTrue(normaliseMuscleVolume(emptyList()).isEmpty())
        assertTrue(normaliseMuscleVolume(listOf(MuscleVolume("arms", 0.0, 0))).isEmpty())
    }
}
