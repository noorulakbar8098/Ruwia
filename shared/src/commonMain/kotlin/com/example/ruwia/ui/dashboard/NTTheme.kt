package com.example.ruwia.ui.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
//  Neer Thuli — Design Token System
//  8pt grid · 24dp corner radius · Teal primary · Orange accent
// ─────────────────────────────────────────────────────────────

object NTColors {
    // Primary – Teal
    val Primary      = Color(0xFF0D9488)
    val PrimaryLight = Color(0xFFCCFBF1)
    val PrimaryMid   = Color(0xFF14B8A6)
    val PrimaryDark  = Color(0xFF0F766E)
    val PrimaryDeep  = Color(0xFF115E59)

    // Accent – Orange (FAB, highlights)
    val Accent       = Color(0xFFF97316)
    val AccentLight  = Color(0xFFFED7AA)
    val AccentDark   = Color(0xFFEA580C)

    // Surfaces
    val Background   = Color(0xFFF8FAFC)
    val Surface      = Color(0xFFFFFFFF)
    val SurfaceVar   = Color(0xFFF1F5F9)

    // Text
    val TextPrimary   = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextTertiary  = Color(0xFF94A3B8)
    val TextDisabled  = Color(0xFFCBD5E1)
    val TextOnPrimary = Color(0xFFFFFFFF)

    // Semantic – Success
    val Success      = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessText  = Color(0xFF065F46)

    // Semantic – Warning
    val Warning      = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFEF3C7)
    val WarningText  = Color(0xFF92400E)

    // Semantic – Error
    val Error        = Color(0xFFEF4444)
    val ErrorLight   = Color(0xFFFEE2E2)
    val ErrorText    = Color(0xFF991B1B)

    // Semantic – Info
    val Info         = Color(0xFF3B82F6)
    val InfoLight    = Color(0xFFDBEAFE)
    val InfoText     = Color(0xFF1E40AF)

    // Borders
    val Border       = Color(0xFFE2E8F0)
    val Divider      = Color(0xFFF1F5F9)

    // Shimmer
    val ShimmerBase  = Color(0xFFE2E8F0)
    val ShimmerHigh  = Color(0xFFF8FAFC)

    // Avatars
    val AvatarGold   = Color(0xFFD4844C)
    val AvatarTeal   = Color(0xFF0D9488)
    val AvatarPurple = Color(0xFF8B5CF6)
    val AvatarBlue   = Color(0xFF3B82F6)

    // Revenue hero card gradient
    val GradStart    = Color(0xFF0F766E)
    val GradEnd      = Color(0xFF115E59)
    val GradAccent   = Color(0x3014B8A6)

    // Chart
    val ChartLine    = Color(0xFF0D9488)
    val ChartFillTop = Color(0x600D9488)
    val ChartFillBot = Color(0x000D9488)
    val ChartGrid    = Color(0xFFF1F5F9)
    val ChartLabel   = Color(0xFF94A3B8)

    // Activity icon bg/fg pairs
    val SaleIconBg    = Color(0xFFCCFBF1); val SaleIconFg    = Color(0xFF0D9488)
    val StockIconBg   = Color(0xFFEDE9FE); val StockIconFg   = Color(0xFF8B5CF6)
    val OverdueIconBg = Color(0xFFFEE2E2); val OverdueIconFg = Color(0xFFEF4444)
    val PayIconBg     = Color(0xFFD1FAE5); val PayIconFg     = Color(0xFF10B981)
    val DelivIconBg   = Color(0xFFDBEAFE); val DelivIconFg   = Color(0xFF3B82F6)
    val CancelIconBg  = Color(0xFFF1F5F9); val CancelIconFg  = Color(0xFF64748B)
    val PendIconBg    = Color(0xFFFEF3C7); val PendIconFg    = Color(0xFFF59E0B)
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
