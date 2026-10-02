package com.example.ruwia.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ruwia.ui.dashboard.NTColors

/**
 * Reactive brand color palette for the employee-facing UI.
 * All tokens are backed by [mutableStateOf] and animated whenever
 * [NTColors.isDarkMode] changes — so every composable that reads
 * these values recomposes automatically.
 *
 * Values are set by [RuwiaTheme]; never mutate them directly.
 */
object RuwiaColor {

    // ── Animated backing fields (written by RuwiaTheme via SideEffect) ────────
    internal var animTealPrimary    by mutableStateOf(Color(0xFF24C7B7))
    internal var animTealDark       by mutableStateOf(Color(0xFF1E9D91))
    internal var animTealLight      by mutableStateOf(Color(0xFFCCF0EC))
    internal var animTealExtraLight by mutableStateOf(Color(0xFFDDF5F1))
    internal var animMint           by mutableStateOf(Color(0xFFF3F7F6))
    internal var animMintLight      by mutableStateOf(Color(0xFFE8F5F3))
    internal var animBackground     by mutableStateOf(Color(0xFFF5FAF9))
    internal var animSurface        by mutableStateOf(Color(0xFFFFFFFF))
    internal var animNavBackground  by mutableStateOf(Color(0xFFFFFFFF))
    internal var animTextPrimary    by mutableStateOf(Color(0xFF172B2D))
    internal var animTextSecondary  by mutableStateOf(Color(0xFF607176))
    internal var animTextMuted      by mutableStateOf(Color(0xFF8BA0A5))
    internal var animDisabled       by mutableStateOf(Color(0xFFB0C0C4))
    internal var animDivider        by mutableStateOf(Color(0xFFD8E4E2))
    internal var animLightGray      by mutableStateOf(Color(0xFFECF3F2))
    internal var animNavActiveChip  by mutableStateOf(Color(0xFFDDF5F1))
    internal var animSummaryBorder  by mutableStateOf(Color(0xFFB2DFDB))

    // ── Public getters ────────────────────────────────────────────────────────

    // Brand — same in both modes
    val TealPrimary: Color       get() = animTealPrimary
    val TealDark: Color          get() = animTealDark

    // Contextual — switch with theme
    val TealLight: Color         get() = animTealLight
    val TealExtraLight: Color    get() = animTealExtraLight
    val Mint: Color              get() = animMint
    val MintLight: Color         get() = animMintLight
    val Background: Color        get() = animBackground
    val Surface: Color           get() = animSurface
    val NavBackground: Color     get() = animNavBackground
    val TextPrimary: Color       get() = animTextPrimary
    val TextSecondary: Color     get() = animTextSecondary
    val TextMuted: Color         get() = animTextMuted
    val Disabled: Color          get() = animDisabled
    val Divider: Color           get() = animDivider
    val LightGray: Color         get() = animLightGray
    val NavActiveChip: Color     get() = animNavActiveChip
    val SummaryCardBorder: Color get() = animSummaryBorder

    // Design-compat aliases
    val Orange: Color            get() = animTealPrimary
    val OrangeLight: Color       get() = animMint
    val OrangeSurface: Color     get() = animMint
    val IconTealBg: Color        get() = animTealLight
}

/**
 * Composable wrapper that animates [RuwiaColor] tokens between
 * dark and light palettes whenever [NTColors.isDarkMode] changes.
 *
 * Wrap all employee-facing screens inside:
 * ```
 * NTTheme { RuwiaTheme { EmployeeDashboardScreen(...) } }
 * ```
 */
@Composable
fun RuwiaTheme(
    content: @Composable () -> Unit,
) {
    val dark   = NTColors.isDarkMode
    val easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
    val spec   = tween<Color>(durationMillis = 450, easing = easing)

    @Composable
    fun ac(target: Color): Color = animateColorAsState(target, spec).value

    // Brand (same in both modes)
    val tealPrimary    = ac(Color(0xFF24C7B7))
    val tealDark       = ac(Color(0xFF1E9D91))

    // Contextual
    val tealLight      = ac(if (dark) Color(0xFF123A39) else Color(0xFFCCF0EC))
    val tealExtraLight = ac(if (dark) Color(0xFF123A39) else Color(0xFFDDF5F1))
    val mint           = ac(if (dark) Color(0xFF111C1F) else Color(0xFFF3F7F6))
    val mintLight      = ac(if (dark) Color(0xFF16262A) else Color(0xFFE8F5F3))
    val background     = ac(if (dark) Color(0xFF0B1214) else Color(0xFFF5FAF9))
    val surface        = ac(if (dark) Color(0xFF111C1F) else Color(0xFFFFFFFF))
    val navBackground  = ac(if (dark) Color(0xFF0F2B29) else Color(0xFFFFFFFF))
    val textPrimary    = ac(if (dark) Color(0xFFF5F7F8) else Color(0xFF172B2D))
    val textSecondary  = ac(if (dark) Color(0xFFA7B5B9) else Color(0xFF607176))
    val textMuted      = ac(if (dark) Color(0xFF7F959A) else Color(0xFF8BA0A5))
    val disabled       = ac(if (dark) Color(0xFF5C6B70) else Color(0xFFB0C0C4))
    val divider        = ac(if (dark) Color(0xFF1D3135) else Color(0xFFD8E4E2))
    val lightGray      = ac(if (dark) Color(0xFF16262A) else Color(0xFFECF3F2))
    val navActiveChip  = ac(if (dark) Color(0xFF155B56) else Color(0xFFDDF5F1))
    val summaryBorder  = ac(if (dark) Color(0xFF1F5451) else Color(0xFFB2DFDB))

    SideEffect {
        RuwiaColor.animTealPrimary    = tealPrimary
        RuwiaColor.animTealDark       = tealDark
        RuwiaColor.animTealLight      = tealLight
        RuwiaColor.animTealExtraLight = tealExtraLight
        RuwiaColor.animMint           = mint
        RuwiaColor.animMintLight      = mintLight
        RuwiaColor.animBackground     = background
        RuwiaColor.animSurface        = surface
        RuwiaColor.animNavBackground  = navBackground
        RuwiaColor.animTextPrimary    = textPrimary
        RuwiaColor.animTextSecondary  = textSecondary
        RuwiaColor.animTextMuted      = textMuted
        RuwiaColor.animDisabled       = disabled
        RuwiaColor.animDivider        = divider
        RuwiaColor.animLightGray      = lightGray
        RuwiaColor.animNavActiveChip  = navActiveChip
        RuwiaColor.animSummaryBorder  = summaryBorder
    }

    // Soft scale-spring on mode switch (mirrors NTTheme)
    val scale = remember { Animatable(1f) }
    LaunchedEffect(dark) {
        scale.animateTo(0.98f, tween(120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Box(modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
        content()
    }
}
