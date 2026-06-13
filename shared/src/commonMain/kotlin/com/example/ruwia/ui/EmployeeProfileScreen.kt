package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.theme.RuwiaColor

@Composable
fun EmployeeProfileScreen(
    employeeName: String = "Ravi Velu",
    employeeId: String = "EMP-001",
    phone: String = "+91 98432 11290",
    shopInfo: String = "Shop 1  ·  Saibaba Colony",
    role: String = "Stock Manager",
    joinDate: String = "Jan 2025",
    todayOutward: Int = 42,
    todayInward: Int = 64,
    todaySales: Double = 4832.0,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Logout?") },
            text    = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC3333)),
                ) { Text("Logout", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }

    Scaffold(
        containerColor = RuwiaColor.Background,
        topBar = {
            ProfileTopBar(onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Avatar + name hero ─────────────────────────────
            ProfileHero(
                name       = employeeName,
                employeeId = employeeId,
                role       = role,
                shopInfo   = shopInfo,
            )

            Spacer(Modifier.height(20.dp))

            // ── Today's stats ──────────────────────────────────
            TodayStatsRow(
                outward  = todayOutward,
                inward   = todayInward,
                sales    = todaySales,
            )

            Spacer(Modifier.height(20.dp))

            // ── Info rows ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RuwiaColor.Surface, RoundedCornerShape(18.dp))
                    .padding(vertical = 8.dp),
            ) {
                ProfileInfoSectionHeader("PROFILE INFO")
                ProfileInfoRow(
                    icon  = Icons.Rounded.Person,
                    label = "Full name",
                    value = employeeName,
                )
                ProfileDivider()
                ProfileInfoRow(
                    icon  = Icons.Rounded.Phone,
                    label = "Phone",
                    value = phone,
                )
                ProfileDivider()
                ProfileInfoRow(
                    icon  = Icons.Rounded.Store,
                    label = "Shop",
                    value = shopInfo,
                )
                ProfileDivider()
                ProfileInfoRow(
                    icon  = Icons.Rounded.Badge,
                    label = "Role",
                    value = role,
                )
                ProfileDivider()
                ProfileInfoRow(
                    icon  = Icons.Rounded.CalendarMonth,
                    label = "Employee ID",
                    value = employeeId,
                )
                ProfileDivider()
                ProfileInfoRow(
                    icon  = Icons.Rounded.DateRange,
                    label = "Joined",
                    value = joinDate,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Account actions ────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RuwiaColor.Surface, RoundedCornerShape(18.dp))
                    .padding(vertical = 8.dp),
            ) {
                ProfileInfoSectionHeader("ACCOUNT")
                ProfileActionRow(
                    icon    = Icons.Rounded.Lock,
                    label   = "Change password",
                    tint    = RuwiaColor.TealPrimary,
                    onClick = { /* future */ },
                )
                ProfileDivider()
                ProfileActionRow(
                    icon    = Icons.Rounded.Notifications,
                    label   = "Notification settings",
                    tint    = RuwiaColor.TealPrimary,
                    onClick = { /* future */ },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Logout button ──────────────────────────────────
            Button(
                onClick  = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEEEE),
                    contentColor   = Color(0xFFCC3333),
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Icon(Icons.Rounded.ExitToApp, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(RuwiaColor.Surface, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = RuwiaColor.TextPrimary, modifier = Modifier.size(18.dp))
        }
        Text(
            "My profile", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = RuwiaColor.TextPrimary, modifier = Modifier.align(Alignment.Center),
        )
    }
}

// ── Profile hero ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileHero(name: String, employeeId: String, role: String, shopInfo: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.TealDark, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Large avatar
        val initials = name.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.20f), CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(14.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))

        // Role badge
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Text(role.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = Color.White)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Store, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(shopInfo, fontSize = 12.sp, color = Color.White.copy(alpha = 0.80f))
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).background(Color(0xFF4CAF7C), CircleShape))
            Spacer(Modifier.width(5.dp))
            Text("Active · $employeeId", fontSize = 11.sp, color = Color.White.copy(alpha = 0.70f))
        }
    }
}

// ── Today's stats row ─────────────────────────────────────────────────────────

@Composable
private fun TodayStatsRow(outward: Int, inward: Int, sales: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("$outward", "Outward", "UNITS", RuwiaColor.TealExtraLight, RuwiaColor.TealPrimary, Modifier.weight(1f))
        StatCard("$inward",  "Inward",  "UNITS", RuwiaColor.OrangeLight, RuwiaColor.Orange,  Modifier.weight(1f))
        StatCard("₹${sales.toInt()}", "Sales", "TODAY", Color(0xFFEEF7FF), Color(0xFF2563EB), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    sublabel: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = fg)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg.copy(alpha = 0.75f))
        Text(sublabel, fontSize = 9.sp, letterSpacing = 0.6.sp, color = fg.copy(alpha = 0.55f))
    }
}

// ── Info rows ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoSectionHeader(title: String) {
    Text(
        title,
        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
        color    = RuwiaColor.TealPrimary,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(RuwiaColor.TealExtraLight, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = RuwiaColor.TextMuted)
            Spacer(Modifier.height(1.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
        }
    }
}

@Composable
private fun ProfileActionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(RuwiaColor.TealExtraLight, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 62.dp, end = 18.dp),
        color     = RuwiaColor.Divider.copy(alpha = 0.6f),
        thickness = 0.6.dp,
    )
}
