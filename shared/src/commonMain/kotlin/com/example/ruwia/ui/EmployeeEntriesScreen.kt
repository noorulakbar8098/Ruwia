package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockMovement

import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

// ─────────────────────────────────────────────────────────────────────────────
//  Employee Entries Tab
//  Full feed of the logged-in employee's stock movements (inward / outward /
//  adjustments) — backed by stock_movements rows filtered server-side by
//  employee_id, with client-side filter chips on top.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeEntriesScreen(
    movements: List<StockMovement>,
    currentDate: String,
    isLoading: Boolean,
    emptyCansTotal: Int = 0,
    products: List<ProductCategory> = emptyList(), // Added products
    onRefresh: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showDatePickerStart by remember { mutableStateOf(false) }
    var showDatePickerEnd by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDate?>(null) }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val sevenDaysAgo = remember(today) { today.minus(7, DateTimeUnit.DAY) }
    val thisMonthStart = remember(today) { LocalDate(today.year, today.month, 1) }
    val lastMonthStart = remember(today) {
        if (today.month == Month.JANUARY) LocalDate(today.year - 1, 12, 1)
        else LocalDate(today.year, today.monthNumber - 1, 1)
    }
    val lastMonthEnd = remember(thisMonthStart) { thisMonthStart.minus(1, DateTimeUnit.DAY) }

    val filtered = remember(movements, selectedFilter, customStartDate, customEndDate) {
        movements.filter { mov ->
            // Filter to only display outward transactions
            if (mov.type != "outward") return@filter false
            
            val movDate = try {
                LocalDate.parse(mov.createdAt?.take(10) ?: "")
            } catch (_: Exception) {
                null
            }
            
            if (movDate == null) return@filter false
            
            when (selectedFilter) {
                "Today" -> movDate == today
                "Last 7 Days" -> movDate in sevenDaysAgo..today
                "This Month" -> movDate.year == today.year && movDate.month == today.month
                "Last Month" -> movDate.year == lastMonthStart.year && movDate.month == lastMonthStart.month
                "Custom" -> {
                    val start = customStartDate
                    val end = customEndDate
                    when {
                        start != null && end != null -> movDate in start..end
                        start != null -> movDate >= start
                        end != null -> movDate <= end
                        else -> true
                    }
                }
                else -> true // "All"
            }
        }
    }

    // Delivered units follow the selected date filter (sum of outward qty).
    val deliveredUnits = remember(filtered) { filtered.sumOf { it.qty } }

    if (showDatePickerStart) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerStart = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        customStartDate = instant.toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePickerStart = false
                }) { Text("OK", color = NTColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerStart = false }) { Text("Cancel", color = NTColors.TextTertiary) }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showDatePickerEnd) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        customEndDate = instant.toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePickerEnd = false
                }) { Text("OK", color = NTColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerEnd = false }) { Text("Cancel", color = NTColors.TextTertiary) }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        // ── Header with refresh ─────────────────────────────────────────────
        item { EntriesTopBar(currentDate = currentDate, onRefresh = onRefresh) }

        // ── Totals card ─────────────────────────────────────────────────────
        item {
            EntriesTotalsCard(
                outward   = deliveredUnits,
                emptyCans = emptyCansTotal,
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        // ── Date Filter Chips ────────────────────────────────────────────────
        item {
            val chips = listOf("All", "Today", "Last 7 Days", "This Month", "Last Month", "Custom")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(chips) { chip ->
                    val isSelected = selectedFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) NTColors.Primary
                                else NTColors.PrimaryLight
                            )
                            .clickable { selectedFilter = chip }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (isSelected) Color.White else NTColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Custom Date Pickers Row ─────────────────────────────────────────
        if (selectedFilter == "Custom") {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NTColors.Divider, RoundedCornerShape(12.dp))
                            .background(NTColors.Surface)
                            .clickable { showDatePickerStart = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customStartDate?.toString() ?: "Start Date",
                            color = if (customStartDate != null) NTColors.TextPrimary else NTColors.TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NTColors.Divider, RoundedCornerShape(12.dp))
                            .background(NTColors.Surface)
                            .clickable { showDatePickerEnd = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customEndDate?.toString() ?: "End Date",
                            color = if (customEndDate != null) NTColors.TextPrimary else NTColors.TextTertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── List body ───────────────────────────────────────────────────────
        when {
            isLoading && movements.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NTColors.Primary, modifier = Modifier.size(28.dp))
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
                    EntryRow(movement = mov, products = products, modifier = Modifier.padding(horizontal = 20.dp))
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
                color      = NTColors.TextPrimary,
            )
            if (currentDate.isNotEmpty()) {
                Text(
                    currentDate,
                    fontSize = 12.sp,
                    color    = NTColors.TextTertiary,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, NTColors.Divider, CircleShape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint     = NTColors.TextSecondary,
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
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(
                Brush.linearGradient(
                    listOf(NTColors.Accent, NTColors.AccentDark),
                    start = Offset.Zero,
                    end = Offset(1000f, 0f),
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TotalsCell(
            value    = "$outward",
            label    = "Delivered Units",
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
private fun EntryRow(
    movement: StockMovement,
    modifier: Modifier = Modifier,
    products: List<ProductCategory> = emptyList()
) {
val isInward = movement.type == "inward"
    val iconBg   = if (isInward) NTColors.PrimaryLight else NTColors.InfoLight
    val iconTint = if (isInward) NTColors.Warning else NTColors.InfoText
    val amtColor = if (isInward) NTColors.Warning else NTColors.Success
    val sign     = if (isInward) "+"                       else "-"
    val statusLabel = if (isInward) "RETURN" else "SALE"

    val displayQty = "${movement.qty} units"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(14.dp))
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
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                    color      = NTColors.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
Box(
                    modifier = Modifier
                        .background(
                            if (isInward) NTColors.WarningLight else NTColors.SuccessLight,
                            RoundedCornerShape(5.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        statusLabel,
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (isInward) NTColors.WarningText else NTColors.SuccessText,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text     = "$displayQty · ${formatDateLabel(movement.createdAt)}",
                fontSize = 11.sp,
                color    = NTColors.TextTertiary,
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

private fun formatDateLabel(createdAt: String?): String =
    com.example.ruwia.util.isoToDisplayDateTime(createdAt)

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
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = NTColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontSize  = 13.sp,
            color     = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 20.dp),
            lineHeight = 18.sp,
        )
    }
}

