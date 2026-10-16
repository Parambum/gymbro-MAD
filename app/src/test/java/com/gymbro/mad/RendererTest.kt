package com.gymbro.mad

import com.gymbro.mad.anatomy.AnatomyCamera
import com.gymbro.mad.anatomy.AnatomyRenderer
import com.gymbro.mad.anatomy.AnatomyStyle
import com.gymbro.mad.anatomy.DefaultPose
import com.gymbro.mad.anatomy.buildAnatomyScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/**
 * The camera, the culling and the hit testing, checked on the JVM.
 *
 * None of this needs a device: `computeFrame` does the whole geometry pass and
 * stops before the first drawing call, so the questions that actually matter -
 * is the chest facing me, is the back hidden, does a tap land on the muscle
 * under the finger - are answerable in a unit test.
 */
class RendererTest {

    private val scene = buildAnatomyScene()
    private val style = AnatomyStyle(
        zoneBaseArgb = 0xFF2A2A30.toInt(),
        chassisBaseArgb = 0xFF141417.toInt(),
        chassisAccentArgb = 0xFFFFB020.toInt(),
        groundArgb = 0x33FF5A1F,
    )
    private val noGlow = FloatArray(scene.zones.size)

    private fun frameFrom(yaw: Float, pitch: Float = DefaultPose.pitch): AnatomyRenderer {
        val renderer = AnatomyRenderer(scene)
        renderer.computeFrame(
            width = WIDTH,
            height = HEIGHT,
            camera = AnatomyCamera(
                target = DefaultPose.target,
                yaw = yaw,
                pitch = pitch,
                distance = DefaultPose.distance,
            ),
            style = style,
            zoneGlow = noGlow,
        )
        return renderer
    }

    @Test
    fun `culling keeps the front half and drops the rest`() {
        val renderer = AnatomyRenderer(scene)
        val visible = renderer.computeFrame(
            WIDTH,
            HEIGHT,
            AnatomyCamera(
                DefaultPose.target,
                DefaultPose.yaw,
                DefaultPose.pitch,
                DefaultPose.distance,
            ),
            style,
            noGlow,
        )
        assertTrue("nothing was drawn", visible > 100)
        assertTrue(
            "culling dropped nothing: $visible of ${scene.triangleCount}",
            visible < scene.triangleCount * 0.75,
        )
    }

    /**
     * Culling alone does not hide the far side of the body: the back slab sits
     * inside the torso and its front face points at the camera even from the
     * front. What hides it is the body in front of it, so the question to ask
     * is whether it can be TAPPED, not whether it projects.
     */
    @Test
    fun `from the front the chest is tappable and the back is not`() {
        val renderer = frameFrom(yaw = 0f)
        val chest = scene.zoneIndex("chest")
        val back = scene.zoneIndex("back")

        val chestPoint = renderer.zoneScreenPoint(chest)
        assertNotNull("chest should be on screen", chestPoint)
        assertEquals(chest, renderer.pickZone(chestPoint!![0], chestPoint[1]))

        val backPoint = renderer.zoneScreenPoint(back)
        if (backPoint != null) {
            assertTrue(
                "the torso should block a tap reaching the back from the front",
                renderer.pickZone(backPoint[0], backPoint[1]) != back,
            )
        }
    }

    @Test
    fun `turning the camera round makes the back tappable instead`() {
        val renderer = frameFrom(yaw = PI.toFloat())
        val chest = scene.zoneIndex("chest")
        val back = scene.zoneIndex("back")

        val backPoint = renderer.zoneScreenPoint(back)
        assertNotNull("back should be on screen from behind", backPoint)
        assertEquals(back, renderer.pickZone(backPoint!![0], backPoint[1]))

        val chestPoint = renderer.zoneScreenPoint(chest)
        if (chestPoint != null) {
            assertTrue(
                "the chest should not be reachable from behind",
                renderer.pickZone(chestPoint[0], chestPoint[1]) != chest,
            )
        }
    }

    @Test
    fun `a tap on a muscle picks that muscle`() {
        val renderer = frameFrom(yaw = 0f)
        for (slug in listOf("chest", "abs", "quads", "shoulders")) {
            val index = scene.zoneIndex(slug)
            val point = renderer.zoneScreenPoint(index)
            assertNotNull("$slug is not on screen from the front", point)
            assertEquals(
                "tapping the middle of $slug picked something else",
                index,
                renderer.pickZone(point!![0], point[1]),
            )
        }
    }

    @Test
    fun `a tap on empty space picks nothing`() {
        val renderer = frameFrom(yaw = 0f)
        assertEquals(-1, renderer.pickZone(4f, 4f))
    }

    @Test
    fun `the figure is upright on screen - head above feet`() {
        val renderer = frameFrom(yaw = 0f)
        val shoulders = renderer.zoneScreenPoint(scene.zoneIndex("shoulders"))!!
        val calves = renderer.zoneScreenPoint(scene.zoneIndex("calves"))
        // calves face backwards, so from the front use the quads instead
        val legs = calves ?: renderer.zoneScreenPoint(scene.zoneIndex("quads"))!!
        assertTrue(
            "shoulders at ${shoulders[1]} should be above legs at ${legs[1]}",
            shoulders[1] < legs[1],
        )
    }

    @Test
    fun `nothing is picked before a frame has been computed`() {
        assertEquals(-1, AnatomyRenderer(scene).pickZone(WIDTH / 2f, HEIGHT / 2f))
        assertNull(AnatomyRenderer(scene).zoneScreenPoint(0))
    }

    private companion object {
        const val WIDTH = 1080f
        const val HEIGHT = 1600f
    }
}
