package com.example.ruwia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_logo

// ─── Premium SaaS Color Palette ─────────────────────────────────────────────
private val DeepTeal = Color(0xFF006D77)
private val MidTeal = Color(0xFF1E8A6E)
private val EmeraldGreen = Color(0xFF2A9D8F)
private val AccentOrange = Color(0xFFF4A261)
private val DarkSurface = Color(0xFF0D1B2A)
private val CardDark = Color(0xFF152238)
private val CardBorder = Color(0xFF1E3050)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF8899AA)
private val TextMuted = Color(0xFF5C6E7F)
private val InputBackground = Color(0xFF1A2D42)
private val InputBorder = Color(0xFF253D56)
private val InputFocusBorder = Color(0xFF2A9D8F)
private val ErrorRed = Color(0xFFFF6B6B)
private val SuccessGreen = Color(0xFF51CF66)

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onForgotPassword: () -> Unit,
    onLoggedIn: (UserRole?) -> Unit,
) {
    val state by vm.state.collectAsState()

    // Screen state
    var isSignUp by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.admin) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var signupName by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }

    // Animated background shimmer
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn(state.role)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        Color(0xFF0F2237),
                        Color(0xFF122B44)
                    )
                )
            )
    ) {
        // ── Animated Ambient Background Orbs ──────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DeepTeal.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.85f + shimmerOffset * 0.1f),
                        y = size.height * 0.12f
                    ),
                    radius = size.width * 0.45f
                ),
                center = Offset(
                    x = size.width * (0.85f + shimmerOffset * 0.1f),
                    y = size.height * 0.12f
                ),
                radius = size.width * 0.45f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGreen.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(
                        x = size.width * (0.1f - shimmerOffset * 0.05f),
                        y = size.height * 0.85f
                    ),
                    radius = size.width * 0.55f
                ),
                center = Offset(
                    x = size.width * (0.1f - shimmerOffset * 0.05f),
                    y = size.height * 0.85f
                ),
                radius = size.width * 0.55f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentOrange.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(
                        x = size.width * 0.7f,
                        y = size.height * (0.55f + shimmerOffset * 0.05f)
                    ),
                    radius = size.width * 0.3f
                ),
                center = Offset(
                    x = size.width * 0.7f,
                    y = size.height * (0.55f + shimmerOffset * 0.05f)
                ),
                radius = size.width * 0.3f
            )
        }

        // ── Main Content ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Brand Header with Logo ────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(Res.drawable.app_logo),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(140.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── 2. Glassmorphic Card Container ────────────────────────────
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardDark.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CardBorder.copy(alpha = 0.6f),
                                CardBorder.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Title ──────────────────────────────────────────────
                    Text(
                        text = if (isSignUp) "Create your account" else "Welcome back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (isSignUp) "Register as an admin to manage your franchise"
                        else "Sign in to manage your water business",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // ── Role Selector (Sign-In only) ──────────────────────
                    if (!isSignUp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(InputBackground, RoundedCornerShape(14.dp))
                                .border(1.dp, InputBorder, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Owner Tab
                            val isOwnerSelected = selectedRole == UserRole.admin
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isOwnerSelected) Brush.linearGradient(
                                            colors = listOf(DeepTeal, EmeraldGreen)
                                        ) else Brush.linearGradient(
                                            colors = listOf(Color.Transparent, Color.Transparent)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedRole = UserRole.admin },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = "Owner",
                                        tint = if (isOwnerSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Admin",
                                        color = if (isOwnerSelected) Color.White else TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Employee Tab
                            val isEmpSelected = selectedRole == UserRole.employee
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isEmpSelected) Brush.linearGradient(
                                            colors = listOf(DeepTeal, EmeraldGreen)
                                        ) else Brush.linearGradient(
                                            colors = listOf(Color.Transparent, Color.Transparent)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedRole = UserRole.employee },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalShipping,
                                        contentDescription = "Employee",
                                        tint = if (isEmpSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Employee",
                                        color = if (isEmpSelected) Color.White else TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── Sign-Up Additional Fields (Animated) ──────────────
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SaasInputField(
                                value = signupName,
                                onValueChange = { signupName = it },
                                label = "Business / Owner Name",
                                placeholder = "Enter your business or full name",
                                icon = Icons.Outlined.Business
                            )

                            SaasInputField(
                                value = signupPhone,
                                onValueChange = { signupPhone = it },
                                label = "Contact Number",
                                placeholder = "Enter your phone number",
                                icon = Icons.Outlined.Phone
                            )
                        }
                    }

                    // ── Email Field ───────────────────────────────────────
                    SaasInputField(
                        value = email,
                        onValueChange = { email = it },
                        label = if (selectedRole == UserRole.employee && !isSignUp)
                            "Employee ID or Email" else "Email Address",
                        placeholder = if (selectedRole == UserRole.employee && !isSignUp)
                            "Enter your employee ID or email" else "you@company.com",
                        icon = Icons.Outlined.Email,
                        trailingContent = if (email.contains("@") && email.contains(".")) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null
                    )

                    // ── Password Field ────────────────────────────────────
                    SaasInputField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        trailingContent = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility
                                    else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // ── Options Row (Login Only) ──────────────────────────
                    if (!isSignUp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { onForgotPassword() }
                            )
                        }
                    }

                    // ── Primary Action Button ─────────────────────────────
                    Button(
                        onClick = {
                            if (isSignUp) {
                                vm.signUp(email.trim(), pass, signupName.trim(), signupPhone.trim())
                            } else {
                                vm.login(email.trim(), pass, selectedRole)
                            }
                        },
                        enabled = !state.loading && email.isNotBlank() && pass.length >= 6,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (!state.loading && email.isNotBlank() && pass.length >= 6)
                                        Brush.linearGradient(
                                            colors = listOf(DeepTeal, EmeraldGreen)
                                        )
                                    else Brush.linearGradient(
                                        colors = listOf(
                                            DeepTeal.copy(alpha = 0.4f),
                                            EmeraldGreen.copy(alpha = 0.4f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUp) "Create Account" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // ── Error Message ─────────────────────────────────────
                    state.error?.let { err ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ErrorRed.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Error",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = err,
                                    color = ErrorRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ── Divider ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = InputBorder
                        )
                        Text(
                            text = "  OR  ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = InputBorder
                        )
                    }

                    // ── Social / SSO Button ───────────────────────────────
                    OutlinedButton(
                        onClick = { /* Google SSO stub */ },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InputBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = InputBackground.copy(alpha = 0.5f),
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Stylized Google "G" using bold colored text
                            Text(
                                text = "G",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEA4335)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    // ── Toggle Sign In / Sign Up ──────────────────────────
                    if (isSignUp || selectedRole != UserRole.employee) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = if (isSignUp) "Sign In" else "Sign Up",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.clickable { isSignUp = !isSignUp }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Footer ────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(0.5f)
            ) {
                Text(
                    text = "Secured by Supabase  ·  256-bit TLS",
                    fontSize = 11.sp,
                    color = TextMuted,
                    style = TextStyle(letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Terms of Service  ·  Privacy Policy",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.loading) {
            SaaSLoadingOverlay(message = "Signing In")
        }
    }
}

// ─── Reusable SaaS Input Field with Material Icons & Placeholder ─────────────
@Composable
private fun SaasInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = TextMuted
                )
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
            else VisualTransformation.None,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = trailingContent,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InputFocusBorder,
                unfocusedBorderColor = InputBorder,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                cursorColor = EmeraldGreen,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
