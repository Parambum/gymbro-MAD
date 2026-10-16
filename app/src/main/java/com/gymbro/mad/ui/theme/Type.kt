package com.gymbro.mad.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale.
 *
 * The web app runs Space Grotesk for headlines and IBM Plex for body and
 * numbers; those are not bundled here yet (ticket A-6 adds them through
 * downloadable fonts). Until then the platform sans carries the UI and the
 * platform monospace carries numbers and micro-labels, which is the part of
 * the look that actually depends on the family: a set of reps in a
 * proportional font jitters as the digits change width.
 */
val GymBroTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
    ),
)

/**
 * Uppercase, wide-tracked, monospace: the section labels and chip text that
 * give the app its instrument-panel feel. Pass the text already uppercased -
 * `textTransform` has no Compose equivalent.
 */
val MicroStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.8.sp,
)

/** Numbers that should line up in a column: weights, reps, volume, kcal. */
val NumericStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
)
