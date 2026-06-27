package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.new_app_logo

// ── Premium SaaS Color Palette ─────────────────────────────────────────────
private object DroplyColor {
    val Background   = Color(0xFF08141F)
    val Surface      = Color(0xFF132437)
    val Card         = Color(0xD1162538) // Glassmorphism alpha
    val Input        = Color(0xFF162B40)
    val Border       = Color(0x14FFFFFF) // 0.08 alpha
    val InputBorder  = Color(0xFF29455F)
    val Primary      = Color(0xFF00C9A7)
    val Secondary    = Color(0xFF14D8B3)
    val Accent       = Color(0xFF0EA5E9)
    val TextPrimary  = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8)
    val Placeholder  = Color(0xFF64748B)
    val Error        = Color(0xFFEF4444)
    
    val PrimaryGradient = Brush.linearGradient(listOf(Primary, Secondary))
}

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onForgotPassword: () -> Unit,
    onLoggedIn: (UserRole?) -> Unit,
) {
    val state by vm.state.collectAsState()

    // Screen states
    var isSignUp by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.admin) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var signupName by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer"
    )

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn(state.role)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DroplyColor.Background)
    ) {
        // ── 1. Layered Premium Background ──────────────────────────────
        PremiumBackgroundEffects(shimmerOffset)

        // ── 2. Scrollable Content ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Spacer(modifier = Modifier.height(40.dp))

            // ── Hero Section (Logo + Brand) ───────────────────────────
            HeroBrandSection()

            Spacer(modifier = Modifier.height(32.dp))

            // ── Welcome Text ──────────────────────────────────────────
            WelcomeSection(isSignUp)

            Spacer(modifier = Modifier.height(32.dp))

            // ── Main Login Card (Glassmorphism) ───────────────────────
            LoginGlassCard(
                isSignUp = isSignUp,
                selectedRole = selectedRole,
                onRoleChange = { selectedRole = it },
                email = email,
                onEmailChange = { email = it },
                password = pass,
                onPasswordChange = { pass = it },
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                signupName = signupName,
                onSignupNameChange = { signupName = it },
                signupPhone = signupPhone,
                onSignupPhoneChange = { signupPhone = it },
                isLoading = state.loading,
                error = state.error,
                onPrimaryAction = {
                    if (isSignUp) {
                        vm.signUp(email.trim(), pass, signupName.trim(), signupPhone.trim())
                    } else {
                        vm.login(email.trim(), pass, selectedRole)
                    }
                },
                onForgotPass = onForgotPassword,
                onToggleMode = { isSignUp = !isSignUp }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Footer ────────────────────────────────────────────────
            LoginFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (state.loading) {
            SaaSLoadingOverlay(message = if (isSignUp) "Creating Account" else "Signing In")
        }
    }
}

@Composable
private fun PremiumBackgroundEffects(shimmerOffset: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Radial glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top Left Teal Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DroplyColor.Primary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.1f),
                    radius = size.width * 0.8f
                ),
                alpha = 0.6f
            )

            // Bottom Right Blue Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DroplyColor.Accent.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.9f),
                    radius = size.width * 0.7f
                ),
                alpha = 0.5f
            )

            // Blurred Floating Circles
            drawCircle(
                color = DroplyColor.Primary.copy(alpha = 0.12f),
                center = Offset(
                    size.width * (0.8f - shimmerOffset * 0.1f),
                    size.height * (0.2f + shimmerOffset * 0.05f)
                ),
                radius = 120.dp.toPx()
            )
        }
        
        // Subtle Noise Overlay (Simulated with alpha grain)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.02f)
                .background(Color.White)
        )
    }
}

@Composable
private fun HeroBrandSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { translationY = floatAnim },
            contentAlignment = Alignment.Center
        ) {
            // Logo Glow
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .blur(20.dp)
                    .background(DroplyColor.Primary.copy(alpha = 0.3f), CircleShape)
            )
            
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "NEER THULI",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "NEER THULI",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DroplyColor.TextPrimary,
                letterSpacing = (-0.5).sp
            )
        )
        
        Text(
            text = "Water Distribution Management",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DroplyColor.TextSecondary,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
private fun WelcomeSection(isSignUp: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isSignUp) "Create Account ✨" else "Welcome back 👋",
            style = TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = DroplyColor.TextPrimary,
                letterSpacing = (-1).sp
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSignUp) "Join the leading network of water distributors and grow your enterprise."
                  else "Manage deliveries, customers, inventory and payments from one unified platform.",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DroplyColor.TextSecondary,
                lineHeight = 24.sp
            )
        )
    }
}

@Composable
private fun LoginGlassCard(
    isSignUp: Boolean,
    selectedRole: UserRole,
    onRoleChange: (UserRole) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    signupName: String,
    onSignupNameChange: (String) -> Unit,
    signupPhone: String,
    onSignupPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onPrimaryAction: () -> Unit,
    onForgotPass: () -> Unit,
    onToggleMode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 40.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = DroplyColor.Card,
        border = BorderStroke(1.dp, DroplyColor.Border)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Role Selector ──────────────────────────────────────
            if (!isSignUp) {
                PremiumSegmentedControl(
                    selectedRole = selectedRole,
                    onRoleChange = onRoleChange
                )
            }

            // ── Inputs ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isSignUp) {
                    DroplyInput(
                        value = signupName,
                        onValueChange = onSignupNameChange,
                        label = "Business Name",
                        placeholder = "Acme Water Corp",
                        icon = Icons.Outlined.Business
                    )
                    DroplyInput(
                        value = signupPhone,
                        onValueChange = onSignupPhoneChange,
                        label = "Phone Number",
                        placeholder = "+1 (555) 000-0000",
                        icon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone
                    )
                }

                DroplyInput(
                    value = email,
                    onValueChange = onEmailChange,
                    label = if (selectedRole == UserRole.employee && !isSignUp) "Employee ID" else "Email Address",
                    placeholder = if (selectedRole == UserRole.employee && !isSignUp) "EMP-12345" else "name@company.com",
                    icon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DroplyInput(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        placeholder = "••••••••",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = onTogglePassword
                    )
                    
                    if (!isSignUp) {
                        Text(
                            text = "Forgot password? →",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = DroplyColor.Primary
                            ),
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable(onClick = onForgotPass)
                        )
                    }
                }
            }

            // ── Primary Action ─────────────────────────────────────
            PrimaryActionButton(
                text = if (isSignUp) "Create Account" else "Sign In to Platform",
                onClick = onPrimaryAction,
                enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                isLoading = isLoading
            )

            // ── Error State ────────────────────────────────────────
            if (error != null) {
                ErrorDisplay(error)
            }

            // ── Divider ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DroplyColor.Border)
                Text(
                    text = "  or continue with  ",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DroplyColor.Placeholder
                    )
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DroplyColor.Border)
            }

            // ── Google Button ──────────────────────────────────────
//            GoogleSSOButton()

            // ── Toggle Mode ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                    style = TextStyle(fontSize = 14.sp, color = DroplyColor.TextSecondary)
                )
                Text(
                    text = if (isSignUp) "Sign In →" else "Sign Up →",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DroplyColor.Primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable(onClick = onToggleMode)
                )
            }
        }
    }
}

@Composable
private fun PremiumSegmentedControl(
    selectedRole: UserRole,
    onRoleChange: (UserRole) -> Unit
) {
    val isOwner = selectedRole == UserRole.admin
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(DroplyColor.Background, RoundedCornerShape(20.dp))
            .border(1.dp, DroplyColor.InputBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val indicatorWidth = maxWidth / 2
        val targetOffset = if (isOwner) 0.dp else indicatorWidth
        
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "indicator"
        )

        // Sliding Indicator
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = DroplyColor.Primary.copy(alpha = 0.4f))
                .background(DroplyColor.PrimaryGradient, RoundedCornerShape(16.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            RoleTab(
                label = "Admin",
                icon = Icons.Rounded.AdminPanelSettings,
                isSelected = isOwner,
                modifier = Modifier.weight(1f),
                onClick = { onRoleChange(UserRole.admin) }
            )
            RoleTab(
                label = "Employee",
                icon = Icons.Rounded.Badge,
                isSelected = !isOwner,
                modifier = Modifier.weight(1f),
                onClick = { onRoleChange(UserRole.employee) }
            )
        }
    }
}

@Composable
private fun RoleTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else DroplyColor.TextSecondary,
        animationSpec = tween(300),
        label = "color"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            )
        }
    }
}

@Composable
private fun DroplyInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var isFocused by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(if (isFocused) 0.15f else 0f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DroplyColor.TextSecondary
            )
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .drawWithContent {
                    if (isFocused) {
                        // Focus Glow Effect
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(DroplyColor.Primary.copy(alpha = glowAlpha), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.width
                            ),
                            alpha = glowAlpha
                        )
                    }
                    drawContent()
                },
            placeholder = {
                Text(
                    text = placeholder,
                    style = TextStyle(fontSize = 16.sp, color = DroplyColor.Placeholder)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) DroplyColor.Primary else DroplyColor.Placeholder,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = DroplyColor.Placeholder,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DroplyColor.Primary,
                unfocusedBorderColor = DroplyColor.InputBorder,
                focusedContainerColor = DroplyColor.Input,
                unfocusedContainerColor = DroplyColor.Input,
                focusedTextColor = DroplyColor.TextPrimary,
                unfocusedTextColor = DroplyColor.TextPrimary,
                cursorColor = DroplyColor.Primary
            ),
            textStyle = TextStyle(fontSize = 16.sp)
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f)

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled && !isLoading) 16.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = DroplyColor.Primary.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.White.copy(alpha = 0.05f)
        ),
        contentPadding = PaddingValues(),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled && !isLoading) DroplyColor.PrimaryGradient 
                    else Brush.linearGradient(listOf(Color.Gray.copy(0.1f), Color.Gray.copy(0.1f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = text,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color.White else DroplyColor.Placeholder
                    )
                )
            }
        }
    }
}

@Composable
private fun GoogleSSOButton() {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)

    OutlinedButton(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.03f)
        ),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google Icon (Simplified Drawing using paths for the 'G')
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        val strokeWidth = 2.5.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        addArc(
                            androidx.compose.ui.geometry.Rect(
                                size.width / 2 - radius,
                                size.height / 2 - radius,
                                size.width / 2 + radius,
                                size.height / 2 + radius
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = -280f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // The cross bar of the 'G'
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Continue with Google",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DroplyColor.TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = DroplyColor.Placeholder,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ErrorDisplay(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DroplyColor.Error.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .border(1.dp, DroplyColor.Error.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = DroplyColor.Error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DroplyColor.Error
            )
        )
    }
}

@Composable
private fun LoginFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.alpha(0.6f)
        ) {
            Icon(Icons.Rounded.Https, null, tint = DroplyColor.TextSecondary, modifier = Modifier.size(14.dp))
            Text(
                text = "Enterprise-grade security",
                style = TextStyle(fontSize = 12.sp, color = DroplyColor.TextSecondary, fontWeight = FontWeight.Medium)
            )
        }
        
        Text(
            text = "Powered by Supabase - safe secure Authentication",
            style = TextStyle(fontSize = 11.sp, color = DroplyColor.Placeholder, letterSpacing = 0.5.sp)
        )
//
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(16.dp),
//            modifier = Modifier.alpha(0.5f)
//        ) {
//            Text("Privacy Policy", style = TextStyle(fontSize = 11.sp, color = DroplyColor.Placeholder))
//            Text("•", style = TextStyle(fontSize = 11.sp, color = DroplyColor.Placeholder))
//            Text("Terms of Service", style = TextStyle(fontSize = 11.sp, color = DroplyColor.Placeholder))
//        }
    }
}

