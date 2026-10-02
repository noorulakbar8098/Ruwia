package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_icon

// Reusing the same SaaS palette from LoginScreen
private val DarkSurface = Color(0xFF0D1B2A)
private val CardDark = Color(0xFF152238)
private val CardBorder = Color(0xFF1E3050)
private val DeepTeal = Color(0xFF006D77)
private val EmeraldGreen = Color(0xFF2A9D8F)
private val MidTeal = Color(0xFF1E8A6E)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF8899AA)
private val TextMuted = Color(0xFF5C6E7F)
private val InputBackground = Color(0xFF1A2D42)
private val InputBorder = Color(0xFF253D56)
private val InputFocusBorder = Color(0xFF2A9D8F)
private val SuccessGreen = Color(0xFF51CF66)

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

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
        // Ambient Orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DeepTeal.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.15f),
                    radius = size.width * 0.4f
                ),
                center = Offset(size.width * 0.8f, size.height * 0.15f),
                radius = size.width * 0.4f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGreen.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.8f),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.8f),
                radius = size.width * 0.5f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Row with Material Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back",
                    color = EmeraldGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "NEER THULI",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Reset Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter your email address and we'll send you instructions to reset your password.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphic Card
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
                    if (!submitted) {
                        // Email Input with Material Icon and Placeholder
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Email Address",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = "you@company.com",
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Email,
                                        contentDescription = "Email",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
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

                        // Submit button
                        Button(
                            onClick = { submitted = true },
                            enabled = email.contains("@"),
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
                                        brush = if (email.contains("@"))
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
                                Text(
                                    text = "Send Recovery Email",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        // Success State with Material Icon
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    SuccessGreen.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Success",
                                tint = SuccessGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Recovery Email Sent",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )

                        Text(
                            "Check your inbox at $email for password reset instructions.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
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
                                        Brush.linearGradient(
                                            colors = listOf(DeepTeal, EmeraldGreen)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Return to Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
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
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
