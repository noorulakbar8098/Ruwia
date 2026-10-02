package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.example.ruwia.util.sanitizeError
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Skeleton loaders, empty states, error states — no emojis
// ─────────────────────────────────────────────────────────────

// ── Skeleton loaders ──────────────────────────────────────────

@Composable
fun NTDashboardSkeleton(contentPadding: PaddingValues = PaddingValues()) {
    Column(
        modifier = Modifier.fillMaxSize().background(NTColors.Background)
            .padding(contentPadding)
    ) {
        // Header row — mirrors NTDashboardHeader: logo + titles + bell + avatar
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NTShimmerBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                NTShimmerBox(width = 100.dp, height = 10.dp)
                Spacer(modifier = Modifier.height(6.dp))
                NTShimmerBox(width = 150.dp, height = 19.dp)
            }
            NTShimmerBox(width = 44.dp, height = 44.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(10.dp))
            NTShimmerBox(width = 44.dp, height = 44.dp, shape = CircleShape)
        }
        // Greeting
        Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
            Spacer(modifier = Modifier.height(NTDp.sm))
            NTShimmerBox(width = 210.dp, height = 24.dp)
            Spacer(modifier = Modifier.height(8.dp))
            NTShimmerBox(width = 260.dp, height = 14.dp)
        }
        Spacer(modifier = Modifier.height(NTDp.md))
        // Business overview card
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad).height(208.dp)
                .clip(RoundedCornerShape(24.dp)).background(ntShimmerBrush())
        )
        Spacer(modifier = Modifier.height(NTDp.md))
        // Empty-cases row
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad)
                .clip(RoundedCornerShape(24.dp))
                .background(NTColors.Surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NTShimmerBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(14.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                NTShimmerBox(width = 90.dp, height = 10.dp)
                Spacer(modifier = Modifier.height(6.dp))
                NTShimmerBox(width = 130.dp, height = 18.dp)
            }
            NTShimmerBox(width = 84.dp, height = 44.dp, shape = RoundedCornerShape(14.dp))
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        // Analytics section — header + picker + chart card
        Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
            NTShimmerBox(width = 80.dp, height = 10.dp)
            Spacer(modifier = Modifier.height(6.dp))
            NTShimmerBox(width = 150.dp, height = 20.dp)
            Spacer(modifier = Modifier.height(NTDp.md))
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(NTDp.radFull)).background(ntShimmerBrush())
            )
            Spacer(modifier = Modifier.height(NTDp.md))
            Box(
                modifier = Modifier.fillMaxWidth().height(272.dp)
                    .clip(RoundedCornerShape(24.dp)).background(ntShimmerBrush())
            )
        }
    }
}

@Composable
fun NTShimmerBox(
    width: Dp,
    height: Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(NTDp.radSm),
    brush: androidx.compose.ui.graphics.Brush = ntShimmerBrush()
) {
    Box(modifier = Modifier.width(width).height(height).clip(shape).background(brush))
}

// ── Empty state ───────────────────────────────────────────────

@Composable
fun NTEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ctaLabel: String = "",
    onCtaClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(NTDp.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(NTColors.SurfaceVar),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NTColors.TextTertiary,
                modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        Text(title, color = NTColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(NTDp.sm))
        Text(subtitle, color = NTColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
            textAlign = TextAlign.Center)
        if (ctaLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NTDp.lg))
            Button(
                onClick = onCtaClick,
                colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(ctaLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────

@Composable
fun NTErrorState(
    message: String,
    onRetry: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(NTColors.Background)
            .padding(contentPadding).padding(NTDp.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(NTColors.ErrorLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null,
                tint = NTColors.Error, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        Text("Something went wrong", color = NTColors.TextPrimary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(NTDp.sm))
        Text(sanitizeError(message), color = NTColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(NTDp.lg))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null,
                modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(NTDp.sm))
            Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
