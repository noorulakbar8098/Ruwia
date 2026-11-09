package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.DeliveryTask
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import com.example.ruwia.ui.components.DeliveryTimelineView
import com.example.ruwia.ui.components.DeliveryPrimaryButton
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp

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
    onNavigateToAddSale: (DeliveryTask) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    SystemBackHandler(onBack)

    val openTasks = tasks.count { !it.isClosedForEmployee() }
    val completedCount = tasks.count { it.isCompleted() }
    val cancelledCount = tasks.count { it.status.equals("cancelled", ignoreCase = true) || it.status.equals("canceled", ignoreCase = true) }
    val trackingManager = remember { getDeliveryTrackingManager() }
    val isTracking by trackingManager.isTracking.collectAsState()
    val activeTaskId by trackingManager.activeTaskId.collectAsState()

    var taskPendingPermission by remember { mutableStateOf<DeliveryTask?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Active") }
    val visibleTasks = when (selectedFilter) {
        "Completed" -> tasks.filter { it.isCompleted() }
        "Cancelled" -> tasks.filter {
            it.status.equals("cancelled", ignoreCase = true) || it.status.equals("canceled", ignoreCase = true)
        }
        "All" -> tasks
        else -> tasks.filterNot { it.isClosedForEmployee() }
    }

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
        } else {
            showRationaleDialog = true
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
        onNavigateToAddSale(task)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliveryTrackingPalette.Canvas)
            .padding(contentPadding),
    ) {
        DeliveryTrackingHeader(
            subtitle = when {
                tasks.isEmpty() -> "No deliveries scheduled"
                openTasks == 0 && cancelledCount > 0 -> "$completedCount completed · $cancelledCount cancelled"
                openTasks == 0 -> "All ${tasks.size} deliveries completed"
                else -> "$openTasks open · ${tasks.size} scheduled"
            },
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = DeliveryTrackingPalette.Surface,
                    border = BorderStroke(1.dp, DeliveryTrackingPalette.Border),
                    shadowElevation = NTDp.cardElevation,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "YOUR ROUTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = DeliveryTrackingPalette.Orange,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                when {
                                    isTracking -> "You're on the way"
                                    openTasks == 0 -> "All caught up"
                                    openTasks == 1 -> "1 delivery to go"
                                    else -> "$openTasks deliveries to go"
                                },
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeliveryTrackingPalette.Ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$completedCount completed · ${tasks.size} assigned",
                                fontSize = 12.sp,
                                color = DeliveryTrackingPalette.SecondaryInk,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeliveryTrackingPalette.SoftOrange),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Navigation,
                                contentDescription = null,
                                tint = DeliveryTrackingPalette.Orange,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Active", "Completed", "Cancelled", "All").forEach { filter ->
                        val count = when (filter) {
                            "Active" -> openTasks
                            "Completed" -> completedCount
                            "Cancelled" -> cancelledCount
                            else -> tasks.size
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("$filter  $count", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeliveryTrackingPalette.Orange,
                                selectedLabelColor = Color.White,
                                containerColor = DeliveryTrackingPalette.Surface,
                                labelColor = DeliveryTrackingPalette.SecondaryInk,
                            ),
                        )
                    }
                }
            }

            if (visibleTasks.isEmpty()) {
                item {
                    EmptyTasksState(
                        title = if (tasks.isEmpty()) "No tasks for today" else "No $selectedFilter tasks",
                        message = if (tasks.isEmpty()) {
                            "Deliveries assigned to you will appear here."
                        } else {
                            "Tasks matching this status will appear here."
                        },
                    )
                }
            } else {
                items(visibleTasks, key = { it.id }) { task ->
                    DeliveryTaskCard(
                        task = task,
                        isActivelyTracking = isTracking && activeTaskId == task.id,
                        onAccept = { handleAcceptTask(task) },
                        onRecordCheckpoint = { onRecordCheckpoint(task.id) },
                        onMarkDone = { handleCompleteTask(task) },
                        onNavigateToAddSale = { onNavigateToAddSale(task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryTrackingHeader(
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeliveryTrackingPalette.Surface)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = DeliveryTrackingPalette.Ink,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "My deliveries",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeliveryTrackingPalette.Ink,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = DeliveryTrackingPalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    title: String = "No tasks for today",
    message: String = "Deliveries assigned to you will appear here.",
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DeliveryTrackingPalette.SoftOrange),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = DeliveryTrackingPalette.Orange,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeliveryTrackingPalette.Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = DeliveryTrackingPalette.SecondaryInk,
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
    onNavigateToAddSale: () -> Unit = {},
) {
    val completed = task.isCompleted()
    val cancelled = task.status.equals("cancelled", ignoreCase = true) ||
        task.status.equals("canceled", ignoreCase = true)
    val isPending = task.status.equals("pending", ignoreCase = true)
    val isInTransit = task.status.equals("in_transit", ignoreCase = true) || task.status.equals("accepted", ignoreCase = true)

    val statusColor = when {
        completed -> DeliveryTrackingPalette.Green
        cancelled -> DeliveryTrackingPalette.Red
        isPending -> DeliveryTrackingPalette.Orange
        isInTransit -> DeliveryTrackingPalette.Orange
        else -> DeliveryTrackingPalette.SecondaryInk
    }
    val statusBackground = when {
        completed -> DeliveryTrackingPalette.SoftGreen
        cancelled -> DeliveryTrackingPalette.SoftRed
        isPending -> DeliveryTrackingPalette.SoftOrange
        isInTransit -> DeliveryTrackingPalette.SoftOrange
        else -> DeliveryTrackingPalette.SoftNeutral
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .shadow(NTDp.cardElevation, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(DeliveryTrackingPalette.Surface)
            .border(1.dp, DeliveryTrackingPalette.Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateToAddSale() }
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (completed) DeliveryTrackingPalette.SoftGreen else DeliveryTrackingPalette.SoftOrange),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = if (completed) DeliveryTrackingPalette.Green else DeliveryTrackingPalette.Orange,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.customerName.ifBlank { "Delivery" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeliveryTrackingPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = taskProductSummary(task),
                    fontSize = 13.sp,
                    color = DeliveryTrackingPalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onNavigateToAddSale,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Rounded.ShoppingCart,
                    contentDescription = "Record sale for customer",
                    tint = DeliveryTrackingPalette.Orange,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(2.dp))
            Text(
                text = when {
                    completed -> "Completed"
                    cancelled -> "Cancelled"
                    isInTransit -> "In transit"
                    isPending -> "Pending"
                    else -> task.status.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }.ifBlank { "Pending" }
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

        val destinationName = task.destName?.takeIf { it.isNotBlank() } ?: "Destination shop"
        val hasDestination = task.destLatitude != null && task.destLongitude != null
        if (hasDestination) {
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        openMapNavigation(task.destLatitude!!, task.destLongitude!!, destinationName)
                    }
                    .background(DeliveryTrackingPalette.Surface)
                    .border(1.dp, DeliveryTrackingPalette.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = "Open destination in Maps",
                    tint = DeliveryTrackingPalette.Orange,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shop: $destinationName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeliveryTrackingPalette.Ink,
                    )
                    Text(
                        text = "Tap to open in Maps",
                        fontSize = 10.sp,
                        color = DeliveryTrackingPalette.SecondaryInk,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = DeliveryTrackingPalette.SecondaryInk,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (isActivelyTracking || isInTransit) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = if (isActivelyTracking) DeliveryTrackingPalette.SoftOrange else DeliveryTrackingPalette.SoftNeutral,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isActivelyTracking) DeliveryTrackingPalette.Orange.copy(alpha = 0.3f)
                    else DeliveryTrackingPalette.Border,
                ),
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
                                .background(if (isActivelyTracking) DeliveryTrackingPalette.Orange else DeliveryTrackingPalette.SecondaryInk)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isActivelyTracking) {
                                "Location sharing is on"
                            } else {
                                "Delivery in progress"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActivelyTracking) DeliveryTrackingPalette.Orange else DeliveryTrackingPalette.SecondaryInk
                        )
                    }

                    if (task.distanceRemainingMeters != null) {
                        Text(
                            text = "${LocationUtils.formatDistance(task.distanceRemainingMeters)} remaining",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeliveryTrackingPalette.Orange
                        )
                    }
                }
            }
        }

        val contactAddress = task.address.ifBlank { task.customerAddress.orEmpty() }
        if (contactAddress.isNotBlank() || task.phone.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (contactAddress.isNotBlank()) {
                    ContactDetail(
                        icon = Icons.Rounded.LocationOn,
                        text = contactAddress.substringBefore(',').ifBlank { contactAddress },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (task.phone.isNotBlank()) {
                    ContactDetail(
                        icon = Icons.Rounded.Phone,
                        text = task.phone,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (completed) {
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DeliveryTrackingPalette.SoftGreen,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = DeliveryTrackingPalette.Green,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Delivery completed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeliveryTrackingPalette.Green,
                        )
                        Text(
                            "Order delivered successfully",
                            fontSize = 11.sp,
                            color = DeliveryTrackingPalette.SecondaryInk,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DeliveryPrimaryButton(
                text = "Record sale for this customer",
                icon = Icons.Rounded.ShoppingCart,
                onClick = onNavigateToAddSale,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        DeliveryTimelineView(
            task = task,
            initialExpanded = task.timeline.isNotEmpty(),
        )

        if (isPending) {
            Spacer(Modifier.height(12.dp))
            DeliveryPrimaryButton(
                text = "Start delivery",
                icon = Icons.Rounded.PlayArrow,
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (isInTransit) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRecordCheckpoint,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeliveryTrackingPalette.Orange.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeliveryTrackingPalette.Orange),
                ) {
                    Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, tint = DeliveryTrackingPalette.Orange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Log checkpoint",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeliveryTrackingPalette.Orange,
                    )
                }
                DeliveryPrimaryButton(
                    text = "Complete & Sale",
                    icon = Icons.Rounded.CheckCircle,
                    onClick = onMarkDone,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContactDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DeliveryTrackingPalette.SecondaryInk,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = DeliveryTrackingPalette.SecondaryInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun taskProductSummary(task: DeliveryTask): String {
    val product = task.productName?.takeIf { it.isNotBlank() } ?: "Delivery"
    val cases = "${task.canQty} ${if (task.canQty == 1) "Case" else "Cases"}"
    val eta = task.etaText.takeIf { it.isNotBlank() }
    return listOfNotNull(product, cases, eta).joinToString(" · ")
}

private object DeliveryTrackingPalette {
    val Canvas get() = NTColors.Background
    val Surface get() = NTColors.Surface
    val Ink get() = NTColors.TextPrimary
    val SecondaryInk get() = NTColors.TextSecondary
    val Border get() = NTColors.Border
    val SoftNeutral get() = NTColors.SurfaceVar
    val Orange get() = if (NTColors.isDarkMode) Color(0xFFFB923C) else Color(0xFFE86D2F)
    val SoftOrange get() = NTColors.WarningLight
    val Green get() = if (NTColors.isDarkMode) Color(0xFF34D399) else Color(0xFF26834A)
    val SoftGreen get() = NTColors.SuccessLight
    val Red get() = NTColors.Error
    val SoftRed get() = NTColors.ErrorLight
}

private fun DeliveryTask.isCompleted(): Boolean =
    status.equals("completed", ignoreCase = true) ||
    status.equals("done", ignoreCase = true) ||
    status.equals("delivered", ignoreCase = true)

private fun DeliveryTask.isClosedForEmployee(): Boolean =
    isCompleted() ||
    status.equals("cancelled", ignoreCase = true) ||
    status.equals("canceled", ignoreCase = true)
