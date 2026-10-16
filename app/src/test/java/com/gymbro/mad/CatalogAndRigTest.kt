package com.gymbro.mad

import com.gymbro.mad.anatomy.buildAnatomyScene
import com.gymbro.mad.data.catalog.ExerciseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog and the 3D rig have to agree: a muscle group with no zone is a
 * group the body map silently cannot show, which is exactly the kind of bug
 * that survives a demo and then turns up in front of a marker.
 */
class CatalogAndRigTest {

    @Test
    fun `every muscle group has a zone on the figure`() {
        val scene = buildAnatomyScene()
        for (group in ExerciseCatalog.groups) {
            assertTrue(
                "muscle group ${group.slug} has no 3D zone",
                scene.zoneIndex(group.slug) >= 0,
            )
        }
        assertEquals(ExerciseCatalog.groups.size, scene.zones.size)
    }

    @Test
    fun `the scene is consistent`() {
        val scene = buildAnatomyScene()
        assertEquals(scene.triangleCount, scene.triangleZone.size)
        assertEquals(0, scene.indices.size % 3)
        assertTrue("no geometry was generated", scene.triangleCount > 500)

        // every index has to point at a vertex that exists
        for (index in scene.indices) {
            assertTrue("index $index out of range", index in 0 until scene.vertexCount)
        }
        // and every triangle belongs to the chassis or to a real zone
        for (zone in scene.triangleZone) {
            assertTrue("bad zone $zone", zone >= -1 && zone < scene.zones.size)
        }
    }

    @Test
    fun `the figure stands on the floor and is roughly human`() {
        val scene = buildAnatomyScene()
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until scene.vertexCount) {
            val y = scene.positions[i * 3 + 1]
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        assertEquals("feet should rest on y = 0", 0f, minY, 0.03f)
        assertTrue("figure is $maxY tall", maxY in 1.8f..2.0f)
    }

    @Test
    fun `exercise ids are unique and resolvable`() {
        val ids = ExerciseCatalog.exercises.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (exercise in ExerciseCatalog.exercises) {
            assertEquals(exercise, ExerciseCatalog.exercise(exercise.id))
            assertNotNull(
                "${exercise.id} points at a group that does not exist",
                ExerciseCatalog.group(exercise.groupSlug),
            )
        }
    }

    @Test
    fun `every group has movements and search narrows them`() {
        for (group in ExerciseCatalog.groups) {
            assertTrue(
                "${group.slug} has no movements",
                ExerciseCatalog.forGroup(group.slug).isNotEmpty(),
            )
        }
        val squats = ExerciseCatalog.search("squat")
        assertTrue(squats.isNotEmpty())
        assertTrue(squats.all { it.name.contains("squat", ignoreCase = true) })

        val chestOnly = ExerciseCatalog.search("press", groupSlug = "chest")
        assertTrue(chestOnly.isNotEmpty())
        assertTrue(chestOnly.all { it.groupSlug == "chest" })

        // an empty query is not a filter
        assertEquals(ExerciseCatalog.exercises.size, ExerciseCatalog.search("   ").size)
    }
}
