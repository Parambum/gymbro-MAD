package com.gymbro.mad.data.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Every shape the app stores or shows. Deliberately free of Compose and
 * Android types: these have to stay usable from a plain JVM unit test, and
 * later from a Room DAO without a second set of entities.
 *
 * Weights are kilograms and distances are metres everywhere inside the app.
 * Pounds and miles are a display concern - convert at the edge, in the UI, or
 * the first mixed-unit bug will be a silently wrong personal best.
 */

fun newId(): String = UUID.randomUUID().toString()

// ── strength ──────────────────────────────────────────────────────

data class MuscleGroup(
    val slug: String,
    val displayName: String,
    /** glow colour for the 3D zone and the group chips, as 0xAARRGGBB */
    val accentArgb: Long,
)

data class Exercise(
    val id: String,
    val name: String,
    val groupSlug: String,
)

data class SetEntry(
    val id: String = newId(),
    val weightKg: Double,
    val reps: Int,
    /** rate of perceived exertion, 1..10, null when the lifter did not say */
    val rpe: Double? = null,
    val completed: Boolean = false,
) {
    val volumeKg: Double get() = weightKg * reps
}

data class LoggedExercise(
    val exerciseId: String,
    val name: String,
    val groupSlug: String,
    val sets: List<SetEntry> = emptyList(),
) {
    val volumeKg: Double get() = sets.filter { it.completed }.sumOf { it.volumeKg }
    val completedSets: Int get() = sets.count { it.completed }
}

data class WorkoutSession(
    val id: String = newId(),
    val title: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val exercises: List<LoggedExercise> = emptyList(),
) {
    val isActive: Boolean get() = finishedAt == null
    val volumeKg: Double get() = exercises.sumOf { it.volumeKg }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val duration: Duration get() = Duration.between(startedAt, finishedAt ?: Instant.now())
    val date: LocalDate get() = startedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
}

// ── cardio ────────────────────────────────────────────────────────

enum class CardioKind(val displayName: String) {
    RUN("Run"),
    WALK("Walk"),
    CYCLE("Cycle"),
    ROW("Row"),
    SWIM("Swim"),
    OTHER("Other"),
}

data class CardioActivity(
    val id: String = newId(),
    val kind: CardioKind,
    val startedAt: Instant,
    val duration: Duration,
    val distanceMeters: Double? = null,
    val avgHeartRate: Int? = null,
    val route: List<RoutePoint> = emptyList(),
) {
    /** minutes per kilometre, or null when no distance was recorded */
    val paceMinPerKm: Double?
        get() = distanceMeters?.takeIf { it > 0 }?.let {
            duration.seconds / 60.0 / (it / 1000.0)
        }
}

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val recordedAt: Instant,
)

// ── fuel ──────────────────────────────────────────────────────────

data class Macros(
    val kcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
) {
    operator fun plus(other: Macros) = Macros(
        kcal = kcal + other.kcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
    )

    fun scaled(factor: Double) = Macros(
        kcal = kcal * factor,
        proteinG = proteinG * factor,
        carbsG = carbsG * factor,
        fatG = fatG * factor,
    )

    companion object {
        val ZERO = Macros()
    }
}

data class FoodItem(
    val id: String,
    val name: String,
    val brand: String? = null,
    /** macros for 100 g (or 100 ml) of the food as sold */
    val per100g: Macros,
    val barcode: String? = null,
)

enum class Meal(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}

data class MealEntry(
    val id: String = newId(),
    val foodId: String,
    val name: String,
    val grams: Double,
    val meal: Meal,
    val day: LocalDate,
    /** macros for this portion, resolved when the entry was logged */
    val macros: Macros,
)

data class BodyWeightEntry(
    val day: LocalDate,
    val kg: Double,
)

// ── analytics ─────────────────────────────────────────────────────

data class MuscleVolume(
    val groupSlug: String,
    val volumeKg: Double,
    val sets: Int,
)

data class WeekVolume(
    /** Monday of the week */
    val weekStart: LocalDate,
    val volumeKg: Double,
    val sessions: Int,
)

data class PersonalBest(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val achievedOn: LocalDate,
) {
    /**
     * Epley one-rep-max estimate. Only honest up to about 10 reps, which is
     * why anything heavier than that is usually shown as-is instead.
     */
    val estimated1RmKg: Double get() = weightKg * (1 + reps / 30.0)
}
