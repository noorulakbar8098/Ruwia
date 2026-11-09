package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .verticalScroll(rememberScrollState())
    ) {
        // Header row — mirrors NTDashboardHeader: logo + titles + bell + avatar
        Row(
            modifier = Modifier.fillMaxWidth()
                .statusBarsPadding()
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
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F2E2C))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NTShimmerBox(width = 36.dp, height = 36.dp, shape = CircleShape)
                Spacer(modifier = Modifier.width(10.dp))
                NTShimmerBox(width = 132.dp, height = 12.dp)
                Spacer(modifier = Modifier.weight(1f))
                NTShimmerBox(width = 86.dp, height = 40.dp, shape = RoundedCornerShape(10.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) {
                    Column(modifier = Modifier.weight(1f)) {
                        NTShimmerBox(width = 88.dp, height = 10.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        NTShimmerBox(width = 110.dp, height = 28.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        NTShimmerBox(width = 72.dp, height = 10.dp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        // Delivery routes card
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = NTDp.screenPad)
                .clip(RoundedCornerShape(22.dp))
                .background(NTColors.Surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NTShimmerBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(14.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    NTShimmerBox(width = 124.dp, height = 17.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    NTShimmerBox(width = 184.dp, height = 12.dp)
                }
                NTShimmerBox(width = 20.dp, height = 20.dp, shape = CircleShape)
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(NTColors.Border)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    NTShimmerBox(width = 88.dp, height = 10.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    NTShimmerBox(width = 150.dp, height = 14.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    NTShimmerBox(width = 190.dp, height = 12.dp)
                }
                NTShimmerBox(width = 36.dp, height = 32.dp)
            }
        }
        Spacer(modifier = Modifier.height(NTDp.md))
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
fun NTStockDashboardSkeleton(contentPadding: PaddingValues = PaddingValues()) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(NTColors.Background)
            .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(NTColors.Surface)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NTShimmerBox(width = 38.dp, height = 38.dp, shape = RoundedCornerShape(10.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    NTShimmerBox(width = 146.dp, height = 20.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    NTShimmerBox(width = 202.dp, height = 12.dp)
                }
                NTShimmerBox(width = 38.dp, height = 38.dp, shape = CircleShape)
            }
        }

        Column(
            modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = NTDp.screenPad)
                    .clip(RoundedCornerShape(NTDp.radXxl))
                    .background(NTColors.Surface)
                    .padding(NTDp.cardPad),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NTShimmerBox(width = 34.dp, height = 34.dp, shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    NTShimmerBox(width = 142.dp, height = 10.dp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                NTShimmerBox(width = 152.dp, height = 42.dp)
                Spacer(modifier = Modifier.height(8.dp))
                NTShimmerBox(width = 108.dp, height = 12.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier.weight(1f).height(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ntShimmerBrush())
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ntShimmerBrush())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(NTDp.xl))
            Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                NTShimmerBox(width = 138.dp, height = 11.dp)
                Spacer(modifier = Modifier.height(NTDp.md))
            }
            repeat(2) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad)
                        .clip(RoundedCornerShape(NTDp.radXxl))
                        .background(NTColors.Surface)
                        .padding(NTDp.cardPad),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NTShimmerBox(width = 50.dp, height = 50.dp, shape = CircleShape)
                        Spacer(modifier = Modifier.width(NTDp.md))
                        Column(modifier = Modifier.weight(1f)) {
                            NTShimmerBox(width = 116.dp, height = 15.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            NTShimmerBox(width = 84.dp, height = 12.dp)
                        }
                        NTShimmerBox(width = 44.dp, height = 20.dp, shape = RoundedCornerShape(12.dp))
                    }
                    Spacer(modifier = Modifier.height(NTDp.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier.weight(1f).height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ntShimmerBrush())
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(NTDp.md))
            }
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
