package com.gymbro.mad

import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.repo.DerivedAnalyticsRepository
import com.gymbro.mad.data.repo.InMemoryWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The repository rules, pinned down. These are the behaviours the Train screen
 * is allowed to assume, so breaking one of them should break a test rather
 * than a screen somebody else is building.
 */
class WorkoutAndAnalyticsTest {

    private val bench = ExerciseCatalog.exercise("chest-flat-barbell-bench-press")!!
    private val squat = ExerciseCatalog.exercise("quads-barbell-back-squat")!!

    @Test
    fun `only completed sets count towards volume`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val session = workouts.startSession("Push")

        workouts.addExercise(session.id, bench)
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 60.0, reps = 10, completed = true))
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 60.0, reps = 10, completed = false))

        val active = workouts.activeSession.value!!
        assertEquals(600.0, active.volumeKg, 0.001)
        assertEquals(1, active.completedSets)
    }

    @Test
    fun `finishing files the session and clears the active one`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val session = workouts.startSession("Push")
        workouts.addExercise(session.id, bench)
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 80.0, reps = 5, completed = true))

        workouts.finishSession(session.id)

        assertNull(workouts.activeSession.value)
        assertEquals(1, workouts.sessions.value.size)
        assertEquals(400.0, workouts.sessions.value.first().volumeKg, 0.001)
    }

    @Test
    fun `a session with nothing completed is thrown away, not filed`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val session = workouts.startSession("Accidental tap")
        workouts.addExercise(session.id, bench)
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 60.0, reps = 10))

        workouts.finishSession(session.id)

        assertNull(workouts.activeSession.value)
        assertTrue("an empty session should leave no trace", workouts.sessions.value.isEmpty())
    }

    @Test
    fun `putSet replaces a set with the same id`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val session = workouts.startSession("Push")
        workouts.addExercise(session.id, bench)

        val set = SetEntry(weightKg = 60.0, reps = 10)
        workouts.putSet(session.id, bench.id, set)
        workouts.putSet(session.id, bench.id, set.copy(reps = 12, completed = true))

        val sets = workouts.activeSession.value!!.exercises.first().sets
        assertEquals(1, sets.size)
        assertEquals(12, sets.first().reps)
    }

    @Test
    fun `muscle volume is grouped by the muscle group of each exercise`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val analytics = DerivedAnalyticsRepository(workouts)

        val session = workouts.startSession("Full body")
        workouts.addExercise(session.id, bench)
        workouts.addExercise(session.id, squat)
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 60.0, reps = 10, completed = true))
        workouts.putSet(session.id, squat.id, SetEntry(weightKg = 100.0, reps = 8, completed = true))
        workouts.finishSession(session.id)

        val volumes = analytics.muscleVolume(days = 28).first()
        assertEquals(2, volumes.size)
        // sorted heaviest first: 800kg of squats before 600kg of bench
        assertEquals("quads", volumes.first().groupSlug)
        assertEquals(800.0, volumes.first().volumeKg, 0.001)
        assertEquals("chest", volumes.last().groupSlug)
    }

    @Test
    fun `weekly volume pads the weeks with no training`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val analytics = DerivedAnalyticsRepository(workouts)

        val weeks = analytics.weeklyVolume(weeks = 8).first()

        assertEquals(8, weeks.size)
        assertTrue("weeks must be in order", weeks.zipWithNext().all { it.first.weekStart < it.second.weekStart })
        assertTrue("an untrained week is a zero, not a gap", weeks.all { it.sessions == 0 })
    }

    @Test
    fun `personal bests take the heaviest set and estimate a 1RM`() = runTest {
        val workouts = InMemoryWorkoutRepository()
        val analytics = DerivedAnalyticsRepository(workouts)

        val session = workouts.startSession("Push")
        workouts.addExercise(session.id, bench)
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 80.0, reps = 5, completed = true))
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 90.0, reps = 3, completed = true))
        workouts.putSet(session.id, bench.id, SetEntry(weightKg = 95.0, reps = 1, completed = false))
        workouts.finishSession(session.id)

        val best = analytics.personalBests().first().single()
        assertEquals(90.0, best.weightKg, 0.001)
        assertEquals(3, best.reps)
        // Epley: 90 * (1 + 3/30) = 99
        assertEquals(99.0, best.estimated1RmKg, 0.001)
    }
}
