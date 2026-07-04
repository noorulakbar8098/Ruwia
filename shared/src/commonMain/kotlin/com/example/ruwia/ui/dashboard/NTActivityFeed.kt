package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Order

// ─────────────────────────────────────────────────────────────
//  Activity Feed — real Orders + Customers, Material Icons only
// ─────────────────────────────────────────────────────────────

@Composable
fun NTActivityFeedSection(
    orders: List<Order>,
    customers: List<Customer>,
    onViewAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = NTDp.screenPad)) {
        NTSectionHeader(
            label = "RECENT ACTIVITY",
            title = "Live feed",
            trailingText = "VIEW ALL →",
            onTrailingClick = onViewAll
        )
        Spacer(modifier = Modifier.height(NTDp.md))

        if (orders.isEmpty() && customers.isEmpty()) {
            NTFeedEmptyInline()
            return
        }

        val pending   = orders.filter { it.status == "pending" }
        val delivered = orders.filter { it.status == "delivered" }
        val overdue   = customers.filter { it.balance > 0 }.take(3)

        if (pending.isNotEmpty()) {
            NTFeedGroupHeader("Today")
            pending.take(3).forEach { order ->
                val cust = customers.find { it.id == order.customerId }
                NTOrderActivityCard(
                    customerName = cust?.name ?: "Customer",
                    meta         = "Qty: ${order.qty} cans · Pending",
                    amountLabel  = null,
                    badge        = "PENDING",
                    badgeStatus  = NTBadgeStatus.PENDING,
                    icon         = Icons.Rounded.AccessTime,
                    iconBg       = NTColors.PendIconBg,
                    iconFg       = NTColors.PendIconFg,
                    isPositive   = false
                )
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }

        if (delivered.isNotEmpty()) {
            NTFeedGroupHeader("Delivered")
            delivered.take(2).forEach { order ->
                val cust = customers.find { it.id == order.customerId }
                NTOrderActivityCard(
                    customerName = cust?.name ?: "Customer",
                    meta         = "Qty: ${order.qty} cans · Delivered",
                    amountLabel  = "+₹${order.qty * 120}",
                    badge        = "CASH",
                    badgeStatus  = NTBadgeStatus.CASH,
                    icon         = Icons.Rounded.CallMade,
                    iconBg       = NTColors.SaleIconBg,
                    iconFg       = NTColors.SaleIconFg,
                    isPositive   = true
                )
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }

        if (overdue.isNotEmpty()) {
            NTFeedGroupHeader("Due Payments")
            overdue.forEach { customer ->
                NTOrderActivityCard(
                    customerName = customer.name,
                    meta         = "Balance due · ${customer.cansHeld} cans held",
                    amountLabel  = "₹${customer.balance.toInt()}",
                    badge        = "OVERDUE",
                    badgeStatus  = NTBadgeStatus.OVERDUE,
                    icon         = Icons.Rounded.ErrorOutline,
                    iconBg       = NTColors.OverdueIconBg,
                    iconFg       = NTColors.OverdueIconFg,
                    isPositive   = false
                )
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }
    }
}

@Composable
private fun NTFeedGroupHeader(label: String) {
    Text(
        text = label,
        color = NTColors.TextTertiary, fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
        modifier = Modifier.padding(vertical = NTDp.sm)
    )
}

@Composable
private fun NTFeedEmptyInline() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = NTDp.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(NTColors.SurfaceVar),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Inbox, contentDescription = null,
                    tint = NTColors.TextTertiary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(NTDp.sm))
            Text("No recent activity", color = NTColors.TextSecondary,
                fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Orders will appear here once placed",
                color = NTColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

enum class NTBadgeStatus { CASH, CREDIT, OVERDUE, PENDING, COMPLETED }

@Composable
fun NTOrderActivityCard(
    customerName: String,
    meta: String,
    amountLabel: String?,
    badge: String?,
    badgeStatus: NTBadgeStatus,
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(NTDp.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconFg,
                    modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        customerName, color = NTColors.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NTBadge(text = badge, status = badgeStatus)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(meta, color = NTColors.TextTertiary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (amountLabel != null) {
                Spacer(modifier = Modifier.width(NTDp.sm))
                Text(amountLabel, color = if (isPositive) NTColors.Success else NTColors.Error,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NTBadge(text: String, status: NTBadgeStatus) {
    val (bg, fg) = when (status) {
        NTBadgeStatus.CASH      -> NTColors.SuccessLight to NTColors.SuccessText
        NTBadgeStatus.CREDIT    -> NTColors.InfoLight    to NTColors.InfoText
        NTBadgeStatus.OVERDUE   -> NTColors.ErrorLight   to NTColors.ErrorText
        NTBadgeStatus.PENDING   -> NTColors.WarningLight to NTColors.WarningText
        NTBadgeStatus.COMPLETED -> NTColors.SurfaceVar   to NTColors.TextSecondary
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
            .background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = fg, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}
