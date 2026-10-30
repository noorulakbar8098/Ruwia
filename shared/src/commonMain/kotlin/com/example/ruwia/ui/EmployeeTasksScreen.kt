package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar

/**
 * Today's delivery tasks assigned to the logged-in employee.
 *
 * The task feed is loaded by [com.example.ruwia.presentation.EmployeeViewModel]
 * as part of the dashboard refresh; this screen only renders the list and
 * forwards the "mark delivered" action so the status update stays single-sourced
 * in the view model.
 */
@Composable
fun EmployeeTasksScreen(
    tasks: List<DeliveryTask>,
    onBack: () -> Unit,
    onMarkDone: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SystemBackHandler(onBack)

    val openTasks = tasks.count { !it.isCompleted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background)
            .padding(contentPadding),
    ) {
        NTPrimaryTopBar(
            title = "My Tasks",
            subtitle = when {
                tasks.isEmpty() -> "No deliveries scheduled"
                openTasks == 0 -> "All ${tasks.size} deliveries completed"
                else -> "$openTasks open · ${tasks.size} scheduled"
            },
            onBack = onBack,
            horizontalPadding = 16.dp,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tasks.isEmpty()) {
                item { EmptyTasksState() }
            } else {
                items(tasks) { task ->
                    DeliveryTaskCard(
                        task = task,
                        onMarkDone = { onMarkDone(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState() {
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
                    Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = NTColors.Primary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "No tasks for today",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Deliveries assigned to you will appear here.",
                fontSize = 13.sp,
                color = NTColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun DeliveryTaskCard(
    task: DeliveryTask,
    onMarkDone: () -> Unit,
) {
    val completed = task.isCompleted()
    val statusColor = when {
        completed -> NTColors.Success
        task.status.equals("pending", ignoreCase = true) -> NTColors.Warning
        else -> NTColors.Info
    }
    val statusBackground = when {
        completed -> NTColors.SuccessLight
        task.status.equals("pending", ignoreCase = true) -> NTColors.WarningLight
        else -> NTColors.InfoLight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (completed) NTColors.SuccessLight else NTColors.PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = if (completed) NTColors.Success else NTColors.Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.customerName.ifBlank { "Delivery" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = taskDetail(task),
                    fontSize = 12.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = task.status.replace("_", " ").uppercase().ifBlank { "PENDING" },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (task.address.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = NTColors.TextTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = task.address,
                    fontSize = 12.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (task.phone.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Phone,
                    contentDescription = null,
                    tint = NTColors.TextTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = task.phone,
                    fontSize = 12.sp,
                    color = NTColors.TextSecondary,
                )
            }
        }

        if (!completed) {
            TextButton(
                onClick = onMarkDone,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Success),
            ) {
                Text(
                    text = "Mark delivered",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun taskDetail(task: DeliveryTask): String {
    val cases = "${task.canQty} Case${if (task.canQty == 1) "" else "s"}"
    val product = task.productName?.takeIf { it.isNotBlank() }
    val eta = task.etaText.takeIf { it.isNotBlank() }
    return listOfNotNull(product, cases, eta).joinToString(" · ")
}

private fun DeliveryTask.isCompleted(): Boolean =
    status.equals("completed", ignoreCase = true) || status.equals("done", ignoreCase = true)
