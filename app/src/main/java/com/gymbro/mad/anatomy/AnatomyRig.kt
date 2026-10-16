package com.gymbro.mad.anatomy

import com.gymbro.mad.data.catalog.ExerciseCatalog
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The figure: a low-poly human rig, about 1.9 units tall with the feet at
 * y = 0, ported part for part from the web app's anatomy-config.ts so the
 * phone and the website show the same body.
 *
 * It is made of two kinds of part. The chassis - head, spine, pelvis, limbs,
 * feet - is scenery and never responds to touch. The eleven zones are the
 * muscle groups from [ExerciseCatalog], and each one can be lit, tinted by how
 * much work it has taken, and tapped.
 *
 * Parts flagged `mirror` are built twice, with x and the y/z rotations
 * negated, so the rig is described as a half-body and comes out symmetrical.
 *
 * Every part is baked into world space once at startup. Nothing here is
 * recomputed per frame: the figure does not animate, the camera moves around
 * it, and that is what keeps a software renderer affordable.
 */

private class Part(
    val mesh: Mesh,
    val position: Vec3,
    val rotation: Vec3 = Vec3.ZERO,
    val mirror: Boolean = false,
)

/** Where the camera sits and what it looks at, in rig units. */
data class CameraPose(val position: Vec3, val target: Vec3) {
    private val offset: Vec3 get() = position - target

    val distance: Float get() = offset.length
    val yaw: Float get() = atan2(offset.x, offset.z)
    val pitch: Float get() = asin((offset.y / distance).coerceIn(-1f, 1f))
}

val DefaultPose = CameraPose(
    position = Vec3(0f, 1.35f, 3.1f),
    target = Vec3(0f, 1.05f, 0f),
)

/** One tappable muscle group on the figure. */
class AnatomyZone(
    val slug: String,
    val displayName: String,
    val accentArgb: Long,
    val pose: CameraPose,
)

/**
 * The whole figure, flattened for the renderer: world-space vertices, triangle
 * indices, and for each triangle the index of the zone it belongs to (-1 for
 * the chassis).
 */
class AnatomyScene(
    val positions: FloatArray,
    val indices: IntArray,
    val triangleZone: IntArray,
    val zones: List<AnatomyZone>,
) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3

    fun zoneIndex(slug: String): Int = zones.indexOfFirst { it.slug == slug }
}

// ── the rig ───────────────────────────────────────────────────────

private val chassis: List<Part> = listOf(
    // head, neck
    Part(icosahedron(0.13f, 1), Vec3(0f, 1.78f, 0f)),
    Part(cylinder(0.05f, 0.06f, 0.1f, 6), Vec3(0f, 1.65f, 0f)),
    // torso block and pelvis
    Part(box(0.3f, 0.52f, 0.16f), Vec3(0f, 1.32f, 0f)),
    Part(box(0.26f, 0.16f, 0.15f), Vec3(0f, 0.98f, 0f)),
    // upper arm, forearm, hand
    Part(capsule(0.045f, 0.22f, 2, 6), Vec3(0.315f, 1.4f, 0f), mirror = true),
    Part(
        capsule(0.04f, 0.2f, 2, 6),
        Vec3(0.35f, 1.1f, 0.02f),
        Vec3(0.12f, 0f, -0.08f),
        mirror = true,
    ),
    Part(sphere(0.05f, 6, 5), Vec3(0.365f, 0.94f, 0.05f), mirror = true),
    // shin and foot
    Part(capsule(0.045f, 0.34f, 2, 6), Vec3(0.12f, 0.3f, 0.02f), mirror = true),
    Part(box(0.09f, 0.05f, 0.22f), Vec3(0.12f, 0.03f, 0.06f), mirror = true),
)

private class ZoneDef(val slug: String, val parts: List<Part>, val pose: CameraPose)

private val zoneDefs: List<ZoneDef> = listOf(
    ZoneDef(
        slug = "chest",
        parts = listOf(
            Part(
                box(0.16f, 0.13f, 0.07f),
                Vec3(0.085f, 1.46f, 0.1f),
                Vec3(0f, -0.15f, 0f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.25f, 1.55f, 1.5f), Vec3(0f, 1.44f, 0f)),
    ),
    ZoneDef(
        slug = "abs",
        parts = listOf(
            Part(box(0.085f, 0.075f, 0.05f), Vec3(0.05f, 1.28f, 0.095f), mirror = true),
            Part(box(0.085f, 0.075f, 0.05f), Vec3(0.05f, 1.19f, 0.095f), mirror = true),
            Part(box(0.085f, 0.075f, 0.05f), Vec3(0.05f, 1.1f, 0.095f), mirror = true),
        ),
        pose = CameraPose(Vec3(0f, 1.28f, 1.35f), Vec3(0f, 1.19f, 0f)),
    ),
    ZoneDef(
        slug = "back",
        parts = listOf(
            Part(box(0.3f, 0.3f, 0.06f), Vec3(0f, 1.42f, -0.095f)),
            Part(
                box(0.1f, 0.26f, 0.06f),
                Vec3(0.17f, 1.28f, -0.075f),
                Vec3(0f, 0.2f, 0.18f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.2f, 1.6f, -1.7f), Vec3(0f, 1.35f, 0f)),
    ),
    ZoneDef(
        slug = "shoulders",
        parts = listOf(
            Part(sphere(0.085f, 7, 6), Vec3(0.3f, 1.57f, 0f), mirror = true),
        ),
        pose = CameraPose(Vec3(0.5f, 1.75f, 1.35f), Vec3(0f, 1.56f, 0f)),
    ),
    ZoneDef(
        slug = "traps",
        parts = listOf(
            // slabs sloping from the base of the neck down to each shoulder
            Part(
                box(0.2f, 0.07f, 0.11f),
                Vec3(0.14f, 1.55f, -0.02f),
                Vec3(0f, 0f, -0.55f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.2f, 1.92f, 1.25f), Vec3(0f, 1.55f, -0.02f)),
    ),
    ZoneDef(
        slug = "biceps",
        parts = listOf(
            Part(
                capsule(0.05f, 0.16f, 2, 6),
                Vec3(0.315f, 1.4f, 0.055f),
                Vec3(-0.08f, 0f, 0f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.85f, 1.45f, 1.05f), Vec3(0.3f, 1.4f, 0f)),
    ),
    ZoneDef(
        slug = "triceps",
        parts = listOf(
            Part(
                capsule(0.05f, 0.17f, 2, 6),
                Vec3(0.32f, 1.39f, -0.05f),
                Vec3(0.08f, 0f, 0f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.95f, 1.45f, -0.95f), Vec3(0.3f, 1.4f, 0f)),
    ),
    ZoneDef(
        slug = "forearms",
        parts = listOf(
            Part(
                capsule(0.045f, 0.19f, 2, 6),
                Vec3(0.35f, 1.1f, 0.03f),
                Vec3(0.12f, 0f, -0.08f),
                mirror = true,
            ),
        ),
        pose = CameraPose(Vec3(0.85f, 1.15f, 1.05f), Vec3(0.35f, 1.05f, 0.02f)),
    ),
    ZoneDef(
        slug = "quads",
        parts = listOf(
            Part(capsule(0.08f, 0.32f, 2, 7), Vec3(0.115f, 0.72f, 0.045f), mirror = true),
        ),
        pose = CameraPose(Vec3(0.3f, 0.95f, 1.6f), Vec3(0f, 0.75f, 0f)),
    ),
    ZoneDef(
        slug = "hams-glutes",
        parts = listOf(
            Part(sphere(0.085f, 7, 6), Vec3(0.1f, 0.96f, -0.08f), mirror = true),
            Part(capsule(0.07f, 0.28f, 2, 7), Vec3(0.115f, 0.7f, -0.055f), mirror = true),
        ),
        pose = CameraPose(Vec3(0.3f, 1f, -1.65f), Vec3(0f, 0.82f, 0f)),
    ),
    ZoneDef(
        slug = "calves",
        parts = listOf(
            Part(capsule(0.055f, 0.22f, 2, 7), Vec3(0.12f, 0.34f, -0.045f), mirror = true),
        ),
        pose = CameraPose(Vec3(0.45f, 0.5f, -1.4f), Vec3(0f, 0.34f, 0f)),
    ),
)

// ── assembly ──────────────────────────────────────────────────────

/** Rotation about X, then Y, then Z - the order three.js uses by default. */
private fun rotate(v: Vec3, r: Vec3): Vec3 {
    var x = v.x
    var y = v.y
    var z = v.z

    if (r.x != 0f) {
        val c = cos(r.x)
        val s = sin(r.x)
        val ny = y * c - z * s
        val nz = y * s + z * c
        y = ny
        z = nz
    }
    if (r.y != 0f) {
        val c = cos(r.y)
        val s = sin(r.y)
        val nx = x * c + z * s
        val nz = -x * s + z * c
        x = nx
        z = nz
    }
    if (r.z != 0f) {
        val c = cos(r.z)
        val s = sin(r.z)
        val nx = x * c - y * s
        val ny = x * s + y * c
        x = nx
        y = ny
    }
    return Vec3(x, y, z)
}

private fun mirrored(part: Part) = Part(
    mesh = part.mesh,
    position = Vec3(-part.position.x, part.position.y, part.position.z),
    rotation = Vec3(part.rotation.x, -part.rotation.y, -part.rotation.z),
)

private fun expand(parts: List<Part>): List<Part> =
    parts.flatMap { if (it.mirror) listOf(it, mirrored(it)) else listOf(it) }

/**
 * Bakes the rig into flat world-space arrays.
 *
 * Mirroring by negating x would flip every triangle inside out, so the mirror
 * is applied to the part transform rather than to the mesh: the geometry is
 * rotated and moved, never reflected, and the winding survives. That is also
 * why [mirrored] negates the y and z rotations but not x.
 */
fun buildAnatomyScene(): AnatomyScene {
    val positions = ArrayList<Float>(4096)
    val indices = ArrayList<Int>(6144)
    val triangleZone = ArrayList<Int>(2048)

    fun append(parts: List<Part>, zone: Int) {
        for (part in expand(parts)) {
            val base = positions.size / 3
            for (i in 0 until part.mesh.vertexCount) {
                val world = rotate(part.mesh.vertex(i), part.rotation) + part.position
                positions.add(world.x)
                positions.add(world.y)
                positions.add(world.z)
            }
            for (t in 0 until part.mesh.triangleCount) {
                indices.add(base + part.mesh.indices[t * 3])
                indices.add(base + part.mesh.indices[t * 3 + 1])
                indices.add(base + part.mesh.indices[t * 3 + 2])
                triangleZone.add(zone)
            }
        }
    }

    append(chassis, -1)

    val zones = zoneDefs.mapIndexed { index, def ->
        append(def.parts, index)
        val group = ExerciseCatalog.group(def.slug)
        AnatomyZone(
            slug = def.slug,
            // the catalog owns the display name and the accent; the rig only
            // owns the shape, so renaming a group never means editing geometry
            displayName = group?.displayName ?: def.slug,
            accentArgb = group?.accentArgb ?: 0xFF7C3AED,
            pose = def.pose,
        )
    }

    return AnatomyScene(
        positions = positions.toFloatArray(),
        indices = indices.toIntArray(),
        triangleZone = triangleZone.toIntArray(),
        zones = zones,
    )
}
