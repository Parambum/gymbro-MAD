package com.gymbro.mad.data.repo

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val active: Boolean,
    val payload: String,
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val completed: Boolean,
)

@Entity(tableName = "cardio_activities")
data class CardioActivityEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val payload: String,
)

@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val day: String,
    val meal: String,
    val payload: String,
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(@PrimaryKey val day: String, val millilitres: Int)

@Entity(tableName = "body_weights")
data class BodyWeightEntity(@PrimaryKey val day: String, val kilograms: Double)

@Entity(tableName = "fuel_targets")
data class FuelTargetsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    companion object { const val SINGLETON_ID = 1 }
}

@Dao
interface AppDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC") fun observeWorkouts(): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT * FROM workout_sessions") suspend fun allWorkouts(): List<WorkoutSessionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWorkouts(rows: List<WorkoutSessionEntity>)
    @Query("DELETE FROM workout_sessions") suspend fun clearWorkouts()
    @Query("DELETE FROM workout_sets") suspend fun clearSets()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSets(rows: List<WorkoutSetEntity>)
    @Query("SELECT * FROM workout_sets") suspend fun allSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM cardio_activities ORDER BY startedAt DESC") fun observeCardio(): Flow<List<CardioActivityEntity>>
    @Query("SELECT * FROM cardio_activities") suspend fun allCardio(): List<CardioActivityEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCardio(row: CardioActivityEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCardio(rows: List<CardioActivityEntity>)
    @Query("DELETE FROM cardio_activities WHERE id = :id") suspend fun deleteCardio(id: String)

    @Query("SELECT * FROM meal_entries WHERE day = :day ORDER BY rowid") fun observeMeals(day: String): Flow<List<MealEntryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeal(row: MealEntryEntity)
    @Query("DELETE FROM meal_entries WHERE id = :id") suspend fun deleteMeal(id: String)

    @Query("SELECT millilitres FROM water_logs WHERE day = :day") suspend fun waterFor(day: String): Int?
    @Query("SELECT * FROM water_logs WHERE day = :day") fun observeWater(day: String): Flow<WaterLogEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWater(row: WaterLogEntity)

    @Query("SELECT * FROM body_weights ORDER BY day") fun observeWeights(): Flow<List<BodyWeightEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWeight(row: BodyWeightEntity)

    @Query("SELECT * FROM fuel_targets WHERE id = 1 LIMIT 1") fun observeTargets(): Flow<FuelTargetsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTargets(row: FuelTargetsEntity)
}

@Database(
    entities = [
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        CardioActivityEntity::class,
        MealEntryEntity::class,
        WaterLogEntity::class,
        BodyWeightEntity::class,
        FuelTargetsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        fun open(context: android.content.Context): AppDatabase =
            androidx.room.Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "gymbro.db")
                // Future changes must add explicit Migration objects; never discard user data.
                .build()
    }
}
