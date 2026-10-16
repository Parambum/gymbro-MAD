package com.gymbro.mad.anatomy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * A small software renderer for the anatomy figure.
 *
 * The pipeline, per frame, is the classic one:
 *
 *  1. transform every vertex into view space with a look-at basis;
 *  2. project to the screen through a pinhole camera;
 *  3. drop back faces using the view-space normal;
 *  4. flat-shade each surviving triangle - one lambert term plus a rim term,
 *     mixed with its muscle group accent;
 *  5. sort back to front and fill them in that order.
 *
 * Step 5 is the painter's algorithm, which gets the depth order wrong for
 * interpenetrating surfaces. On a figure made of separated convex limbs that
 * never shows, and it costs one sort instead of a per-pixel depth buffer we
 * would have to allocate and clear every frame.
 *
 * Everything the loop touches is allocated once in the constructor. A renderer
 * that allocates per frame is a renderer that stutters when the collector
 * runs, and this one is asked to redraw at 60fps while a finger drags it.
 */
class AnatomyRenderer(val scene: AnatomyScene) {

    private val vertexCount = scene.vertexCount
    private val triangleCount = scene.triangleCount

    // view space
    private val vx = FloatArray(vertexCount)
    private val vy = FloatArray(vertexCount)
    private val vz = FloatArray(vertexCount)

    // screen space
    private val sx = FloatArray(vertexCount)
    private val sy = FloatArray(vertexCount)

    // per triangle
    private val depth = FloatArray(triangleCount)
    private val colour = IntArray(triangleCount)
    private val order = IntArray(triangleCount)
    private var visible = 0
    private var hasFrame = false

    // Lazy, so the geometry pass can run in a plain JVM unit test: android
    // .graphics.Paint and Path only exist on a device, and the maths worth
    // testing - camera, culling, hit testing - does not need them.
    private val path by lazy { Path() }
    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            // A hairline stroke in the fill colour closes the seams
            // antialiasing leaves between neighbouring triangles, which
            // otherwise read as a net of dark cracks across the figure.
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 0.7f
        }
    }
    private val groundPaint by lazy { Paint().apply { isAntiAlias = true } }
    private var groundRadius = -1f

    /** Light fixed in view space, so the figure never spins into the dark. */
    private val light = Vec3(-0.4f, 0.75f, 0.85f).normalized()

    // the camera basis for the frame being drawn, written by project() and
    // read again by drawGround()
    private var eye = Vec3.ZERO
    private var xAxis = Vec3.ZERO
    private var yAxis = Vec3.ZERO
    private var zAxis = Vec3.ZERO
    private var focal = 1f
    private var centreX = 0f
    private var centreY = 0f

    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        camera: AnatomyCamera,
        style: AnatomyStyle,
        /** per-zone glow, 0f..1.6f, indexed like [AnatomyScene.zones] */
        zoneGlow: FloatArray,
    ) {
        if (width <= 0f || height <= 0f) return

        computeFrame(width, height, camera, style, zoneGlow)
        drawGround(canvas, style)
        drawTriangles(canvas)
    }

    /**
     * Steps 1 to 4 without touching a canvas: everything needed to know what
     * is on screen and where. [render] calls this and then draws; a test calls
     * it and then asks [pickZone] or [zoneScreenPoint] what it would have hit.
     *
     * Returns the number of triangles that survived culling.
     */
    fun computeFrame(
        width: Float,
        height: Float,
        camera: AnatomyCamera,
        style: AnatomyStyle,
        zoneGlow: FloatArray,
    ): Int {
        project(width, height, camera)
        shadeAndCull(style, zoneGlow)
        sortByDepth()
        hasFrame = true
        return visible
    }

    // ── 1 and 2: view transform and projection ────────────────────

    private fun project(width: Float, height: Float, camera: AnatomyCamera) {
        val cp = cos(camera.pitch)

        // Camera orbits the target: the pose is spherical, the basis is a
        // look-at built from it.
        val dir = Vec3(sin(camera.yaw) * cp, sin(camera.pitch), cos(camera.yaw) * cp)
        eye = camera.target + dir * camera.distance

        zAxis = dir.normalized() // points from the target back to the eye
        xAxis = (Vec3(0f, 1f, 0f) cross zAxis).normalized()
        yAxis = zAxis cross xAxis

        focal = (height / 2f) / tan(Math.toRadians(camera.fovDegrees / 2.0).toFloat())
        centreX = width / 2f
        centreY = height / 2f

        val p = scene.positions
        for (i in 0 until vertexCount) {
            val dx = p[i * 3] - eye.x
            val dy = p[i * 3 + 1] - eye.y
            val dz = p[i * 3 + 2] - eye.z

            val x = dx * xAxis.x + dy * xAxis.y + dz * xAxis.z
            val y = dx * yAxis.x + dy * yAxis.y + dz * yAxis.z
            val z = dx * zAxis.x + dy * zAxis.y + dz * zAxis.z

            vx[i] = x
            vy[i] = y
            vz[i] = z

            // the camera looks down -z, so anything in front has z < 0
            val d = max(0.01f, -z)
            sx[i] = centreX + focal * x / d
            sy[i] = centreY - focal * y / d
        }
    }

    // ── 3 and 4: cull, shade ──────────────────────────────────────

    private fun shadeAndCull(style: AnatomyStyle, zoneGlow: FloatArray) {
        visible = 0
        val idx = scene.indices

        for (t in 0 until triangleCount) {
            val a = idx[t * 3]
            val b = idx[t * 3 + 1]
            val c = idx[t * 3 + 2]

            // anything that crossed behind the lens is dropped whole: clipping
            // a triangle properly is a lot of code for a figure that is never
            // close enough to the camera to need it
            if (vz[a] > -0.02f || vz[b] > -0.02f || vz[c] > -0.02f) continue

            val e1x = vx[b] - vx[a]
            val e1y = vy[b] - vy[a]
            val e1z = vz[b] - vz[a]
            val e2x = vx[c] - vx[a]
            val e2y = vy[c] - vy[a]
            val e2z = vz[c] - vz[a]

            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
            if (len < 1e-7f) continue
            nx /= len
            ny /= len
            nz /= len

            // outward normals face the camera, which sits at +z in view space
            if (nz <= 0f) continue

            val zone = scene.triangleZone[t]
            val glow = if (zone >= 0) zoneGlow.glowAt(zone) else style.chassisGlow
            val base = if (zone >= 0) style.zoneBaseArgb else style.chassisBaseArgb
            val accent = if (zone >= 0) scene.zones[zone].accentArgb.toInt() else style.chassisAccentArgb

            val lambert = max(0f, nx * light.x + ny * light.y + nz * light.z)
            val lit = AMBIENT + (1f - AMBIENT) * lambert
            // edge-on faces pick up the accent, which is what gives the figure
            // its neon outline without drawing a single outline
            val rim = (1f - nz).let { it * it * it } * (0.25f + 0.45f * glow)
            val tint = glow * 0.8f + rim

            // packed by hand rather than through Color.argb: one less call per
            // triangle, and the shading pass stays testable off a device
            colour[t] = OPAQUE or
                (channel(base shr 16, accent shr 16, lit, tint) shl 16) or
                (channel(base shr 8, accent shr 8, lit, tint) shl 8) or
                channel(base, accent, lit, tint)
            depth[t] = (vz[a] + vz[b] + vz[c]) / 3f
            order[visible++] = t
        }
    }

    private fun channel(base: Int, accent: Int, lit: Float, tint: Float): Int {
        val b = (base and 0xFF) * lit
        val a = (accent and 0xFF) * tint
        return min(255f, b + a).toInt()
    }

    // ── 5: depth sort and draw ────────────────────────────────────

    /**
     * Quicksort over [order], keyed on [depth], most negative (farthest) first.
     * Hand-rolled because the library sorts want boxed keys or a comparator,
     * and 1,800 Integers per frame is 1,800 objects for the collector to find
     * again sixteen milliseconds later.
     */
    private fun sortByDepth() {
        quicksort(0, visible - 1)
    }

    private fun quicksort(lo: Int, hi: Int) {
        if (lo >= hi) return
        var i = lo
        var j = hi
        val pivot = depth[order[(lo + hi) / 2]]
        while (i <= j) {
            while (depth[order[i]] < pivot) i++
            while (depth[order[j]] > pivot) j--
            if (i <= j) {
                val tmp = order[i]
                order[i] = order[j]
                order[j] = tmp
                i++
                j--
            }
        }
        quicksort(lo, j)
        quicksort(i, hi)
    }

    private fun drawTriangles(canvas: Canvas) {
        val idx = scene.indices
        for (k in 0 until visible) {
            val t = order[k]
            val a = idx[t * 3]
            val b = idx[t * 3 + 1]
            val c = idx[t * 3 + 2]

            // a triangle under a pixel across contributes nothing but cost
            val area = abs(
                (sx[b] - sx[a]) * (sy[c] - sy[a]) - (sx[c] - sx[a]) * (sy[b] - sy[a]),
            ) * 0.5f
            if (area < 0.35f) continue

            path.rewind()
            path.moveTo(sx[a], sy[a])
            path.lineTo(sx[b], sy[b])
            path.lineTo(sx[c], sy[c])
            path.close()

            paint.color = colour[t]
            canvas.drawPath(path, paint)
        }
    }

    /**
     * A squashed radial glow where the figure meets the floor. Without it the
     * body floats; it is also the only thing on screen that tells you where
     * the ground is once the camera tilts.
     */
    private fun drawGround(canvas: Canvas, style: AnatomyStyle) {
        // the point between the feet, through the basis project() just built
        val dx = -eye.x
        val dy = -eye.y
        val dz = -eye.z
        val z = dx * zAxis.x + dy * zAxis.y + dz * zAxis.z
        if (z >= -0.02f) return

        val d = -z
        val fx = centreX + focal * (dx * xAxis.x + dy * xAxis.y + dz * xAxis.z) / d
        val fy = centreY - focal * (dx * yAxis.x + dy * yAxis.y + dz * yAxis.z) / d
        val radius = focal * 0.42f / d

        if (radius <= 1f) return
        if (abs(radius - groundRadius) > 1.5f) {
            groundRadius = radius
            groundPaint.shader = RadialGradient(
                0f,
                0f,
                radius,
                intArrayOf(style.groundArgb, style.groundArgb and 0x00FFFFFF),
                null,
                Shader.TileMode.CLAMP,
            )
        }

        canvas.save()
        canvas.translate(fx, fy)
        canvas.scale(1f, 0.3f)
        canvas.drawCircle(0f, 0f, radius, groundPaint)
        canvas.restore()
    }

    // ── hit testing ───────────────────────────────────────────────

    /**
     * Which zone is under the touch, or -1 for none.
     *
     * The walk is front to back and the FIRST surface wins, chassis included.
     * That matters: the back slab sits inside the torso, and its front face
     * survives culling even when you are looking at the chest. Letting the
     * chassis block a pick is what stops a tap on the sternum selecting the
     * lats behind it.
     *
     * A tap that lands on chassis then gets a second chance at the nearest
     * zone within [slopPx] - the calves on a phone-sized figure are a target
     * about four millimetres across, and a fingertip is wider than that - but
     * only among zones at least as near as the chassis it hit, so the slop can
     * never reach through the body.
     */
    fun pickZone(x: Float, y: Float, slopPx: Float = 26f): Int {
        if (!hasFrame) return -1
        val idx = scene.indices

        var blockedAt = Float.NEGATIVE_INFINITY
        for (k in visible - 1 downTo 0) {
            val t = order[k]
            val a = idx[t * 3]
            val b = idx[t * 3 + 1]
            val c = idx[t * 3 + 2]
            if (!pointInTriangle(x, y, sx[a], sy[a], sx[b], sy[b], sx[c], sy[c])) continue

            val zone = scene.triangleZone[t]
            if (zone >= 0) return zone
            blockedAt = depth[t] // view z, so nearer is larger
            break
        }

        var best = -1
        var bestDistance = slopPx * slopPx
        for (k in visible - 1 downTo 0) {
            val t = order[k]
            val zone = scene.triangleZone[t]
            if (zone < 0 || depth[t] < blockedAt) continue
            val cx = (sx[idx[t * 3]] + sx[idx[t * 3 + 1]] + sx[idx[t * 3 + 2]]) / 3f
            val cy = (sy[idx[t * 3]] + sy[idx[t * 3 + 1]] + sy[idx[t * 3 + 2]]) / 3f
            val dd = (cx - x) * (cx - x) + (cy - y) * (cy - y)
            if (dd < bestDistance) {
                bestDistance = dd
                best = zone
            }
        }
        return best
    }

    /**
     * Where a zone sits on screen right now - the centroid of its nearest
     * triangle facing the camera - or null if none of it faces the camera at
     * all.
     *
     * The nearest triangle rather than an average of the zone, because the
     * mirrored zones have two halves: the average of both pecs is the middle
     * of the sternum, which is not on either of them. Occlusion is not
     * considered, so a point can come back for a zone hidden behind the body;
     * ask [pickZone] about that point if you need to know.
     */
    fun zoneScreenPoint(zoneIndex: Int): FloatArray? {
        if (!hasFrame) return null
        val idx = scene.indices
        var nearest = -1
        for (k in visible - 1 downTo 0) {
            val t = order[k]
            if (scene.triangleZone[t] != zoneIndex) continue
            nearest = t
            break
        }
        if (nearest < 0) return null
        return floatArrayOf(
            (sx[idx[nearest * 3]] + sx[idx[nearest * 3 + 1]] + sx[idx[nearest * 3 + 2]]) / 3f,
            (sy[idx[nearest * 3]] + sy[idx[nearest * 3 + 1]] + sy[idx[nearest * 3 + 2]]) / 3f,
        )
    }

    private fun pointInTriangle(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
    ): Boolean {
        val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
        val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
        val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
        val hasNeg = d1 < 0f || d2 < 0f || d3 < 0f
        val hasPos = d1 > 0f || d2 > 0f || d3 > 0f
        return !(hasNeg && hasPos)
    }

    private companion object {
        /** how lit a surface is with the light behind it; 0 would be pitch black */
        const val AMBIENT = 0.24f

        /** alpha 255, ready to be or-ed with the three colour channels */
        const val OPAQUE = 0xFF shl 24
    }
}

/** Zone glow is indexed by zone, and a short array must not take down a frame. */
private fun FloatArray.glowAt(index: Int): Float =
    if (index >= 0 && index < size) this[index] else 0f

/** Where the camera is, in orbit terms around [target]. */
data class AnatomyCamera(
    val target: Vec3,
    val yaw: Float,
    val pitch: Float,
    val distance: Float,
    val fovDegrees: Float = 42f,
)

/**
 * The palette the renderer needs, as plain ARGB ints. Converted from the theme
 * by the screen, so this file stays free of Compose.
 */
data class AnatomyStyle(
    val zoneBaseArgb: Int,
    val chassisBaseArgb: Int,
    val chassisAccentArgb: Int,
    val groundArgb: Int,
    val chassisGlow: Float = 0.05f,
)
