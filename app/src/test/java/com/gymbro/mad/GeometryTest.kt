package com.gymbro.mad

import com.gymbro.mad.anatomy.Mesh
import com.gymbro.mad.anatomy.Vec3
import com.gymbro.mad.anatomy.box
import com.gymbro.mad.anatomy.capsule
import com.gymbro.mad.anatomy.cylinder
import com.gymbro.mad.anatomy.icosahedron
import com.gymbro.mad.anatomy.sphere
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Winding is the one thing that cannot be eyeballed from a screenshot: a
 * triangle wound the wrong way is culled by the renderer, and what you see is
 * a hole in the figure somewhere you were not looking.
 *
 * Every builder makes a convex solid centred on the origin, so a face normal
 * must point away from the centre. That is a cheap, total check - it catches a
 * flipped quad, a reversed cap and a bad subdivision in one assertion.
 */
class GeometryTest {

    private fun assertOutwardWinding(mesh: Mesh, name: String) {
        assertTrue("$name has no triangles", mesh.triangleCount > 0)
        for (t in 0 until mesh.triangleCount) {
            val a = mesh.vertex(mesh.indices[t * 3])
            val b = mesh.vertex(mesh.indices[t * 3 + 1])
            val c = mesh.vertex(mesh.indices[t * 3 + 2])

            val normal = (b - a) cross (c - a)
            if (normal.length < 1e-7f) continue // degenerate sliver at a pole

            val centroid = Vec3(
                (a.x + b.x + c.x) / 3f,
                (a.y + b.y + c.y) / 3f,
                (a.z + b.z + c.z) / 3f,
            )
            val outward = normal.normalized() dot centroid.normalized()
            assertTrue(
                "$name triangle $t is wound inwards (dot = $outward)",
                outward > 0f,
            )
        }
    }

    @Test
    fun `box faces point outwards`() {
        assertOutwardWinding(box(0.3f, 0.5f, 0.16f), "box")
        assertEquals(12, box(1f, 1f, 1f).triangleCount)
    }

    @Test
    fun `sphere faces point outwards`() {
        assertOutwardWinding(sphere(0.085f, 7, 6), "sphere")
    }

    @Test
    fun `cylinder faces and caps point outwards`() {
        assertOutwardWinding(cylinder(0.05f, 0.06f, 0.1f, 6), "cylinder")
    }

    @Test
    fun `capsule faces point outwards`() {
        assertOutwardWinding(capsule(0.05f, 0.16f, 2, 6), "capsule")
    }

    @Test
    fun `icosahedron faces point outwards at every detail level`() {
        assertOutwardWinding(icosahedron(0.13f, 0), "icosahedron d0")
        assertOutwardWinding(icosahedron(0.13f, 1), "icosahedron d1")
        assertEquals(20, icosahedron(1f, 0).triangleCount)
        assertEquals(80, icosahedron(1f, 1).triangleCount)
    }

    @Test
    fun `icosahedron vertices sit on the sphere`() {
        val mesh = icosahedron(0.5f, 1)
        for (i in 0 until mesh.vertexCount) {
            assertEquals(0.5f, mesh.vertex(i).length, 1e-4f)
        }
    }

    @Test
    fun `capsule is as tall as its length plus both caps`() {
        val mesh = capsule(0.05f, 0.2f, 2, 6)
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (i in 0 until mesh.vertexCount) {
            val y = mesh.vertex(i).y
            if (y < min) min = y
            if (y > max) max = y
        }
        assertEquals(0.3f, max - min, 1e-4f)
    }
}
