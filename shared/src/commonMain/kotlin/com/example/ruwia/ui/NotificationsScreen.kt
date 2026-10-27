package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.NotificationItem
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar
import com.example.ruwia.util.isoToDisplayDateTime

/**
 * Shared in-app notification center, used by both the admin and employee
 * apps. Tapping a row marks it read; the bell badges clear on the next
 * refresh (or immediately for the tapped row).
 */
@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBackHandler(onBack)

    Column(modifier = modifier.fillMaxSize().background(NTColors.Background)) {
        NTPrimaryTopBar(
            title = "Notifications",
            subtitle = if (unreadCount > 0) "$unreadCount unread" else "You're all caught up",
            onBack = onBack,
            horizontalPadding = 16.dp,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (unreadCount > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onMarkAllRead) {
                            Text(
                                "Mark all read",
                                color = NTColors.Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(NTColors.PrimaryLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = NTColors.Primary,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "No notifications yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.TextPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Sales and stock updates will appear here.",
                                fontSize = 13.sp,
                                color = NTColors.TextTertiary,
                            )
                        }
                    }
                }
            } else {
                items(notifications, key = { it.id.ifBlank { "${it.title}-${it.createdAt}" } }) { n ->
                    NotificationRow(
                        item = n,
                        onClick = { if (!n.isRead) onMarkRead(n.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    val (icon, bg, fg) = when (item.kind.lowercase()) {
        "sale" -> Triple(
            Icons.Rounded.ShoppingCart, NTColors.PrimaryLight, NTColors.Primary,
        )
        "stock" -> Triple(
            Icons.Rounded.Inventory2, NTColors.SuccessLight, NTColors.Success,
        )
        else -> Triple(
            Icons.Rounded.Notifications, NTColors.SurfaceVar, NTColors.TextSecondary,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Mark read", onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title.ifBlank { "Notification" },
                    fontSize = 14.sp,
                    fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = NTColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!item.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(NTColors.Primary),
                    )
                }
            }
            if (item.body.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.body,
                    fontSize = 12.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = isoToDisplayDateTime(item.createdAt),
                fontSize = 10.sp,
                color = NTColors.TextTertiary,
            )
        }
    }
}
