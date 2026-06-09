package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Employees Screen
// ─────────────────────────────────────────────────────────────

private val avatarPalette = listOf(
    Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFFF97316),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6)
)

@Composable
fun EmployeesScreen(
    employees: List<EmployeeInfo>,
    onBack: () -> Unit,
    onAddEmployee: () -> Unit = {}
) {
    var selectedRole by remember { mutableStateOf("All") }

    val roles = buildList {
        add("All")
        employees.map { it.role.replaceFirstChar { c -> c.uppercaseChar() } }
            .distinct().forEach { add(it) }
    }
    val filtered = if (selectedRole == "All") employees
    else employees.filter { it.role.equals(selectedRole, ignoreCase = true) }

    val onlineCount = employees.count { it.status == "active" || it.status == "on_delivery" }
    val shopCount   = employees.map { it.shopName }.distinct().size
    val roleCount   = employees.map { it.role }.distinct().size

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)
            .statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Top bar ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.md, vertical = NTDp.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScreenIconButton(icon = Icons.Rounded.ArrowBack, onClick = onBack)
                    Row(
                        modifier = Modifier.weight(1f).padding(horizontal = NTDp.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Employees", color = NTColors.TextPrimary,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("· ${employees.size}", color = NTColors.TextTertiary,
                            fontSize = 18.sp, fontWeight = FontWeight.Normal)
                    }
                    ScreenIconButton(icon = Icons.Rounded.PersonAdd, onClick = onAddEmployee)
                }
            }

            // ── Hero card ─────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad)
                        .clip(RoundedCornerShape(NTDp.radXxl))
                        .background(Brush.linearGradient(
                            listOf(NTColors.PrimaryDark, NTColors.GradEnd)))
                ) {
                    Box(
                        modifier = Modifier.size(130.dp)
                            .offset(x = 220.dp, y = (-20).dp)
                            .clip(CircleShape).background(NTColors.GradAccent)
                    )
                    Row(
                        modifier = Modifier.padding(NTDp.cardPad),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NTDp.md)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp)
                                .clip(RoundedCornerShape(NTDp.radLg))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Groups, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("TEAM", color = NTColors.PrimaryLight.copy(alpha = 0.8f),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${employees.size} employees · $onlineCount online",
                                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                lineHeight = 26.sp)
                            Text("$shopCount SHOPS · $roleCount ROLES",
                                color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(NTDp.md)) }

            // ── Filter chips ──────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = NTDp.screenPad),
                    horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
                ) {
                    roles.forEach { role ->
                        val count = if (role == "All") employees.size
                        else employees.count { it.role.equals(role, ignoreCase = true) }
                        val isSelected = role == selectedRole
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(NTDp.radFull))
                                    .background(
                                        if (isSelected) NTColors.AccentLight
                                        else NTColors.PrimaryLight
                                    )
                                    .clickable { selectedRole = role }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "$role · $count",
                                    color = if (isSelected) NTColors.AccentDark else NTColors.Primary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(NTDp.md)) }

            // ── Employee cards ────────────────────────────────
            itemsIndexed(items = filtered, key = { _, e -> e.id }) { index, employee ->
                EmployeeCard(employee = employee, colorIndex = employees.indexOf(employee))
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: EmployeeInfo, colorIndex: Int) {
    val isOnline = employee.status == "active" || employee.status == "on_delivery"
    val avatarColor = avatarPalette[colorIndex % avatarPalette.size]
    val initials = employee.name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar + online dot
            Box {
                Box(
                    modifier = Modifier.size(52.dp)
                        .clip(RoundedCornerShape(NTDp.radMd))
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.size(12.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-2).dp, y = 2.dp)
                        .clip(CircleShape)
                        .border(2.dp, NTColors.Surface, CircleShape)
                        .background(if (isOnline) NTColors.Success else NTColors.TextDisabled)
                )
            }

            Spacer(modifier = Modifier.width(NTDp.md))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.name, color = NTColors.TextPrimary,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(employee.phone, color = NTColors.TextTertiary,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoleBadge(role = employee.role)
                    ShopBadge(shopName = employee.shopName)
                }
            }

            // Stat
            if (employee.todayStat > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${employee.todayStat}",
                        color = NTColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        employee.statLabel,
                        color = NTColors.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val (bg, fg) = when (role.lowercase()) {
        "manager" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "stock"   -> NTColors.PrimaryLight to NTColors.Primary
        "cashier" -> NTColors.InfoLight   to NTColors.InfoText
        "driver"  -> NTColors.SurfaceVar  to NTColors.TextSecondary
        else      -> NTColors.SurfaceVar  to NTColors.TextSecondary
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
            .background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(role.uppercase(), color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun ShopBadge(shopName: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
            .background(NTColors.PrimaryLight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(shopName.uppercase(), color = NTColors.Primary,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun ScreenIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = NTColors.TextPrimary,
            modifier = Modifier.size(NTDp.iconMd))
    }
}
