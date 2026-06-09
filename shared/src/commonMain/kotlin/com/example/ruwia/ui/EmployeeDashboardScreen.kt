package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.theme.RuwiaColor

// ── Entry display model ──────────────────────────────────────────────────────

private data class EntryDisplay(
    val name: String,
    val detail: String,
    val amount: String,
    val status: String,
    val isInward: Boolean,
)

private fun DeliveryTask.toEntryDisplay(): EntryDisplay = EntryDisplay(
    name     = customerName,
    detail   = "$canQty cans  ·  $etaText",
    amount   = if (status == "delivered") "+₹${canQty * 28}" else "+$canQty",
    status   = if (status == "delivered") "SOLD" else status.replace("_", " ").uppercase(),
    isInward = false,
)

// ── Sample entries shown when no real data is loaded ─────────────────────────

private val sampleEntries = listOf(
    EntryDisplay("Selvi Apartments",           "20L × 12  ·  @ ₹26  ·  9:24 AM",    "+₹312", "SOLD",   false),
    EntryDisplay("Aqua Pure Plant",            "20L × 50  ·  @ ₹13.60  ·  8:45 AM", "+50",   "INWARD", true),
    EntryDisplay("Murugan Mess",               "20L × 8  ·  @ ₹28  ·  8:20 AM",     "+₹224", "SOLD",   false),
    EntryDisplay("Lakshmi Bhavan",             "20L × 6  ·  @ ₹25  ·  8:00 AM",     "+₹150", "SOLD",   false),
    EntryDisplay("Crystal Plant · 1L bottles", "1L × 24  ·  @ ₹88  ·  7:30 AM",     "+24",   "INWARD", true),
)

// ── Screen navigation ─────────────────────────────────────────────────────────

private sealed class EmpScreen {
    object Home      : EmpScreen()
    object AddInward : EmpScreen()
    object Entries   : EmpScreen()
    object Stock     : EmpScreen()
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun EmployeeDashboardScreen(
    vm: EmployeeViewModel,
    onLogout: () -> Unit,
    employeeName: String = "Ravi Velu",
    shopInfo: String = "Shop 1  ·  Saibaba",
) {
    val state by vm.state.collectAsState()
    var screen by remember { mutableStateOf<EmpScreen>(EmpScreen.Home) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.loadDashboard("e1") }

    when (screen) {
        EmpScreen.AddInward -> AddStockPurchaseScreen(
            stockItems = emptyList(),
            onBack     = { screen = EmpScreen.Home },
            onClose    = { screen = EmpScreen.Home },
            onSave     = { _, _, _, _, _ -> screen = EmpScreen.Home },
        )

        else -> {
            val entries = if (state.tasks.isNotEmpty())
                state.tasks.map { it.toEntryDisplay() }
            else
                sampleEntries

            val totalEntries  = entries.size
            val inwardCount   = entries.count { it.isInward }
            val outwardCount  = entries.count { !it.isInward }
            val salesTotal    = if (state.dailyEarnings > 0) state.dailyEarnings else 4832.0
            val unitsInward   = if (state.emptyReturned > 0) state.emptyReturned else 64
            val unitsOutward  = if (state.filledDelivered > 0) state.filledDelivered else 42

            Scaffold(
                containerColor = RuwiaColor.Background,
                bottomBar = {
                    EmpBottomNavBar(
                        selectedTab  = selectedTab,
                        onTabChange  = { selectedTab = it },
                        onAddInward  = { screen = EmpScreen.AddInward },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    // ── Header ──────────────────────────────────
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(16.dp))
                            EmpHeader(
                                name      = employeeName,
                                onLogout  = onLogout,
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text       = "Today's stock",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 26.sp,
                                    color      = RuwiaColor.TextPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = RuwiaColor.Orange,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text  = "Sun 17 May  ·  $shopInfo",
                                fontSize = 13.sp,
                                color = RuwiaColor.TextMuted,
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // ── Activity card ────────────────────────────
                    item {
                        EmpActivityCard(
                            totalEntries = totalEntries,
                            inwardCount  = inwardCount,
                            outwardCount = outwardCount,
                            salesTotal   = salesTotal,
                            unitsInward  = unitsInward,
                            unitsOutward = unitsOutward,
                            modifier     = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // ── Action buttons ────────────────────────────
                    item {
                        Row(Modifier.padding(horizontal = 20.dp)) {
                            EmpActionCard(
                                label    = "FROM SUPPLIER",
                                title    = "Add\ninward",
                                isInward = true,
                                onClick  = { screen = EmpScreen.AddInward },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            EmpActionCard(
                                label    = "TO CUSTOMER",
                                title    = "Add\noutward",
                                isInward = false,
                                onClick  = {},
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(26.dp))
                    }

                    // ── Recent entries header ─────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth(),
                            verticalAlignment    = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text         = "RECENT ENTRIES",
                                    fontSize     = 10.sp,
                                    letterSpacing = 1.4.sp,
                                    fontWeight   = FontWeight.SemiBold,
                                    color        = RuwiaColor.TealPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text       = "Your activity",
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = RuwiaColor.TextPrimary,
                                )
                            }
                            Text(
                                text         = "VIEW ALL →",
                                fontSize     = 11.sp,
                                fontWeight   = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color        = RuwiaColor.TealPrimary,
                                modifier     = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Entry list ────────────────────────────────
                    if (state.loading) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color    = RuwiaColor.TealPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    } else {
                        items(entries) { entry ->
                            EmpEntryItem(
                                entry    = entry,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── EMP stock chip ────────────────────────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        RuwiaColor.TextPrimary.copy(alpha = 0.06f),
                                        RoundedCornerShape(20.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text         = "EMP  ·  ${unitsInward + unitsOutward}",
                                    fontSize     = 11.sp,
                                    fontWeight   = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color        = RuwiaColor.TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EmpHeader(name: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with online dot
        Box(modifier = Modifier.size(46.dp)) {
            val initials = name.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(RuwiaColor.TealPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = initials,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF4CAF7C), CircleShape)
                    .border(2.dp, RuwiaColor.Background, CircleShape)
                    .align(Alignment.BottomEnd),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text         = "VANAKKAM",
                fontSize     = 9.sp,
                letterSpacing = 1.8.sp,
                fontWeight   = FontWeight.Medium,
                color        = RuwiaColor.TextMuted,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text       = name,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = RuwiaColor.TextPrimary,
            )
        }

        // Search
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint     = RuwiaColor.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Bell with badge
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Logout",
                    tint     = RuwiaColor.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .background(RuwiaColor.Orange, CircleShape)
                    .border(1.5.dp, RuwiaColor.Background, CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("2", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Activity card ─────────────────────────────────────────────────────────────

@Composable
private fun EmpActivityCard(
    totalEntries: Int,
    inwardCount: Int,
    outwardCount: Int,
    salesTotal: Double,
    unitsInward: Int,
    unitsOutward: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.TealPrimary),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.07f),
                radius = size.height * 0.95f,
                center = Offset(size.width * 0.50f, size.height * 1.05f),
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.05f),
                radius = size.height * 0.70f,
                center = Offset(size.width * 0.78f, size.height * 0.85f),
            )
        }
        Column(modifier = Modifier.padding(20.dp)) {
            // Title row + date chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text         = "TODAY'S ACTIVITY",
                    fontSize     = 10.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight   = FontWeight.SemiBold,
                    color        = Color.White.copy(alpha = 0.75f),
                )
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = "17 May",
                            color      = Color.White,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text       = "$totalEntries entries",
                fontSize   = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                lineHeight = 48.sp,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text         = "$inwardCount INWARD  ·  $outwardCount OUTWARD  ·  ₹${salesTotal.toInt()} SALES",
                fontSize     = 11.sp,
                letterSpacing = 0.5.sp,
                color        = Color.White.copy(alpha = 0.80f),
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.22f), thickness = 0.8.dp)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                EmpStatBox("$unitsInward",  "UNITS INWARD",  Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                EmpStatBox("$unitsOutward", "UNITS OUTWARD", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmpStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                text         = label,
                fontSize     = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight   = FontWeight.Medium,
                color        = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

// ── Action buttons ─────────────────────────────────────────────────────────────

@Composable
private fun EmpActionCard(
    label: String,
    title: String,
    isInward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg    = if (isInward) RuwiaColor.Orange    else RuwiaColor.TealDark
    val alpha = if (isInward) 0.25f               else 0.18f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = alpha), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp).rotate(if (isInward) 45f else -135f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text         = label,
                fontSize     = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight   = FontWeight.SemiBold,
                color        = Color.White.copy(alpha = 0.70f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = title,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

// ── Entry list item ────────────────────────────────────────────────────────────

@Composable
private fun EmpEntryItem(entry: EntryDisplay, modifier: Modifier = Modifier) {
    val iconBg    = if (entry.isInward) RuwiaColor.OrangeLight  else RuwiaColor.IconTealBg
    val iconTint  = if (entry.isInward) RuwiaColor.Orange       else RuwiaColor.TealPrimary
    val amtColor  = if (entry.isInward) RuwiaColor.Orange       else RuwiaColor.TextPrimary
    val statColor = if (entry.isInward) RuwiaColor.Orange.copy(alpha = 0.65f) else RuwiaColor.TextMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                tint     = iconTint,
                modifier = Modifier.size(16.dp).rotate(if (entry.isInward) 45f else -135f),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = entry.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = RuwiaColor.TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = entry.detail,
                fontSize = 11.sp,
                color    = RuwiaColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = entry.amount,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = amtColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text         = entry.status,
                fontSize     = 10.sp,
                letterSpacing = 0.6.sp,
                fontWeight   = FontWeight.Medium,
                color        = statColor,
            )
        }
    }
}

// ── Bottom navigation ──────────────────────────────────────────────────────────

@Composable
private fun EmpBottomNavBar(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onAddInward: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.NavBackground)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = RuwiaColor.Divider, thickness = 0.6.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmpNavItem(
                icon     = Icons.Rounded.Home,
                label    = "Home",
                selected = selectedTab == 0,
                onClick  = { onTabChange(0) },
                modifier = Modifier.weight(1f),
            )
            EmpNavItem(
                icon     = Icons.Rounded.History,
                label    = "Entries",
                selected = selectedTab == 1,
                onClick  = { onTabChange(1) },
                modifier = Modifier.weight(1f),
            )
            // Centre FAB
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(RuwiaColor.TealPrimary, CircleShape)
                        .clickable(onClick = onAddInward),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add entry",
                        tint     = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            EmpNavItem(
                icon     = Icons.Rounded.Inventory2,
                label    = "Stock",
                selected = selectedTab == 3,
                onClick  = { onTabChange(3) },
                modifier = Modifier.weight(1f),
            )
            EmpNavItem(
                icon     = Icons.Rounded.Person,
                label    = "Profile",
                selected = selectedTab == 4,
                onClick  = { onTabChange(4) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmpNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .background(RuwiaColor.NavActiveChip, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(icon, label, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(17.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, label, tint = RuwiaColor.TextMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 10.sp, color = RuwiaColor.TextMuted)
            }
        }
    }
}
