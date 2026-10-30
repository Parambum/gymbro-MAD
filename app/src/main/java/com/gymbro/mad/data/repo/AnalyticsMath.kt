package com.gymbro.mad.data.repo

import com.gymbro.mad.data.model.MuscleVolume

/** Relative 0..1 tint intensity for the body map, scaled to the busiest group. */
internal fun normaliseMuscleVolume(volumes: List<MuscleVolume>): Map<String, Float> {
    val peak = volumes.maxOfOrNull { it.volumeKg } ?: return emptyMap()
    if (peak <= 0.0) return emptyMap()
    return volumes.associate { it.groupSlug to (it.volumeKg / peak).toFloat().coerceIn(0f, 1f) }
}
