package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NTShimmerBox(width = 48.dp, height = 48.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                NTShimmerBox(width = 100.dp, height = 10.dp)
                Spacer(modifier = Modifier.height(6.dp))
                NTShimmerBox(width = 160.dp, height = 18.dp)
            }
        }
        // Greeting
        Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
            NTShimmerBox(width = 220.dp, height = 26.dp)
            Spacer(modifier = Modifier.height(8.dp))
            NTShimmerBox(width = 280.dp, height = 14.dp)
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        // Revenue card
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad).height(180.dp)
                .clip(RoundedCornerShape(NTDp.radXxl)).background(ntShimmerBrush())
        )
        Spacer(modifier = Modifier.height(NTDp.lg))
        // KPI grid
        Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
            NTShimmerBox(width = 80.dp, height = 10.dp)
            Spacer(modifier = Modifier.height(6.dp))
            NTShimmerBox(width = 140.dp, height = 20.dp)
            Spacer(modifier = Modifier.height(NTDp.md))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
                repeat(2) { NTKpiSkeleton(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(NTDp.md))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
                repeat(2) { NTKpiSkeleton(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NTKpiSkeleton(modifier: Modifier = Modifier) {
    val brush = ntShimmerBrush()
    Box(
        modifier = modifier.height(130.dp).clip(RoundedCornerShape(NTDp.radXxl))
            .background(NTColors.Surface)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Box(modifier = Modifier.size(NTDp.kpiIconBox)
                .clip(RoundedCornerShape(NTDp.radMd)).background(brush))
            Spacer(modifier = Modifier.height(NTDp.md))
            NTShimmerBox(width = 80.dp,  height = 10.dp, brush = brush)
            Spacer(modifier = Modifier.height(NTDp.xs))
            NTShimmerBox(width = 60.dp,  height = 22.dp, brush = brush)
            Spacer(modifier = Modifier.height(NTDp.xs))
            NTShimmerBox(width = 100.dp, height = 10.dp, brush = brush)
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
        Text(subtitle, color = NTColors.TextSecondary, fontSize = 14.sp,
            textAlign = TextAlign.Center)
        if (ctaLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NTDp.lg))
            Button(
                onClick = onCtaClick,
                colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                shape = RoundedCornerShape(NTDp.radFull)
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
        Text(message, color = NTColors.TextSecondary, fontSize = 13.sp,
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(NTDp.lg))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
            shape = RoundedCornerShape(NTDp.radFull)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null,
                modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(NTDp.sm))
            Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
