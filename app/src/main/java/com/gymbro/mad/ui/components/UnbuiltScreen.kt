package com.gymbro.mad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Roadmap
import com.gymbro.mad.project.Ticket
import com.gymbro.mad.ui.theme.LocalPalette
import com.gymbro.mad.ui.theme.MicroStyle

/**
 * The placeholder every unbuilt tab shows.
 *
 * It is deliberately not a spinner and not a "coming soon" - it is a brief.
 * Whoever opens the tab sees who owns it, which tickets cover it, what the
 * screen is supposed to do, and which parts are already wired up for them. The
 * teacher sees the plan; the three of you see your next task without opening
 * another file.
 *
 * Replacing one of these is the whole job: delete the call, build the screen.
 */
@Composable
fun UnbuiltScreen(
    title: String,
    oneLiner: String,
    owner: Owner,
    route: String,
    whatGoesHere: List<String>,
    wiredForYou: List<String>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val tickets = Roadmap.forRoute(route)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.warn.copy(alpha = 0.14f))
                    .border(1.dp, palette.warn.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("NOT BUILT", style = MicroStyle, color = palette.warn)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = oneLiner,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )

        Spacer(Modifier.height(18.dp))
        OwnerBadge(owner)

        Spacer(Modifier.height(20.dp))
        SectionHeader("what goes here")
        Spacer(Modifier.height(10.dp))
        GymBroCard {
            whatGoesHere.forEachIndexed { index, line ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Bullet(line, palette.accent)
            }
        }

        if (tickets.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader(
                "tickets",
                trailing = {
                    MicroLabel(
                        "${tickets.sumOf { it.hours }}h left",
                        color = palette.faint,
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
            tickets.forEach { ticket ->
                TicketRow(ticket)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = "Acceptance criteria for each one are in TASKS.md.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.faint,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("already wired for you")
        Spacer(Modifier.height(10.dp))
        GymBroCard {
            wiredForYou.forEachIndexed { index, line ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Bullet(line, palette.info)
            }
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.raised)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            Text(
                text = "House rule: ship an empty state, never a fake one. If there is " +
                    "no data yet, say so on screen. A placeholder number that looks " +
                    "real is a bug nobody can see.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
    }
}

@Composable
fun OwnerBadge(owner: Owner, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.16f))
                .border(1.dp, palette.accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = owner.initials,
                style = MicroStyle.copy(fontWeight = FontWeight.Bold),
                color = palette.accentInk,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Owner: ${owner.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            Text(
                text = owner.area,
                style = MaterialTheme.typography.bodySmall,
                color = palette.faint,
            )
        }
    }
}

@Composable
private fun TicketRow(ticket: Ticket, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ticket.id,
            style = MicroStyle.copy(fontWeight = FontWeight.Bold),
            color = palette.accentInk,
            modifier = Modifier.width(38.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = ticket.title,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${ticket.hours}h",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = palette.faint,
        )
    }
}

@Composable
private fun Bullet(text: String, dotColor: Color, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            modifier = Modifier.weight(1f),
        )
    }
}
