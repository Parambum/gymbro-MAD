package com.gymbro.mad

import com.gymbro.mad.data.model.LoggedExercise
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WorkoutSession
import com.gymbro.mad.data.repo.WorkoutSnapshot
import com.gymbro.mad.data.repo.WorkoutStateCodec
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStateCodecTest {
    @Test
    fun `encoding keeps finished and active session data`() {
        val finished = WorkoutSession(
            id = "finished",
            title = "Push day",
            startedAt = Instant.parse("2026-03-10T09:00:00Z"),
            finishedAt = Instant.parse("2026-03-10T10:00:00Z"),
            exercises = listOf(
                LoggedExercise(
                    exerciseId = "bench",
                    name = "Bench Press",
                    groupSlug = "chest",
                    sets = listOf(
                        SetEntry(
                            id = "set-a",
                            weightKg = 82.5,
                            reps = 5,
                            rpe = 8.5,
                            completed = true,
                        ),
                        SetEntry(
                            id = "set-b",
                            weightKg = 80.0,
                            reps = 5,
                            completed = false,
                        ),
                    ),
                ),
            ),
        )
        val active = WorkoutSession(
            id = "active",
            title = "Pull",
            startedAt = Instant.parse("2026-03-11T09:00:00Z"),
        )
        val snapshot = WorkoutSnapshot(
            sessions = listOf(finished),
            activeSession = active,
        )

        assertEquals(snapshot, WorkoutStateCodec.decode(WorkoutStateCodec.encode(snapshot)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported backup version is rejected`() {
        val encoded = WorkoutStateCodec.encode(WorkoutSnapshot())
        val bytes = java.util.Base64.getDecoder().decode(encoded)
        bytes[3] = 2
        WorkoutStateCodec.decode(java.util.Base64.getEncoder().encodeToString(bytes))
    }
}
