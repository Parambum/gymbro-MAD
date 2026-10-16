package com.gymbro.mad.anatomy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Primitive mesh construction for the anatomy figure.
 *
 * There is no 3D engine in this app. Filament or SceneView would each add
 * several megabytes and a .glb asset pipeline to draw what is, in the end,
 * about thirty boxes and capsules; the figure is parametric, so the maths is
 * cheaper than the dependency. The renderer that consumes these meshes is in
 * Renderer.kt.
 *
 * ONE CONVENTION, AND IT MATTERS: every triangle is wound counter-clockwise
 * when seen from outside the solid, so `cross(b - a, c - a)` points out of the
 * surface. The renderer uses that to drop back faces, so a triangle wound the
 * wrong way turns into a hole. GeometryTest checks every builder here by
 * asserting each face normal points away from the centre.
 */

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    infix fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x,
    )

    val length: Float get() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val l = length
        return if (l < 1e-6f) ZERO else Vec3(x / l, y / l, z / l)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

/**
 * A triangle mesh. Positions are flat x,y,z triples and [indices] holds three
 * vertex indices per triangle - flat arrays rather than object lists because
 * the renderer walks these every frame.
 */
class Mesh(val positions: FloatArray, val indices: IntArray) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3

    fun vertex(i: Int) = Vec3(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2])
}

class MeshBuilder {
    private val positions = ArrayList<Float>(512)
    private val indices = ArrayList<Int>(768)

    fun vertex(x: Float, y: Float, z: Float): Int {
        positions.add(x)
        positions.add(y)
        positions.add(z)
        return positions.size / 3 - 1
    }

    fun triangle(a: Int, b: Int, c: Int) {
        indices.add(a)
        indices.add(b)
        indices.add(c)
    }

    /** Vertices given in counter-clockwise order as seen from outside. */
    fun quad(a: Int, b: Int, c: Int, d: Int) {
        triangle(a, b, c)
        triangle(a, c, d)
    }

    fun build() = Mesh(positions.toFloatArray(), indices.toIntArray())
}

/** One horizontal circle of a surface of revolution: [y] and its radius. */
private data class Ring(val y: Float, val radius: Float)

/**
 * Stitches rings top to bottom into a closed surface. A ring with radius 0 is
 * a pole and becomes a triangle fan rather than a band of quads, which is what
 * keeps spheres and capsules free of degenerate slivers at the top and bottom.
 */
private fun lathe(rings: List<Ring>, radialSegments: Int): Mesh {
    val b = MeshBuilder()
    val seg = radialSegments.coerceAtLeast(3)

    val ringIndices = rings.map { ring ->
        if (ring.radius <= 1e-6f) {
            intArrayOf(b.vertex(0f, ring.y, 0f))
        } else {
            IntArray(seg) { u ->
                val phi = 2.0 * PI * u / seg
                b.vertex(
                    (sin(phi) * ring.radius).toFloat(),
                    ring.y,
                    (cos(phi) * ring.radius).toFloat(),
                )
            }
        }
    }

    for (i in 0 until rings.size - 1) {
        val upper = ringIndices[i]
        val lower = ringIndices[i + 1]
        for (u in 0 until seg) {
            val u1 = (u + 1) % seg
            val a = upper[if (upper.size == 1) 0 else u]
            val bb = upper[if (upper.size == 1) 0 else u1]
            val d = lower[if (lower.size == 1) 0 else u]
            val c = lower[if (lower.size == 1) 0 else u1]
            when {
                upper.size == 1 -> b.triangle(a, d, c)
                lower.size == 1 -> b.triangle(a, c, bb)
                else -> {
                    b.triangle(a, d, c)
                    b.triangle(a, c, bb)
                }
            }
        }
    }
    return b.build()
}

/** Axis-aligned box centred on the origin. */
fun box(width: Float, height: Float, depth: Float): Mesh {
    val hx = width / 2f
    val hy = height / 2f
    val hz = depth / 2f
    val b = MeshBuilder()
    val v0 = b.vertex(-hx, -hy, hz)
    val v1 = b.vertex(hx, -hy, hz)
    val v2 = b.vertex(hx, hy, hz)
    val v3 = b.vertex(-hx, hy, hz)
    val v4 = b.vertex(-hx, -hy, -hz)
    val v5 = b.vertex(hx, -hy, -hz)
    val v6 = b.vertex(hx, hy, -hz)
    val v7 = b.vertex(-hx, hy, -hz)
    b.quad(v0, v1, v2, v3) // front  +z
    b.quad(v1, v5, v6, v2) // right  +x
    b.quad(v5, v4, v7, v6) // back   -z
    b.quad(v4, v0, v3, v7) // left   -x
    b.quad(v3, v2, v6, v7) // top    +y
    b.quad(v4, v5, v1, v0) // bottom -y
    return b.build()
}

/** UV sphere centred on the origin. */
fun sphere(radius: Float, widthSegments: Int = 6, heightSegments: Int = 5): Mesh {
    val h = heightSegments.coerceAtLeast(2)
    val rings = (0..h).map { v ->
        val theta = PI * v / h
        Ring(
            y = (cos(theta) * radius).toFloat(),
            radius = (sin(theta) * radius).toFloat(),
        )
    }
    return lathe(rings, widthSegments)
}

/** Cylinder or truncated cone along Y, centred on the origin, with flat caps. */
fun cylinder(
    radiusTop: Float,
    radiusBottom: Float,
    height: Float,
    radialSegments: Int = 6,
): Mesh {
    val seg = radialSegments.coerceAtLeast(3)
    val hy = height / 2f
    val b = MeshBuilder()

    fun ring(y: Float, r: Float) = IntArray(seg) { u ->
        val phi = 2.0 * PI * u / seg
        b.vertex((sin(phi) * r).toFloat(), y, (cos(phi) * r).toFloat())
    }

    val top = ring(hy, radiusTop)
    val bottom = ring(-hy, radiusBottom)
    for (u in 0 until seg) {
        val u1 = (u + 1) % seg
        b.quad(top[u], bottom[u], bottom[u1], top[u1])
    }
    if (radiusTop > 1e-6f) {
        val centre = b.vertex(0f, hy, 0f)
        for (u in 0 until seg) b.triangle(centre, top[u], top[(u + 1) % seg])
    }
    if (radiusBottom > 1e-6f) {
        val centre = b.vertex(0f, -hy, 0f)
        for (u in 0 until seg) b.triangle(centre, bottom[(u + 1) % seg], bottom[u])
    }
    return b.build()
}

/**
 * Capsule along Y: a cylinder of [length] with a hemisphere of [radius] on
 * each end, so the total height is `length + 2 * radius`. Same parameter order
 * as three.js CapsuleGeometry, because the rig was ported from there.
 */
fun capsule(
    radius: Float,
    length: Float,
    capSegments: Int = 2,
    radialSegments: Int = 6,
): Mesh {
    val caps = capSegments.coerceAtLeast(1)
    val half = length / 2f
    val rings = ArrayList<Ring>(2 * caps + 2)
    // top hemisphere, pole first
    for (v in 0..caps) {
        val theta = (PI / 2.0) * v / caps
        rings += Ring(
            y = (cos(theta) * radius).toFloat() + half,
            radius = (sin(theta) * radius).toFloat(),
        )
    }
    // bottom hemisphere; its first ring is the equator again, one `length`
    // lower, so the straight side of the capsule falls out of the stitching
    for (v in 0..caps) {
        val theta = (PI / 2.0) * (1.0 + v.toDouble() / caps)
        rings += Ring(
            y = (cos(theta) * radius).toFloat() - half,
            radius = (sin(theta) * radius).toFloat(),
        )
    }
    return lathe(rings, radialSegments)
}

/**
 * Icosahedron, optionally subdivided and pushed back out to [radius] - an
 * icosphere. detail 0 gives 20 faces, detail 1 gives 80, and each step after
 * that multiplies by four.
 */
fun icosahedron(radius: Float, detail: Int = 0): Mesh {
    val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()

    val verts = mutableListOf(
        Vec3(-1f, t, 0f), Vec3(1f, t, 0f), Vec3(-1f, -t, 0f), Vec3(1f, -t, 0f),
        Vec3(0f, -1f, t), Vec3(0f, 1f, t), Vec3(0f, -1f, -t), Vec3(0f, 1f, -t),
        Vec3(t, 0f, -1f), Vec3(t, 0f, 1f), Vec3(-t, 0f, -1f), Vec3(-t, 0f, 1f),
    ).map { it.normalized() }.toMutableList()

    // the standard face table, already wound counter-clockwise from outside
    var faces = mutableListOf(
        intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7),
        intArrayOf(0, 7, 10), intArrayOf(0, 10, 11), intArrayOf(1, 5, 9),
        intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6),
        intArrayOf(7, 1, 8), intArrayOf(3, 9, 4), intArrayOf(3, 4, 2),
        intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
        intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10),
        intArrayOf(8, 6, 7), intArrayOf(9, 8, 1),
    )

    repeat(detail.coerceAtLeast(0)) {
        val midpoints = HashMap<Long, Int>()
        val next = ArrayList<IntArray>(faces.size * 4)

        fun midpoint(a: Int, b: Int): Int {
            val key = if (a < b) a.toLong() * 100_000L + b else b.toLong() * 100_000L + a
            return midpoints.getOrPut(key) {
                verts.add(((verts[a] + verts[b]) * 0.5f).normalized())
                verts.size - 1
            }
        }

        for (f in faces) {
            val (a, b, c) = Triple(f[0], f[1], f[2])
            val ab = midpoint(a, b)
            val bc = midpoint(b, c)
            val ca = midpoint(c, a)
            next += intArrayOf(a, ab, ca)
            next += intArrayOf(ab, b, bc)
            next += intArrayOf(ca, bc, c)
            next += intArrayOf(ab, bc, ca)
        }
        faces = next
    }

    val positions = FloatArray(verts.size * 3)
    verts.forEachIndexed { i, v ->
        positions[i * 3] = v.x * radius
        positions[i * 3 + 1] = v.y * radius
        positions[i * 3 + 2] = v.z * radius
    }
    val indices = IntArray(faces.size * 3)
    faces.forEachIndexed { i, f ->
        indices[i * 3] = f[0]
        indices[i * 3 + 1] = f[1]
        indices[i * 3 + 2] = f[2]
    }
    return Mesh(positions, indices)
}
