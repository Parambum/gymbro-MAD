package com.gymbro.mad.feature.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gymbro.mad.data.model.Exercise
import com.gymbro.mad.data.model.SetEntry
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.data.repo.WorkoutRepository

/** The repository remains the source of truth, including its persisted active session. */
class TrainViewModel(
    private val workouts: WorkoutRepository = Repos.workouts,
) : ViewModel() {
    val activeSession get() = workouts.activeSession
    val sessions get() = workouts.sessions

    suspend fun start(title: String) = workouts.startSession(title)
    suspend fun addExercise(sessionId: String, exercise: Exercise) = workouts.addExercise(sessionId, exercise)
    suspend fun removeExercise(sessionId: String, exerciseId: String) = workouts.removeExercise(sessionId, exerciseId)
    suspend fun putSet(sessionId: String, exerciseId: String, set: SetEntry) =
        workouts.putSet(sessionId, exerciseId, set)
    suspend fun deleteSet(sessionId: String, exerciseId: String, setId: String) =
        workouts.deleteSet(sessionId, exerciseId, setId)
    suspend fun finish(sessionId: String) = workouts.finishSession(sessionId)
    suspend fun discard(sessionId: String) = workouts.discardSession(sessionId)
}

class TrainViewModelFactory(private val workouts: WorkoutRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TrainViewModel(workouts) as T
}
