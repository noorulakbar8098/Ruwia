package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.theme.RuwiaColor

// ─────────────────────────────────────────────────────────────────────────────
//  Employee Entries Tab
//  Full feed of the logged-in employee's stock movements (inward / outward /
//  adjustments) — backed by stock_movements rows filtered server-side by
//  employee_id, with client-side filter chips on top.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmployeeEntriesScreen(
    movements: List<StockMovement>,
    todayInward: Int,
    todayOutward: Int,
    todayEmptyCans: Int,
    dailyEarnings: Double,
    currentDate: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val filtered = remember(movements) {
        movements
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RuwiaColor.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        // ── Header with refresh ─────────────────────────────────────────────
        item { EntriesTopBar(currentDate = currentDate, onRefresh = onRefresh) }

        // ── Today's totals card ─────────────────────────────────────────────
        item {
            EntriesTotalsCard(
                outward   = todayOutward,
                emptyCans = todayEmptyCans,
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        // ── List body ───────────────────────────────────────────────────────
        when {
            isLoading && movements.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = RuwiaColor.TealPrimary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            filtered.isEmpty() -> {
                item {
                    EntriesEmptyState(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    )
                }
            }
            else -> {
                items(items = filtered, key = { "${it.id}-${it.createdAt}" }) { mov ->
                    EntryRow(movement = mov, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun EntriesTopBar(currentDate: String, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Entries",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = RuwiaColor.TextPrimary,
            )
            if (currentDate.isNotEmpty()) {
                Text(
                    currentDate,
                    fontSize = 12.sp,
                    color    = RuwiaColor.TextMuted,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint     = RuwiaColor.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Today's totals card ───────────────────────────────────────────────────────

@Composable
private fun EntriesTotalsCard(
    outward: Int,
    emptyCans: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RuwiaColor.TealDark)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TotalsCell(
            value    = "$outward",
            label    = "Outward Units",
            tint     = Color.White,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(Color.White.copy(alpha = 0.25f))
        )
        TotalsCell(
            value    = "$emptyCans",
            label    = "Empty Returned",
            tint     = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TotalsCell(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = tint,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = tint.copy(alpha = 0.78f),
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.18f)),
    )
}


// ── Entry row ─────────────────────────────────────────────────────────────────

@Composable
private fun EntryRow(movement: StockMovement, modifier: Modifier = Modifier) {
    val isInward = movement.type == "inward"
    val iconBg   = if (isInward) RuwiaColor.OrangeLight    else RuwiaColor.IconTealBg
    val iconTint = if (isInward) RuwiaColor.Orange         else RuwiaColor.TealPrimary
    val amtColor = if (isInward) RuwiaColor.Orange         else Color(0xFF1BAF70)
    val sign     = if (isInward) "+"                       else "-"
    val statusLabel = if (isInward) "INWARD" else "OUTWARD"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconBg, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                tint     = iconTint,
                modifier = Modifier.size(17.dp).rotate(if (isInward) 45f else -135f),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = movement.source.ifBlank { "Stock movement" },
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = RuwiaColor.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isInward) RuwiaColor.OrangeSurface else RuwiaColor.TealExtraLight,
                            RoundedCornerShape(5.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        statusLabel,
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (isInward) RuwiaColor.Orange else RuwiaColor.TealPrimary,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text     = "${movement.qty} units · ${formatDateLabel(movement.createdAt)}" +
                           if (movement.shopName.isNotBlank()) " · ${movement.shopName}" else "",
                fontSize = 11.sp,
                color    = RuwiaColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text       = "$sign${movement.qty}",
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = amtColor,
        )
    }
}

private fun formatDateLabel(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return "—"
    val datePart = createdAt.take(10)
    val parts = datePart.split("-")
    if (parts.size != 3) return datePart
    val month = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        .getOrNull((parts[1].toIntOrNull() ?: 1) - 1) ?: parts[1]
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    val timePart = createdAt.drop(11).take(5)
    return if (timePart.isNotEmpty()) "$day $month · $timePart" else "$day $month"
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EntriesEmptyState(modifier: Modifier = Modifier) {
    val title = "No outward entries"
    val subtitle = "Tap \"Add Sale\" on the home screen to record stock you sold."
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RuwiaColor.TealExtraLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = RuwiaColor.TealPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = RuwiaColor.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontSize  = 13.sp,
            color     = RuwiaColor.TextMuted,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 20.dp),
            lineHeight = 18.sp,
        )
    }
}

