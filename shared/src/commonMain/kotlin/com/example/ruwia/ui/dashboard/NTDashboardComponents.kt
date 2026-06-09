package com.example.ruwia.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
//  Reusable dashboard components — Material Icons only, no emojis
// ─────────────────────────────────────────────────────────────

// ── Section header ────────────────────────────────────────────

@Composable
fun NTSectionHeader(
    label: String,
    title: String,
    trailingText: String = "",
    onTrailingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = NTColors.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = NTColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (trailingText.isNotEmpty()) {
                Text(
                    text = trailingText,
                    color = NTColors.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onTrailingClick)
                )
            }
        }
    }
}

// ── Dashboard header ─────────────────────────────────────────

@Composable
fun NTDashboardHeader(
    shopName: String,
    adminName: String,
    notificationCount: Int = 0,
    onAvatarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initials = adminName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2).joinToString("")

        Box(
            modifier = Modifier.size(NTDp.avatarLg).clip(CircleShape)
                .background(NTColors.AvatarGold)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(NTDp.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "OWNER · ${shopName.uppercase()}",
                color = NTColors.TextTertiary, fontSize = 10.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = adminName, color = NTColors.TextPrimary,
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        NTIconCircleButton(icon = Icons.Rounded.Search, onClick = onSearchClick)
        Spacer(modifier = Modifier.width(NTDp.sm))

        Box {
            NTIconCircleButton(icon = Icons.Rounded.Notifications, onClick = onNotificationClick)
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp).size(18.dp)
                        .clip(CircleShape).background(NTColors.Error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                        color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NTIconCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = NTColors.TextPrimary,
    bgColor: Color = NTColors.Surface
) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(bgColor).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint,
            modifier = Modifier.size(NTDp.iconLg))
    }
}

// ── Greeting ──────────────────────────────────────────────────

@Composable
fun NTGreetingSection(
    greeting: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .padding(top = NTDp.xs, bottom = NTDp.md)
    ) {
        Text(text = greeting, color = NTColors.TextPrimary,
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
        Spacer(modifier = Modifier.height(NTDp.xs))
        Text(text = subtext, color = NTColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

// ── Revenue hero card ─────────────────────────────────────────

@Composable
fun NTRevenueHeroCard(
    @Suppress("UNUSED_PARAMETER") shopName: String = "",
    mrr: Double,
    totalOrders: Int,
    totalCustomers: Int,
    activeStaff: Int,
    fleetActive: Int,
    growthPercent: Double = 8.2,
    dateLabel: String = "This Month",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(Brush.linearGradient(colors = listOf(NTColors.GradStart, NTColors.GradEnd)))
    ) {
        Box(
            modifier = Modifier.size(180.dp).offset(x = 200.dp, y = (-40).dp)
                .clip(CircleShape).background(NTColors.GradAccent)
        )
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MONTHLY REVENUE",
                    color = NTColors.PrimaryLight.copy(alpha = 0.8f),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.GradAccent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(NTDp.md))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                Text(formatMrr(mrr), color = Color.White, fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                NTGrowthBadge(percent = growthPercent)
            }
            Spacer(modifier = Modifier.height(NTDp.xs))
            Text(
                "vs ${formatMrr(mrr * 0.92)} last month · $fleetActive fleet active",
                color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(NTDp.lg))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(NTDp.md))
            Row(modifier = Modifier.fillMaxWidth()) {
                NTRevStat("ORDERS",    totalOrders.toString(),    Modifier.weight(1f))
                NTRevStat("CUSTOMERS", totalCustomers.toString(), Modifier.weight(1f))
                NTRevStat("FLEET",     fleetActive.toString(),    Modifier.weight(1f))
                NTRevStat("STAFF",     activeStaff.toString(),    Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NTGrowthBadge(percent: Double) {
    val isPos = percent >= 0
    Row(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isPos) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
            contentDescription = null,
            tint = if (isPos) NTColors.PrimaryLight else NTColors.ErrorLight,
            modifier = Modifier.size(14.dp)
        )
        Text(
            "${if (isPos) "+" else ""}${(abs(percent) * 10).toLong() / 10.0}%",
            color = if (isPos) NTColors.PrimaryLight else NTColors.ErrorLight,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NTRevStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    }
}

private fun formatMrr(amount: Double): String = when {
    amount >= 1_00_000 -> "₹${(amount / 1_00_000 * 10).toLong() / 10.0}L"
    amount >= 1_000    -> "₹${amount.toLong()}"
    else               -> "₹${amount.toLong()}"
}

// ── KPI metric grid ───────────────────────────────────────────

data class NTKpiItem(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconFg: Color,
    val statusColor: Color,
    val subtitleColor: Color
)

@Composable
fun NTKpiGrid(items: List<NTKpiItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = NTDp.screenPad)) {
        NTSectionHeader(label = "AT A GLANCE", title = "Key metrics")
        Spacer(modifier = Modifier.height(NTDp.md))
        items.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
                row.forEach { item -> NTKpiCard(item = item, modifier = Modifier.weight(1f)) }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(NTDp.md))
        }
    }
}

@Composable
fun NTKpiCard(item: NTKpiItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Box(
                modifier = Modifier.size(NTDp.kpiIconBox)
                    .clip(RoundedCornerShape(NTDp.radMd)).background(item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.iconFg,
                    modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.height(NTDp.md))
            Text(item.title, color = NTColors.TextTertiary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(NTDp.xs))
            Text(item.value, color = NTColors.TextPrimary, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Spacer(modifier = Modifier.height(NTDp.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(item.statusColor))
                Spacer(modifier = Modifier.width(NTDp.xs))
                Text(item.subtitle, color = item.subtitleColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────

data class NTQuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val badgeCount: Int = 0
)

@Composable
fun NTQuickActionsRow(
    actions: List<NTQuickAction>,
    onActionClick: (NTQuickAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        NTSectionHeader(label = "QUICK ACCESS", title = "Shortcuts",
            modifier = Modifier.padding(horizontal = NTDp.screenPad))
        Spacer(modifier = Modifier.height(NTDp.md))
        LazyRow(
            contentPadding = PaddingValues(horizontal = NTDp.screenPad),
            horizontalArrangement = Arrangement.spacedBy(NTDp.md)
        ) {
            items(items = actions, key = { it.id }) { action ->
                NTQuickActionButton(action = action, onClick = { onActionClick(action) })
            }
        }
    }
}

@Composable
private fun NTQuickActionButton(action: NTQuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp).clickable(onClick = onClick)
    ) {
        Box {
            Box(
                modifier = Modifier.size(NTDp.qaIconBox)
                    .clip(RoundedCornerShape(NTDp.radLg)).background(action.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, contentDescription = action.label,
                    tint = Color.White, modifier = Modifier.size(NTDp.iconXl))
            }
            if (action.badgeCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp).size(18.dp)
                        .clip(CircleShape).background(NTColors.Error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(action.badgeCount.toString(), color = Color.White,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(NTDp.sm))
        Text(action.label, color = NTColors.TextSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 14.sp)
    }
}

// ── Date range toggle ─────────────────────────────────────────

enum class NTDateRange { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

@Composable
fun NTDateRangePicker(
    selected: NTDateRange,
    onSelect: (NTDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(NTColors.SurfaceVar).padding(NTDp.xs),
        horizontalArrangement = Arrangement.spacedBy(NTDp.xs)
    ) {
        NTDateRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(if (isSelected) NTColors.Surface else Color.Transparent)
                    .clickable { onSelect(range) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    range.label(),
                    color = if (isSelected) NTColors.TextPrimary else NTColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private fun NTDateRange.label() = when (this) {
    NTDateRange.WEEKLY    -> "Week"
    NTDateRange.MONTHLY   -> "Month"
    NTDateRange.QUARTERLY -> "Quarter"
    NTDateRange.YEARLY    -> "Year"
}

// ── Bottom navigation with centre FAB ────────────────────────

data class NTNavTab(val index: Int, val label: String, val icon: ImageVector, val badge: Int = 0)

@Composable
fun NTBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onFabClick: () -> Unit,
    tabs: List<NTNavTab> = defaultNavTabs(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().background(NTColors.Surface)) {
        HorizontalDivider(color = NTColors.Border, thickness = 0.5.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(NTDp.bottomNavH)
                .padding(horizontal = NTDp.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftTabs  = tabs.filter { it.index <= 1 }
            val rightTabs = tabs.filter { it.index >= 2 }

            leftTabs.forEach { tab ->
                NTNavTabItem(tab, selectedTab == tab.index) { onTabSelected(tab.index) }
            }

            // Centre FAB
            Box(modifier = Modifier.weight(1f).wrapContentSize(Alignment.Center)) {
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.size(NTDp.fabSz).offset(y = (-8).dp),
                    containerColor = NTColors.Accent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp, pressedElevation = 3.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "New",
                        modifier = Modifier.size(NTDp.iconXl))
                }
            }

            rightTabs.forEach { tab ->
                NTNavTabItem(tab, selectedTab == tab.index) { onTabSelected(tab.index) }
            }
        }
    }
}

@Composable
private fun RowScope.NTNavTabItem(tab: NTNavTab, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick)
            .padding(vertical = NTDp.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
                    .background(if (isSelected) NTColors.PrimaryLight else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tab.icon, contentDescription = tab.label,
                    tint = if (isSelected) NTColors.Primary else NTColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (tab.badge > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp).size(16.dp)
                        .clip(CircleShape).background(NTColors.Error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.badge.toString(), color = Color.White,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            tab.label,
            color = if (isSelected) NTColors.Primary else NTColors.TextTertiary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

fun defaultNavTabs() = listOf(
    NTNavTab(0, "Home",      Icons.Rounded.Home),
    NTNavTab(1, "Stock",     Icons.Rounded.Inventory2),
    NTNavTab(2, "Customers", Icons.Rounded.Group),
    NTNavTab(3, "Reports",   Icons.Rounded.BarChart),
)

// ── Shimmer brush ─────────────────────────────────────────────

@Composable
fun ntShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )
    return Brush.linearGradient(
        colors = listOf(NTColors.ShimmerBase, NTColors.ShimmerHigh, NTColors.ShimmerBase),
        start = Offset(offset - 400f, 0f), end = Offset(offset, 0f)
    )
}
