package com.gymbro.mad.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.gymbro.mad.data.model.BodyWeightEntry
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.CardioKind
import com.gymbro.mad.data.model.Exercise
import com.gymbro.mad.data.model.LoggedExercise
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.model.Meal
import com.gymbro.mad.data.model.MealEntry
import com.gymbro.mad.data.model.RoutePoint
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WorkoutSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Room backed repositories. User-facing state is published only after a write commits. */
class RoomWorkoutRepository(
    private val context: Context,
    private val db: AppDatabase,
) : WorkoutRepository {
    private val dao = db.appDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Unit>()
    private val mutex = Mutex()
    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val _active = MutableStateFlow<WorkoutSession?>(null)
    override val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()
    override val activeSession: StateFlow<WorkoutSession?> = _active.asStateFlow()

    init {
        scope.launch {
            runCatching {
                var rows = dao.allWorkouts()
                if (rows.isEmpty()) {
                    val legacy = PersistentWorkoutRepository(context)
                    val oldSessions = legacy.sessions.value
                    val oldActive = legacy.activeSession.value
                    if (oldSessions.isNotEmpty() || oldActive != null) {
                        saveSnapshot(oldSessions, oldActive)
                        rows = dao.allWorkouts()
                    }
                }
                publish(rows)
            }
            ready.complete(Unit)
            dao.observeWorkouts().collect(::publish)
        }
    }

    override suspend fun startSession(title: String): WorkoutSession = mutex.withLock {
        ready.await()
        WorkoutSession(title = title, startedAt = Instant.now()).also {
            saveSnapshot(_sessions.value, it)
            _active.value = it
        }
    }

    override suspend fun addExercise(sessionId: String, exercise: Exercise) = editActive(sessionId) { session ->
        if (session.exercises.any { it.exerciseId == exercise.id }) session else session.copy(
            exercises = session.exercises + LoggedExercise(exercise.id, exercise.name, exercise.groupSlug),
        )
    }

    override suspend fun removeExercise(sessionId: String, exerciseId: String) = editActive(sessionId) { session ->
        session.copy(exercises = session.exercises.filterNot { it.exerciseId == exerciseId })
    }

    override suspend fun putSet(sessionId: String, exerciseId: String, set: SetEntry) = editActive(sessionId) { session ->
        session.copy(exercises = session.exercises.map { exercise ->
            if (exercise.exerciseId != exerciseId) exercise else {
                val index = exercise.sets.indexOfFirst { it.id == set.id }
                val sets = if (index < 0) exercise.sets + set else exercise.sets.toMutableList().also { it[index] = set }
                exercise.copy(sets = sets)
            }
        })
    }

    override suspend fun deleteSet(sessionId: String, exerciseId: String, setId: String) = editActive(sessionId) { session ->
        session.copy(exercises = session.exercises.map { exercise ->
            if (exercise.exerciseId == exerciseId) exercise.copy(sets = exercise.sets.filterNot { it.id == setId }) else exercise
        })
    }

    override suspend fun finishSession(sessionId: String) = mutex.withLock {
        ready.await()
        val current = _active.value?.takeIf { it.id == sessionId } ?: return@withLock
        val nextActive = null
        val nextSessions = if (current.completedSets == 0) _sessions.value else
            (listOf(current.copy(finishedAt = Instant.now())) + _sessions.value).sortedByDescending { it.startedAt }
        saveSnapshot(nextSessions, nextActive)
        _sessions.value = nextSessions
        _active.value = null
    }

    override suspend fun discardSession(sessionId: String) = mutex.withLock {
        ready.await()
        val nextSessions = _sessions.value.filterNot { it.id == sessionId }
        val nextActive = _active.value?.takeUnless { it.id == sessionId }
        saveSnapshot(nextSessions, nextActive)
        _sessions.value = nextSessions
        _active.value = nextActive
    }

    override fun session(id: String): WorkoutSession? =
        _active.value?.takeIf { it.id == id } ?: _sessions.value.firstOrNull { it.id == id }

    private suspend fun editActive(id: String, edit: (WorkoutSession) -> WorkoutSession) = mutex.withLock {
        ready.await()
        val current = _active.value?.takeIf { it.id == id } ?: return@withLock
        val updated = edit(current)
        saveSnapshot(_sessions.value, updated)
        _active.value = updated
    }

    private suspend fun saveSnapshot(sessions: List<WorkoutSession>, active: WorkoutSession?) {
        val rows = (sessions.map { workoutEntity(it, false) } + listOfNotNull(active?.let { workoutEntity(it, true) }))
        val sets = (sessions + listOfNotNull(active)).flatMap { session ->
            session.exercises.flatMap { exercise -> exercise.sets.map { set ->
                WorkoutSetEntity(set.id, session.id, exercise.exerciseId, set.weightKg, set.reps, set.rpe, set.completed)
            } }
        }
        db.withTransaction {
            dao.clearWorkouts()
            dao.clearSets()
            if (rows.isNotEmpty()) dao.putWorkouts(rows)
            if (sets.isNotEmpty()) dao.putSets(sets)
        }
    }

    private fun publish(rows: List<WorkoutSessionEntity>) {
        val sessions = mutableListOf<WorkoutSession>()
        var active: WorkoutSession? = null
        rows.forEach { row ->
            val snapshot = runCatching { WorkoutStateCodec.decode(row.payload) }.getOrNull() ?: return@forEach
            if (row.active) active = snapshot.activeSession
            else sessions += snapshot.sessions
        }
        _sessions.value = sessions.sortedByDescending { it.startedAt }
        _active.value = active
    }

    private fun workoutEntity(session: WorkoutSession, active: Boolean) = WorkoutSessionEntity(
        id = session.id,
        title = session.title,
        startedAt = session.startedAt.toEpochMilli(),
        finishedAt = session.finishedAt?.toEpochMilli(),
        active = active,
        payload = WorkoutStateCodec.encode(
            if (active) WorkoutSnapshot(activeSession = session) else WorkoutSnapshot(sessions = listOf(session)),
        ),
    )
}

class RoomCardioRepository(context: Context, private val db: AppDatabase) : CardioRepository {
    private val dao = db.appDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Unit>()
    private val mutex = Mutex()
    private val _activities = MutableStateFlow<List<CardioActivity>>(emptyList())
    override val activities: StateFlow<List<CardioActivity>> = _activities.asStateFlow()

    init {
        scope.launch {
            var rows = dao.allCardio()
            if (rows.isEmpty()) {
                val legacy = PersistentCardioRepository(context).activities.value
                if (legacy.isNotEmpty()) dao.putCardio(legacy.map(::cardioEntity))
                rows = dao.allCardio()
            }
            publish(rows)
            ready.complete(Unit)
            dao.observeCardio().collect(::publish)
        }
    }

    override suspend fun log(activity: CardioActivity) = mutex.withLock {
        ready.await()
        dao.putCardio(cardioEntity(activity))
        _activities.value = ( _activities.value.filterNot { it.id == activity.id } + activity).sortedByDescending { it.startedAt }
    }

    override suspend fun delete(id: String) = mutex.withLock {
        ready.await()
        dao.deleteCardio(id)
        _activities.value = _activities.value.filterNot { it.id == id }
    }

    private fun publish(rows: List<CardioActivityEntity>) {
        _activities.value = rows.mapNotNull { runCatching { decodeCardio(it.payload) }.getOrNull() }
            .sortedByDescending { it.startedAt }
    }
}

class RoomFuelRepository(private val db: AppDatabase) : FuelRepository {
    private val dao = db.appDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _targets = MutableStateFlow(Macros.ZERO)
    private val _weights = MutableStateFlow<List<BodyWeightEntry>>(emptyList())
    override val targets: StateFlow<Macros> = _targets.asStateFlow()
    override val weights: StateFlow<List<BodyWeightEntry>> = _weights.asStateFlow()

    init {
        scope.launch {
            dao.observeTargets().collect { row ->
                _targets.value = row?.let { Macros(it.kcal, it.proteinG, it.carbsG, it.fatG) } ?: Macros.ZERO
            }
        }
        scope.launch {
            dao.observeWeights().collect { rows ->
                _weights.value = rows.mapNotNull { row -> runCatching { BodyWeightEntry(LocalDate.parse(row.day), row.kilograms) }.getOrNull() }
            }
        }
    }

    override fun entriesFor(day: LocalDate): Flow<List<MealEntry>> = dao.observeMeals(day.toString()).map { rows ->
        rows.mapNotNull { runCatching { decodeMeal(it.payload) }.getOrNull() }
    }

    override fun waterMlFor(day: LocalDate): Flow<Int> = dao.observeWater(day.toString()).map { it?.millilitres ?: 0 }

    override suspend fun addEntry(entry: MealEntry) = dao.putMeal(mealEntity(entry))
    override suspend fun removeEntry(id: String) = dao.deleteMeal(id)
    override suspend fun setTargets(targets: Macros) = dao.putTargets(
        FuelTargetsEntity(kcal = targets.kcal, proteinG = targets.proteinG, carbsG = targets.carbsG, fatG = targets.fatG),
    )
    override suspend fun addWater(day: LocalDate, ml: Int) = db.withTransaction {
        val total = ((dao.waterFor(day.toString()) ?: 0) + ml).coerceAtLeast(0)
        dao.putWater(WaterLogEntity(day.toString(), total))
    }
    override suspend fun logWeight(entry: BodyWeightEntry) = dao.putWeight(BodyWeightEntity(entry.day.toString(), entry.kg))
}

private fun cardioEntity(activity: CardioActivity): CardioActivityEntity = CardioActivityEntity(
    id = activity.id,
    startedAt = activity.startedAt.toEpochMilli(),
    payload = JSONObject()
        .put("id", activity.id)
        .put("kind", activity.kind.name)
        .put("startedAt", activity.startedAt.toEpochMilli())
        .put("duration", activity.duration.seconds)
        .put("distance", activity.distanceMeters ?: JSONObject.NULL)
        .put("heartRate", activity.avgHeartRate ?: JSONObject.NULL)
        .put("route", JSONArray().apply { activity.route.forEach { p -> put(JSONObject().put("lat", p.latitude).put("lon", p.longitude).put("at", p.recordedAt.toEpochMilli())) } })
        .toString(),
)

private fun decodeCardio(payload: String): CardioActivity {
    val row = JSONObject(payload)
    val route = row.getJSONArray("route")
    return CardioActivity(
        id = row.getString("id"),
        kind = CardioKind.valueOf(row.getString("kind")),
        startedAt = Instant.ofEpochMilli(row.getLong("startedAt")),
        duration = Duration.ofSeconds(row.getLong("duration")),
        distanceMeters = if (row.isNull("distance")) null else row.getDouble("distance"),
        avgHeartRate = if (row.isNull("heartRate")) null else row.getInt("heartRate"),
        route = List(route.length()) { index -> route.getJSONObject(index).let { RoutePoint(it.getDouble("lat"), it.getDouble("lon"), Instant.ofEpochMilli(it.getLong("at"))) } },
    )
}

private fun mealEntity(entry: MealEntry): MealEntryEntity = MealEntryEntity(
    id = entry.id,
    day = entry.day.toString(),
    meal = entry.meal.name,
    payload = JSONObject()
        .put("id", entry.id).put("foodId", entry.foodId).put("name", entry.name)
        .put("grams", entry.grams).put("meal", entry.meal.name).put("day", entry.day.toString())
        .put("kcal", entry.macros.kcal).put("protein", entry.macros.proteinG)
        .put("carbs", entry.macros.carbsG).put("fat", entry.macros.fatG).toString(),
)

private fun decodeMeal(payload: String): MealEntry = JSONObject(payload).let { row ->
    MealEntry(
        id = row.getString("id"), foodId = row.getString("foodId"), name = row.getString("name"),
        grams = row.getDouble("grams"), meal = Meal.valueOf(row.getString("meal")),
        day = LocalDate.parse(row.getString("day")),
        macros = Macros(row.getDouble("kcal"), row.getDouble("protein"), row.getDouble("carbs"), row.getDouble("fat")),
    )
}

