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
import androidx.compose.material.icons.automirrored.rounded.Undo
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
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp

@Composable
fun EmployeeProfileScreen(
    employeeName: String = "Employee",
    employeeId: String = "",
    email: String = "",
    phone: String = "",
    shopInfo: String = "",
    role: String = "",
    joinDate: String = "",
    todayOutward: Int = 0,
    todayInward: Int = 0,
    customerCount: Int = 0,
    supplierCount: Int = 0,
    onAddCustomer: (com.example.ruwia.domain.Customer) -> Unit = {},
    onAddSupplier: (name: String, location: String?) -> Unit = { _, _ -> },
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(NTColors.isDarkMode) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Logout?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text    = { Text("Are you sure you want to log out of your account?", color = NTColors.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = NTColors.Error),
                ) { Text("Logout", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors  = ButtonDefaults.textButtonColors(contentColor = NTColors.TextSecondary)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(NTDp.radXxl),
            containerColor = NTColors.Surface,
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        containerColor = NTColors.Background,
        topBar = { ProfileTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Profile Card ─────────────────────────────
            ProfileCard(
                name     = employeeName,
                shopName = shopInfo
            )

            // ── Today's Performance KPI ──────────────────
            TodayPerformanceKPI(
                outward = todayOutward,
                inward  = todayInward,
            )

            // ── Information Section ──────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NTDp.radXxl))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
                    .padding(vertical = 8.dp),
            ) {
                ProfileSectionHeader("INFORMATION")
                ProfileInfoRow(icon = Icons.Rounded.Person, label = "Name", value = employeeName)
                if (email.isNotBlank()) {
                    ProfileDivider()
                    ProfileInfoRow(icon = Icons.Rounded.Email, label = "Email", value = email)
                }
                if (phone.isNotBlank()) {
                    ProfileDivider()
                    ProfileInfoRow(icon = Icons.Rounded.Phone, label = "Phone", value = phone)
                }
                if (shopInfo.isNotBlank()) {
                    ProfileDivider()
                    ProfileInfoRow(icon = Icons.Rounded.Store, label = "Assigned Shop", value = shopInfo.replace("·", "•"))
                }
                if (role.isNotBlank()) {
                    ProfileDivider()
                    ProfileInfoRow(icon = Icons.Rounded.Badge, label = "Role", value = role)
                }
            }

            // ── Settings ─────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NTDp.radXxl))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
                    .padding(vertical = 8.dp),
            ) {
                ProfileSectionHeader("SETTINGS")

                // Dark / Light Mode toggle
                ProfileToggleRow(
                    icon    = if (darkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    label   = if (darkMode) "Dark Mode" else "Light Mode",
                    checked = darkMode,
                    onToggle = {
                        darkMode = it
                        NTColors.isDarkMode = it
                    }
                )

                ProfileDivider()
                ProfileSettingRow(icon = Icons.Rounded.NotificationsActive, label = "Notifications") { }
                ProfileDivider()
                ProfileSettingRow(icon = Icons.Rounded.Help, label = "Help & Support") { }
                ProfileDivider()
                ProfileSettingRow(
                    icon  = Icons.Rounded.ExitToApp,
                    label = "Logout",
                    tint  = Color(0xFFEF4444)
                ) {
                    showLogoutDialog = true
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NTColors.Surface, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                "Profile", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// ── Profile Card ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(
    name: String,
    shopName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Circle
            val initials = name.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NTColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = shopName.replace("·", "•"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NTColors.TextSecondary
                )
            }
            // Active status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radXxl))
                    .background(NTColors.SuccessLight)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.SuccessText
                )
            }
        }
    }
}

// ── Today's Performance KPI ──────────────────────────────────────────────────

@Composable
private fun TodayPerformanceKPI(
    outward: Int,
    inward: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KPICard(
            value = "$outward",
            label = "Cans Sold",
            icon = Icons.Rounded.LocalShipping,
            accent = NTColors.Info,
            modifier = Modifier.weight(1f)
        )
        KPICard(
            value = "$inward",
            label = "Empty Returned",
            icon = Icons.AutoMirrored.Rounded.Undo,
            accent = NTColors.Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KPICard(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(NTColors.PrimaryLight)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (value.length > 5) NTColors.TextPrimary else NTColors.PrimaryDeep,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NTColors.TextSecondary
            )
        }
    }
}

// ── Section Helpers ───────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = NTColors.Primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NTColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NTColors.TextSecondary
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary
        )
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    label: String,
    tint: Color = NTColors.TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (tint != NTColors.TextSecondary) tint else NTColors.TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NTColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = NTColors.Divider.copy(alpha = 0.6f),
        thickness = 0.6.dp,
    )
}

// ── Dark / Light mode toggle row ─────────────────────────────────────────────

@Composable
private fun ProfileToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NTColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = NTColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary
                )
                Text(
                    text = if (checked) "Currently in dark mode" else "Currently in light mode",
                    fontSize = 11.sp,
                    color = NTColors.TextTertiary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = NTColors.Primary,
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = NTColors.TextDisabled
            )
        )
    }
}
