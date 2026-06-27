package com.example.ruwia.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import com.example.ruwia.ui.dashboard.*
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ruwia.shared.generated.resources.app_icon

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
    onLogout: () -> Unit,
    onDeleteAllData: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    // Toggle states
    var pushNotif    by remember { mutableStateOf(true) }
    var whatsApp     by remember { mutableStateOf(true) }
    var autoBackup   by remember { mutableStateOf(true) }
    var darkMode     by remember { mutableStateOf(NTColors.isDarkMode) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    if (isLoggingOut) {
        SaaSLoadingOverlay(message = "Signing Out")
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000L)
            onLogout()
        }
    }

    val shopCount  = state.shopStocks.size.coerceAtLeast(2)
    val staffCount = state.employees.size
    val custCount  = state.customers.size

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text = { Text("This will permanently delete all transactions, customers, suppliers, expenses, and staff accounts. Product stocks will be reset to 0. This action cannot be undone.", color = NTColors.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Error)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = NTColors.TextSecondary)
                }
            },
            containerColor = NTColors.Surface
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text = { Text("Are you sure you want to sign out of your account?", color = NTColors.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        isLoggingOut = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Error)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = NTColors.TextSecondary)
                }
            },
            containerColor = NTColors.Surface
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ── Unified Top Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface)
                    .statusBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
                            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = NTColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NTColors.TextPrimary
                        )
                        Text(
                            text = "Workspace & Account Customization",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NTColors.TextTertiary
                        )
                    }
                }
            }

            // ── Scrollable Body ──
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. Business Hero Profile Card ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                        border = BorderStroke(1.dp, NTColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Premium Gradient Logo Avatar
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                    ,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.app_icon),
                                        contentDescription = "Neerthuli Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = adminName.ifEmpty { "Neerthuli Water" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NTColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (adminEmail.isNotEmpty()) {
                                        Text(
                                            text = adminEmail,
                                            fontSize = 12.sp,
                                            color = NTColors.TextTertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                                    ) {
//                                        Box(
//                                            modifier = Modifier
//                                                .clip(RoundedCornerShape(6.dp))
//                                                .background(NTColors.AccentLight)
//                                                .padding(horizontal = 8.dp, vertical = 2.dp)
//                                        ) {
//                                            Text(
//                                                "Business Pro",
//                                                color = NTColors.Accent,
//                                                fontSize = 10.sp,
//                                                fontWeight = FontWeight.Bold
//                                            )
//                                        }
//                                        Text(
//                                            "★★★★ Premium",
//                                            fontSize = 11.sp,
//                                            color = NTColors.Accent,
//                                            fontWeight = FontWeight.SemiBold
//                                        )
//                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = NTColors.Divider)
                            Spacer(Modifier.height(16.dp))
                            
                            // Stats strip cells
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatCell("$shopCount", "SHOPS")
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(NTColors.Border))
                                StatCell("$staffCount", "STAFF")
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(NTColors.Border))
                                StatCell("$custCount", "CUSTOMERS")
                            }
                        }
                    }
                }

                // ── 2. Business settings ──
                item {
                    SettingsGroupCard("BUSINESS MANAGEMENT") {
                        SettingsNavItem(
                            icon = Icons.Rounded.Group,
                            iconBg = NTColors.AccentLight,
                            iconFg = NTColors.Accent,
                            title = "Staff & Roles",
                            subtitle = "Manage $staffCount active team members and access controls",
                            onClick = onNavigateToEmployees
                        )
                        SettingsDivider()
                        SettingsNavItem(
                            icon = Icons.Rounded.Store,
                            iconBg = NTColors.InfoLight,
                            iconFg = NTColors.Info,
                            title = "Suppliers",
                            subtitle = "${state.suppliersFull.size} active logistics and procurement sources",
                            onClick = onNavigateToSuppliers
                        )
                        SettingsDivider()
                        SettingsNavItem(
                            icon = Icons.Rounded.People,
                            iconBg = Color(0xFF1E153A),
                            iconFg = Color(0xFF8B5CF6),
                            title = "Customers",
                            subtitle = "$custCount registered distribution clients",
                            onClick = onNavigateToCustomers
                        )
                    }
                }

                // ── 3. Preferences settings ──
                item {
                    SettingsGroupCard("SYSTEM PREFERENCES") {
                        SettingsToggleItem(
                            icon = Icons.Rounded.NotificationsActive,
                            iconBg = Color(0xFF1E153A),
                            iconFg = Color(0xFF8B5CF6),
                            title = "Push Notifications",
                            subtitle = "Orders, dues, and transaction alerts",
                            checked = pushNotif,
                            onToggle = { pushNotif = it }
                        )
                        SettingsDivider()
                        SettingsToggleItem(
                            icon = Icons.Rounded.DarkMode,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Dark Mode",
                            subtitle = "Configure theme appearance settings",
                            checked = darkMode,
                            onToggle = { 
                                darkMode = it
                                NTColors.isDarkMode = it
                            }
                        )
                    }
                }

                // ── 5. Danger Zone ──
//                item {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(24.dp),
//                        colors = CardDefaults.cardColors(containerColor = NTColors.ErrorLight),
//                        border = BorderStroke(1.dp, NTColors.Error)
//                    ) {
//                        Column(modifier = Modifier.padding(20.dp)) {
//                            Text(
//                                "DANGER ZONE",
//                                fontSize = 11.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = NTColors.ErrorText,
//                                letterSpacing = 1.2.sp
//                            )
//                            Spacer(Modifier.height(14.dp))
//
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .clickable { showDeleteDialog = true }
//                                    .padding(vertical = 4.dp),
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(12.dp)
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(36.dp)
//                                        .clip(RoundedCornerShape(8.dp))
//                                        .background(NTColors.ErrorLight),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Icon(Icons.Rounded.DeleteForever, null, tint = NTColors.Error, modifier = Modifier.size(20.dp))
//                                }
//                                Column(modifier = Modifier.weight(1f)) {
//                                    Text("Delete All Data", color = NTColors.ErrorText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
//                                    Text("Permanently erase database transactions", color = NTColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Medium)
//                                }
//                                Icon(Icons.Rounded.ChevronRight, null, tint = NTColors.Error, modifier = Modifier.size(20.dp))
//                            }
//                        }
//                    }
//                }
                
                // ── 6. Sign Out Button ──
                item {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.ErrorLight),
                        border = BorderStroke(1.dp, NTColors.Error)
                    ) {
                        Icon(Icons.Rounded.Logout, null, tint = NTColors.Error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out of Account", color = NTColors.Error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Helper Mini Composables ──

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NTColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = NTColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.Primary,
            letterSpacing = 1.2.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
            border = BorderStroke(1.dp, NTColors.Border),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = NTColors.Divider,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = NTColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NTColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = NTColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
