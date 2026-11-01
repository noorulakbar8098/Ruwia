package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import com.example.ruwia.ui.components.AddressAutoCompleteField
import com.example.ruwia.ui.components.InteractiveMapLocationDialog
import com.example.ruwia.ui.components.InteractiveMapPickerCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import com.example.ruwia.ui.components.DeliveryTimelineView
import com.example.ruwia.ui.components.DeliveryPrimaryButton
import com.example.ruwia.util.LocationUtils
import com.example.ruwia.util.openMapNavigation
import com.example.ruwia.data.getLocationService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar
import com.example.ruwia.ui.components.formKeyboardActions
import com.example.ruwia.util.dbToDisplayDate
import com.example.ruwia.util.displayDateToDb
import com.example.ruwia.util.LocationPermissionStatus
import com.example.ruwia.util.rememberLocationPermissionController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * DTO emitted by the admin create-task form. Date is normalised to the
 * `YYYY-MM-DD` shape expected by `route_tasks.scheduled_date`; time is kept in
 * the display form (`h:mm AM/PM`) and persisted in `eta_text`.
 */
data class CreateDeliveryTaskInput(
    val customerId: String,
    val customerName: String,
    val customerAddress: String,
    val customerPhone: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val employeeId: String,
    val employeeName: String,
    val deliveryDate: String,
    val deliveryTime: String,
    val destLatitude: Double? = null,
    val destLongitude: Double? = null,
    val destName: String? = null,
    val mapsLink: String? = null,
)

// ── Delivery task board ───────────────────────────────────────────────────────

@Composable
fun AdminDeliveryTasksScreen(
    tasks: List<DeliveryTask>,
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onUpdateStatus: (taskId: String, status: String) -> Unit,
    onRefresh: () -> Unit = {},
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    SystemBackHandler(onBack)

    // Auto-refresh tasks every 4 seconds so admin sees live location, checkpoints, and ETA dynamically
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000L)
            onRefresh()
        }
    }

    var taskToCancel by remember { mutableStateOf<DeliveryTask?>(null) }
    var selectedFilter by remember { mutableStateOf("Open") }

    val sorted = remember(tasks) { tasks.sortedWith(deliveryTaskComparator) }
    val openTasks = sorted.filter { !it.isTaskClosed() }
    val completedCount = tasks.count { it.normalizedStatus() == "completed" }
    val cancelledCount = tasks.count { it.normalizedStatus() == "cancelled" }
    val visibleTasks = when (selectedFilter) {
        "Completed" -> sorted.filter { it.normalizedStatus() == "completed" }
        "Cancelled" -> sorted.filter { it.normalizedStatus() == "cancelled" }
        "All" -> sorted
        else -> openTasks
    }

    if (taskToCancel != null) {
        AlertDialog(
            onDismissRequest = { taskToCancel = null },
            title = { Text("Cancel delivery?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text = {
                Text(
                    "This removes the task from the employee's route. The delivery will not be marked as completed.",
                    color = NTColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToCancel?.let { onUpdateStatus(it.id, "cancelled") }
                        taskToCancel = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NTColors.Error),
                ) {
                    Text("Cancel task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToCancel = null }) {
                    Text("Keep task", color = NTColors.TextSecondary)
                }
            },
            containerColor = NTColors.Surface,
            shape = RoundedCornerShape(20.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background)
            .padding(contentPadding),
    ) {
        NTPrimaryTopBar(
            title = "Delivery tasks",
            subtitle = when {
                tasks.isEmpty() -> "No deliveries scheduled"
                openTasks.isEmpty() -> "All deliveries are closed"
                else -> "${openTasks.size} open · $completedCount completed"
            },
            onBack = onBack,
            horizontalPadding = 16.dp,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = NTColors.Surface,
                    border = BorderStroke(1.dp, NTColors.Border),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "DELIVERY OVERVIEW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = NTColors.PrimaryDeep,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    when {
                                        openTasks.isEmpty() -> "All deliveries are closed"
                                        openTasks.size == 1 -> "1 delivery still open"
                                        else -> "${openTasks.size} deliveries still open"
                                    },
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NTColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$completedCount completed · ${tasks.size} scheduled",
                                    fontSize = 12.sp,
                                    color = NTColors.TextSecondary,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NTColors.PrimaryLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Assignment,
                                    contentDescription = null,
                                    tint = NTColors.PrimaryDeep,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        DeliveryPrimaryButton(
                            text = "New delivery",
                            icon = Icons.Rounded.Add,
                            onClick = onCreateTask,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskMetric(
                        label = "Open",
                        value = openTasks.size.toString(),
                        color = NTColors.PrimaryDeep,
                        modifier = Modifier.weight(1f),
                    )
                    TaskMetric(
                        label = "Completed",
                        value = completedCount.toString(),
                        color = NTColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Open", "Completed", "Cancelled", "All").forEach { filter ->
                        val count = when (filter) {
                            "Open" -> openTasks.size
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
                                selectedContainerColor = NTColors.PrimaryDeep,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = NTColors.ErrorLight,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NTColors.ErrorText,
                            )
                            IconButton(onClick = onClearError) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Dismiss error",
                                    tint = NTColors.ErrorText,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (visibleTasks.isEmpty()) {
                item {
                    EmptyDeliveryTasks(
                        title = if (tasks.isEmpty()) "No delivery tasks yet" else "No $selectedFilter deliveries",
                        message = if (tasks.isEmpty()) {
                            "Create a task to assign a product delivery to a customer."
                        } else {
                            "Tasks matching this status will appear here."
                        },
                    )
                }
            } else {
                item {
                    TaskSectionLabel(
                        when (selectedFilter) {
                            "All" -> "All deliveries"
                            "Open" -> "Active deliveries"
                            else -> "${selectedFilter} deliveries"
                        },
                    )
                }
                items(visibleTasks, key = { it.id }) { task ->
                    AdminDeliveryTaskCard(
                        task = task,
                        onComplete = { onUpdateStatus(task.id, "completed") },
                        onCancel = { taskToCancel = task },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .animateContentSize()
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = NTColors.TextTertiary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = color,
        )
    }
}

@Composable
private fun TaskSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = NTColors.TextTertiary,
        modifier = Modifier.padding(top = 8.dp, start = 2.dp),
    )
}

@Composable
private fun EmptyDeliveryTasks(
    title: String = "No delivery tasks yet",
    message: String = "Create a task to assign a product delivery to a customer.",
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(NTColors.PrimaryLight),
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
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            fontSize = 13.sp,
            color = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AdminDeliveryTaskCard(
    task: DeliveryTask,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = task.normalizedStatus()
    val closed = task.isTaskClosed()
    val statusColor = when (status) {
        "completed" -> NTColors.Success
        "cancelled" -> NTColors.Error
        "pending" -> NTColors.Warning
        "in_transit", "accepted" -> NTColors.Primary
        else -> NTColors.Info
    }
    val statusBackground = when (status) {
        "completed" -> NTColors.SuccessLight
        "cancelled" -> NTColors.ErrorLight
        "pending" -> NTColors.WarningLight
        "in_transit", "accepted" -> NTColors.PrimaryLight
        else -> NTColors.InfoLight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
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
                    .background(if (closed) NTColors.SurfaceVar else NTColors.PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (status == "completed") Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = if (closed) statusColor else NTColors.Primary,
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
                    text = deliveryTaskProductLabel(task),
                    fontSize = 12.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = deliveryTaskStatusLabel(task).lowercase()
                    .replaceFirstChar { it.uppercase() },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))

        TaskInfoRow(Icons.Rounded.CalendarMonth, deliveryTaskScheduleLabel(task))
        Spacer(Modifier.height(6.dp))
        TaskInfoRow(Icons.Rounded.Person, task.employeeName?.takeIf { it.isNotBlank() } ?: "Unassigned")
        val address = task.address.ifBlank { task.customerAddress.orEmpty() }
        if (address.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            TaskInfoRow(Icons.Rounded.LocationOn, address, maxLines = 2)
        }

        val destLabel = task.destName?.takeIf { it.isNotBlank() } ?: "Destination Shop"
        if (task.destLatitude != null && task.destLongitude != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        openMapNavigation(task.destLatitude, task.destLongitude, destLabel)
                    }
                    .padding(vertical = 2.dp)
            ) {
                Icon(
                    Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = NTColors.Primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Open directions to $destLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NTColors.Primary,
                )
            }
        }

        // Live tracking & ETA badge
        val isTrackingActive = task.status in listOf("in_transit", "accepted", "arrived") || task.currentLatitude != null
        if (isTrackingActive) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (task.status == "arrived") NTColors.SuccessLight else NTColors.PrimaryLight.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (task.status == "arrived") NTColors.Success.copy(alpha = 0.5f) else NTColors.Primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (task.status == "arrived") NTColors.Success else NTColors.Primary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (task.status == "arrived") "Arrived at destination" else "Live route updates",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = if (task.status == "arrived") NTColors.Success else NTColors.Primary
                            )
                        }

                        val etaDisplay = when {
                            task.status == "arrived" -> "Arrived ${task.arrivalTime ?: ""}".trim()
                            !task.liveEta.isNullOrBlank() -> "ETA: ${task.liveEta}"
                            else -> null
                        }
                        if (etaDisplay != null) {
                            Text(
                                text = etaDisplay,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.status == "arrived") NTColors.Success else NTColors.Primary
                            )
                        }
                    }

                    if (task.currentLocationName != null || task.currentLatitude != null) {
                        Text(
                            text = "Current: ${task.currentLocationName ?: "${task.currentLatitude}, ${task.currentLongitude}"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NTColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (task.distanceRemainingMeters != null && task.status != "arrived") {
                            Text(
                                text = "Remaining: ${LocationUtils.formatDistance(task.distanceRemainingMeters)}",
                                fontSize = 11.sp,
                                color = NTColors.TextTertiary
                            )
                        }
                        if (task.lastLocationUpdateTime != null) {
                            Text(
                                text = "Updated: ${task.lastLocationUpdateTime}",
                                fontSize = 10.sp,
                                color = NTColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        DeliveryTimelineView(
            task = task,
            initialExpanded = task.timeline.isNotEmpty(),
        )

        if (!closed) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NTColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.TextSecondary),
                ) {
                    Text("Cancel delivery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(4.dp))
                DeliveryPrimaryButton(
                    text = "Complete",
                    icon = Icons.Rounded.CheckCircle,
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TaskInfoRow(
    icon: ImageVector,
    text: String,
    maxLines: Int = 1,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NTColors.TextTertiary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = NTColors.TextSecondary,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Create-task form ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateDeliveryTaskScreen(
    customers: List<Customer>,
    products: List<ProductCategory>,
    employees: List<EmployeeInfo>,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (CreateDeliveryTaskInput) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val sellableProducts = remember(products) {
        products.filter { it.isActive && !it.isDeleted && it.id.isNotBlank() }
    }
    val assignableEmployees = remember(employees) {
        employees.filter { it.id.isNotBlank() && !it.status.equals("inactive", ignoreCase = true) }
    }
    val savedCustomers = remember(customers) {
        customers.filter { !it.id.isNullOrBlank() && !it.id.startsWith("local_") }
    }

    val defaultSchedule = remember {
        val nextHour = Clock.System.now().plus(1.hours)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        formatDisplayDate(nextHour.date) to formatDisplayTime(nextHour.hour, nextHour.minute)
    }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var destShopName by remember { mutableStateOf("") }
    var mapsLocationInput by remember { mutableStateOf("") }
    var resolvedCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isResolvingLocation by remember { mutableStateOf(false) }
    var locationResolveError by remember { mutableStateOf<String?>(null) }
    var isUsingCurrentLocation by remember { mutableStateOf(false) }
    var currentLocationError by remember { mutableStateOf<String?>(null) }
    var showLocationRationale by remember { mutableStateOf(false) }
    var showInteractiveMapDialog by remember { mutableStateOf(false) }
    val locationService = remember { getLocationService() }
    val locationScope = rememberCoroutineScope()

    val effectiveCoordinates = LocationUtils.parseGoogleMapsCoordinates(mapsLocationInput) ?: resolvedCoordinates

    LaunchedEffect(mapsLocationInput) {
        if (mapsLocationInput.isBlank()) {
            resolvedCoordinates = null
            locationResolveError = null
            return@LaunchedEffect
        }
        val direct = LocationUtils.parseGoogleMapsCoordinates(mapsLocationInput)
        if (direct != null) {
            resolvedCoordinates = direct
            locationResolveError = null
        } else if (LocationUtils.isGoogleMapsUrl(mapsLocationInput) || mapsLocationInput.contains("http")) {
            isResolvingLocation = true
            locationResolveError = null
            try {
                val resolved = locationService.resolveCoordinatesFromInput(mapsLocationInput)
                if (resolved != null) {
                    resolvedCoordinates = resolved
                    if (destShopName.isBlank()) {
                        val addr = locationService.reverseGeocode(resolved.first, resolved.second)
                        if (addr.isNotBlank() && !addr.startsWith("Point")) {
                            destShopName = addr
                        }
                    }
                } else {
                    locationResolveError = "Could not extract GPS coordinates from this link."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                locationResolveError = "Failed to resolve link: ${e.message}"
            } finally {
                isResolvingLocation = false
            }
        }
    }

    suspend fun useCurrentLocation() {
        isUsingCurrentLocation = true
        currentLocationError = null
        locationResolveError = null
        try {
            val point = locationService.getCurrentLocation()
            if (point == null) {
                currentLocationError = "Location unavailable. Check GPS and try again."
            } else {
                val coordinateText = "${point.latitude}, ${point.longitude}"
                mapsLocationInput = coordinateText
                resolvedCoordinates = point.latitude to point.longitude
                val address = locationService.reverseGeocode(point.latitude, point.longitude)
                if (address.isNotBlank() && !address.startsWith("Point")) {
                    destShopName = address
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentLocationError = e.message ?: "Could not read the device location."
        } finally {
            isUsingCurrentLocation = false
        }
    }

    val locationPermissionController = rememberLocationPermissionController { granted ->
        if (granted) {
            locationScope.launch { useCurrentLocation() }
        } else {
            currentLocationError = "Location permission is needed to use the device's current position."
        }
    }

    if (showLocationRationale) {
        val permanentlyDenied = locationPermissionController.status == LocationPermissionStatus.PERMANENTLY_DENIED
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = {
                Text(
                    if (permanentlyDenied) "Location access is off" else "Set destination from this device?",
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary,
                )
            },
            text = {
                Text(
                    if (permanentlyDenied) {
                        "Allow location access in Settings to use this device's position as the delivery destination."
                    } else {
                        "Ruwia will use your current position only to set the delivery destination."
                    },
                    color = NTColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationRationale = false
                        if (permanentlyDenied) {
                            locationPermissionController.openAppSettings()
                        } else {
                            locationPermissionController.requestPermission()
                        }
                    },
                ) {
                    Text(
                        if (permanentlyDenied) "Open settings" else "Continue",
                        color = NTColors.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) {
                    Text("Not now", color = NTColors.TextSecondary)
                }
            },
            containerColor = NTColors.Surface,
            shape = RoundedCornerShape(20.dp),
        )
    }
    var selectedProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var selectedEmployee by remember { mutableStateOf<EmployeeInfo?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var deliveryDateDisplay by remember { mutableStateOf(defaultSchedule.first) }
    var deliveryTimeDisplay by remember { mutableStateOf(defaultSchedule.second) }

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductMenu by remember { mutableStateOf(false) }
    var showEmployeeMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val deliveryDateDb = displayDateToDb(deliveryDateDisplay)
    val validationError = when {
        selectedCustomer == null -> "Select the customer receiving this delivery."
        selectedCustomer?.id.isNullOrBlank() || selectedCustomer?.id?.startsWith("local_") == true ->
            "This customer is still saving. Pick them again in a moment."
        selectedProduct == null -> "Select the product to deliver."
        quantity <= 0 -> "Enter a quantity of at least one case."
        selectedEmployee == null -> "Assign an employee to the route."
        isResolvingLocation || isUsingCurrentLocation -> "Wait for the destination location to finish resolving."
        mapsLocationInput.isNotBlank() && effectiveCoordinates == null ->
            "Resolve the destination link or enter valid GPS coordinates."
        deliveryDateDb == null -> "Choose a valid delivery date."
        deliveryTimeDisplay.isBlank() -> "Choose a delivery time."
        isDeliveryScheduleInPast(deliveryDateDisplay, deliveryTimeDisplay) ->
            "Choose a delivery date and time in the future."
        else -> null
    }
    // Show the field validation first; otherwise surface the last backend
    // failure so a failed save never disappears silently.
    val formError = validationError?.takeIf { attemptedSave }
        ?: errorMessage?.takeIf { it.isNotBlank() }

    fun saveTask() {
        attemptedSave = true
        val customer = selectedCustomer
        val product = selectedProduct
        val employee = selectedEmployee
        val date = deliveryDateDb
        if (validationError == null && customer != null && product != null && employee != null && date != null) {
            onSave(
                CreateDeliveryTaskInput(
                    customerId = customer.id.orEmpty(),
                    customerName = customer.name,
                    customerAddress = customer.address.orEmpty(),
                    customerPhone = customer.phone.orEmpty(),
                    productId = product.id,
                    productName = product.displayName.ifBlank { product.name },
                    quantity = quantity,
                    employeeId = employee.id,
                    employeeName = employee.name,
                    deliveryDate = date,
                    deliveryTime = deliveryTimeDisplay,
                    destLatitude = effectiveCoordinates?.first,
                    destLongitude = effectiveCoordinates?.second,
                    destName = destShopName.ifBlank { customer.name },
                    mapsLink = mapsLocationInput.ifBlank { null },
                )
            )
        }
    }

    SystemBackHandler { if (!isSaving) onBack() }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NTColors.Background)
                .padding(contentPadding),
        ) {
            NTPrimaryTopBar(
                title = "New delivery",
                subtitle = "Set up a customer drop-off",
                onBack = { if (!isSaving) onBack() },
                horizontalPadding = 16.dp,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FormSection(title = "Customer") {
                        DeliverySelectionField(
                            icon = Icons.Rounded.Person,
                            label = "Deliver to",
                            value = selectedCustomer?.name.orEmpty(),
                            supporting = selectedCustomer?.let {
                                listOfNotNull(it.phone, it.address).joinToString(" · ").ifBlank { null }
                            },
                            placeholder = "Select customer",
                            onClick = { showCustomerPicker = true },
                        )
                    }
                }

                item {
                    FormSection(title = "Delivery destination") {
                        OutlinedTextField(
                            value = destShopName,
                            onValueChange = { destShopName = it },
                            label = { Text("Shop Name") },
                            placeholder = { Text(selectedCustomer?.name?.ifBlank { "e.g. City Mart" } ?: "Shop Name") },
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = taskFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(10.dp))

                        AddressAutoCompleteField(
                            value = mapsLocationInput,
                            onValueChange = {
                                mapsLocationInput = it
                                currentLocationError = null
                                locationResolveError = null
                            },
                            label = "Delivery Address or GPS Location",
                            placeholder = "Type area, street, or paste Maps link…",
                            enabled = !isSaving,
                            onSuggestionSelected = { suggestion ->
                                mapsLocationInput = suggestion.fullAddress
                                resolvedCoordinates = Pair(suggestion.latitude, suggestion.longitude)
                                if (destShopName.isBlank()) {
                                    destShopName = suggestion.title
                                }
                                locationResolveError = null
                            },
                            trailingContent = {
                                if (isResolvingLocation || isUsingCurrentLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = NTColors.Primary
                                    )
                                } else if (effectiveCoordinates != null) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Location detected",
                                        tint = NTColors.Success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        // Directly visible interactive map layout on screen with red point
                        val mapLat = effectiveCoordinates?.first ?: 12.9716
                        val mapLng = effectiveCoordinates?.second ?: 77.5946

                        InteractiveMapPickerCard(
                            latitude = mapLat,
                            longitude = mapLng,
                            address = mapsLocationInput.ifBlank { destShopName },
                            height = 240.dp,
                            onLocationChanged = { lat, lng, addr ->
                                resolvedCoordinates = Pair(lat, lng)
                                mapsLocationInput = addr
                                if (destShopName.isBlank()) {
                                    destShopName = addr.substringBefore(",").ifBlank { selectedCustomer?.name.orEmpty() }
                                }
                                locationResolveError = null
                            },
                            onExpandFullScreen = {
                                showInteractiveMapDialog = true
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showInteractiveMapDialog = true },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NTColors.Primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.PrimaryDeep),
                            ) {
                                Icon(
                                    Icons.Rounded.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = NTColors.Primary,
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Expand Map",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (locationPermissionController.status == LocationPermissionStatus.GRANTED) {
                                        locationScope.launch { useCurrentLocation() }
                                    } else {
                                        showLocationRationale = true
                                    }
                                },
                                enabled = !isSaving && !isUsingCurrentLocation,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NTColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.TextSecondary),
                            ) {
                                if (isUsingCurrentLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = NTColors.Primary,
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = NTColors.Primary,
                                    )
                                }
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (isUsingCurrentLocation) "Locating…" else "GPS Location",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            if (effectiveCoordinates != null) {
                                OutlinedButton(
                                    onClick = {
                                        openMapNavigation(
                                            effectiveCoordinates.first,
                                            effectiveCoordinates.second,
                                            destShopName.ifBlank { selectedCustomer?.name ?: "Delivery destination" },
                                        )
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NTColors.Border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.TextSecondary),
                                ) {
                                    Icon(
                                        Icons.Rounded.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = NTColors.Primary,
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isResolvingLocation) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NTColors.Primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = NTColors.Primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Resolving location details…",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NTColors.Primary
                                )
                            }
                        } else if (effectiveCoordinates != null) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NTColors.SuccessLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = NTColors.Success,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                val latFormatted = ((effectiveCoordinates.first * 10000).toLong() / 10000.0).toString()
                                val lonFormatted = ((effectiveCoordinates.second * 10000).toLong() / 10000.0).toString()
                                Text(
                                    text = "Location set: $latFormatted, $lonFormatted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NTColors.Success
                                )
                            }
                        } else if (locationResolveError != null || currentLocationError != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = locationResolveError ?: currentLocationError.orEmpty(),
                                fontSize = 11.sp,
                                color = NTColors.Error
                            )
                        }
                    }
                }

                item {
                    FormSection(title = "Product and quantity") {
                        Box {
                            DeliverySelectionField(
                                icon = Icons.Rounded.ShoppingBag,
                                label = "Product",
                                value = selectedProduct?.let { it.displayName.ifBlank { it.name } }.orEmpty(),
                                supporting = selectedProduct?.let { "₹${formatTaskPrice(it.defaultSellPrice)} · ${it.stockAvailable} in stock" },
                                placeholder = "Select product",
                                onClick = { showProductMenu = true },
                            )
                            DropdownMenu(
                                expanded = showProductMenu,
                                onDismissRequest = { showProductMenu = false },
                                modifier = Modifier
                                    .background(NTColors.Surface)
                                    .heightIn(max = 360.dp)
                                    .widthIn(min = 280.dp),
                            ) {
                                if (sellableProducts.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No products available", color = NTColors.TextTertiary) },
                                        onClick = { showProductMenu = false },
                                        enabled = false,
                                    )
                                } else {
                                    sellableProducts.forEach { product ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        product.displayName.ifBlank { product.name },
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = NTColors.TextPrimary,
                                                    )
                                                    Text(
                                                        "${product.stockAvailable} in stock · ₹${formatTaskPrice(product.defaultSellPrice)}",
                                                        fontSize = 11.sp,
                                                        color = NTColors.TextTertiary,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedProduct = product
                                                showProductMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Quantity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NTColors.TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = { quantityText = (quantity - 1).coerceAtLeast(1).toString() },
                                enabled = !isSaving && quantity > 1,
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity", tint = NTColors.TextSecondary)
                            }
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { value ->
                                    if (value.length <= 4 && value.all { it.isDigit() }) quantityText = value
                                },
                                label = { Text("Cases") },
                                singleLine = true,
                                enabled = !isSaving,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = NTColors.TextPrimary,
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = formKeyboardActions(onDone = ::saveTask),
                                shape = RoundedCornerShape(14.dp),
                                colors = taskFieldColors(),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { quantityText = (quantity + 1).toString() },
                                enabled = !isSaving && quantity < 9999,
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase quantity", tint = NTColors.TextSecondary)
                            }
                        }
                        selectedProduct?.let { product ->
                            if (quantity > product.stockAvailable) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Only ${product.stockAvailable} cases are currently in stock. The task can still be scheduled.",
                                    fontSize = 11.sp,
                                    color = NTColors.Warning,
                                )
                            }
                        }
                    }
                }

                item {
                    FormSection(title = "Schedule") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScheduleButton(
                                icon = Icons.Rounded.CalendarToday,
                                label = deliveryDateDisplay,
                                enabled = !isSaving,
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                            )
                            ScheduleButton(
                                icon = Icons.Rounded.Schedule,
                                label = deliveryTimeDisplay,
                                enabled = !isSaving,
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The task is visible to the employee on its scheduled day.",
                            fontSize = 11.sp,
                            color = NTColors.TextTertiary,
                        )
                    }
                }

                item {
                    FormSection(title = "Assign delivery to") {
                        Box {
                            DeliverySelectionField(
                                icon = Icons.Rounded.Person,
                                label = "Delivery employee",
                                value = selectedEmployee?.name.orEmpty(),
                                supporting = selectedEmployee?.let {
                                    listOf(
                                        it.role.replaceFirstChar { c -> c.uppercase() },
                                        it.shopName,
                                        it.status.replace("_", " "),
                                    ).filter { part -> part.isNotBlank() }.joinToString(" · ")
                                },
                                placeholder = "Select employee",
                                onClick = { showEmployeeMenu = true },
                            )
                            DropdownMenu(
                                expanded = showEmployeeMenu,
                                onDismissRequest = { showEmployeeMenu = false },
                                modifier = Modifier
                                    .background(NTColors.Surface)
                                    .heightIn(max = 360.dp)
                                    .widthIn(min = 280.dp),
                            ) {
                                if (assignableEmployees.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No active employees", color = NTColors.TextTertiary) },
                                        onClick = { showEmployeeMenu = false },
                                        enabled = false,
                                    )
                                } else {
                                    assignableEmployees.forEach { employee ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        employee.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = NTColors.TextPrimary,
                                                    )
                                                    Text(
                                                        "${employee.role.replaceFirstChar { it.uppercase() }} · ${employee.shopName}",
                                                        fontSize = 11.sp,
                                                        color = NTColors.TextTertiary,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedEmployee = employee
                                                showEmployeeMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    DeliveryTaskSummary(
                        customerName = selectedCustomer?.name.orEmpty(),
                        productName = selectedProduct?.let { it.displayName.ifBlank { it.name } }.orEmpty(),
                        quantity = quantity,
                        employeeName = selectedEmployee?.name.orEmpty(),
                        deliveryDate = deliveryDateDisplay,
                        deliveryTime = deliveryTimeDisplay,
                        destination = when {
                            effectiveCoordinates != null ->
                                destShopName.ifBlank { "GPS destination selected" }
                            selectedCustomer?.address.orEmpty().isNotBlank() -> "Customer address"
                            else -> "No destination pin"
                        },
                    )
                }

            }

            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = NTColors.Surface,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, NTColors.Border),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (formError != null) {
                        Surface(
                            color = NTColors.ErrorLight,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = formError,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NTColors.ErrorText,
                            )
                        }
                    }
                    DeliveryPrimaryButton(
                        text = if (isSaving) "Creating delivery…" else "Create delivery",
                        icon = if (isSaving) null else Icons.AutoMirrored.Rounded.Assignment,
                        onClick = ::saveTask,
                        enabled = !isSaving && !isResolvingLocation && !isUsingCurrentLocation,
                        isLoading = isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (showCustomerPicker) {
            CustomerPickerOverlay(
                initialCustomers = savedCustomers,
                selectedCustomer = selectedCustomer,
                onDismiss = { showCustomerPicker = false },
                onSelect = { customer ->
                    selectedCustomer = customer
                    if (destShopName.isBlank()) {
                        destShopName = customer.name
                    }
                    if (mapsLocationInput.isBlank() && !customer.address.isNullOrBlank()) {
                        mapsLocationInput = customer.address
                    }
                },
                allowAddNew = false,
            )
        }

        if (showInteractiveMapDialog) {
            InteractiveMapLocationDialog(
                initialLatitude = effectiveCoordinates?.first,
                initialLongitude = effectiveCoordinates?.second,
                initialAddress = mapsLocationInput.ifBlank { destShopName },
                onConfirmLocation = { lat, lng, addr ->
                    resolvedCoordinates = Pair(lat, lng)
                    mapsLocationInput = addr
                    if (destShopName.isBlank()) {
                        destShopName = addr.substringBefore(",").ifBlank { selectedCustomer?.name.orEmpty() }
                    }
                    locationResolveError = null
                    showInteractiveMapDialog = false
                },
                onDismiss = { showInteractiveMapDialog = false }
            )
        }

        if (showDatePicker) {
            // DatePicker works in UTC-midnight dates; compare against the
            // user's local calendar day so "today" stays selectable in every
            // timezone.
            val todayLocal = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = deliveryDateDb?.let { raw ->
                    runCatching {
                        LocalDate.parse(raw).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    }.getOrNull()
                },
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val picked = Instant.fromEpochMilliseconds(utcTimeMillis)
                            .toLocalDateTime(TimeZone.UTC).date
                        return picked >= todayLocal
                    }
                },
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date
                                deliveryDateDisplay = formatDisplayDate(date)
                            }
                            showDatePicker = false
                        },
                    ) {
                        Text("OK", color = NTColors.Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = NTColors.TextSecondary)
                    }
                },
                colors = deliveryDatePickerColors(),
            ) {
                DatePicker(
                    state = pickerState,
                    colors = deliveryDatePickerColors(),
                )
            }
        }

        if (showTimePicker) {
            val (initialHour, initialMinute) = remember(deliveryTimeDisplay) {
                parseDisplayTime(deliveryTimeDisplay)
            }
            val timeState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute,
                is24Hour = false,
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Delivery time", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(
                            state = timeState,
                            colors = deliveryTimePickerColors(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deliveryTimeDisplay = formatDisplayTime(timeState.hour, timeState.minute)
                            showTimePicker = false
                        },
                    ) {
                        Text("OK", color = NTColors.Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel", color = NTColors.TextSecondary)
                    }
                },
                containerColor = NTColors.Surface,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextTertiary,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun DeliverySelectionField(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String,
    supporting: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = NTColors.Primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextTertiary)
            Spacer(Modifier.height(1.dp))
            Text(
                text = value.ifBlank { placeholder },
                fontSize = 14.sp,
                fontWeight = if (value.isBlank()) FontWeight.Medium else FontWeight.Bold,
                color = if (value.isBlank()) NTColors.TextTertiary else NTColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!supporting.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = supporting,
                    fontSize = 11.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = NTColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ScheduleButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = NTColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) NTColors.TextPrimary else NTColors.TextDisabled,
        )
    }
}

@Composable
private fun DeliveryTaskSummary(
    customerName: String,
    productName: String,
    quantity: Int,
    employeeName: String,
    deliveryDate: String,
    deliveryTime: String,
    destination: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.PrimaryLight.copy(alpha = 0.55f))
            .border(1.dp, NTColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            "DELIVERY SUMMARY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = NTColors.Primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                customerName.isBlank() -> "Select a customer to preview this delivery."
                productName.isBlank() -> "$customerName · choose a product"
                else -> "$quantity × $productName for $customerName"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = listOf(
                deliveryDate,
                deliveryTime,
                employeeName.ifBlank { "No employee assigned" },
            ).joinToString(" · "),
            fontSize = 12.sp,
            color = NTColors.TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                destination,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NTColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun taskFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NTColors.Primary,
    unfocusedBorderColor = NTColors.Border,
    focusedTextColor = NTColors.TextPrimary,
    unfocusedTextColor = NTColors.TextPrimary,
    focusedContainerColor = NTColors.Surface,
    unfocusedContainerColor = NTColors.Surface,
)

@Composable
private fun deliveryDatePickerColors() = DatePickerDefaults.colors(
    containerColor = NTColors.Surface,
    titleContentColor = NTColors.TextSecondary,
    headlineContentColor = NTColors.TextPrimary,
    weekdayContentColor = NTColors.TextSecondary,
    subheadContentColor = NTColors.TextPrimary,
    navigationContentColor = NTColors.TextSecondary,
    yearContentColor = NTColors.TextPrimary,
    currentYearContentColor = NTColors.Primary,
    selectedYearContentColor = Color.White,
    selectedYearContainerColor = NTColors.Primary,
    dayContentColor = NTColors.TextPrimary,
    disabledDayContentColor = NTColors.TextDisabled,
    selectedDayContentColor = Color.White,
    disabledSelectedDayContentColor = Color.White.copy(alpha = 0.7f),
    selectedDayContainerColor = NTColors.Primary,
    disabledSelectedDayContainerColor = NTColors.Primary.copy(alpha = 0.4f),
    todayContentColor = NTColors.Primary,
    todayDateBorderColor = NTColors.Primary,
    dayInSelectionRangeContentColor = NTColors.TextPrimary,
    dayInSelectionRangeContainerColor = NTColors.PrimaryLight,
    dividerColor = NTColors.Divider,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun deliveryTimePickerColors() = TimePickerDefaults.colors(
    clockDialColor = NTColors.SurfaceVar,
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = NTColors.TextPrimary,
    selectorColor = NTColors.Primary,
    containerColor = NTColors.Surface,
    periodSelectorBorderColor = NTColors.Border,
    periodSelectorSelectedContainerColor = NTColors.Primary,
    periodSelectorUnselectedContainerColor = NTColors.SurfaceVar,
    periodSelectorSelectedContentColor = Color.White,
    periodSelectorUnselectedContentColor = NTColors.TextSecondary,
    timeSelectorSelectedContainerColor = NTColors.PrimaryLight,
    timeSelectorUnselectedContainerColor = NTColors.SurfaceVar,
    timeSelectorSelectedContentColor = NTColors.Primary,
    timeSelectorUnselectedContentColor = NTColors.TextSecondary,
)

private fun formatTaskPrice(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    val whole = rounded.toInt()
    val paise = kotlin.math.round((rounded - whole) * 100).toInt()
    return if (paise == 0) whole.toString() else "$whole.${paise.toString().padStart(2, '0')}"
}

private fun formatDisplayDate(date: LocalDate): String {
    val day = date.day.toString().padStart(2, '0')
    val month = (date.month.ordinal + 1).toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

private fun formatDisplayTime(hour: Int, minute: Int): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val ampm = if (hour >= 12) "PM" else "AM"
    return "$h:${minute.toString().padStart(2, '0')} $ampm"
}

private fun parseDisplayTime(value: String): Pair<Int, Int> {
    return try {
        val parts = value.trim().split(" ")
        if (parts.size != 2) return 12 to 0
        val hm = parts[0].split(":")
        var hour = hm.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = hm.getOrNull(1)?.toIntOrNull() ?: 0
        val ampm = parts[1].uppercase()
        if (ampm != "AM" && ampm != "PM") return 12 to 0
        hour = when {
            ampm == "AM" && hour == 12 -> 0
            ampm == "PM" && hour < 12 -> hour + 12
            else -> hour
        }
        hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    } catch (_: Exception) {
        12 to 0
    }
}

private fun isDeliveryScheduleInPast(dateDisplay: String, timeDisplay: String): Boolean {
    val dbDate = displayDateToDb(dateDisplay) ?: return false
    val date = runCatching { LocalDate.parse(dbDate) }.getOrNull() ?: return false
    val (hour, minute) = parseDisplayTime(timeDisplay)
    val scheduled = LocalDateTime(date, LocalTime(hour, minute))
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return scheduled < now
}

internal fun DeliveryTask.normalizedStatus(): String =
    when (val raw = status.trim().lowercase()) {
        "" -> "pending"
        "done" -> "completed"
        else -> raw
    }

internal fun DeliveryTask.isTaskClosed(): Boolean =
    normalizedStatus() == "completed" || normalizedStatus() == "cancelled"

internal fun deliveryTaskStatusLabel(task: DeliveryTask): String =
    task.normalizedStatus().replace("_", " ").uppercase()

internal fun deliveryTaskProductLabel(task: DeliveryTask): String {
    val cases = "${task.canQty} Case${if (task.canQty == 1) "" else "s"}"
    val product = task.productName?.takeIf { it.isNotBlank() }
    return if (product != null) "$cases · $product" else cases
}

internal fun deliveryTaskScheduleLabel(task: DeliveryTask): String {
    val rawDate = deliveryTaskDateKey(task)
    if (rawDate.isBlank()) return "Date not set"
    val date = dbToDisplayDate(rawDate)
    val time = task.etaText.takeIf { it.isNotBlank() }
    return if (time != null) "$date · $time" else date
}

private fun deliveryTaskDateKey(task: DeliveryTask): String =
    task.scheduledDate?.take(10)?.takeIf { it.isNotBlank() }
        ?: task.deliveryDate?.take(10)?.takeIf { it.isNotBlank() }
        ?: ""

private fun deliveryTaskTimeSortKey(raw: String): Int {
    if (raw.isBlank()) return Int.MAX_VALUE
    val (hour, minute) = parseDisplayTime(raw)
    return hour * 60 + minute
}

private val deliveryTaskComparator: Comparator<DeliveryTask> =
    compareBy<DeliveryTask> { deliveryTaskDateKey(it) }
        .thenBy { deliveryTaskTimeSortKey(it.etaText) }

internal fun List<DeliveryTask>.nextScheduledTask(): DeliveryTask? =
    filter { !it.isTaskClosed() }.minWithOrNull(deliveryTaskComparator)
