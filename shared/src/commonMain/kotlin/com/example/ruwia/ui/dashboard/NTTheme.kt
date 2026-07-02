package com.example.ruwia.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
//  Neer Thuli — Design Token System
//  8pt grid · 24dp corner radius · Dynamic animated theme transition
// ─────────────────────────────────────────────────────────────

object NTColors {
    var isDarkMode by mutableStateOf(true)

    // Backing states for dynamic animated colors (initially Dark Mode defaults)
    internal var animPrimary by mutableStateOf(Color(0xFF24C7B7))
    internal var animPrimaryLight by mutableStateOf(Color(0xFF123A39))
    internal var animPrimaryMid by mutableStateOf(Color(0xFF24C7B7))
    internal var animPrimaryDark by mutableStateOf(Color(0xFF1E9D91))
    internal var animPrimaryDeep by mutableStateOf(Color(0xFF123A39))

    internal var animAccent by mutableStateOf(Color(0xFFF97316))
    internal var animAccentLight by mutableStateOf(Color(0xFF382312))
    internal var animAccentDark by mutableStateOf(Color(0xFFEA580C))

    internal var animBackground by mutableStateOf(Color(0xFF0B1214))
    internal var animSurface by mutableStateOf(Color(0xFF111C1F))
    internal var animSurfaceVar by mutableStateOf(Color(0xFF16262A))

    internal var animTextPrimary by mutableStateOf(Color(0xFFF5F7F8))
    internal var animTextSecondary by mutableStateOf(Color(0xFFB4C2C6))
    internal var animTextTertiary by mutableStateOf(Color(0xFF7F959A))
    internal var animTextDisabled by mutableStateOf(Color(0xFF5C6B70))
    internal var animTextOnPrimary by mutableStateOf(Color(0xFFFFFFFF))

    internal var animSuccess by mutableStateOf(Color(0xFF10B981))
    internal var animSuccessLight by mutableStateOf(Color(0xFF0A3625))
    internal var animSuccessText by mutableStateOf(Color(0xFF34D399))

    internal var animWarning by mutableStateOf(Color(0xFFF59E0B))
    internal var animWarningLight by mutableStateOf(Color(0xFF332005))
    internal var animWarningText by mutableStateOf(Color(0xFFFBBF24))

    internal var animError by mutableStateOf(Color(0xFFEF4444))
    internal var animErrorLight by mutableStateOf(Color(0xFF3B1616))
    internal var animErrorText by mutableStateOf(Color(0xFFFCA5A5))

    internal var animInfo by mutableStateOf(Color(0xFF3B82F6))
    internal var animInfoLight by mutableStateOf(Color(0xFF0F2547))
    internal var animInfoText by mutableStateOf(Color(0xFF93C5FD))

    internal var animBorder by mutableStateOf(Color(0xFF1D3135))
    internal var animDivider by mutableStateOf(Color(0xFF1D3135))

    internal var animShimmerBase by mutableStateOf(Color(0xFF111C1F))
    internal var animShimmerHigh by mutableStateOf(Color(0xFF16262A))

    internal var animAvatarGold by mutableStateOf(Color(0xFFD4844C))
    internal var animAvatarTeal by mutableStateOf(Color(0xFF24C7B7))
    internal var animAvatarPurple by mutableStateOf(Color(0xFF8B5CF6))
    internal var animAvatarBlue by mutableStateOf(Color(0xFF3B82F6))

    internal var animGradStart by mutableStateOf(Color(0xFF111C1F))
    internal var animGradEnd by mutableStateOf(Color(0xFF0B1214))
    internal var animGradAccent by mutableStateOf(Color(0x1A24C7B7))

    internal var animChartLine by mutableStateOf(Color(0xFF24C7B7))
    internal var animChartFillTop by mutableStateOf(Color(0x3324C7B7))
    internal var animChartFillBot by mutableStateOf(Color(0x0024C7B7))
    internal var animChartGrid by mutableStateOf(Color(0xFF1D3135))
    internal var animChartLabel by mutableStateOf(Color(0xFF7F959A))

    internal var animSaleIconBg by mutableStateOf(Color(0xFF123A39))
    internal var animSaleIconFg by mutableStateOf(Color(0xFF24C7B7))
    internal var animStockIconBg by mutableStateOf(Color(0xFF1E153A))
    internal var animStockIconFg by mutableStateOf(Color(0xFF8B5CF6))
    internal var animOverdueIconBg by mutableStateOf(Color(0xFF3B1616))
    internal var animOverdueIconFg by mutableStateOf(Color(0xFFEF4444))
    internal var animPayIconBg by mutableStateOf(Color(0xFF0A3625))
    internal var animPayIconFg by mutableStateOf(Color(0xFF10B981))
    internal var animDelivIconBg by mutableStateOf(Color(0xFF0F2547))
    internal var animDelivIconFg by mutableStateOf(Color(0xFF3B82F6))
    internal var animCancelIconBg by mutableStateOf(Color(0xFF16262A))
    internal var animCancelIconFg by mutableStateOf(Color(0xFFB4C2C6))
    internal var animPendIconBg by mutableStateOf(Color(0xFF332005))
    internal var animPendIconFg by mutableStateOf(Color(0xFFF59E0B))

    // Public getters (returning animated colors dynamically)
    val Primary: Color get() = animPrimary
    val PrimaryLight: Color get() = animPrimaryLight
    val PrimaryMid: Color get() = animPrimaryMid
    val PrimaryDark: Color get() = animPrimaryDark
    val PrimaryDeep: Color get() = animPrimaryDeep

    val Accent: Color get() = animAccent
    val AccentLight: Color get() = animAccentLight
    val AccentDark: Color get() = animAccentDark

    val Background: Color get() = animBackground
    val Surface: Color get() = animSurface
    val SurfaceVar: Color get() = animSurfaceVar

    val TextPrimary: Color get() = animTextPrimary
    val TextSecondary: Color get() = animTextSecondary
    val TextTertiary: Color get() = animTextTertiary
    val TextDisabled: Color get() = animTextDisabled
    val TextOnPrimary: Color get() = animTextOnPrimary

    val Success: Color get() = animSuccess
    val SuccessLight: Color get() = animSuccessLight
    val SuccessText: Color get() = animSuccessText

    val Warning: Color get() = animWarning
    val WarningLight: Color get() = animWarningLight
    val WarningText: Color get() = animWarningText

    val Error: Color get() = animError
    val ErrorLight: Color get() = animErrorLight
    val ErrorText: Color get() = animErrorText

    val Info: Color get() = animInfo
    val InfoLight: Color get() = animInfoLight
    val InfoText: Color get() = animInfoText

    val Border: Color get() = animBorder
    val Divider: Color get() = animDivider

    val ShimmerBase: Color get() = animShimmerBase
    val ShimmerHigh: Color get() = animShimmerHigh

    val AvatarGold: Color get() = animAvatarGold
    val AvatarTeal: Color get() = animAvatarTeal
    val AvatarPurple: Color get() = animAvatarPurple
    val AvatarBlue: Color get() = animAvatarBlue

    val GradStart: Color get() = animGradStart
    val GradEnd: Color get() = animGradEnd
    val GradAccent: Color get() = animGradAccent

    val ChartLine: Color get() = animChartLine
    val ChartFillTop: Color get() = animChartFillTop
    val ChartFillBot: Color get() = animChartFillBot
    val ChartGrid: Color get() = animChartGrid
    val ChartLabel: Color get() = animChartLabel

    val SaleIconBg: Color get() = animSaleIconBg
    val SaleIconFg: Color get() = animSaleIconFg
    val StockIconBg: Color get() = animStockIconBg
    val StockIconFg: Color get() = animStockIconFg
    val OverdueIconBg: Color get() = animOverdueIconBg
    val OverdueIconFg: Color get() = animOverdueIconFg
    val PayIconBg: Color get() = animPayIconBg
    val PayIconFg: Color get() = animPayIconFg
    val DelivIconBg: Color get() = animDelivIconBg
    val DelivIconFg: Color get() = animDelivIconFg
    val CancelIconBg: Color get() = animCancelIconBg
    val CancelIconFg: Color get() = animCancelIconFg
    val PendIconBg: Color get() = animPendIconBg
    val PendIconFg: Color get() = animPendIconFg
}

object NTDp {
    // 8pt spacing grid
    val xs        = 4.dp
    val sm        = 8.dp
    val md        = 16.dp
    val lg        = 24.dp
    val xl        = 32.dp
    val screenPad = 20.dp
    val cardPad   = 20.dp

    // Corner radii
    val radSm   = 8.dp
    val radMd   = 12.dp
    val radLg   = 16.dp
    val radXl   = 20.dp
    val radXxl  = 24.dp
    val radFull = 100.dp

    // Sizes
    val avatarMd    = 40.dp
    val avatarLg    = 48.dp
    val iconSm      = 16.dp
    val iconMd      = 20.dp
    val iconLg      = 24.dp
    val iconXl      = 28.dp
    val fabSz       = 56.dp
    val bottomNavH  = 80.dp
    val kpiIconBox  = 44.dp
    val qaIconBox   = 56.dp
    val chartH      = 130.dp
}

@Composable
fun NTTheme(
    content: @Composable () -> Unit
) {
    val dark = NTColors.isDarkMode
    val PremiumEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)

    @Composable
    fun animateColor(target: Color): Color {
        return animateColorAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 450, easing = PremiumEasing)
        ).value
    }

    val primary = animateColor(if (dark) Color(0xFF24C7B7) else Color(0xFF0F9D8A))
    val primaryLight = animateColor(if (dark) Color(0xFF123A39) else Color(0xFFDDF5F1))
    val primaryMid = animateColor(if (dark) Color(0xFF24C7B7) else Color(0xFF0F9D8A))
    val primaryDark = animateColor(if (dark) Color(0xFF1E9D91) else Color(0xFF0D8A7A))
    val primaryDeep = animateColor(if (dark) Color(0xFF123A39) else Color(0xFF0A6F62))

    val accent = animateColor(if (dark) Color(0xFFF97316) else Color(0xFFF97316))
    val accentLight = animateColor(if (dark) Color(0xFF382312) else Color(0xFFFED7AA))
    val accentDark = animateColor(if (dark) Color(0xFFEA580C) else Color(0xFFEA580C))

    val background = animateColor(if (dark) Color(0xFF0B1214) else Color(0xFFF7FAF8))
    val surface = animateColor(if (dark) Color(0xFF111C1F) else Color(0xFFFFFFFF))
    val surfaceVar = animateColor(if (dark) Color(0xFF16262A) else Color(0xFFF3F7F6))

    val textPrimary = animateColor(if (dark) Color(0xFFF5F7F8) else Color(0xFF172B2D))
    val textSecondary = animateColor(if (dark) Color(0xFFB4C2C6) else Color(0xFF607176))
    val textTertiary = animateColor(if (dark) Color(0xFF7F959A) else Color(0xFF8BA0A5))
    val textDisabled = animateColor(if (dark) Color(0xFF5C6B70) else Color(0xFFB0C0C4))
    val textOnPrimary = animateColor(Color(0xFFFFFFFF))

    val success = animateColor(Color(0xFF10B981))
    val successLight = animateColor(if (dark) Color(0xFF0A3625) else Color(0xFFD1FAE5))
    val successText = animateColor(if (dark) Color(0xFF34D399) else Color(0xFF065F46))

    val warning = animateColor(Color(0xFFF59E0B))
    val warningLight = animateColor(if (dark) Color(0xFF332005) else Color(0xFFFEF3C7))
    val warningText = animateColor(if (dark) Color(0xFFFBBF24) else Color(0xFF92400E))

    val error = animateColor(Color(0xFFEF4444))
    val errorLight = animateColor(if (dark) Color(0xFF3B1616) else Color(0xFFFEE2E2))
    val errorText = animateColor(if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B))

    val info = animateColor(Color(0xFF3B82F6))
    val infoLight = animateColor(if (dark) Color(0xFF0F2547) else Color(0xFFDBEAFE))
    val infoText = animateColor(if (dark) Color(0xFF93C5FD) else Color(0xFF1E40AF))

    val border = animateColor(if (dark) Color(0xFF1D3135) else Color(0xFFD8E4E2))
    val divider = animateColor(if (dark) Color(0xFF1D3135) else Color(0xFFD8E4E2))

    val shimmerBase = animateColor(if (dark) Color(0xFF111C1F) else Color(0xFFE2E8F0))
    val shimmerHigh = animateColor(if (dark) Color(0xFF16262A) else Color(0xFFF8FAFC))

    val avatarGold = animateColor(Color(0xFFD4844C))
    val avatarTeal = animateColor(if (dark) Color(0xFF24C7B7) else Color(0xFF0F9D8A))
    val avatarPurple = animateColor(Color(0xFF8B5CF6))
    val avatarBlue = animateColor(Color(0xFF3B82F6))

    val gradStart = animateColor(if (dark) Color(0xFF111C1F) else Color(0xFF0F9D8A))
    val gradEnd = animateColor(if (dark) Color(0xFF0B1214) else Color(0xFF0A6F62))
    val gradAccent = animateColor(if (dark) Color(0x1A24C7B7) else Color(0x300F9D8A))

    val chartLine = animateColor(if (dark) Color(0xFF24C7B7) else Color(0xFF0F9D8A))
    val chartFillTop = animateColor(if (dark) Color(0x3324C7B7) else Color(0x600F9D8A))
    val chartFillBot = animateColor(if (dark) Color(0x0024C7B7) else Color(0x000F9D8A))
    val chartGrid = animateColor(if (dark) Color(0xFF1D3135) else Color(0xFFD8E4E2))
    val chartLabel = animateColor(if (dark) Color(0xFF7F959A) else Color(0xFF607176))

    val saleIconBg = animateColor(if (dark) Color(0xFF123A39) else Color(0xFFDDF5F1))
    val saleIconFg = animateColor(if (dark) Color(0xFF24C7B7) else Color(0xFF0F9D8A))
    val stockIconBg = animateColor(if (dark) Color(0xFF1E153A) else Color(0xFFEDE9FE))
    val stockIconFg = animateColor(if (dark) Color(0xFF8B5CF6) else Color(0xFF8B5CF6))
    val overdueIconBg = animateColor(if (dark) Color(0xFF3B1616) else Color(0xFFFEE2E2))
    val overdueIconFg = animateColor(if (dark) Color(0xFFEF4444) else Color(0xFFEF4444))
    val payIconBg = animateColor(if (dark) Color(0xFF0A3625) else Color(0xFFD1FAE5))
    val payIconFg = animateColor(if (dark) Color(0xFF10B981) else Color(0xFF10B981))
    val delivIconBg = animateColor(if (dark) Color(0xFF0F2547) else Color(0xFFDBEAFE))
    val delivIconFg = animateColor(if (dark) Color(0xFF3B82F6) else Color(0xFF3B82F6))
    val cancelIconBg = animateColor(if (dark) Color(0xFF16262A) else Color(0xFFF3F7F6))
    val cancelIconFg = animateColor(if (dark) Color(0xFFB4C2C6) else Color(0xFF607176))
    val pendIconBg = animateColor(if (dark) Color(0xFF332005) else Color(0xFFFEF3C7))
    val pendIconFg = animateColor(if (dark) Color(0xFFF59E0B) else Color(0xFFF59E0B))

    SideEffect {
        NTColors.animPrimary = primary
        NTColors.animPrimaryLight = primaryLight
        NTColors.animPrimaryMid = primaryMid
        NTColors.animPrimaryDark = primaryDark
        NTColors.animPrimaryDeep = primaryDeep
        NTColors.animAccent = accent
        NTColors.animAccentLight = accentLight
        NTColors.animAccentDark = accentDark
        NTColors.animBackground = background
        NTColors.animSurface = surface
        NTColors.animSurfaceVar = surfaceVar
        NTColors.animTextPrimary = textPrimary
        NTColors.animTextSecondary = textSecondary
        NTColors.animTextTertiary = textTertiary
        NTColors.animTextDisabled = textDisabled
        NTColors.animTextOnPrimary = textOnPrimary
        NTColors.animSuccess = success
        NTColors.animSuccessLight = successLight
        NTColors.animSuccessText = successText
        NTColors.animWarning = warning
        NTColors.animWarningLight = warningLight
        NTColors.animWarningText = warningText
        NTColors.animError = error
        NTColors.animErrorLight = errorLight
        NTColors.animErrorText = errorText
        NTColors.animInfo = info
        NTColors.animInfoLight = infoLight
        NTColors.animInfoText = infoText
        NTColors.animBorder = border
        NTColors.animDivider = divider
        NTColors.animShimmerBase = shimmerBase
        NTColors.animShimmerHigh = shimmerHigh
        NTColors.animAvatarGold = avatarGold
        NTColors.animAvatarTeal = avatarTeal
        NTColors.animAvatarPurple = avatarPurple
        NTColors.animAvatarBlue = avatarBlue
        NTColors.animGradStart = gradStart
        NTColors.animGradEnd = gradEnd
        NTColors.animGradAccent = gradAccent
        NTColors.animChartLine = chartLine
        NTColors.animChartFillTop = chartFillTop
        NTColors.animChartFillBot = chartFillBot
        NTColors.animChartGrid = chartGrid
        NTColors.animChartLabel = chartLabel
        NTColors.animSaleIconBg = saleIconBg
        NTColors.animSaleIconFg = saleIconFg
        NTColors.animStockIconBg = stockIconBg
        NTColors.animStockIconFg = stockIconFg
        NTColors.animOverdueIconBg = overdueIconBg
        NTColors.animOverdueIconFg = overdueIconFg
        NTColors.animPayIconBg = payIconBg
        NTColors.animPayIconFg = payIconFg
        NTColors.animDelivIconBg = delivIconBg
        NTColors.animDelivIconFg = delivIconFg
        NTColors.animCancelIconBg = cancelIconBg
        NTColors.animCancelIconFg = cancelIconFg
        NTColors.animPendIconBg = pendIconBg
        NTColors.animPendIconFg = pendIconFg
    }

    val scale = remember { Animatable(1f) }
    LaunchedEffect(dark) {
        // Soft compression, then bouncy spring expand
        scale.animateTo(
            targetValue = 0.98f,
            animationSpec = tween(120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        content()
    }
}
