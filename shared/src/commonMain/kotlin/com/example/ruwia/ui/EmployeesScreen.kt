package com.example.ruwia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.ruwia.util.capitalizeWords
import com.example.ruwia.util.isTextField
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    shops: List<String> = emptyList(),
    onBack: () -> Unit,
    onAddEmployee: () -> Unit = {},
    onUpdateEmployee: (EmployeeInfo) -> Unit = {},
    onDeleteEmployee: (String) -> Unit = {},
) {
    var selectedRole by remember { mutableStateOf("All") }
    var editingEmployee by remember { mutableStateOf<EmployeeInfo?>(null) }
    var deletingEmployee by remember { mutableStateOf<EmployeeInfo?>(null) }

    // Inactive (soft-deleted) employees are kept in state only so their names
    // still resolve in stock history and the report summary — hide them here.
    val active = employees.filter { it.status != "inactive" }

    val roles = buildList {
        add("All")
        active.map { it.role.replaceFirstChar { c -> c.uppercaseChar() } }
            .distinct().forEach { add(it) }
    }
    val filtered = if (selectedRole == "All") active
    else active.filter { it.role.equals(selectedRole, ignoreCase = true) }

    val onlineCount = active.count { it.status == "active" || it.status == "on_delivery" }
    val shopCount   = active.map { it.shopName }.distinct().size
    val roleCount   = active.map { it.role }.distinct().size

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
                        Text("· ${active.size}", color = NTColors.TextTertiary,
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
                            Text("${active.size} employees · $onlineCount online",
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
                        val count = if (role == "All") active.size
                        else active.count { it.role.equals(role, ignoreCase = true) }
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
                EmployeeCard(
                    employee   = employee,
                    colorIndex = employees.indexOf(employee),
                    onEdit     = { editingEmployee = employee },
                    onDelete   = { deletingEmployee = employee },
                )
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }

        editingEmployee?.let { e ->
            val shopOptions = (shops + employees.map { it.shopName })
                .map { it.trim() }.filter { it.isNotBlank() }.distinct()
            EmployeeEditDialog(
                initial = e,
                shops   = shopOptions,
                onDismiss = { editingEmployee = null },
                onSave    = { updated ->
                    onUpdateEmployee(updated)
                    editingEmployee = null
                },
            )
        }

        deletingEmployee?.let { e ->
            AlertDialog(
                onDismissRequest = { deletingEmployee = null },
                icon  = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error) },
                title = { Text("Remove ${e.name}?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
                text  = {
                    Text(
                        "This employee will no longer be able to sign in. Their past sales and stock history will remain visible in reports.",
                        fontSize = 13.sp, color = NTColors.TextSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteEmployee(e.id)
                            deletingEmployee = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Error, contentColor = Color.White),
                    ) { Text("Remove", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { deletingEmployee = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.TextSecondary),
                        border = BorderStroke(1.dp, NTColors.Border),
                    ) { Text("Cancel") }
                },
                containerColor = NTColors.Surface,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
private fun EmployeeCard(
    employee: EmployeeInfo,
    colorIndex: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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

            // Stat + actions
            Column(horizontalAlignment = Alignment.End) {
                if (employee.todayStat >= 0) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier.size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NTColors.SurfaceVar)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, "Edit", tint = NTColors.TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    Box(
                        modifier = Modifier.size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NTColors.ErrorLight)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Delete, "Delete", tint = NTColors.Error, modifier = Modifier.size(15.dp))
                    }
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

@Composable
private fun EmployeeEditDialog(
    initial: EmployeeInfo,
    shops: List<String>,
    onDismiss: () -> Unit,
    onSave: (EmployeeInfo) -> Unit,
) {
    var name   by remember { mutableStateOf(initial.name) }
    var phone  by remember { mutableStateOf(initial.phone) }
    var role   by remember { mutableStateOf(initial.role.ifBlank { "staff" }) }
    var shop   by remember { mutableStateOf(initial.shopName.ifBlank { shops.firstOrNull().orEmpty() }) }
    var salary by remember { mutableStateOf(initial.monthlySalary.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NTColors.Surface, RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Text("Edit employee", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Update this employee's profile details.", fontSize = 12.sp, color = NTColors.TextTertiary)
            Spacer(Modifier.height(NTDp.md))

            EmployeeFieldLabel("Name")
            EmployeeFieldInput(
                value = name, onValueChange = { name = it },
                placeholder = "Employee name",
                keyboardType = KeyboardType.Text,
            )
            Spacer(Modifier.height(NTDp.md))
            EmployeeFieldLabel("Phone")
            EmployeeFieldInput(
                value = phone, onValueChange = { phone = it },
                placeholder = "Mobile number",
                keyboardType = KeyboardType.Phone,
            )
            Spacer(Modifier.height(NTDp.md))
            EmployeeFieldLabel("Monthly salary")
            EmployeeFieldInput(
                value = salary, onValueChange = { salary = it },
                placeholder = "e.g. 15000",
                keyboardType = KeyboardType.Decimal,
            )

            Spacer(Modifier.height(NTDp.md))
            Text("Role", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("manager", "stock", "cashier", "driver").forEach { r ->
                    val selected = role.equals(r, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NTDp.radFull))
                            .background(if (selected) NTColors.AccentLight else NTColors.SurfaceVar)
                            .clickable { role = r }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            r.replaceFirstChar { it.uppercaseChar() },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) NTColors.AccentDark else NTColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(NTDp.md))
            Text("Shop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shopOptions = shops.filter { it.isNotBlank() }
                shopOptions.forEach { s ->
                    val selected = shop == s
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NTDp.radFull))
                            .background(if (selected) NTColors.AccentLight else NTColors.SurfaceVar)
                            .clickable { shop = s }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            s,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) NTColors.AccentDark else NTColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(NTDp.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NTColors.PrimaryLight)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, color = NTColors.Primary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (name.isNotBlank()) NTColors.Primary else NTColors.TextDisabled)
                        .clickable(enabled = name.isNotBlank()) {
                            onSave(
                                initial.copy(
                                    name          = name.trim().ifBlank { initial.name },
                                    phone         = phone.trim(),
                                    role          = role.lowercase(),
                                    shopName      = shop,
                                    monthlySalary = salary.toDoubleOrNull() ?: initial.monthlySalary,
                                )
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = NTColors.TextOnPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmployeeFieldLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = NTColors.TextSecondary,
        letterSpacing = 0.4.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun EmployeeFieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = NTColors.TextTertiary)
        }
        BasicTextField(
            value = value,
            onValueChange = if (keyboardType.isTextField()) { { v -> onValueChange(capitalizeWords(v)) } } else onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary),
            cursorBrush = SolidColor(NTColors.Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = if (keyboardType.isTextField()) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
