package com.gymbro.mad

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.CardioKind
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.repo.AppDatabase
import com.gymbro.mad.data.repo.RoomCardioRepository
import com.gymbro.mad.data.repo.RoomFuelRepository
import com.gymbro.mad.data.repo.RoomWorkoutRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Test
    fun workoutFinishAndCardioWritesPersistThroughRepositoryInterfaces() = runBlocking {
        val workouts = RoomWorkoutRepository(context, database)
        val session = workouts.startSession("Room contract")
        val exercise = ExerciseCatalog.exercises.first()
        workouts.addExercise(session.id, exercise)
        workouts.putSet(session.id, exercise.id, SetEntry(weightKg = 40.0, reps = 8, completed = true))
        workouts.finishSession(session.id)

        val storedSession = database.appDao().allWorkouts().single { it.id == session.id }
        assertNotNull(storedSession.finishedAt)
        assertEquals(1, database.appDao().allSets().count { it.sessionId == session.id && it.completed })

        val cardio = RoomCardioRepository(context, database)
        val activity = CardioActivity(
            kind = CardioKind.RUN,
            startedAt = Instant.now(),
            duration = Duration.ofMinutes(20),
            distanceMeters = 2500.0,
        )
        cardio.log(activity)
        assertEquals(activity.id, database.appDao().allCardio().single().id)
    }

    @Test
    fun fuelValuesAreStoredByDayAndTarget() = runBlocking {
        val fuel = RoomFuelRepository(database)
        val today = java.time.LocalDate.now()
        fuel.addWater(today, 250)
        fuel.setTargets(Macros(2200.0, 150.0, 240.0, 70.0))
        fuel.logWeight(com.gymbro.mad.data.model.BodyWeightEntry(today, 72.5))

        assertEquals(250, database.appDao().waterFor(today.toString()))
        assertEquals(2200.0, database.appDao().observeTargets().first()!!.kcal, 0.0)
        assertEquals(72.5, database.appDao().observeWeights().first().single().kilograms, 0.0)
    }
}
