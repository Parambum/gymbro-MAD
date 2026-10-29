package com.gymbro.mad.data.repo

import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.Exercise
import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.LoggedExercise
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.model.MuscleVolume
import com.gymbro.mad.data.model.PersonalBest
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WeekVolume
import com.gymbro.mad.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * THE SEAM.
 *
 * Screens talk to these interfaces and never to a database, a network client
 * or each other. That is the whole reason four people can work on this app at
 * once: the Train screen can be half-built while the Deck already reads
 * finished sessions, and swapping the in-memory store for Room (ticket A-5)
 * touches no screen at all.
 *
 * Two rules, and they are the ones that keep this app honest:
 *
 *  1. A repository returns what the user actually recorded. Empty is a real
 *     answer - never seed a demo row to make a screen look populated. The
 *     screen is responsible for an empty state that says so.
 *  2. Reads are Flows, writes are suspend functions. A screen that wants to
 *     react to new data collects; it does not poll and it does not cache.
 */

// ── strength ──────────────────────────────────────────────────────

interface WorkoutRepository {
    /** finished sessions, newest first */
    val sessions: StateFlow<List<WorkoutSession>>

    /** the session being logged right now, if any */
    val activeSession: StateFlow<WorkoutSession?>

    suspend fun startSession(title: String): WorkoutSession
    suspend fun addExercise(sessionId: String, exercise: Exercise)
    suspend fun removeExercise(sessionId: String, exerciseId: String)

    /** adds the set if its id is new, replaces it if it is not */
    suspend fun putSet(sessionId: String, exerciseId: String, set: SetEntry)
    suspend fun deleteSet(sessionId: String, exerciseId: String, setId: String)

    suspend fun finishSession(sessionId: String)
    suspend fun discardSession(sessionId: String)

    fun session(id: String): WorkoutSession?
}

// ── cardio ────────────────────────────────────────────────────────

interface CardioRepository {
    /** newest first */
    val activities: StateFlow<List<CardioActivity>>

    suspend fun log(activity: CardioActivity)
    suspend fun delete(id: String)
}

// ── fuel ──────────────────────────────────────────────────────────

interface FuelRepository {
    /** what the user ate on one day, in the order it was logged */
    fun entriesFor(day: LocalDate): Flow<List<MealEntry>>

    /** daily target; Macros.ZERO until the user has been through onboarding */
    val targets: StateFlow<Macros>

    val weights: StateFlow<List<BodyWeightEntry>>

    fun waterMlFor(day: LocalDate): Flow<Int>

    suspend fun addEntry(entry: MealEntry)
    suspend fun removeEntry(id: String)
    suspend fun setTargets(targets: Macros)
    suspend fun addWater(day: LocalDate, ml: Int)
    suspend fun logWeight(entry: BodyWeightEntry)
}

/**
 * Food lookup. Separate from [FuelRepository] because the day log is local and
 * this one will eventually hit the network: the website already has 13k
 * products with barcodes behind /api/fuel/foods, and ticket J-2 points this at
 * it. Keeping them apart means offline logging never waits on a search.
 */
interface FoodRepository {
    suspend fun search(query: String): List<FoodItem>
    suspend fun byBarcode(barcode: String): FoodItem?
}

// ── analytics ─────────────────────────────────────────────────────

interface AnalyticsRepository {
    /** volume per muscle group over the last [days] days, biggest first */
    fun muscleVolume(days: Int = 28): Flow<List<MuscleVolume>>

    /** one entry per week, oldest first, including weeks with no training */
    fun weeklyVolume(weeks: Int = 8): Flow<List<WeekVolume>>

    /** heaviest completed set per exercise */
    fun personalBests(): Flow<List<PersonalBest>>
}

// ══ in-memory implementations ═════════════════════════════════════
//
// The in-memory implementations are used by tests and previews. The app wires
// persisted workout and cardio repositories; Fuel remains in memory until A-5.

class InMemoryWorkoutRepository(
    initialSessions: List<WorkoutSession> = emptyList(),
    initialActiveSession: WorkoutSession? = null,
) : WorkoutRepository {

    private val _sessions = MutableStateFlow(initialSessions.sortedByDescending { it.startedAt })
    override val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow(initialActiveSession)
    override val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    override suspend fun startSession(title: String): WorkoutSession {
        val session = WorkoutSession(title = title, startedAt = Instant.now())
        _activeSession.value = session
        return session
    }

    override suspend fun addExercise(sessionId: String, exercise: Exercise) =
        editSession(sessionId) { session ->
            if (session.exercises.any { it.exerciseId == exercise.id }) {
                session
            } else {
                session.copy(
                    exercises = session.exercises + LoggedExercise(
                        exerciseId = exercise.id,
                        name = exercise.name,
                        groupSlug = exercise.groupSlug,
                    ),
                )
            }
        }

    override suspend fun removeExercise(sessionId: String, exerciseId: String) =
        editSession(sessionId) { session ->
            session.copy(exercises = session.exercises.filterNot { it.exerciseId == exerciseId })
        }

    override suspend fun putSet(sessionId: String, exerciseId: String, set: SetEntry) =
        editExercise(sessionId, exerciseId) { logged ->
            val index = logged.sets.indexOfFirst { it.id == set.id }
            if (index == -1) {
                logged.copy(sets = logged.sets + set)
            } else {
                logged.copy(sets = logged.sets.toMutableList().also { it[index] = set })
            }
        }

    override suspend fun deleteSet(sessionId: String, exerciseId: String, setId: String) =
        editExercise(sessionId, exerciseId) { logged ->
            logged.copy(sets = logged.sets.filterNot { it.id == setId })
        }

    /**
     * A finished session with nothing completed in it is thrown away rather
     * than filed. Otherwise every accidental tap on Start leaves a zero-volume
     * entry in History, and the user has to clean up after the app.
     */
    override suspend fun finishSession(sessionId: String) {
        val active = _activeSession.value?.takeIf { it.id == sessionId } ?: return
        _activeSession.value = null
        if (active.completedSets == 0) return
        val finished = active.copy(finishedAt = Instant.now())
        _sessions.update { listOf(finished) + it }
    }

    override suspend fun discardSession(sessionId: String) {
        if (_activeSession.value?.id == sessionId) _activeSession.value = null
        _sessions.update { list -> list.filterNot { it.id == sessionId } }
    }

    override fun session(id: String): WorkoutSession? =
        _activeSession.value?.takeIf { it.id == id } ?: _sessions.value.find { it.id == id }

    // crossinline because `edit` is called from inside the update lambdas,
    // which rules out a non-local return from it.
    private inline fun editSession(
        id: String,
        crossinline edit: (WorkoutSession) -> WorkoutSession,
    ) {
        _activeSession.update { current -> if (current?.id == id) edit(current) else current }
        _sessions.update { list -> list.map { if (it.id == id) edit(it) else it } }
    }

    private inline fun editExercise(
        sessionId: String,
        exerciseId: String,
        crossinline edit: (LoggedExercise) -> LoggedExercise,
    ) = editSession(sessionId) { session ->
        session.copy(
            exercises = session.exercises.map {
                if (it.exerciseId == exerciseId) edit(it) else it
            },
        )
    }
}

class InMemoryCardioRepository : CardioRepository {

    private val _activities = MutableStateFlow<List<CardioActivity>>(emptyList())
    override val activities: StateFlow<List<CardioActivity>> = _activities.asStateFlow()

    override suspend fun log(activity: CardioActivity) {
        _activities.update { (it + activity).sortedByDescending { a -> a.startedAt } }
    }

    override suspend fun delete(id: String) {
        _activities.update { list -> list.filterNot { it.id == id } }
    }
}

class InMemoryFuelRepository : FuelRepository {

    private val _entries = MutableStateFlow<List<MealEntry>>(emptyList())
    private val _targets = MutableStateFlow(Macros.ZERO)
    private val _weights = MutableStateFlow<List<BodyWeightEntry>>(emptyList())
    private val _water = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())

    override val targets: StateFlow<Macros> = _targets.asStateFlow()
    override val weights: StateFlow<List<BodyWeightEntry>> = _weights.asStateFlow()

    override fun entriesFor(day: LocalDate): Flow<List<MealEntry>> =
        _entries.map { list -> list.filter { it.day == day } }

    override fun waterMlFor(day: LocalDate): Flow<Int> = _water.map { it[day] ?: 0 }

    override suspend fun addEntry(entry: MealEntry) {
        _entries.update { it + entry }
    }

    override suspend fun removeEntry(id: String) {
        _entries.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun setTargets(targets: Macros) {
        _targets.value = targets
    }

    override suspend fun addWater(day: LocalDate, ml: Int) {
        _water.update { it + (day to ((it[day] ?: 0) + ml).coerceAtLeast(0)) }
    }

    /** One weigh-in per day: a second one on the same date replaces the first. */
    override suspend fun logWeight(entry: BodyWeightEntry) {
        _weights.update { list ->
            (list.filterNot { it.day == entry.day } + entry).sortedBy { it.day }
        }
    }
}

/**
 * Placeholder until J-2. It returns nothing rather than inventing a chicken
 * breast with plausible macros - a made-up food that gets logged becomes a
 * made-up calorie total, and the user cannot tell.
 */
class EmptyFoodRepository : FoodRepository {
    override suspend fun search(query: String): List<FoodItem> = emptyList()
    override suspend fun byBarcode(barcode: String): FoodItem? = null
}

/**
 * Analytics are derived, never stored: every number here is computed from the
 * sessions the user logged, so it cannot drift out of sync with them. It also
 * means the charts in ticket A-3 light up the moment the logger in P-3 lands,
 * with no extra plumbing.
 */
class DerivedAnalyticsRepository(
    private val workouts: WorkoutRepository,
) : AnalyticsRepository {

    override fun muscleVolume(days: Int): Flow<List<MuscleVolume>> =
        workouts.sessions.map { sessions ->
            val cutoff = LocalDate.now().minusDays(days.toLong())
            sessions
                .filter { it.date >= cutoff }
                .flatMap { it.exercises }
                .filter { it.completedSets > 0 }
                .groupBy { it.groupSlug }
                .map { (slug, logged) ->
                    MuscleVolume(
                        groupSlug = slug,
                        volumeKg = logged.sumOf { it.volumeKg },
                        sets = logged.sumOf { it.completedSets },
                    )
                }
                .sortedByDescending { it.volumeKg }
        }

    override fun weeklyVolume(weeks: Int): Flow<List<WeekVolume>> =
        workouts.sessions.map { sessions ->
            val thisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val byWeek = sessions.groupBy {
                it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
            // Weeks with no training are still weeks: a gap has to show up as a
            // zero bar, not as a missing one, or the chart lies about the gap.
            (weeks - 1 downTo 0).map { back ->
                val start = thisWeek.minusWeeks(back.toLong())
                val inWeek = byWeek[start].orEmpty()
                WeekVolume(
                    weekStart = start,
                    volumeKg = inWeek.sumOf { it.volumeKg },
                    sessions = inWeek.size,
                )
            }
        }

    override fun personalBests(): Flow<List<PersonalBest>> =
        workouts.sessions.map { sessions ->
            sessions
                .flatMap { session ->
                    session.exercises.flatMap { logged ->
                        logged.sets
                            .filter { it.completed }
                            .map { set -> Triple(logged, set, session.date) }
                    }
                }
                .groupBy { it.first.exerciseId }
                .mapNotNull { (_, rows) ->
                    val best = rows.maxByOrNull { it.second.weightKg } ?: return@mapNotNull null
                    PersonalBest(
                        exerciseId = best.first.exerciseId,
                        exerciseName = best.first.name,
                        weightKg = best.second.weightKg,
                        reps = best.second.reps,
                        achievedOn = best.third,
                    )
                }
                .sortedByDescending { it.estimated1RmKg }
        }
}

/**
 * Where the implementations are chosen, and the only place in the app that
 * knows which they are.
 *
 * A hand-rolled locator rather than Hilt: four people learning Compose do not
 * also need to learn an annotation processor, and swapping this for real
 * dependency injection later is a one-file change. Screens take repositories
 * as parameters with these as defaults, so a test or a @Preview can pass its
 * own without touching the locator.
 */
object Repos {
    var workouts: WorkoutRepository = InMemoryWorkoutRepository()
        private set
    var cardio: CardioRepository = InMemoryCardioRepository()
        private set
    val fuel: FuelRepository = InMemoryFuelRepository()
    val food: FoodRepository = NetworkFoodRepository()
    var analytics: AnalyticsRepository = DerivedAnalyticsRepository(workouts)
        private set

    fun initialize(context: android.content.Context) {
        workouts = PersistentWorkoutRepository(context.applicationContext)
        cardio = PersistentCardioRepository(context.applicationContext)
        analytics = DerivedAnalyticsRepository(workouts)
    }
}
