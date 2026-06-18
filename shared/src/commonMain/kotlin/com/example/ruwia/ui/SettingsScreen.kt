package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Settings Screen
// ─────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    state: AdminState,
    adminName: String = "Admin",
    adminEmail: String = "",
    onBack: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToSuppliers: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onLogout: () -> Unit
) {
    // Toggle states
    var pushNotif    by remember { mutableStateOf(true) }
    var whatsApp     by remember { mutableStateOf(true) }
    var autoBackup   by remember { mutableStateOf(true) }
    var darkMode     by remember { mutableStateOf(false) }

    val shopCount     = 2
    val staffCount    = state.employees.size
    val custCount     = state.customers.size

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient extends behind the status bar (intentional — matches mockup)
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.46f)
                .background(Brush.verticalGradient(
                    listOf(NTColors.GradStart, NTColors.GradEnd)))
        )
        // White/light background below the gradient
        Box(
            modifier = Modifier.fillMaxWidth()
                .fillMaxHeight(0.54f)
                .align(Alignment.BottomCenter)
                .background(NTColors.Background)
        )

        // Content starts BELOW the status bar
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding()
                .background(Color.Transparent),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Top bar (back + title + edit) ─────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.md, vertical = NTDp.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back",
                            tint = Color.White, modifier = Modifier.size(NTDp.iconMd))
                    }
                    Text("Settings", color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f).padding(horizontal = NTDp.md))
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable { /* edit profile */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(NTDp.iconMd))
                    }
                }
            }

            // ── Profile hero ──────────────────────────────────
            item {
                // Derive initials from the real admin name
                val initials = adminName
                    .split(" ").filter { it.isNotEmpty() }
                    .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "AD" }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with glass effect
                    Box {
                        Box(
                            modifier = Modifier.size(84.dp)
                                .clip(RoundedCornerShape(NTDp.radXxl))
                                .background(Color.White.copy(alpha = 0.22f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(NTDp.radXxl)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        // Orange dot
                        Box(
                            modifier = Modifier.size(22.dp).align(Alignment.BottomEnd)
                                .clip(CircleShape).border(2.dp, Color.White, CircleShape)
                                .background(NTColors.Accent)
                        )
                    }
                    Spacer(modifier = Modifier.height(NTDp.md))
                    Text(
                        text       = adminName.ifEmpty { "Admin" },
                        color      = Color.White,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val roleEmailLine = buildString {
                        append("OWNER")
                        if (adminEmail.isNotEmpty()) append(" · $adminEmail")
                    }
                    Text(
                        text       = roleEmailLine,
                        color      = Color.White.copy(alpha = 0.75f),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                    )
                    Spacer(modifier = Modifier.height(NTDp.md))
                    // Business Pro badge
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
                            .background(NTColors.Accent)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("BUSINESS PRO", color = Color.White, fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(NTDp.lg))
                }
            }

            // ── Stats strip ───────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad),
                    shape = RoundedCornerShape(NTDp.radXxl),
                    colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = NTDp.md),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCell("$shopCount", "SHOPS")
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatCell("$staffCount", "STAFF")
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatCell("$custCount", "CUST")
                    }
                }
                Spacer(modifier = Modifier.height(NTDp.lg))
            }

            // ── Group 1: Business ─────────────────────────────
            item {
                SettingsGroup {
                    SettingsNavItem(
                        icon = Icons.Rounded.Store,
                        iconBg = Color(0xFFDFF5F3),
                        iconFg = NTColors.Primary,
                        title = "Business profile",
                        subtitle = "SHOPS · GST · INVOICE",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Rounded.Group,
                        iconBg = Color(0xFFFEDCC5),
                        iconFg = NTColors.Accent,
                        title = "Staff & roles",
                        subtitle = "${staffCount} USERS · PERMISSIONS",
                        onClick = onNavigateToEmployees
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Rounded.LocalOffer,
                        iconBg = Color(0xFFDFF5F3),
                        iconFg = NTColors.Primary,
                        title = "Pricing & products",
                        subtitle = "5L · 10L · 20L · BULK",
                        onClick = onNavigateToPricing
                    )
                    SettingsDivider()
                    // Suppliers — admin maintains the list, employees see it
                    // immediately in their Add Inward picker on next refresh.
                    SettingsNavItem(
                        icon = Icons.Rounded.Store,
                        iconBg = Color(0xFFFFF3E8),
                        iconFg = Color(0xFFF97316),
                        title = "Suppliers",
                        subtitle = "${state.suppliersFull.size} ACTIVE · MANAGE",
                        onClick = onNavigateToSuppliers
                    )
                    SettingsDivider()
                    // Customers — admin directory of every customer in the
                    // system. Adding here propagates to every employee's
                    // Add Sale picker thanks to RLS on the customers table.
                    SettingsNavItem(
                        icon = Icons.Rounded.Group,
                        iconBg = Color(0xFFE0EEFF),
                        iconFg = Color(0xFF3B7BE0),
                        title = "Customers",
                        subtitle = "${state.customers.size} TOTAL · MANAGE",
                        onClick = onNavigateToCustomers
                    )
                }
                Spacer(modifier = Modifier.height(NTDp.md))
            }

            // ── Group 2: Notifications ────────────────────────
            item {
                SettingsGroup {
                    SettingsToggleItem(
                        icon = Icons.Rounded.NotificationsActive,
                        iconBg = Color(0xFFEDE9FE),
                        iconFg = Color(0xFF8B5CF6),
                        title = "Push notifications",
                        subtitle = "ORDERS · DUES · STOCK",
                        checked = pushNotif,
                        onToggle = { pushNotif = it }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Rounded.Chat,
                        iconBg = Color(0xFFDCFCE7),
                        iconFg = Color(0xFF10B981),
                        title = "WhatsApp reminders",
                        subtitle = "AUTO-SEND TO OVERDUE",
                        checked = whatsApp,
                        onToggle = { whatsApp = it }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Rounded.CloudDownload,
                        iconBg = NTColors.PrimaryLight,
                        iconFg = NTColors.Primary,
                        title = "Auto backup",
                        subtitle = "DAILY · CLOUD",
                        checked = autoBackup,
                        onToggle = { autoBackup = it }
                    )
                }
                Spacer(modifier = Modifier.height(NTDp.md))
            }

            // ── Group 3: Preferences ──────────────────────────
            item {
                SettingsGroup {
                    SettingsNavItem(
                        icon = Icons.Rounded.Translate,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        title = "Language",
                        subtitle = "ENGLISH · தமிழ்",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Rounded.DarkMode,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        title = "Dark mode",
                        subtitle = "SYSTEM DEFAULT",
                        checked = darkMode,
                        onToggle = { darkMode = it }
                    )
                    SettingsDivider()
                    SettingsNavItem(
                        icon = Icons.Rounded.Headphones,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        title = "Help & support",
                        subtitle = "CALL · CHAT · FAQ",
                        onClick = {}
                    )
                }
                Spacer(modifier = Modifier.height(NTDp.md))
            }

            // ── Group 4: Sign out ─────────────────────────────
            item {
                SettingsGroup {
                    SettingsNavItem(
                        icon = Icons.Rounded.Logout,
                        iconBg = NTColors.ErrorLight,
                        iconFg = NTColors.Error,
                        title = "Sign out",
                        subtitle = "",
                        titleColor = NTColors.TextPrimary,
                        onClick = onLogout
                    )
                }
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NTColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = NTColors.TextTertiary, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = NTColors.Divider,
        modifier = Modifier.padding(horizontal = NTDp.cardPad)
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    title: String,
    subtitle: String,
    titleColor: Color = NTColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = NTDp.cardPad, vertical = NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NTDp.md)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(NTDp.iconLg))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = NTColors.TextTertiary, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null,
            tint = NTColors.TextTertiary, modifier = Modifier.size(NTDp.iconMd))
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = NTDp.cardPad, vertical = NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NTDp.md)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(NTDp.iconLg))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NTColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = NTColors.TextTertiary, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NTColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = NTColors.TextDisabled
            )
        )
    }
}
