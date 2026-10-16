package com.gymbro.mad.ui.nav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.MicroStyle

/**
 * The app chrome: a slim identity bar at the top, the six tabs at the bottom.
 * Both are driven entirely by the current route, so navigating is the only way
 * to change them and there is no second source of truth about where you are.
 */

@Composable
fun GymBroTopBar(
    route: String?,
    onToggleTheme: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val innerTitle = innerRouteTitle(route)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null && innerTitle != null) {
                IconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = innerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                    modifier = Modifier.weight(1f),
                )
            } else {
                GymBroMark()
                Spacer(Modifier.weight(1f))
            }

            IconAction(Icons.Filled.Palette, "Change theme", onToggleTheme)
            IconAction(Icons.Filled.Person, "Profile", onOpenProfile)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(palette.border),
        )
    }
}

@Composable
fun GymBroBottomBar(
    current: Tab?,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    NavigationBar(
        modifier = modifier,
        containerColor = palette.surface,
        contentColor = palette.muted,
        tonalElevation = 0.dp,
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(21.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MicroStyle.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accentInk,
                    selectedTextColor = palette.accentInk,
                    indicatorColor = palette.accent.copy(alpha = 0.14f),
                    unselectedIconColor = palette.faint,
                    unselectedTextColor = palette.faint,
                ),
            )
        }
    }
}

/** The wordmark: a drawn barbell and the name, matching the launcher icon. */
@Composable
fun GymBroMark(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 22.dp, height = 16.dp)) {
            val h = size.height
            val w = size.width
            val plate = Size(w * 0.13f, h * 0.86f)
            val collar = Size(w * 0.11f, h * 0.55f)
            val bar = Size(w * 0.42f, h * 0.2f)

            // left and right plates
            drawRoundRect(
                color = palette.accent2,
                topLeft = Offset(0f, (h - plate.height) / 2f),
                size = plate,
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            drawRoundRect(
                color = palette.accent2,
                topLeft = Offset(w - plate.width, (h - plate.height) / 2f),
                size = plate,
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            // inner collars
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(w * 0.18f, (h - collar.height) / 2f),
                size = collar,
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(w * 0.71f, (h - collar.height) / 2f),
                size = collar,
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            // the bar between them
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(w * 0.29f, (h - bar.height) / 2f),
                size = bar,
                cornerRadius = CornerRadius(1f, 1f),
            )
        }
        Text(
            text = "GYMBRO",
            style = MicroStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.4.sp,
            ),
            color = palette.ink,
        )
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.muted,
            modifier = Modifier.size(19.dp),
        )
    }
}
