package com.gymbro.mad.data.repo

import android.content.Context
import com.gymbro.mad.data.model.CardioActivity
import com.gymbro.mad.data.model.CardioKind
import com.gymbro.mad.data.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Stores cardio sessions and their GPS route points on-device. */
class PersistentCardioRepository(context: Context) : CardioRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _activities = MutableStateFlow(readActivities())
    override val activities: StateFlow<List<CardioActivity>> = _activities.asStateFlow()

    override suspend fun log(activity: CardioActivity) {
        _activities.update { (it.filterNot { row -> row.id == activity.id } + activity).sortedByDescending { row -> row.startedAt } }
        save()
    }

    override suspend fun delete(id: String) {
        _activities.update { list -> list.filterNot { it.id == id } }
        save()
    }

    private fun save() {
        val rows = JSONArray()
        _activities.value.forEach { activity ->
            val row = JSONObject()
                .put("id", activity.id)
                .put("kind", activity.kind.name)
                .put("startedAt", activity.startedAt.toEpochMilli())
                .put("durationSeconds", activity.duration.seconds)
                .put("distanceMeters", activity.distanceMeters ?: JSONObject.NULL)
                .put("avgHeartRate", activity.avgHeartRate ?: JSONObject.NULL)
            val route = JSONArray()
            activity.route.forEach { point ->
                route.put(JSONObject()
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("recordedAt", point.recordedAt.toEpochMilli()))
            }
            row.put("route", route)
            rows.put(row)
        }
        preferences.edit().putString(KEY_ACTIVITIES, rows.toString()).apply()
    }

    private fun readActivities(): List<CardioActivity> {
        return runCatching {
            val stored = preferences.getString(KEY_ACTIVITIES, null) ?: return@runCatching emptyList()
            val rows = JSONArray(stored)
            List(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                val routeJson = row.optJSONArray("route") ?: JSONArray()
                val route = List(routeJson.length()) { pointIndex ->
                    val point = routeJson.getJSONObject(pointIndex)
                    RoutePoint(
                        latitude = point.getDouble("latitude"),
                        longitude = point.getDouble("longitude"),
                        recordedAt = Instant.ofEpochMilli(point.getLong("recordedAt")),
                    )
                }
                CardioActivity(
                    id = row.getString("id"),
                    kind = CardioKind.valueOf(row.getString("kind")),
                    startedAt = Instant.ofEpochMilli(row.getLong("startedAt")),
                    duration = Duration.ofSeconds(row.getLong("durationSeconds")),
                    distanceMeters = row.optDouble("distanceMeters").takeUnless { row.isNull("distanceMeters") || it.isNaN() },
                    avgHeartRate = row.optInt("avgHeartRate").takeUnless { row.isNull("avgHeartRate") },
                    route = route,
                )
            }.sortedByDescending { it.startedAt }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES = "gymbro_cardio"
        const val KEY_ACTIVITIES = "activities"
    }
}
