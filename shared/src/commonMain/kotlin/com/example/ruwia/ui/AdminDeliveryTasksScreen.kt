package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ruwia.util.dbToDisplayDate
import com.example.ruwia.util.displayDateToDb
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
)

// ── Delivery task board ───────────────────────────────────────────────────────

@Composable
fun AdminDeliveryTasksScreen(
    tasks: List<DeliveryTask>,
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    onUpdateStatus: (taskId: String, status: String) -> Unit,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    SystemBackHandler(onBack)

    var taskToCancel by remember { mutableStateOf<DeliveryTask?>(null) }

    val sorted = remember(tasks) { tasks.sortedWith(deliveryTaskComparator) }
    val openTasks = sorted.filter { !it.isTaskClosed() }
    val closedTasks = sorted.filter { it.isTaskClosed() }
    val completedCount = tasks.count { it.normalizedStatus() == "completed" }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskMetric(
                        label = "OPEN",
                        value = openTasks.size.toString(),
                        color = NTColors.Warning,
                        modifier = Modifier.weight(1f),
                    )
                    TaskMetric(
                        label = "COMPLETED",
                        value = completedCount.toString(),
                        color = NTColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Button(
                    onClick = onCreateTask,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "New delivery task",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
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

            if (tasks.isEmpty()) {
                item { EmptyDeliveryTasks() }
            } else {
                if (openTasks.isNotEmpty()) {
                    item { TaskSectionLabel("OPEN DELIVERIES") }
                    items(openTasks) { task ->
                        AdminDeliveryTaskCard(
                            task = task,
                            onComplete = { onUpdateStatus(task.id, "completed") },
                            onCancel = { taskToCancel = task },
                        )
                    }
                }
                if (closedTasks.isNotEmpty()) {
                    item { TaskSectionLabel("CLOSED") }
                    items(closedTasks) { task ->
                        AdminDeliveryTaskCard(
                            task = task,
                            onComplete = {},
                            onCancel = {},
                        )
                    }
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
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = NTColors.TextTertiary,
        modifier = Modifier.padding(top = 10.dp, start = 2.dp),
    )
}

@Composable
private fun EmptyDeliveryTasks() {
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
        Text("No delivery tasks yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Create a task to assign a product delivery to a customer.",
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
        else -> NTColors.Info
    }
    val statusBackground = when (status) {
        "completed" -> NTColors.SuccessLight
        "cancelled" -> NTColors.ErrorLight
        "pending" -> NTColors.WarningLight
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
                text = deliveryTaskStatusLabel(task),
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

        if (!closed) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NTColors.Error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onComplete) {
                    Text("Mark completed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NTColors.Success)
                }
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

    SystemBackHandler { if (!isSaving) onBack() }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NTColors.Background)
                .padding(contentPadding),
        ) {
            NTPrimaryTopBar(
                title = "New delivery task",
                subtitle = "Customer · product · schedule · route",
                onBack = { if (!isSaving) onBack() },
                horizontalPadding = 16.dp,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FormSection(title = "CUSTOMER") {
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
                    FormSection(title = "PRODUCT") {
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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    FormSection(title = "SCHEDULE") {
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
                    FormSection(title = "ASSIGN TO") {
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
                    )
                }

                if (formError != null) {
                    item {
                        Surface(
                            color = NTColors.ErrorLight,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = formError,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NTColors.ErrorText,
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
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
                                    )
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                    ) {
                        if (isSaving) {
                            Text("Saving task…", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Create delivery task", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        if (showCustomerPicker) {
            CustomerPickerOverlay(
                initialCustomers = savedCustomers,
                selectedCustomer = selectedCustomer,
                onDismiss = { showCustomerPicker = false },
                onSelect = { selectedCustomer = it },
                allowAddNew = false,
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
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
