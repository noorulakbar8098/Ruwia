package com.example.ruwia.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import kotlin.math.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_icon

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
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radSm))
                        .clickable(onClickLabel = trailingText, onClick = onTrailingClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Dashboard header ─────────────────────────────────────────
//  Premium SaaS header: logo · OWNER label · brand · bell · avatar.

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
    val brandTitle = "neerthuli"
    val ownerLabel = "OWNER · ${shopName.uppercase().ifBlank { "Admin" }}"
    val avatarInitial = adminName.trim().firstOrNull()?.uppercase() ?: "N"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Open settings", onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "Neerthuli logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ownerLabel,
                color = NTColors.TextTertiary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = brandTitle, color = NTColors.TextPrimary,
                fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        // Notification bell with red indicator
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Border, CircleShape)
                    .clickable(onClickLabel = "Notifications", onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = NTColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-1).dp, y = 1.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(2.dp, NTColors.Background, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Circular user avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F2E2C))
                .clickable(onClickLabel = "Open settings", onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarInitial,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Uniform primary top bar ───────────────────────────────────
//  Full-bleed deep-teal glossy banner shared by every inner screen:
//  back button · title (+ optional badge) · subtitle · count pill.

@Composable
fun NTPrimaryTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = NTDp.screenPad,
    trailingText: String = "",
    titleBadge: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(OverviewBg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent,
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = horizontalPadding, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    titleBadge()
                }
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailingText.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = trailingText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
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

// ── Glossy animated arrow button ──────────────────────────────
//  Springy press scale + glassy sheen that brightens while tapped.

@Composable
fun GlossyArrowButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glossyArrowScale",
    )
    val sheen by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "glossyArrowSheen",
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(NTColors.SurfaceVar)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f + 0.30f * sheen),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent,
                    )
                )
            )
            .border(1.dp, NTColors.Border, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = contentDescription,
            tint = NTColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Glossy deep-teal container shared across screens ────────────
//  Same tone + top-light sheen as the home Business Overview card.

private val GlossTeal = Color(0xFF0F2E2C)

private val glossSheen: Brush
    get() = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f),
            Color.Transparent,
        )
    )

@Composable
fun GlossyTealBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    var m = modifier
        .clip(shape)
        .background(if (enabled) GlossTeal else NTColors.TextDisabled)
    if (enabled) {
        m = m
            .background(glossSheen)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
    }
    if (onClick != null && enabled) {
        m = m.clickable(onClickLabel = onClickLabel, onClick = onClick)
    }
    Box(modifier = m, contentAlignment = contentAlignment) {
        content()
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
            .padding(top = NTDp.sm, bottom = NTDp.md)
    ) {
        Text(text = greeting, color = NTColors.TextPrimary,
            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp,
            letterSpacing = (-0.4).sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtext, color = NTColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

// ── Business overview card color tokens ──────────────────────
//  Solid deep forest teal — no gradient, premium minimal.

private val OverviewBg      = Color(0xFF0F2E2C)
private val OverviewSurface = Color(0xFF1A4340)
private val OverviewMint    = Color(0xFF5EEAD4)
private val OverviewMuted   = Color.White

// Legacy aliases (old hero used gradients — retained only so legacy preview compiles)
private val HeroBg1 = OverviewBg
private val HeroBg2 = OverviewBg
private val HeroSurface = OverviewSurface
private val HeroLine = OverviewMint

// ── Business overview card ────────────────────────────────────
//  Premium dark-teal KPI card. NO graph — NET PROFIT + TOTAL CUSTOMERS.
//  Period chip opens a dropdown: Month (default) · Last Month · Last Quarter · Yearly.

enum class OverviewPeriod { MONTH, LAST_MONTH, LAST_QUARTER, YEARLY }

fun OverviewPeriod.label(): String = when (this) {
    OverviewPeriod.MONTH        -> "Month"
    OverviewPeriod.LAST_MONTH   -> "Last Month"
    OverviewPeriod.LAST_QUARTER -> "Last Quarter"
    OverviewPeriod.YEARLY       -> "Yearly"
}

@Composable
fun NTBusinessOverviewCard(
    period: OverviewPeriod,
    onPeriodChange: (OverviewPeriod) -> Unit,
    chipLabel: String,
    leftLabel: String,
    leftValue: Double,
    leftGrowth: Double?,
    rightValue: Int,
    rightGrowth: Double?,
    modifier: Modifier = Modifier,
) {
    var dropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(24.dp))
            .background(OverviewBg)
            .padding(20.dp),
    ) {
        // ── Top row ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(OverviewSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet, null,
                        tint = OverviewMint, modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "BUSINESS OVERVIEW",
                    color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.Info, null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OverviewSurface)
                        .clickable(onClickLabel = "Change period", onClick = { dropdownOpen = true })
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        chipLabel.ifBlank { "This Month" },
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    OverviewPeriod.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label(),
                                    fontSize = 14.sp,
                                    fontWeight = if (option == period) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            trailingIcon = if (option == period) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                dropdownOpen = false
                                onPeriodChange(option)
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Main KPIs ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            OverviewMainKpi(
                icon = Icons.Rounded.AccountBalanceWallet,
                label = leftLabel,
                value = formatMrr(leftValue),
                growth = leftGrowth,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f))
            )
            OverviewMainKpi(
                icon = Icons.Rounded.Groups,
                label = "TOTAL CUSTOMERS",
                value = "$rightValue",
                growth = rightGrowth,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewMainKpi(
    icon: ImageVector,
    label: String,
    value: String,
    growth: Double?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = Color.White,
            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp, lineHeight = 34.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        // No baseline → no delta row at all (never a bare dash).
        if (growth != null) {
            Text(
                text = formatDelta(growth),
                color = if (growth >= 0.0) OverviewMint else Color(0xFFFCA5A5),
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = formatDeltaDescription(growth) },
            )
        }
    }
}

private fun formatDelta(pct: Double?): String {
    if (pct == null) return "—"
    val abs = kotlin.math.abs(pct)
    val rounded = (abs * 10).toLong() / 10.0
    val roundedStr = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    val arrow = if (pct >= 0) "↗" else "↘"
    // The sign is load-bearing: + means growth, - means decline.
    val sign = if (pct >= 0) "+" else "-"
    return "$arrow $sign$roundedStr%"
}

/** Screen-reader wording for a delta: "Up 12 percent", "Down 80 percent", "No change". */
private fun formatDeltaDescription(pct: Double?): String {
    if (pct == null) return "No change"
    val abs = kotlin.math.abs(pct)
    val rounded = (abs * 10).toLong() / 10.0
    val str = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return when {
        pct >= 0.5  -> "Up $str percent"
        pct <= -0.5 -> "Down $str percent"
        else        -> "No change"
    }
}

// ── Legacy hero card kept for compatibility (no longer used on home) ──
@Composable
fun NTRevenueHeroCard(
    @Suppress("UNUSED_PARAMETER") shopName: String = "",
    /** Headline value shown big — caller decides revenue vs profit. */
    headlineValue: Double,
    /** Comparison baseline shown as "vs ₹X last month". */
    lastMonthValue: Double,
    /** Real growth %; null = no baseline → render a neutral "—" badge. */
    growthPercent: Double?,
    totalOrders: Int,
    totalCustomers: Int,
    activeStaff: Int,
    fleetActive: Int,
    /** 0..1 normalised daily series for the hero chart. Empty = flat baseline. */
    chartPoints: List<Float> = emptyList(),
    /** Top→bottom Y-axis ticks, e.g. ["30K", "20K", "10K", "0"]. */
    yAxisLabels: List<String> = listOf("30K", "20K", "10K", "0"),
    /** Left→right X-axis labels along the chart. */
    xAxisLabels: List<String> = listOf("1", "8", "15", "22", "30"),
    /** Optional subtitle beneath the headline (e.g. "TOTAL REVENUE" / "NET PROFIT"). */
    headlineLabel: String = "TOTAL REVENUE",
    dateLabel: String = "This Month",
    modifier: Modifier = Modifier,
) {
    val stats = listOf(
        HeroStat("ORDERS",    "$totalOrders",    "This Month", Icons.Rounded.ShoppingBag,   Color(0xFF193E3A), HeroLine.copy(alpha = 0.90f),        HeroLine),
        HeroStat("CUSTOMERS", "$totalCustomers", "This Month", Icons.Rounded.Groups,        Color(0xFF193E3A), HeroLine.copy(alpha = 0.90f),        HeroLine),
        HeroStat("FLEET",     "$fleetActive",    "Active",     Icons.Rounded.LocalShipping, Color(0xFF1A2F50), NTColors.Info.copy(alpha = 0.90f),   NTColors.Info),
        HeroStat("STAFF",     "$activeStaff",    "Active",     Icons.Rounded.Badge,         Color(0xFF3D2E14), NTColors.Warning.copy(alpha = 0.90f),NTColors.Warning),
    )

    // Decide direction & accent for the growth badge.
    val isPositive = (growthPercent ?: 0.0) >= 0.0
    val badgeColor = when {
        growthPercent == null -> Color(0xFF6E7E89) // neutral grey when no baseline
        isPositive            -> NTColors.Success
        else                  -> NTColors.Error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(Brush.verticalGradient(colors = listOf(HeroBg1, HeroBg2)))
            .padding(NTDp.cardPad),
    ) {
        // ── Header ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(HeroSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = HeroLine, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "REVENUE OVERVIEW",
                    color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(14.dp))
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(HeroSurface)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dateLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(NTDp.md))

        // ── Revenue value + Chart ───────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left: numbers
            Column(modifier = Modifier.weight(0.44f).padding(end = 6.dp)) {
                Text(
                    headlineLabel,
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    formatMrr(headlineValue),
                    color = Color.White,
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp, lineHeight = 34.sp,
                )
//                Spacer(Modifier.height(10.dp))
//                // Growth badge — colour & icon depend on real data
//                Row(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(NTDp.radFull))
//                        .background(badgeColor)
//                        .padding(horizontal = 9.dp, vertical = 5.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    when {
//                        growthPercent == null -> {
////                            Text("—", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
//                        }
//                        isPositive -> {
//                            Icon(Icons.Rounded.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(11.dp))
//                            Spacer(Modifier.width(3.dp))
//                            Text("${formatPct(growthPercent)}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
//                        }
//                        else -> {
//                            Icon(Icons.Rounded.ArrowDownward, null, tint = Color.White, modifier = Modifier.size(11.dp))
//                            Spacer(Modifier.width(3.dp))
//                            Text("${formatPct(growthPercent)}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
//                        }
//                    }
//                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "vs ${formatMrr(lastMonthValue)} last month",
                    color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp,
                )
            }

            // Right: chart
            Column(modifier = Modifier.weight(0.56f)) {
                Row(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    HeroRevenueChart(
                        points = chartPoints,
                        isPositive = isPositive,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    // Y-axis labels (top → bottom)
                    Column(
                        modifier = Modifier.width(28.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        yAxisLabels.forEach { lbl ->
                            Text(lbl, color = Color.White.copy(alpha = 0.32f), fontSize = 8.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    xAxisLabels.forEach { lbl ->
                        Text(lbl, color = Color.White.copy(alpha = 0.36f), fontSize = 8.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(NTDp.md))
//        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
//        Spacer(Modifier.height(NTDp.md))

//        // ── Stats row ───────────────────────────────────────────
//        Row(modifier = Modifier.fillMaxWidth()) {
//            stats.forEachIndexed { i, stat ->
//                HeroStatTile(stat = stat, modifier = Modifier.weight(1f))
//                if (i < stats.lastIndex) {
//                    Box(modifier = Modifier.width(0.8.dp).height(60.dp).background(Color.White.copy(alpha = 0.10f)))
//                }
//            }
//        }
    }
}

private data class HeroStat(
    val label: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconFg: Color,
    val subtitleColor: Color,
)

@Composable
private fun HeroStatTile(stat: HeroStat, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(stat.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(stat.icon, null, tint = stat.iconFg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(stat.label, color = Color.White.copy(alpha = 0.48f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(stat.value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(2.dp))
        Text(stat.subtitle, color = stat.subtitleColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeroRevenueChart(
    points: List<Float>,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    // Pad sparse data so the curve still looks like a curve.
    val pts = when {
        points.isEmpty()       -> List(7) { 0f }
        points.size == 1       -> List(7) { points.first() }
        else                   -> points
    }
    val lineColor = if (isPositive) HeroLine else Color(0xFFFF8A8A)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vPad = h * 0.06f
        val chartH = h - vPad * 2

        // Each point is normalised 0..1 where 1 = max bar height.
        // We invert so a higher value sits HIGHER on the canvas.
        val offsets = pts.mapIndexed { i, v ->
            val clamped = v.coerceIn(0f, 1f)
            Offset(
                x = if (pts.size > 1) w * i / (pts.size - 1).toFloat() else w / 2f,
                y = vPad + chartH * (1f - clamped),
            )
        }

        // Dashed grid lines
        listOf(0.0f, 0.33f, 0.67f, 1.0f).forEach { r ->
            val y = vPad + chartH * r
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 0.7.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)),
            )
        }

        // Smooth bezier path through the points
        val line = Path().apply {
            moveTo(offsets[0].x, offsets[0].y)
            for (i in 1 until offsets.size) {
                val mid = (offsets[i - 1].x + offsets[i].x) / 2f
                cubicTo(mid, offsets[i - 1].y, mid, offsets[i].y, offsets[i].x, offsets[i].y)
            }
        }

        // Translucent area fill below the line
        val fill = Path().apply {
            addPath(line)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f, endY = h,
            ),
        )

        // Stroke the line
        drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // End-of-line dot
        val last = offsets.last()
        drawCircle(color = HeroBg1, radius = 5.dp.toPx(), center = last)
        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = last)
    }
}

@Composable
fun NTGrowthBadge(percent: Double) {
    val isPos = percent >= 0.1
    val isNeg = percent <= -0.1
    
    val trendColor = when {
        isPos -> NTColors.PrimaryLight
        isNeg -> NTColors.ErrorLight
        else -> NTColors.TextSecondary
    }
    val trendIcon = when {
        isPos -> Icons.Rounded.TrendingUp
        isNeg -> Icons.Rounded.TrendingDown
        else -> Icons.Rounded.Remove
    }

    Row(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = trendIcon,
            contentDescription = null,
            tint = trendColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "${if (isPos) "+" else ""}${(abs(percent) * 10).toLong() / 10.0}%",
            color = trendColor,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatMrr(amount: Double): String = when {
    amount >= 1_00_000 -> {
        val l = amount / 1_00_000.0
        val rounded = round(l * 100.0) / 100.0
        val s = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
        "₹${s}L"
    }
    amount >= 1_000 -> {
        val k = amount / 1_000.0
        val rounded = round(k * 100.0) / 100.0
        val s = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
        "₹${s}K"
    }
    else -> "₹${amount.toInt()}"
}

/** "+12.4%" / "-3.0%" — keeps one decimal, strips the trailing ".0" when zero. */
internal fun formatPct(p: Double): String {
    val rounded = (abs(p) * 10).toLong() / 10.0
    val sign = if (p >= 0) "+" else "-"
    return "$sign$rounded"
}

// ── KPI metric grid ───────────────────────────────────────────

data class NTKpiItem(
    val title: String,
    val value: String,
    val subtitle: String,
    val subtitleColor: Color,
    val icon: ImageVector,
    val iconBg: Color,
    val iconFg: Color,
    val accentColor: Color,
    val footerText: String?,
    val footerIcon: ImageVector,
    val footerColor: Color,
    val cardIndex: Int,             // 0=stock, 1=revenue, 2=cans, 3=customers
)

@Composable
fun NTKpiGrid(items: List<NTKpiItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = NTDp.screenPad)) {
        NTSectionHeader(label = "AT A GLANCE", title = "Key metrics")
        Spacer(modifier = Modifier.height(NTDp.md))
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(NTDp.md),
            ) {
                row.forEach { item ->
                    NTKpiCard(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(NTDp.md))
        }
    }
}

@Composable
fun NTKpiCard(item: NTKpiItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier

            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .clip(RoundedCornerShape(NTDp.radLg)),
    ) {
        // ── Main body ──────────────────────────────────────────
        Column(modifier = Modifier.padding(NTDp.md)) {
            // Icon + Live badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, null, tint = item.iconFg, modifier = Modifier.size(20.dp))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(item.accentColor.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(item.accentColor))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Live", fontSize = 9.sp, color = item.accentColor,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title + Value + Illustration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontSize = 10.sp, color = NTColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.value,
                        fontSize = 26.sp, color = NTColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp, lineHeight = 30.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.subtitle,
                        fontSize = 12.sp, color = item.subtitleColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                // Decorative illustration
                KpiIllustration(
                    cardIndex = item.cardIndex,
                    color     = item.iconFg,
                    modifier  = Modifier.size(60.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        // ── Footer strip ───────────────────────────────────────
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(item.accentColor.copy(alpha = 0.07f))
//                .padding(horizontal = NTDp.md, vertical = 9.dp),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            Icon(
//                item.footerIcon, null,
//                tint = item.footerColor,
//                modifier = Modifier.size(13.dp),
//            )
//            Spacer(Modifier.width(5.dp))
//            Text(
//                item.footerText ?: "",
//                fontSize = 11.sp, color = item.footerColor,
//                fontWeight = FontWeight.Medium,
//            )
//        }
    }
}

// ── KPI Card illustrations (Canvas) ──────────────────────────

@Composable
private fun KpiIllustration(cardIndex: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        when (cardIndex) {
            0 -> drawBoxStack(color)
            1 -> drawMiniChart(color)
            2 -> drawJarWithWarning(color)
            3 -> drawCustomerGroup(color)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoxStack(color: Color) {
    val bw = size.width * 0.50f
    val bh = size.height * 0.28f
    val off = size.width * 0.09f
    for (i in 2 downTo 0) {
        val alpha = 0.20f + i * 0.13f
        val left = size.width * 0.24f - i * off
        val top  = size.height * 0.60f - i * (bh * 0.88f)
        drawRoundRect(
            color        = color.copy(alpha = alpha),
            topLeft      = Offset(left, top),
            size         = Size(bw, bh),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawLine(
            color       = color.copy(alpha = alpha * 0.6f),
            start       = Offset(left, top + bh * 0.32f),
            end         = Offset(left + bw, top + bh * 0.32f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMiniChart(color: Color) {
    val pts = listOf(0.88f, 0.76f, 0.82f, 0.60f, 0.68f, 0.42f, 0.18f)
    val w   = size.width
    val hRange = size.height * 0.78f
    val yOff   = size.height * 0.10f
    val path     = Path()
    val fillPath = Path()
    val firstY = yOff + hRange * pts.first()
    path.moveTo(0f, firstY)
    fillPath.moveTo(0f, size.height)
    fillPath.lineTo(0f, firstY)
    pts.forEachIndexed { i, p ->
        val x = w * i / (pts.size - 1).toFloat()
        val y = yOff + hRange * p
        if (i > 0) { path.lineTo(x, y); fillPath.lineTo(x, y) }
    }
    fillPath.lineTo(w, size.height)
    fillPath.close()
    drawPath(
        fillPath,
        Brush.verticalGradient(
            listOf(color.copy(alpha = 0.18f), Color.Transparent),
            startY = firstY, endY = size.height,
        ),
    )
    drawPath(path, color = color.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    val lastX = w
    val lastY = yOff + hRange * pts.last()
    drawCircle(color = color, radius = 3.5.dp.toPx(), center = Offset(lastX, lastY))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJarWithWarning(color: Color) {
    val cx    = size.width * 0.50f
    val cy    = size.height * 0.50f
    val rw    = size.width  * 0.22f
    val jarH  = size.height * 0.50f
    // Body
    drawRoundRect(
        color        = color.copy(alpha = 0.28f),
        topLeft      = Offset(cx - rw, cy - jarH / 2),
        size         = Size(rw * 2, jarH),
        cornerRadius = CornerRadius(rw * 0.4f),
    )
    // Lid
    drawRoundRect(
        color        = color.copy(alpha = 0.45f),
        topLeft      = Offset(cx - rw * 0.80f, cy - jarH / 2 - size.height * 0.09f),
        size         = Size(rw * 1.60f, size.height * 0.09f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    // Warning badge
    val bx = cx + rw * 0.70f
    val by = cy + jarH * 0.28f
    drawCircle(color = Color(0xFFF97316), radius = 7.dp.toPx(), center = Offset(bx, by))
    drawLine(
        color       = Color.White,
        start       = Offset(bx, by - 2.5.dp.toPx()),
        end         = Offset(bx, by + 0.8.dp.toPx()),
        strokeWidth = 1.8.dp.toPx(),
        cap         = StrokeCap.Round,
    )
    drawCircle(color = Color.White, radius = 0.9.dp.toPx(), center = Offset(bx, by + 3.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCustomerGroup(color: Color) {
    data class Person(val cx: Float, val cy: Float, val alpha: Float)
    val persons = listOf(
        Person(size.width * 0.22f, size.height * 0.38f, 0.22f),
        Person(size.width * 0.50f, size.height * 0.30f, 0.40f),
        Person(size.width * 0.78f, size.height * 0.38f, 0.28f),
    )
    val headR = size.width * 0.13f
    val bodyW = size.width * 0.20f
    val bodyH = size.height * 0.20f
    persons.forEach { p ->
        drawCircle(color = color.copy(alpha = p.alpha), radius = headR, center = Offset(p.cx, p.cy))
        drawArc(
            color      = color.copy(alpha = p.alpha),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter  = true,
            topLeft    = Offset(p.cx - bodyW / 2, p.cy + headR * 0.55f),
            size       = Size(bodyW, bodyH),
        )
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = NTDp.screenPad)) {
        NTSectionHeader(label = "QUICK ACCESS", title = "Shortcuts")
        Spacer(modifier = Modifier.height(NTDp.md))
        actions.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(NTDp.sm),
            ) {
                row.forEach { action ->
                    NTActionTile(
                        action  = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(NTDp.sm))
        }
    }
}

@Composable
private fun NTActionTile(action: NTQuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(NTDp.radSm))
                    .background(action.bgColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon, null, tint = action.bgColor, modifier = Modifier.size(NTDp.iconMd))
            }
            Spacer(Modifier.height(NTDp.sm))
            Text(
                action.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary, lineHeight = 15.sp,
            )
        }
        if (action.badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NTColors.Error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (action.badgeCount > 9) "9+" else action.badgeCount.toString(),
                    color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Date range toggle ─────────────────────────────────────────

enum class NTDateRange { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

@Composable
fun NTDateRangePicker(
    selected: NTDateRange,
    onSelect: (NTDateRange) -> Unit,
    modifier: Modifier = Modifier,
    options: List<NTDateRange> = NTDateRange.entries,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radFull))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(if (isSelected) OverviewBg else Color.Transparent)
                    // Subtle glassy top-light sheen on the selected chip.
                    .then(
                        if (isSelected) Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent,
                                )
                            )
                        ) else Modifier
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.16f),
                            RoundedCornerShape(NTDp.radFull)
                        ) else Modifier
                    )
                    .then(
                        if (isSelected) Modifier.shadow(
                            3.dp, RoundedCornerShape(NTDp.radFull), clip = false
                        ) else Modifier
                    )
                    .clickable(onClickLabel = range.label(), onClick = { onSelect(range) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    range.label(),
                    color = if (isSelected) Color.White else NTColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
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

// ── Bottom navigation — floating pill ────────────────────────

data class NTNavTab(val index: Int, val label: String, val icon: ImageVector, val badge: Int = 0)

private val NavBarBg = Color(0xFF0F2B29)

@Composable
fun NTBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NTNavTab> = defaultNavTabs(),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp), clip = false)
                .background(NavBarBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                NTNavTabItem(tab, selectedTab == tab.index) { onTabSelected(tab.index) }
            }
        }
    }
}

@Composable
private fun RowScope.NTNavTabItem(tab: NTNavTab, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = tab.label, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) OverviewSurface
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF8AA0A4),
                modifier = Modifier.size(22.dp),
            )
            if (tab.badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(NTColors.Error),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tab.badge.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text       = tab.label,
            color      = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFB4C2C6),
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}

fun defaultNavTabs() = listOf(
    NTNavTab(0, "Home",      Icons.Rounded.Home),
    NTNavTab(1, "Inventory", Icons.Rounded.Inventory2),
    NTNavTab(2, "Txns",      Icons.Rounded.ReceiptLong),
    NTNavTab(3, "Analytics", Icons.Rounded.BarChart),
    NTNavTab(4, "Settings",  Icons.Rounded.Settings),
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
