package com.gymbro.mad.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The extra colour tokens Material3 has no slot for. Read it as
 * `LocalPalette.current` - every screen in the app does.
 */
val LocalPalette = staticCompositionLocalOf { paletteFor(ThemeId.MOLTEN) }

private val GymBroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The app is dark-only by design, so the system light/dark setting is not read
 * at all: there is no light palette to switch to, and pretending otherwise
 * would give a half-themed screen on a phone in light mode.
 *
 * Colours are animated rather than swapped, because a theme change mid-session
 * with 13 colours jumping at once reads as a glitch. 220ms is the same
 * duration the web app uses.
 */
@Composable
fun GymBroTheme(
    themeId: ThemeId = ThemeId.MOLTEN,
    content: @Composable () -> Unit,
) {
    val target = paletteFor(themeId)
    val spec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 220)

    val bg by animateColorAsState(target.bg, spec, label = "bg")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val raised by animateColorAsState(target.raised, spec, label = "raised")
    val border by animateColorAsState(target.border, spec, label = "border")
    val ink by animateColorAsState(target.ink, spec, label = "ink")
    val muted by animateColorAsState(target.muted, spec, label = "muted")
    val faint by animateColorAsState(target.faint, spec, label = "faint")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val accentInk by animateColorAsState(target.accentInk, spec, label = "accentInk")
    val accent2 by animateColorAsState(target.accent2, spec, label = "accent2")
    val info by animateColorAsState(target.info, spec, label = "info")
    val warn by animateColorAsState(target.warn, spec, label = "warn")
    val danger by animateColorAsState(target.danger, spec, label = "danger")

    val palette = Palette(
        id = target.id,
        bg = bg,
        surface = surface,
        raised = raised,
        border = border,
        ink = ink,
        muted = muted,
        faint = faint,
        accent = accent,
        accentInk = accentInk,
        accent2 = accent2,
        info = info,
        warn = warn,
        danger = danger,
    )

    val scheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.bg,
        primaryContainer = palette.raised,
        onPrimaryContainer = palette.accentInk,
        secondary = palette.accent2,
        onSecondary = palette.bg,
        background = palette.bg,
        onBackground = palette.ink,
        surface = palette.surface,
        onSurface = palette.ink,
        surfaceVariant = palette.raised,
        onSurfaceVariant = palette.muted,
        surfaceContainer = palette.raised,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.danger,
        onError = palette.bg,
    )

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GymBroTypography,
            shapes = GymBroShapes,
            content = content,
        )
    }
}
