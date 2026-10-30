package com.gymbro.mad.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as DownloadableFont
import androidx.compose.ui.unit.sp
import com.gymbro.mad.R

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun downloadableFamily(name: String) = FontFamily(
    DownloadableFont(GoogleFont(name), GoogleFontsProvider, FontWeight.Normal),
    DownloadableFont(GoogleFont(name), GoogleFontsProvider, FontWeight.Medium),
    DownloadableFont(GoogleFont(name), GoogleFontsProvider, FontWeight.SemiBold),
    DownloadableFont(GoogleFont(name), GoogleFontsProvider, FontWeight.Bold),
)

private val SpaceGrotesk = downloadableFamily("Space Grotesk")
private val IbmPlexSans = downloadableFamily("IBM Plex Sans")
private val IbmPlexMono = downloadableFamily("IBM Plex Mono")

/**
 * Type scale.
 *
 * The web app runs Space Grotesk for headlines and IBM Plex for body and
 * numbers. Android's downloadable-font provider supplies those families;
 * Compose falls back to the platform family if the provider is unavailable.
 */
val GymBroTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = IbmPlexSans, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = IbmPlexSans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = IbmPlexSans, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = SpaceGrotesk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = IbmPlexSans, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(
        fontFamily = IbmPlexMono,
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
    fontFamily = IbmPlexMono,
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.8.sp,
)

/** Numbers that should line up in a column: weights, reps, volume, kcal. */
val NumericStyle = TextStyle(
    fontFamily = IbmPlexMono,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
)
