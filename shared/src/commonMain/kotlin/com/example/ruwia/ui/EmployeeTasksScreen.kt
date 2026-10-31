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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import com.example.ruwia.ui.components.DeliveryTimelineView
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ruwia.data.getDeliveryTrackingManager
import com.example.ruwia.util.LocationPermissionRationaleDialog
import com.example.ruwia.util.LocationPermissionStatus
import com.example.ruwia.util.LocationUtils
import com.example.ruwia.util.openMapNavigation
import com.example.ruwia.util.rememberLocationPermissionController

/**
 * Today's delivery tasks assigned to the logged-in employee.
 *
 * The task feed is loaded by [com.example.ruwia.presentation.EmployeeViewModel]
 * as part of the dashboard refresh; this screen only renders the list and
 * forwards the task lifecycle actions so the status and location updates
 * stay single-sourced in the view model.
 */
@Composable
fun EmployeeTasksScreen(
    tasks: List<DeliveryTask>,
    onBack: () -> Unit,
    onAccept: (String) -> Unit = {},
    onRecordCheckpoint: (String) -> Unit = {},
    onMarkDone: (String) -> Unit,
    onRefresh: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    SystemBackHandler(onBack)

    val openTasks = tasks.count { !it.isCompleted() }
    val trackingManager = remember { getDeliveryTrackingManager() }
    val isTracking by trackingManager.isTracking.collectAsState()
    val activeTaskId by trackingManager.activeTaskId.collectAsState()

    var taskPendingPermission by remember { mutableStateOf<DeliveryTask?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Auto-refresh tasks every 4 seconds while active so dynamic log points and live ETA refresh automatically
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000L)
            onRefresh()
        }
    }

    fun navigateToTaskDestination(task: DeliveryTask) {
        val coords = if (task.destLatitude != null && task.destLongitude != null) {
            Pair(task.destLatitude, task.destLongitude)
        } else {
            LocationUtils.parseGoogleMapsCoordinates(task.mapsLink)
                ?: LocationUtils.parseGoogleMapsCoordinates(task.address)
                ?: LocationUtils.parseGoogleMapsCoordinates(task.customerAddress)
        }
        if (coords != null) {
            openMapNavigation(
                latitude = coords.first,
                longitude = coords.second,
                label = task.destName ?: task.customerName,
            )
        }
    }

    val permissionController = rememberLocationPermissionController { granted ->
        if (granted) {
            val pending = taskPendingPermission
            if (pending != null) {
                onAccept(pending.id)
                trackingManager.startTracking(
                    pending.id,
                    pending.employeeId ?: "",
                    pending.destLatitude,
                    pending.destLongitude,
                    pending.destName ?: pending.customerName
                )
                navigateToTaskDestination(pending)
                taskPendingPermission = null
            }
        }
    }

    if (showRationaleDialog) {
        LocationPermissionRationaleDialog(
            status = permissionController.status,
            onDismiss = { showRationaleDialog = false },
            onRequestPermission = {
                permissionController.requestPermission()
            },
            onOpenSettings = {
                permissionController.openAppSettings()
            }
        )
    }

    fun handleAcceptTask(task: DeliveryTask) {
        if (permissionController.status == LocationPermissionStatus.GRANTED) {
            onAccept(task.id)
            trackingManager.startTracking(
                task.id,
                task.employeeId ?: "",
                task.destLatitude,
                task.destLongitude,
                task.destName ?: task.customerName
            )
            navigateToTaskDestination(task)
        } else {
            taskPendingPermission = task
            showRationaleDialog = true
        }
    }

    fun handleCompleteTask(task: DeliveryTask) {
        onMarkDone(task.id)
        if (activeTaskId == task.id) {
            trackingManager.stopTracking()
        }
    }

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
                        isActivelyTracking = isTracking && activeTaskId == task.id,
                        onAccept = { handleAcceptTask(task) },
                        onRecordCheckpoint = { onRecordCheckpoint(task.id) },
                        onMarkDone = { handleCompleteTask(task) },
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
    isActivelyTracking: Boolean = false,
    onAccept: () -> Unit,
    onRecordCheckpoint: () -> Unit,
    onMarkDone: () -> Unit,
) {
    val completed = task.isCompleted()
    val isPending = task.status.equals("pending", ignoreCase = true)
    val isInTransit = task.status.equals("in_transit", ignoreCase = true) || task.status.equals("accepted", ignoreCase = true)

    val statusColor = when {
        completed -> NTColors.Success
        isPending -> NTColors.Warning
        isInTransit -> NTColors.Primary
        else -> NTColors.Info
    }
    val statusBackground = when {
        completed -> NTColors.SuccessLight
        isPending -> NTColors.WarningLight
        isInTransit -> NTColors.PrimaryLight
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
                text = if (isInTransit) {
                    "IN TRANSIT (${task.timeline.size}/10)"
                } else {
                    task.status.replace("_", " ").uppercase().ifBlank { "PENDING" }
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        val destName = task.destName?.takeIf { it.isNotBlank() } ?: "Destination Shop"
        if (task.destLatitude != null && task.destLongitude != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        openMapNavigation(task.destLatitude, task.destLongitude, destName)
                    }
                    .background(NTColors.PrimaryLight.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Navigation,
                    contentDescription = "Navigate to shop",
                    tint = NTColors.Primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shop: $destName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                    )
                    Text(
                        text = "Tap to open Google Maps navigation",
                        fontSize = 10.sp,
                        color = NTColors.TextSecondary,
                    )
                }
            }
        }

        if (isActivelyTracking || isInTransit) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = if (isActivelyTracking) NTColors.PrimaryLight else NTColors.SurfaceVar,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isActivelyTracking) NTColors.Primary.copy(alpha = 0.5f) else NTColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isActivelyTracking) NTColors.Primary else NTColors.Warning)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isActivelyTracking) "Background GPS Tracking Active" else "Journey In Transit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActivelyTracking) NTColors.Primary else NTColors.TextSecondary
                        )
                    }

                    if (task.distanceRemainingMeters != null) {
                        Text(
                            text = "${LocationUtils.formatDistance(task.distanceRemainingMeters)} remaining",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NTColors.Primary
                        )
                    }
                }
            }
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

        Spacer(Modifier.height(10.dp))
        DeliveryTimelineView(
            timeline = task.timeline,
            status = task.status,
            initialExpanded = task.timeline.isNotEmpty(),
        )

        if (isPending) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Accept Task & Start Route",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        } else if (isInTransit) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRecordCheckpoint,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NTColors.Primary),
                ) {
                    Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, tint = NTColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (task.timeline.size < 10) "Log Point ${task.timeline.size + 1}" else "Log Checkpoint",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                    )
                }
                Button(
                    onClick = onMarkDone,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Success),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Mark Delivered",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
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
    status.equals("completed", ignoreCase = true) ||
    status.equals("done", ignoreCase = true) ||
    status.equals("delivered", ignoreCase = true)
