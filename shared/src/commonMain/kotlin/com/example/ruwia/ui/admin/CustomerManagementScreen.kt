package com.example.ruwia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.Customer
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp

/**
 * Admin-side customer directory. Lists every customer in the system and lets
 * the admin add new ones inline. RLS already exposes `customers` to all
 * authenticated users so anything saved here is immediately visible to every
 * employee in their Add Sale picker — fulfilling the cross-app sync expectation.
 */
@Composable
fun CustomerManagementScreen(
    customers: List<Customer>,
    onAddCustomer: (Customer) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(search, customers) {
        if (search.isBlank()) customers
        else customers.filter {
            it.name.contains(search, ignoreCase = true) ||
            (it.phone   ?: "").contains(search) ||
            (it.address ?: "").contains(search, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Scaffold(
            containerColor = NTColors.Background,
            topBar = { CustomerTopBar(onBack = onBack, count = customers.size) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm)
                        .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                        .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Search, null,
                        tint = NTColors.TextTertiary, modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text(
                                "Search by name, phone or address",
                                fontSize = 13.sp, color = NTColors.TextTertiary,
                            )
                        }
                        BasicTextField(
                            value         = search,
                            onValueChange = { search = it },
                            textStyle     = TextStyle(fontSize = 14.sp, color = NTColors.TextPrimary),
                            cursorBrush   = SolidColor(NTColors.Primary),
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                    if (search.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close, null,
                            tint = NTColors.TextTertiary,
                            modifier = Modifier.size(16.dp).clickable { search = "" },
                        )
                    }
                }

                if (customers.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(NTColors.PrimaryLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Group, null, tint = NTColors.Primary, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(NTDp.lg))
                        Text("No customers yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                        Spacer(Modifier.height(NTDp.sm))
                        Text(
                            "Tap the + button to add your first customer. They'll be visible to every employee instantly.",
                            fontSize = 13.sp, color = NTColors.TextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = NTDp.screenPad,
                            vertical   = NTDp.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(NTDp.sm),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NTColors.PrimaryLight, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Info, null, tint = NTColors.Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Customers added here are visible to all employees in the Add Sale picker.",
                                    fontSize = 12.sp, color = NTColors.Primary, lineHeight = 16.sp,
                                )
                            }
                        }
                        items(filtered, key = { it.id ?: it.name.hashCode().toString() }) { c ->
                            CustomerRow(c)
                        }
                        item {
                            Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 80.dp))
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = { showAddSheet = true },
            containerColor = NTColors.Primary,
            contentColor   = Color.White,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        ) {
            Icon(Icons.Rounded.Add, "Add customer", modifier = Modifier.size(26.dp))
        }

        if (showAddSheet) {
            CustomerFormDialog(
                onDismiss = { showAddSheet = false },
                onSave    = { name, phone, address, other ->
                    onAddCustomer(
                        Customer(
                            id           = null,
                            name         = name,
                            phone        = phone,
                            address      = address,
                            otherDetails = other,
                        )
                    )
                    showAddSheet = false
                },
            )
        }
    }
}

@Composable
private fun CustomerTopBar(onBack: () -> Unit, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp)
                .background(NTColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }

        Text(
            "Customers", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center),
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(NTDp.radFull))
                .background(NTColors.PrimaryLight)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .align(Alignment.CenterEnd),
        ) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.Primary)
        }
    }
}

@Composable
private fun CustomerRow(customer: Customer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .padding(NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(NTDp.radMd))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                customer.name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = NTColors.Primary,
            )
        }
        Spacer(Modifier.width(NTDp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                customer.name,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(
                customer.phone?.takeIf { it.isNotBlank() },
                customer.address?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    fontSize = 12.sp, color = NTColors.TextTertiary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (customer.cansHeld > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(NTColors.WarningLight)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "${customer.cansHeld} cans",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = NTColors.Warning,
                )
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String?, address: String?, other: String?) -> Unit,
) {
    var name    by remember { mutableStateOf("") }
    var phone   by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var other   by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NTColors.Surface, RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Text(
                "Add customer",
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Saved customers are visible in the admin and employee apps.",
                fontSize = 12.sp, color = NTColors.TextTertiary,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Customer name *")
            CustomerFormInput(
                value = name, onValueChange = { name = it },
                placeholder = "e.g. Murugan Stores",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Phone")
            CustomerFormInput(
                value = phone,
                onValueChange = {
                    if (it.length <= 15 && it.all { c -> c.isDigit() || c == '+' }) phone = it
                },
                placeholder = "10-digit mobile",
                keyboardType = KeyboardType.Phone,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Address")
            CustomerFormInput(
                value = address, onValueChange = { address = it },
                placeholder = "Area / street / landmark",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Notes")
            CustomerFormInput(
                value = other, onValueChange = { other = it },
                placeholder = "Delivery preferences, billing, etc.",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            phone.trim().ifEmpty { null },
                            address.trim().ifEmpty { null },
                            other.trim().ifEmpty { null },
                        )
                    },
                    enabled  = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerFormFieldLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = NTColors.TextTertiary, letterSpacing = 0.4.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CustomerFormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = NTColors.TextDisabled)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary),
            cursorBrush = SolidColor(NTColors.Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

