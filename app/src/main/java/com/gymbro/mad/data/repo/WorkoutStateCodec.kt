package com.gymbro.mad.data.repo

import android.content.Context
import com.gymbro.mad.data.model.LoggedExercise
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.model.WorkoutSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.Instant
import java.util.Base64

internal data class WorkoutSnapshot(
    val sessions: List<WorkoutSession> = emptyList(),
    val activeSession: WorkoutSession? = null,
)

internal object WorkoutStateCodec {
    private const val VERSION = 1

    fun encode(snapshot: WorkoutSnapshot): String {
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(VERSION)
                output.writeInt(snapshot.sessions.size)
                snapshot.sessions.forEach { output.writeSession(it) }
                output.writeBoolean(snapshot.activeSession != null)
                snapshot.activeSession?.let { output.writeSession(it) }
            }
            buffer.toByteArray()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decode(encoded: String): WorkoutSnapshot {
        val bytes = Base64.getDecoder().decode(encoded)
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == VERSION) { "Unsupported workout backup version" }
            val sessions = List(input.readInt().checkedCount()) { input.readSession() }
            val active = if (input.readBoolean()) input.readSession() else null
            WorkoutSnapshot(sessions = sessions, activeSession = active)
        }
    }

    private fun DataOutputStream.writeSession(session: WorkoutSession) {
        writeUTF(session.id)
        writeUTF(session.title)
        writeLong(session.startedAt.toEpochMilli())
        writeBoolean(session.finishedAt != null)
        session.finishedAt?.let { writeLong(it.toEpochMilli()) }
        writeInt(session.exercises.size)
        session.exercises.forEach { exercise ->
            writeUTF(exercise.exerciseId)
            writeUTF(exercise.name)
            writeUTF(exercise.groupSlug)
            writeInt(exercise.sets.size)
            exercise.sets.forEach { set ->
                writeUTF(set.id)
                writeDouble(set.weightKg)
                writeInt(set.reps)
                writeBoolean(set.rpe != null)
                set.rpe?.let { writeDouble(it) }
                writeBoolean(set.completed)
            }
        }
    }

    private fun DataInputStream.readSession(): WorkoutSession {
        val id = readUTF()
        val title = readUTF()
        val startedAt = Instant.ofEpochMilli(readLong())
        val finishedAt = if (readBoolean()) Instant.ofEpochMilli(readLong()) else null
        val exercises = List(readInt().checkedCount()) {
            val exerciseId = readUTF()
            val name = readUTF()
            val groupSlug = readUTF()
            val sets = List(readInt().checkedCount()) {
                val setId = readUTF()
                val weight = readDouble()
                val reps = readInt()
                val rpe = if (readBoolean()) readDouble() else null
                val completed = readBoolean()
                SetEntry(
                    id = setId,
                    weightKg = weight,
                    reps = reps,
                    rpe = rpe,
                    completed = completed,
                )
            }
            LoggedExercise(exerciseId, name, groupSlug, sets)
        }
        return WorkoutSession(id, title, startedAt, finishedAt, exercises)
    }

    private fun Int.checkedCount(): Int {
        require(this in 0..10_000) { "Invalid collection size in workout backup" }
        return this
    }
}

internal class PersistentWorkoutRepository(context: Context) : WorkoutRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val delegate: InMemoryWorkoutRepository

    init {
        val snapshot = preferences.getString(KEY, null)?.let(WorkoutStateCodec::decode)
            ?: WorkoutSnapshot()
        delegate = InMemoryWorkoutRepository(snapshot.sessions, snapshot.activeSession)
    }

    override val sessions get() = delegate.sessions
    override val activeSession get() = delegate.activeSession

    override suspend fun startSession(title: String): WorkoutSession =
        delegate.startSession(title).also { save() }

    override suspend fun addExercise(sessionId: String, exercise: com.gymbro.mad.data.model.Exercise) {
        delegate.addExercise(sessionId, exercise)
        save()
    }

    override suspend fun removeExercise(sessionId: String, exerciseId: String) {
        delegate.removeExercise(sessionId, exerciseId)
        save()
    }

    override suspend fun putSet(
        sessionId: String,
        exerciseId: String,
        set: SetEntry,
    ) {
        delegate.putSet(sessionId, exerciseId, set)
        save()
    }

    override suspend fun deleteSet(sessionId: String, exerciseId: String, setId: String) {
        delegate.deleteSet(sessionId, exerciseId, setId)
        save()
    }

    override suspend fun finishSession(sessionId: String) {
        delegate.finishSession(sessionId)
        save()
    }

    override suspend fun discardSession(sessionId: String) {
        delegate.discardSession(sessionId)
        save()
    }

    override fun session(id: String): WorkoutSession? = delegate.session(id)

    private fun save() {
        preferences.edit().putString(
            KEY,
            WorkoutStateCodec.encode(
                WorkoutSnapshot(
                    sessions = sessions.value,
                    activeSession = activeSession.value,
                ),
            ),
        ).apply()
    }

    private companion object {
        const val PREFERENCES = "gymbro_workouts"
        const val KEY = "snapshot"
    }
}
