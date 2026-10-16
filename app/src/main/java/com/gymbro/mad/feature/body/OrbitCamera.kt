package com.gymbro.mad.feature.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.gymbro.mad.anatomy.AnatomyCamera
import com.gymbro.mad.anatomy.CameraPose
import com.gymbro.mad.anatomy.Vec3
import kotlin.math.PI

/**
 * Camera state for the body map, as plain Compose state.
 *
 * Deliberately not an `Animatable`: a finger on the figure and a camera
 * flight are the same three numbers, and making the gesture snap an animation
 * while the animation fights the gesture is how orbit controls end up
 * stuttering. Here a drag writes the values directly and [flyTo] interpolates
 * them frame by frame, so cancelling a flight is just cancelling its job.
 */
class OrbitCamera(pose: CameraPose) {

    var yaw by mutableFloatStateOf(pose.yaw)
        private set
    var pitch by mutableFloatStateOf(pose.pitch)
        private set
    var distance by mutableFloatStateOf(pose.distance)
        private set
    var target by mutableStateOf(pose.target)
        private set

    fun pose() = AnatomyCamera(target = target, yaw = yaw, pitch = pitch, distance = distance)

    /** Pixels of drag to radians. Negative x so the figure follows the finger. */
    fun drag(dx: Float, dy: Float) {
        yaw -= dx * 0.008f
        pitch = (pitch + dy * 0.006f).coerceIn(MIN_PITCH, MAX_PITCH)
    }

    fun zoomBy(scale: Float) {
        distance = (distance / scale).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
    }

    fun spin(radians: Float) {
        yaw += radians
    }

    /**
     * Eases to [pose] over [durationMillis]. Yaw takes the short way round, so
     * flying from the chest to the back turns 170 degrees rather than 190.
     */
    suspend fun flyTo(pose: CameraPose, durationMillis: Int = 620) {
        val fromYaw = yaw
        val toYaw = shortestTurn(fromYaw, pose.yaw)
        val fromPitch = pitch
        val fromDistance = distance
        val fromTarget = target

        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val linear = ((now - start) / 1_000_000f / durationMillis).coerceIn(0f, 1f)
            val eased = linear * linear * (3f - 2f * linear)

            yaw = fromYaw + (toYaw - fromYaw) * eased
            pitch = fromPitch + (pose.pitch - fromPitch) * eased
            distance = fromDistance + (pose.distance - fromDistance) * eased
            target = Vec3(
                fromTarget.x + (pose.target.x - fromTarget.x) * eased,
                fromTarget.y + (pose.target.y - fromTarget.y) * eased,
                fromTarget.z + (pose.target.z - fromTarget.z) * eased,
            )

            if (linear >= 1f) return
        }
    }

    private companion object {
        /** stop short of straight down, where the orbit basis degenerates */
        const val MIN_PITCH = -0.55f
        const val MAX_PITCH = 1.05f
        const val MIN_DISTANCE = 0.85f
        const val MAX_DISTANCE = 4.2f

        fun shortestTurn(from: Float, to: Float): Float {
            val twoPi = (2.0 * PI).toFloat()
            var delta = (to - from) % twoPi
            if (delta > PI) delta -= twoPi
            if (delta < -PI) delta += twoPi
            return from + delta
        }
    }
}

/**
 * Calls [onFrame] once per display frame for as long as it is in composition,
 * with the seconds elapsed since the previous one.
 *
 * Elapsed time rather than a fixed step, so the idle spin turns at the same
 * rate on a 60Hz and a 120Hz screen. The first frame after a long pause is
 * clamped: coming back from the background with a two-second delta would
 * otherwise snap the figure a third of the way round.
 */
@Composable
fun LaunchedFrameLoop(onFrame: (deltaSeconds: Float) -> Unit) {
    val callback by rememberUpdatedState(onFrame)
    LaunchedEffect(Unit) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val delta = ((now - previous) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
            previous = now
            callback(delta)
        }
    }
}
