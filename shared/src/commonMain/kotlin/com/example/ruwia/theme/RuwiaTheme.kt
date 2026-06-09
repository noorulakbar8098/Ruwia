package com.example.ruwia.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand color tokens for the employee-facing UI (light theme).
 * Extracted from the approved design mockups.
 */
object RuwiaColor {
    // ── Primary teal ──────────────────────────────────────────
    val TealPrimary     = Color(0xFF2D7D73)   // activity card, step badges, FAB
    val TealDark        = Color(0xFF1E4F58)   // "Add outward" button
    val TealLight       = Color(0xFFB5DAD5)   // borders, selected-state bg
    val TealExtraLight  = Color(0xFFD4EDE8)   // section tints, chip bg

    // ── Orange accent ─────────────────────────────────────────
    val Orange          = Color(0xFFC87B4A)   // "Add inward" button, INWARD values
    val OrangeLight     = Color(0xFFF2CCBA)   // entry icon bg
    val OrangeSurface   = Color(0xFFFBF0E8)   // purchase-price field bg

    // ── Backgrounds & surfaces ────────────────────────────────
    val Background      = Color(0xFFEDF5F1)   // overall page bg
    val Surface         = Color(0xFFFFFFFF)   // card white
    val NavBackground   = Color(0xFFFFFFFF)

    // ── Text ──────────────────────────────────────────────────
    val TextPrimary     = Color(0xFF1B282A)
    val TextSecondary   = Color(0xFF6B7D78)
    val TextMuted       = Color(0xFF9BADA8)

    // ── Chrome ────────────────────────────────────────────────
    val Divider         = Color(0xFFDCEAE5)
    val IconTealBg      = Color(0xFFC2DDD9)   // SOLD entry icon bg
    val NavActiveChip   = Color(0xFFCDE8E3)   // active bottom-nav pill
}
