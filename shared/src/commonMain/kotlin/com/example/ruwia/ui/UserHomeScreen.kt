package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Payment
import com.example.ruwia.presentation.UserViewModel
import com.example.ruwia.ui.dashboard.*

@Composable
fun UserHomeScreen(
    vm: UserViewModel,
    onPlaceOrder: () -> Unit,
    onLogout: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Logout?") },
            text    = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout()
                        onLogout()
                    },
                    colors  = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC3333)),
                ) { Text("Logout", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background)
    ) {
        when {
            state.loading        -> UserSkeleton()
            state.error != null  -> NTErrorState(
                message        = state.error!!,
                onRetry        = vm::loadData
            )
            else -> UserContent(
                customer    = state.customer,
                orders      = state.orders,
                payments    = state.payments,
                onPlaceOrder = onPlaceOrder,
                onLogout    = { showLogoutDialog = true }
            )
        }
    }
}

// ── Main content ──────────────────────────────────────────────

@Composable
private fun UserContent(
    customer: Customer?,
    orders: List<Order>,
    payments: List<Payment>,
    onPlaceOrder: () -> Unit,
    onLogout: () -> Unit
) {
    val pendingCount = orders.count { it.status == "pending" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item {
            UserHeader(
                customer          = customer,
                notificationCount = pendingCount,
                onLogout          = onLogout
            )
        }

        // Greeting
        item {
            UserGreeting(customer = customer)
        }

        // Hero card
        item {
            UserHeroCard(customer = customer, onPlaceOrder = onPlaceOrder)
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // KPI pair
        item {
            UserKpiRow(
                cansHeld    = customer?.cansHeld ?: 0,
                ordersTotal = orders.size,
                pendingOrders = pendingCount
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Quick actions
        item {
            UserQuickActions(
                hasBalance   = (customer?.balance ?: 0.0) > 0,
                onPlaceOrder = onPlaceOrder
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Recent orders
        item {
            NTSectionHeader(
                label = "RECENT ACTIVITY",
                title = "My Orders",
                modifier = Modifier.padding(horizontal = NTDp.screenPad)
            )
            Spacer(modifier = Modifier.height(NTDp.md))
        }

        if (orders.isEmpty()) {
            item {
                NTEmptyState(
                    icon     = Icons.Rounded.ReceiptLong,
                    title    = "No Orders Yet",
                    subtitle = "Tap \"Place Order\" above to request your first delivery.",
                    modifier = Modifier.padding(NTDp.lg)
                )
            }
        } else {
            items(items = orders.take(10), key = { it.id ?: it.hashCode().toString() }) { order ->
                UserOrderCard(order = order)
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }

        // Payments section
        if (payments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(NTDp.md))
                NTSectionHeader(
                    label = "PAYMENT HISTORY",
                    title = "Transactions",
                    modifier = Modifier.padding(horizontal = NTDp.screenPad)
                )
                Spacer(modifier = Modifier.height(NTDp.md))
            }
            items(items = payments.take(5), key = { it.id ?: it.hashCode().toString() }) { payment ->
                UserPaymentCard(payment = payment)
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────

@Composable
private fun UserHeader(
    customer: Customer?,
    notificationCount: Int,
    onLogout: () -> Unit
) {
    val initials = customer?.name
        ?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.take(2)
        ?.joinToString("") ?: "U"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(NTDp.avatarLg)
                .clip(CircleShape)
                .background(NTColors.AvatarTeal),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(NTDp.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CUSTOMER ACCOUNT",
                color = NTColors.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Text(
                text = customer?.name ?: "Welcome",
                color = NTColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Pending orders badge
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NTColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Orders",
                    tint = NTColors.TextPrimary, modifier = Modifier.size(NTDp.iconLg))
            }
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(NTColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        notificationCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(NTDp.sm))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NTColors.Surface)
                .clickable(onClick = onLogout),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = "Logout",
                tint = NTColors.TextSecondary, modifier = Modifier.size(NTDp.iconLg))
        }
    }
}

// ── Greeting ──────────────────────────────────────────────────

@Composable
private fun UserGreeting(customer: Customer?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .padding(top = NTDp.xs, bottom = NTDp.md)
    ) {
        Text(
            text = "Good morning",
            color = NTColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(NTDp.xs))
        Text(
            text = if (customer != null) "Here's your account summary." else "Loading your account...",
            color = NTColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}

// ── Customer hero card ────────────────────────────────────────

@Composable
private fun UserHeroCard(customer: Customer?, onPlaceOrder: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(Brush.linearGradient(colors = listOf(NTColors.GradStart, NTColors.GradEnd)))
    ) {
        // Decorative blob
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(NTColors.GradAccent)
        )

        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Text(
                text = "MY ACCOUNT",
                color = NTColors.PrimaryLight.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(NTDp.sm))
            Text(
                text = customer?.name ?: "—",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            customer?.address?.let { address ->
                Text(
                    text = address,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(NTDp.lg))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(NTDp.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stats
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${customer?.cansHeld ?: 0}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CANS HELD",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val balance = customer?.balance ?: 0.0
                    Text(
                        text = if (balance > 0) "₹${balance.toInt()}" else "₹0",
                        color = if (balance > 0) NTColors.AccentLight else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BALANCE DUE",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                // Place order CTA
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.Accent)
                        .clickable(enabled = customer != null, onClick = onPlaceOrder)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Place Order",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── KPI row ───────────────────────────────────────────────────

@Composable
private fun UserKpiRow(
    cansHeld: Int,
    ordersTotal: Int,
    pendingOrders: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad),
        horizontalArrangement = Arrangement.spacedBy(NTDp.md)
    ) {
        UserKpiCard(
            icon        = Icons.Rounded.Opacity,
            title       = "CANS HELD",
            value       = "$cansHeld",
            subtitle    = "With you",
            iconBg      = NTColors.PrimaryLight,
            iconFg      = NTColors.Primary,
            statusColor = NTColors.Primary,
            modifier    = Modifier.weight(1f)
        )
        UserKpiCard(
            icon        = Icons.Rounded.Inventory2,
            title       = "TOTAL ORDERS",
            value       = "$ordersTotal",
            subtitle    = if (pendingOrders > 0) "$pendingOrders pending" else "All fulfilled",
            iconBg      = if (pendingOrders > 0) NTColors.WarningLight else NTColors.SuccessLight,
            iconFg      = if (pendingOrders > 0) NTColors.Warning else NTColors.Success,
            statusColor = if (pendingOrders > 0) NTColors.Warning else NTColors.Success,
            modifier    = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UserKpiCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    iconBg: Color,
    iconFg: Color,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Box(
                modifier = Modifier
                    .size(NTDp.kpiIconBox)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconFg,
                    modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.height(NTDp.md))
            Text(title, color = NTColors.TextTertiary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(NTDp.xs))
            Text(value, color = NTColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(NTDp.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor)
                )
                Spacer(modifier = Modifier.width(NTDp.xs))
                Text(subtitle, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────

@Composable
private fun UserQuickActions(
    hasBalance: Boolean,
    onPlaceOrder: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
        NTSectionHeader(label = "QUICK ACCESS", title = "Shortcuts")
        Spacer(modifier = Modifier.height(NTDp.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NTDp.md)
        ) {
            UserActionButton(
                icon    = Icons.Rounded.AddShoppingCart,
                label   = "New Order",
                bgColor = NTColors.Accent,
                onClick = onPlaceOrder,
                modifier = Modifier.weight(1f)
            )
            UserActionButton(
                icon    = Icons.Rounded.CreditCard,
                label   = "Payments",
                bgColor = NTColors.Primary,
                badge   = if (hasBalance) "DUE" else null,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            UserActionButton(
                icon    = Icons.Rounded.Phone,
                label   = "Support",
                bgColor = NTColors.TextPrimary,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UserActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(NTDp.qaIconBox)
                    .clip(RoundedCornerShape(NTDp.radLg))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = Color.White,
                    modifier = Modifier.size(NTDp.iconXl))
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.Error)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(NTDp.sm))
        Text(
            text = label,
            color = NTColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// ── Order card ────────────────────────────────────────────────

@Composable
private fun UserOrderCard(order: Order) {
    val (iconBg, iconFg, icon, statusBg, statusFg) = when (order.status) {
        "delivered" -> OrderStyle(NTColors.SaleIconBg,   NTColors.SaleIconFg,   Icons.Rounded.CallMade,    NTColors.SuccessLight, NTColors.SuccessText)
        "pending"   -> OrderStyle(NTColors.PendIconBg,   NTColors.PendIconFg,   Icons.Rounded.AccessTime,  NTColors.WarningLight, NTColors.WarningText)
        "approved"  -> OrderStyle(NTColors.DelivIconBg,  NTColors.DelivIconFg,  Icons.Rounded.CheckCircle, NTColors.InfoLight,    NTColors.InfoText)
        "cancelled" -> OrderStyle(NTColors.CancelIconBg, NTColors.CancelIconFg, Icons.Rounded.Cancel,      NTColors.ErrorLight,   NTColors.ErrorText)
        else        -> OrderStyle(NTColors.SurfaceVar,   NTColors.TextTertiary, Icons.Rounded.HourglassEmpty, NTColors.SurfaceVar, NTColors.TextSecondary)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconFg,
                    modifier = Modifier.size(NTDp.iconLg))
            }

            Spacer(modifier = Modifier.width(NTDp.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${order.qty} can${if (order.qty != 1) "s" else ""}",
                    color = NTColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = order.createdAt?.take(10) ?: "—",
                    color = NTColors.TextTertiary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = order.status.replaceFirstChar { it.uppercaseChar() },
                    color = statusFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Payment card ──────────────────────────────────────────────

@Composable
private fun UserPaymentCard(payment: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(NTColors.PayIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CreditCard, contentDescription = null,
                    tint = NTColors.PayIconFg, modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.mode.replaceFirstChar { it.uppercaseChar() },
                    color = NTColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = payment.createdAt?.take(10) ?: "—",
                    color = NTColors.TextTertiary,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "₹${payment.amount.toInt()}",
                color = NTColors.Success,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Skeleton ──────────────────────────────────────────────────

@Composable
private fun UserSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background)
            .padding(NTDp.screenPad)
    ) {
        Spacer(modifier = Modifier.height(NTDp.md))
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            NTShimmerBox(width = 48.dp, height = 48.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(NTDp.md))
            Column {
                NTShimmerBox(width = 80.dp,  height = 10.dp)
                Spacer(modifier = Modifier.height(6.dp))
                NTShimmerBox(width = 140.dp, height = 18.dp)
            }
        }
        Spacer(modifier = Modifier.height(NTDp.lg))
        // Greeting
        NTShimmerBox(width = 200.dp, height = 26.dp)
        Spacer(modifier = Modifier.height(8.dp))
        NTShimmerBox(width = 260.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(NTDp.lg))
        // Hero card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(NTDp.radXxl))
                .background(ntShimmerBrush())
        )
        Spacer(modifier = Modifier.height(NTDp.lg))
        // KPI row
        Row(horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
            val shimmer = ntShimmerBrush()
            Box(modifier = Modifier.weight(1f).height(130.dp)
                .clip(RoundedCornerShape(NTDp.radXxl)).background(shimmer))
            Box(modifier = Modifier.weight(1f).height(130.dp)
                .clip(RoundedCornerShape(NTDp.radXxl)).background(shimmer))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────

private data class OrderStyle(
    val iconBg: Color,
    val iconFg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val statusBg: Color,
    val statusFg: Color
)
