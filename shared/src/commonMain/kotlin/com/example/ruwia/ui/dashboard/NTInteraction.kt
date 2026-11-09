package com.example.ruwia.ui.dashboard

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Lean motion tokens shared by every pressable surface.
 *
 * Keep this list short on purpose: two durations and one curve cover all
 * press, selection, and color transitions in the app.
 */
object NTMotion {
    /** Immediate feedback for touch-down states. */
    const val Fast = 120

    /** Standard transition for selection, color, and layout changes. */
    const val Base = 220

    /** Decelerating curve used by the reference design language. */
    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * Interaction tokens: subtle, consistent, and easy to audit.
 *
 * Wide surfaces compress slightly; compact icon controls compress a little
 * more so the touch target still feels responsive under a thumb.
 */
object NTInteraction {
    const val PressScale = 0.97f
    const val PressScaleCompact = 0.90f
    const val PressAlpha = 0.70f
}

/**
 * Adds a spring-based press scale, an optional alpha dip, and a click handler.
 *
 * Apply this before the surface's background/border so the whole surface
 * compresses together. Pass [shape] to clip the ripple to the surface.
 *
 * ```kotlin
 * Box(
 *     Modifier
 *         .size(44.dp)
 *         .ntPressable(
 *             onClick = onClick,
 *             shape = CircleShape,
 *             pressedScale = NTInteraction.PressScaleCompact,
 *         )
 *         .background(NTColors.Surface)
 *         .border(1.dp, NTColors.Border, CircleShape)
 * )
 * ```
 */
fun Modifier.ntPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    pressedScale: Float = NTInteraction.PressScale,
    pressedAlpha: Float = 1f,
    ripple: Boolean = true,
    shape: Shape? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "ntPressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedAlpha else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "ntPressAlpha",
    )

    var pressable = this
    if (shape != null) pressable = pressable.clip(shape)
    pressable
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = if (ripple) LocalIndication.current else null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}
