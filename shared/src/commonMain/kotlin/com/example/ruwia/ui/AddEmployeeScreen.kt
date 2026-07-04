package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.ui.dashboard.*
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────
//  Add Employee Screen — creates Supabase Auth credential
// ─────────────────────────────────────────────────────────────

private val roles  = listOf("Manager", "Stock", "Cashier", "Driver")
private val shops  = listOf("Shop 1", "Shop 2")

private val avatarPalette = listOf(
    Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFFF97316),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6)
)

// ── Helpers (pure Kotlin, KMP-safe) ───────────────────────────

private fun generateEmail(name: String): String =
    name.trim().lowercase().replace(Regex("\\s+"), ".") + "@neerthuli.in"

private fun generatePassword(): String {
    val upper  = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val lower  = "abcdefghjkmnpqrstuvwxyz"
    val digits = "23456789"
    val special = "@#!"
    val all = upper + lower + digits + special
    // Guarantee at least one of each class, then pad to 10 chars
    val pwd = buildString {
        append(upper[Random.nextInt(upper.length)])
        append(digits[Random.nextInt(digits.length)])
        append(special[Random.nextInt(special.length)])
        repeat(7) { append(all[Random.nextInt(all.length)]) }
    }
    // Shuffle so the guaranteed chars aren't always at the start
    return pwd.toList().shuffled().joinToString("")
}

// ─────────────────────────────────────────────────────────────

// EmployeeCredentials moved to EmployeeCreationState.Success in AdminViewModel.
// The credentials dialog (EmployeeCreatedDialog) now lives in AdminDashboardScreen.kt.

@Composable
fun AddEmployeeScreen(
    isSaving: Boolean = false,
    saveError: String? = null,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSave: (name: String, phone: String, role: String, shop: String,
             salary: Double, email: String, password: String) -> Unit
) {
    // ── Form state ────────────────────────────────────────────
    var name          by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var role          by remember { mutableStateOf("") }
    var shop          by remember { mutableStateOf("") }
    var salary        by remember { mutableStateOf("") }
    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf(generatePassword()) }
    var showPassword  by remember { mutableStateOf(false) }

    // Auto-fill email when name changes (user can still override)
    var emailEdited by remember { mutableStateOf(false) }
    LaunchedEffect(name) {
        if (!emailEdited && name.isNotBlank()) email = generateEmail(name)
    }

    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    val avatarColor = if (name.isNotEmpty())
        avatarPalette[(name.first().code) % avatarPalette.size]
    else NTColors.PrimaryDark

    val canSave = !isSaving &&
                  name.isNotBlank() && phone.isNotBlank() &&
                  role.isNotBlank() && shop.isNotBlank() &&
                  email.contains("@") && password.length >= 8

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ── Top bar ───────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = NTDp.md, vertical = NTDp.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AEIconBtn(Icons.Rounded.ArrowBack, onBack)
                        Text(
                            "Add employee",
                            color = NTColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = NTDp.md)
                        )
                        AEIconBtn(Icons.Rounded.Close, onClose)
                    }
                }

                // ── Hero card ─────────────────────────────────
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = NTDp.screenPad)
                            .clip(RoundedCornerShape(NTDp.radXxl))
                            .background(Brush.linearGradient(
                                listOf(NTColors.PrimaryDark, NTColors.GradEnd)))
                    ) {
                        Box(modifier = Modifier.size(130.dp)
                            .offset(x = 220.dp, y = (-20).dp)
                            .clip(CircleShape).background(NTColors.GradAccent))
                        Row(
                            modifier = Modifier.padding(NTDp.cardPad),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NTDp.md)
                        ) {
                            // Live avatar preview
                            Box(
                                modifier = Modifier.size(56.dp)
                                    .clip(RoundedCornerShape(NTDp.radLg))
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (initials.isNotEmpty()) {
                                    Text(initials, color = Color.White,
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                            Column {
                                Text("NEW EMPLOYEE",
                                    color = NTColors.PrimaryLight.copy(alpha = 0.8f),
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp)
                                Text(
                                    text = if (name.isBlank()) "Joining your team"
                                           else name.trim(),
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = listOfNotNull(
                                        role.ifBlank { null },
                                        shop.ifBlank { null }
                                    ).joinToString(" · ").uppercase().ifBlank {
                                        "Fill in the details below"
                                    },
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(NTDp.md)) }

                // ── Section 1: Personal info ──────────────────
                item {
                    AESectionCard(1, "Personal info") {
                        AELabel("Full name")
                        AETextField(value = name, onValueChange = { name = it },
                            icon = Icons.Rounded.Person, placeholder = "e.g. Ravi Velu")

                        Spacer(modifier = Modifier.height(NTDp.md))

                        AELabel("Phone number")
                        AETextField(value = phone, onValueChange = { v -> if (v.length <= 15) phone = v },
                            icon = Icons.Rounded.Phone, placeholder = "+91 98765 43210",
                            keyboard = KeyboardType.Phone)
                    }
                }

                item { Spacer(modifier = Modifier.height(NTDp.md)) }

                // ── Section 2: Role & Shop ────────────────────
                item {
                    AESectionCard(2, "Role & assignment") {
                        AELabel("Role")
                        AEChipGroup(options = roles, selected = role, onSelect = { role = it })

                        Spacer(modifier = Modifier.height(NTDp.md))

                        AELabel("Shop assignment")
                        AEChipGroup(options = shops, selected = shop, onSelect = { shop = it })
                    }
                }

                item { Spacer(modifier = Modifier.height(NTDp.md)) }

                // ── Section 3: Compensation ───────────────────
                item {
                    AESectionCard(3, "Compensation") {
                        AELabel("Monthly salary (optional)")
                        AETextField(
                            value = salary,
                            onValueChange = { v -> if (v.all { it.isDigit() || it == '.' }) salary = v },
                            icon = Icons.Rounded.CurrencyRupee,
                            placeholder = "0",
                            keyboard = KeyboardType.Number,
                            trailingLabel = "/ month"
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(NTDp.md)) }

                // ── Section 4: Login credentials ─────────────
                item {
                    AESectionCard(4, "Login credentials") {
                        // Info banner
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(NTDp.radMd))
                                .background(NTColors.PrimaryLight)
                                .padding(NTDp.md),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null,
                                tint = NTColors.Primary, modifier = Modifier.size(16.dp)
                                    .padding(top = 2.dp))
                            Text(
                                "A Supabase account will be created for this employee. " +
                                "Share these credentials so they can log in on their device.",
                                color = NTColors.PrimaryDark,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(NTDp.md))

                        AELabel("Email address")
                        AETextField(
                            value = email,
                            onValueChange = { emailEdited = true; email = it },
                            icon = Icons.Rounded.Email,
                            placeholder = "employee@neerthuli.in",
                            keyboard = KeyboardType.Email
                        )

                        Spacer(modifier = Modifier.height(NTDp.md))

                        AELabel("Temporary password")
                        // Password row with show/hide + regenerate
                        Row(
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                .clip(RoundedCornerShape(NTDp.radMd))
                                .border(1.dp,
                                    if (password.isNotBlank()) NTColors.Primary.copy(alpha = 0.5f)
                                    else NTColors.Border,
                                    RoundedCornerShape(NTDp.radMd))
                                .background(NTColors.Background)
                                .padding(horizontal = NTDp.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null,
                                tint = if (password.isNotBlank()) NTColors.Primary
                                       else NTColors.TextTertiary,
                                modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(NTDp.sm))
                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    textStyle = TextStyle(
                                        color = NTColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(NTColors.Primary),
                                    singleLine = true,
                                    visualTransformation = if (showPassword)
                                        VisualTransformation.None
                                    else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // Show / hide
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff
                                              else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = NTColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                                    .clickable { showPassword = !showPassword }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Re-generate
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Regenerate",
                                tint = NTColors.Primary,
                                modifier = Modifier.size(18.dp)
                                    .clickable { password = generatePassword() }
                            )
                        }
                        Spacer(modifier = Modifier.height(NTDp.xs))
                        Text(
                            "Min 8 characters · tap refresh to regenerate",
                            color = NTColors.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(NTDp.sm)) }
            }

            // ── Fixed bottom bar ──────────────────────────────
            HorizontalDivider(color = NTColors.Border, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(NTColors.Surface)
                    .padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
                horizontalArrangement = Arrangement.spacedBy(NTDp.md)
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(52.dp)
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .border(1.5.dp, NTColors.Border, RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.Surface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = NTColors.TextSecondary,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier.weight(2f).height(52.dp)
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(if (canSave) NTColors.Primary else NTColors.TextDisabled)
                        .clickable(enabled = canSave) {
                            onSave(
                                name.trim(), phone.trim(), role, shop,
                                salary.toDoubleOrNull() ?: 0.0,
                                email.trim(), password
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Create account", color = Color.White,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Inline error banner (shown below action bar) ──
            if (saveError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(NTColors.ErrorLight)
                        .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null,
                        tint = NTColors.Error, modifier = Modifier.size(16.dp))
                    Text(saveError, color = NTColors.ErrorText,
                        fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// CredentialsDialog moved to AdminDashboardScreen.kt as EmployeeCreatedDialog.

// ── Reusable local helpers ────────────────────────────────────

@Composable
private fun AEIconBtn(icon: ImageVector, onClick: () -> Unit) {
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
private fun AESectionCard(number: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NTDp.md),
                modifier = Modifier.padding(bottom = NTDp.lg)
            ) {
                Box(
                    modifier = Modifier.size(30.dp)
                        .clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.PrimaryDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$number", color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                }
                Text(title, color = NTColors.TextPrimary, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun AELabel(text: String) {
    Text(text, color = NTColors.TextSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(NTDp.sm))
}

@Composable
private fun AETextField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    trailingLabel: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp)
            .clip(RoundedCornerShape(NTDp.radMd))
            .border(
                1.dp,
                if (value.isNotBlank()) NTColors.Primary.copy(alpha = 0.5f) else NTColors.Border,
                RoundedCornerShape(NTDp.radMd)
            )
            .background(NTColors.Background)
            .padding(horizontal = NTDp.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            tint = if (value.isNotBlank()) NTColors.Primary else NTColors.TextTertiary,
            modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(NTDp.sm))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = NTColors.TextDisabled, fontSize = 14.sp)
            BasicTextField(
                value = value, onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                textStyle = TextStyle(color = NTColors.TextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(NTColors.Primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingLabel != null) {
            Spacer(modifier = Modifier.width(NTDp.sm))
            Text(trailingLabel, color = NTColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AEChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(if (isSelected) NTColors.Primary else NTColors.Background)
                    .border(1.dp, if (isSelected) Color.Transparent else NTColors.Border,
                        RoundedCornerShape(NTDp.radMd))
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option,
                    color = if (isSelected) Color.White else NTColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
