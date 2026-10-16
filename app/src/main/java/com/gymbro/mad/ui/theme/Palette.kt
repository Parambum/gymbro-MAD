package com.gymbro.mad.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The three GymBro palettes, carried over from the web app unchanged.
 *
 * Material3 only has slots for about half of these, so the full set travels
 * through [LocalPalette] instead of the colour scheme. Use these names rather
 * than literal colours: a hard-coded hex is a colour that will not retone when
 * someone switches theme, and it is invisible in review.
 *
 * `accent` is the glow colour. `accentInk` is the one that stays legible as
 * text or as a chart mark on `surface` - they are the same in Court and
 * Classic but deliberately different in Molten, where pure ember on near-black
 * fails contrast at small sizes.
 */
enum class ThemeId(val label: String) {
    MOLTEN("Molten"),
    COURT("Court"),
    CLASSIC("Classic"),
}

@Immutable
data class Palette(
    val id: ThemeId,
    /** page ground */
    val bg: Color,
    /** cards sitting on the ground */
    val surface: Color,
    /** controls and rows sitting on a card */
    val raised: Color,
    val border: Color,
    /** primary text */
    val ink: Color,
    /** secondary text */
    val muted: Color,
    /** micro-labels, disabled text, axis lines */
    val faint: Color,
    val accent: Color,
    val accentInk: Color,
    val accent2: Color,
    val info: Color,
    val warn: Color,
    val danger: Color,
)

private val Molten = Palette(
    id = ThemeId.MOLTEN,
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF141417),
    raised = Color(0xFF1C1C20),
    border = Color(0xFF2A2A30),
    ink = Color(0xFFF2F2F0),
    muted = Color(0xFF96969E),
    faint = Color(0xFF64646C),
    accent = Color(0xFFFF5A1F),
    accentInk = Color(0xFFFF8A5C),
    accent2 = Color(0xFFFFB020),
    info = Color(0xFF5AB0FF),
    warn = Color(0xFFFFB020),
    danger = Color(0xFFFF5B5B),
)

private val Court = Palette(
    id = ThemeId.COURT,
    bg = Color(0xFF0A0F1A),
    surface = Color(0xFF121A2A),
    raised = Color(0xFF1A2438),
    border = Color(0xFF283650),
    ink = Color(0xFFEAF0FF),
    muted = Color(0xFF94A3BE),
    faint = Color(0xFF647491),
    accent = Color(0xFFC6FF3D),
    accentInk = Color(0xFFC6FF3D),
    accent2 = Color(0xFF37D3FF),
    info = Color(0xFF37D3FF),
    warn = Color(0xFFFACC15),
    danger = Color(0xFFFF6B6B),
)

private val Classic = Palette(
    id = ThemeId.CLASSIC,
    bg = Color(0xFF05050A),
    surface = Color(0xFF0A0A14),
    raised = Color(0xFF0E0E1C),
    border = Color(0xFF1E1E33),
    ink = Color(0xFFD4D4DC),
    muted = Color(0xFFA1A1AA),
    faint = Color(0xFF71717A),
    accent = Color(0xFF22FF88),
    accentInk = Color(0xFF4ADE80),
    accent2 = Color(0xFF7C3AED),
    info = Color(0xFF38BDF8),
    warn = Color(0xFFFBBF24),
    danger = Color(0xFFFF2D55),
)

fun paletteFor(id: ThemeId): Palette = when (id) {
    ThemeId.MOLTEN -> Molten
    ThemeId.COURT -> Court
    ThemeId.CLASSIC -> Classic
}
