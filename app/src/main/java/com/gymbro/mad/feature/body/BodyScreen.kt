package com.gymbro.mad.feature.body

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.mad.anatomy.AnatomyCamera
import com.gymbro.mad.anatomy.AnatomyRenderer
import com.gymbro.mad.anatomy.AnatomyStyle
import com.gymbro.mad.anatomy.DefaultPose
import com.gymbro.mad.anatomy.buildAnatomyScene
import com.gymbro.mad.data.catalog.ExerciseCatalog
import com.gymbro.mad.data.model.MuscleVolume
import com.gymbro.mad.data.repo.AnalyticsRepository
import com.gymbro.mad.data.repo.Repos
import com.gymbro.mad.ui.components.GymBroCard
import com.gymbro.mad.ui.components.GymBroChip
import com.gymbro.mad.ui.components.MicroLabel
import com.gymbro.mad.ui.components.SectionHeader
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.MicroStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The body map: the rig from AnatomyRig.kt, drawn by Renderer.kt, with the
 * eleven muscle groups lit by how much work they have actually taken.
 *
 * Drag turns it, pinch zooms, a tap picks a muscle, and a chip flies the
 * camera to that group's angle - the same angles the website uses, so the two
 * show a muscle the same way round.
 *
 * The glow is data, not decoration: a group with no logged volume sits at its
 * idle level, and the only way to brighten it is to train it.
 */
@Composable
fun BodyScreen(
    modifier: Modifier = Modifier,
    analytics: AnalyticsRepository = Repos.analytics,
    onOpenExercises: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current

    // The rig is ~1,800 triangles of generated geometry: built once per
    // process, not once per recomposition.
    val scene = remember { buildAnatomyScene() }
    val renderer = remember(scene) { AnatomyRenderer(scene) }
    val glow = remember(scene) { FloatArray(scene.zones.size) }

    val camera = remember { OrbitCamera(DefaultPose) }
    val scope = rememberCoroutineScope()
    var flight by remember { mutableStateOf<Job?>(null) }

    var selectedSlug by remember { mutableStateOf<String?>(null) }
    var autoRotate by remember { mutableStateOf(true) }

    val volumes by analytics.muscleVolume(days = 28)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val intensity = remember(volumes) { normalise(volumes) }

    // Only subscribed to while something is selected, so an idle screen is a
    // still screen rather than a 60fps battery drain.
    val pulse = if (selectedSlug != null) rememberPulse() else 0f

    if (autoRotate) {
        LaunchedFrameLoop { deltaSeconds -> camera.spin(0.35f * deltaSeconds) }
    }

    val style = AnatomyStyle(
        zoneBaseArgb = lerp(palette.raised, palette.muted, 0.20f).toArgb(),
        chassisBaseArgb = lerp(palette.surface, palette.muted, 0.12f).toArgb(),
        chassisAccentArgb = palette.accent2.toArgb(),
        groundArgb = palette.accent.copy(alpha = 0.20f).toArgb(),
    )

    Column(modifier = modifier.fillMaxSize()) {

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(scene) {
                        detectTapGestures { offset ->
                            val zone = renderer.pickZone(offset.x, offset.y)
                            selectedSlug = if (zone < 0) {
                                null
                            } else {
                                scene.zones[zone].slug.takeIf { it != selectedSlug }
                            }
                        }
                    }
                    .pointerInput(scene) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // a finger on the figure beats both the idle spin
                            // and any camera flight in progress
                            autoRotate = false
                            flight?.cancel()
                            camera.drag(pan.x, pan.y)
                            if (zoom != 1f) camera.zoomBy(zoom)
                        }
                    },
            ) {
                val selectedIndex = selectedSlug?.let { scene.zoneIndex(it) } ?: -1
                for (i in glow.indices) {
                    val trained = intensity[scene.zones[i].slug] ?: 0f
                    glow[i] = when (i) {
                        selectedIndex -> 0.95f + pulse * 0.4f
                        else -> IDLE_GLOW + trained * 0.6f
                    }
                }
                drawIntoCanvas { canvas ->
                    renderer.render(
                        canvas = canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        camera = camera.pose(),
                        style = style,
                        zoneGlow = glow,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlayButton(
                    icon = if (autoRotate) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    description = if (autoRotate) "Stop turning" else "Turn the model",
                ) { autoRotate = !autoRotate }

                OverlayButton(
                    icon = Icons.Filled.CenterFocusWeak,
                    description = "Reset the view",
                ) {
                    autoRotate = false
                    flight?.cancel()
                    flight = scope.launch { camera.flyTo(DefaultPose) }
                    selectedSlug = null
                }
            }

            Text(
                text = "drag to turn  ·  pinch to zoom  ·  tap a muscle",
                style = MicroStyle,
                color = palette.faint,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scene.zones.forEach { zone ->
                    GymBroChip(
                        label = zone.displayName,
                        selected = zone.slug == selectedSlug,
                        accent = Color(zone.accentArgb),
                        onClick = {
                            selectedSlug = zone.slug
                            autoRotate = false
                            flight?.cancel()
                            flight = scope.launch { camera.flyTo(zone.pose) }
                        },
                    )
                }
            }

            val slug = selectedSlug
            if (slug == null) {
                GymBroCard {
                    Text(
                        text = "Pick a muscle",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap the figure or a chip to see what you have been " +
                            "training. Brighter means more volume over the last 28 days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted,
                    )
                }
            } else {
                SelectedGroupCard(
                    slug = slug,
                    volume = volumes.firstOrNull { it.groupSlug == slug },
                    onOpenExercises = onOpenExercises,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectedGroupCard(
    slug: String,
    volume: MuscleVolume?,
    onOpenExercises: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val group = ExerciseCatalog.group(slug) ?: return
    val accent = Color(group.accentArgb)
    val movements = ExerciseCatalog.forGroup(slug).size

    GymBroCard(modifier = modifier, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            MicroLabel("$movements movements", color = palette.faint)
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader("last 28 days")
        Spacer(Modifier.height(8.dp))

        if (volume == null || volume.sets == 0) {
            // Honest empty: this group has had no work, and the figure shows
            // it unlit. Do not substitute a plausible-looking number here.
            Text(
                text = "Nothing logged for this group yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text(
                        text = "${volume.volumeKg.roundToInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                    )
                    MicroLabel("kg volume")
                }
                Column {
                    Text(
                        text = "${volume.sets}",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink,
                    )
                    MicroLabel("sets")
                }
            }
        }

        if (onOpenExercises != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.raised)
                    .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenExercises)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                MicroLabel("see the $movements movements", color = palette.accentInk)
            }
        }
    }
}

@Composable
private fun OverlayButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.surface.copy(alpha = 0.82f))
            .border(1.dp, palette.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 0f..1f per muscle group, scaled against the hardest-worked one. */
private fun normalise(volumes: List<MuscleVolume>): Map<String, Float> {
    val peak = volumes.maxOfOrNull { it.volumeKg } ?: return emptyMap()
    if (peak <= 0.0) return emptyMap()
    return volumes.associate { it.groupSlug to (it.volumeKg / peak).toFloat() }
}

@Composable
private fun rememberPulse(): Float {
    val transition = rememberInfiniteTransition(label = "selection")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    return value
}

private const val IDLE_GLOW = 0.07f
