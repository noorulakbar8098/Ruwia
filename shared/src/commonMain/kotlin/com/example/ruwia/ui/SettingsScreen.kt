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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import com.example.ruwia.appVersionLabel
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.toDashboardMetrics
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import com.example.ruwia.ui.dashboard.*
import com.example.ruwia.util.capitalizeWords
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
    onNavigateToCustomers: () -> Unit = {},
    onLogout: () -> Unit,
    onResetEmptyCases: () -> Unit = {},
    onAddEmptyCases: (Int) -> Unit = {},
    onShopNameChange: (Int, String) -> Unit = { _, _ -> },
    contentPadding: PaddingValues = PaddingValues()
) {
    // Toggle states
    var pushNotif    by remember { mutableStateOf(true) }
    var whatsApp     by remember { mutableStateOf(true) }
    var autoBackup   by remember { mutableStateOf(true) }
    var darkMode     by remember { mutableStateOf(NTColors.isDarkMode) }
    var showResetDialog  by remember { mutableStateOf(false) }
    var showAddEmptyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut   by remember { mutableStateOf(false) }
    var shopOneName    by remember { mutableStateOf(state.shopNames.getOrNull(0).orEmpty()) }
    var shopTwoName    by remember { mutableStateOf(state.shopNames.getOrNull(1).orEmpty()) }
    var editingShopIndex by remember { mutableStateOf<Int?>(null) }
    var editingShopName  by remember { mutableStateOf("") }

    if (isLoggingOut) {
        SaaSLoadingOverlay(message = "Signing Out")
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000L)
            onLogout()
        }
    }

    val shopCount  = state.shopStocks.size.coerceAtLeast(2)
    val staffCount = state.employees.count { it.status != "inactive" }
    val custCount  = state.customers.size
    // Live Empty Cases figure for the stepper dialog (same source as dashboards).
    val liveEmptyCases = remember(state) { state.toDashboardMetrics().emptyCansAtShop }

    if (showAddEmptyDialog) {
        EmptyCasesStepperDialog(
            currentCount = liveEmptyCases,
            onDismiss    = { showAddEmptyDialog = false },
            onConfirm    = { qty ->
                showAddEmptyDialog = false
                onAddEmptyCases(qty)
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Empty Cases?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text = { Text("This sets the live Empty Cases figure to 0 business-wide, shown on the Stock tab and dashboards. Movement history is preserved. This cannot be undone.", color = NTColors.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetEmptyCases()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Error)
                ) {
                    Text("Reset Empty Cases", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
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

    editingShopIndex?.let { index ->
        val editingTitle = (if (index == 0) shopOneName else shopTwoName).ifBlank { "Shop" }
        ShopNameDialog(
            title = "Rename $editingTitle",
            value = editingShopName,
            onValueChange = { editingShopName = it },
            onDismiss = { editingShopIndex = null },
            onConfirm = {
                val clean = editingShopName.trim()
                if (clean.isNotBlank()) {
                    if (index == 0) shopOneName = clean else shopTwoName = clean
                    onShopNameChange(index, clean)
                }
                editingShopIndex = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Unified Top Bar ──
            NTPrimaryTopBar(
                title = "Settings",
                subtitle = "Workspace & Account Customization",
                onBack = onBack,
            )

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
                    GlossyTealBox(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Logo avatar
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(20.dp)),
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
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (adminEmail.isNotEmpty()) {
                                        Text(
                                            text = adminEmail,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                            Spacer(Modifier.height(16.dp))

                            // Stats strip cells
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                GlossStatCell("$shopCount", "SHOPS")
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.12f)))
                                GlossStatCell("$staffCount", "STAFF")
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.12f)))
                                GlossStatCell("$custCount", "CUSTOMERS")
                            }
                        }
                    }
                }

                // ── 2. Business settings ──
                item {
                    SettingsGroupCard("BUSINESS MANAGEMENT") {
                        SettingsNavItem(
                            icon = Icons.Rounded.Group,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Staff & Roles",
                            subtitle = "Manage $staffCount active team members and access controls",
                            onClick = onNavigateToEmployees
                        )

                        SettingsDivider()
                        SettingsNavItem(
                            icon = Icons.Rounded.People,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Customers",
                            subtitle = "$custCount registered distribution clients",
                            onClick = onNavigateToCustomers
                        )
                        SettingsDivider()
                        SettingsNavItem(
                            icon = Icons.Rounded.AddBox,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Add Empty Cases",
                            subtitle = "$liveEmptyCases available · record returned cases",
                            onClick = { showAddEmptyDialog = true }
                        )
                        SettingsDivider()
                        SettingsNavItem(
                            icon = Icons.Rounded.Undo,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Reset Empty Cases",
                            subtitle = "Zero out the live Empty Cases figure business-wide",
                            onClick = { showResetDialog = true }
                        )
                    }
                }

                // ── 3. Preferences settings ──
                item {
                    SettingsGroupCard("SYSTEM PREFERENCES") {
                        SettingsToggleItem(
                            icon = Icons.Rounded.NotificationsActive,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
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
                        SettingsDivider()
                        SettingsShopNameItem(
                            icon = Icons.Rounded.Edit,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Rename Shop",
                            subtitle = "Current: ${shopOneName.ifBlank { "Not set" }}",
                            value = shopOneName,
                            onClick = {
                                editingShopIndex = 0
                                editingShopName = shopOneName
                            }
                        )
                        SettingsDivider()
                        SettingsShopNameItem(
                            icon = Icons.Rounded.Edit,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            title = "Rename Shop",
                            subtitle = "Current: ${shopTwoName.ifBlank { "Not set" }}",
                            value = shopTwoName,
                            onClick = {
                                editingShopIndex = 1
                                editingShopName = shopTwoName
                            }
                        )
                    }
                }

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

                // ── 7. App version ──
                item {
                    Text(
                        text = "Version ${appVersionLabel()}",
                        fontSize = 11.sp,
                        color = NTColors.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// ── Helper Mini Composables ──

@Composable
private fun GlossStatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.60f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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

@Composable
private fun SettingsShopNameItem(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    title: String,
    subtitle: String,
    value: String,
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
private fun ShopNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
        text = {
            Column {
                Text(
                    "Set the display name for this shop. It will update on the inventory and employee screens.",
                    color = NTColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(capitalizeWords(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Shop name", color = NTColors.TextTertiary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. Main Outlet", color = NTColors.TextTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() })
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Primary)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NTColors.TextSecondary)
            }
        },
        containerColor = NTColors.Surface
    )
}