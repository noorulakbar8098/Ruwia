package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.theme.RuwiaColor
import com.example.ruwia.util.capitalizeWords
import com.example.ruwia.util.isTextField

// ── Two modes inside the sheet ────────────────────────────────────────────────

private enum class SheetMode { Pick, AddNew }

// ── Public: full-screen overlay composable ────────────────────────────────────
// Place inside a Box(Modifier.fillMaxSize()) in the calling screen.

@Composable
fun CustomerPickerOverlay(
    initialCustomers: List<Customer>,
    selectedCustomer: Customer?,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit,
    onNewCustomer: (Customer) -> Unit = {},
) {
    var mode by remember { mutableStateOf(SheetMode.Pick) }
    var search by remember { mutableStateOf("") }
    var newName     by remember { mutableStateOf("") }
    var newPhone    by remember { mutableStateOf("") }
    var newLocation by remember { mutableStateOf("") }
    var newOther    by remember { mutableStateOf("") }
    var allCustomers by remember(initialCustomers) { mutableStateOf(initialCustomers) }

    val filtered = remember(search, allCustomers) {
        if (search.isBlank()) allCustomers
        else allCustomers.filter {
            it.name.contains(search, ignoreCase = true) ||
            (it.phone  ?: "").contains(search) ||
            (it.address ?: "").contains(search, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable { if (mode == SheetMode.Pick) onDismiss() },
        )

        // Sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(RuwiaColor.Surface)
                .navigationBarsPadding(),
        ) {
            // Handle bar
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .background(RuwiaColor.Divider, RoundedCornerShape(2.dp)),
                )
            }

            when (mode) {
                SheetMode.Pick -> PickContent(
                    customers        = filtered,
                    selectedCustomer = selectedCustomer,
                    search           = search,
                    onSearchChange   = { search = it },
                    onSelect         = { onSelect(it); onDismiss() },
                    onAddNew         = { mode = SheetMode.AddNew },
                )
                SheetMode.AddNew -> AddNewContent(
                    name           = newName,
                    phone          = newPhone,
                    location       = newLocation,
                    other          = newOther,
                    onNameChange   = { newName = it },
                    onPhoneChange  = { if (it.length <= 15 && it.all { c -> c.isDigit() || c == '+' }) newPhone = it },
                    onLocChange    = { newLocation = it },
                    onOtherChange  = { newOther = it },
                    onBack         = { mode = SheetMode.Pick },
                    onSave         = {
                        // The server-side row will carry the real UUID; we
                        // build a draft (id = null) and let the repo round-trip
                        // it. The selected customer in this picker only needs
                        // a stable name — actual outward sales reference the
                        // name string, not the local id.
                        val draft = Customer(
                            id           = null,
                            name         = newName.trim(),
                            phone        = newPhone.trim().ifEmpty { null },
                            address      = newLocation.trim().ifEmpty { null },
                            otherDetails = newOther.trim().ifEmpty { null },
                        )
                        // Optimistically add to the picker so the next render
                        // already shows them; the real DB row arrives via the
                        // periodic refresh and replaces this entry by name.
                        allCustomers = allCustomers + draft
                        onNewCustomer(draft)
                        onSelect(draft)
                        onDismiss()
                    },
                )
            }
        }
    }
}

// ── Public: picker trigger row (Section 2 in sale/inward screens) ─────────────

@Composable
fun CustomerPickerRow(
    selectedCustomer: Customer?,
    onClick: () -> Unit,
) {
    val isSelected = selectedCustomer != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) RuwiaColor.TealExtraLight.copy(alpha = 0.45f) else RuwiaColor.Background,
                RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) RuwiaColor.TealLight else RuwiaColor.Divider,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar initial or person icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (isSelected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Text(
                    text       = selectedCustomer!!.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            } else {
                Icon(Icons.Rounded.Person, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (isSelected) {
                Text(
                    text       = selectedCustomer!!.name,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = RuwiaColor.TextPrimary,
                )
                val sub = listOfNotNull(selectedCustomer.phone, selectedCustomer.address)
                    .joinToString("  ·  ")
                if (sub.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(sub, fontSize = 11.sp, color = RuwiaColor.TextSecondary)
                }
            } else {
                Text("Select or add customer", fontSize = 14.sp, color = RuwiaColor.TextMuted)
            }
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint     = RuwiaColor.TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Pick mode ─────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.PickContent(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Customer) -> Unit,
    onAddNew: () -> Unit,
) {
    // Header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.People, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Select customer",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = RuwiaColor.TextPrimary,
            modifier   = Modifier.weight(1f),
        )
        // + New chip
        Row(
            modifier = Modifier
                .background(RuwiaColor.TealPrimary, RoundedCornerShape(10.dp))
                .clickable(onClick = onAddNew)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    // Search bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(RuwiaColor.LightGray, RoundedCornerShape(12.dp))
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = RuwiaColor.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (search.isEmpty()) {
                Text("Search by name, phone or location…", fontSize = 13.sp, color = RuwiaColor.TextMuted)
            }
            BasicTextField(
                value         = search,
                onValueChange = onSearchChange,
                textStyle     = TextStyle(fontSize = 14.sp, color = RuwiaColor.TextPrimary),
                cursorBrush   = SolidColor(RuwiaColor.TealPrimary),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
        }
        if (search.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Close, null,
                tint     = RuwiaColor.TextSecondary,
                modifier = Modifier.size(16.dp).clickable { onSearchChange("") },
            )
        }
    }

    if (customers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(RuwiaColor.TealExtraLight, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("No customer found", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("Tap + New to add a new customer", fontSize = 13.sp, color = RuwiaColor.TextMuted)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(customers) { customer ->
                val isSelected = customer.id == selectedCustomer?.id
                CustomerListRow(
                    customer   = customer,
                    isSelected = isSelected,
                    onClick    = { onSelect(customer) },
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 20.dp),
                    color     = RuwiaColor.Divider.copy(alpha = 0.5f),
                    thickness = 0.6.dp,
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun CustomerListRow(customer: Customer, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) RuwiaColor.TealExtraLight.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = customer.name.firstOrNull()?.uppercase() ?: "?",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isSelected) Color.White else RuwiaColor.TealPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = customer.name,
                fontSize   = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color      = RuwiaColor.TextPrimary,
            )
            val details = listOfNotNull(customer.phone, customer.address)
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    customer.phone?.let { ph ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Phone, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(ph, fontSize = 11.sp, color = RuwiaColor.TextMuted)
                        }
                    }
                    customer.address?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(loc, fontSize = 11.sp, color = RuwiaColor.TextMuted)
                        }
                    }
                }
            }
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

// ── Add new mode ──────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.AddNewContent(
    name: String, phone: String, location: String, other: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLocChange: (String) -> Unit,
    onOtherChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = name.isNotBlank()

    // Header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(RuwiaColor.Background, RoundedCornerShape(9.dp))
                .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(9.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = RuwiaColor.TextPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("New customer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
            Text("Fill in basic contact details", fontSize = 11.sp, color = RuwiaColor.TextMuted)
        }
    }

    HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.6f))

    // Form
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AddCustomerField(
            label       = "Customer name",
            required    = true,
            value       = name,
            onChange    = onNameChange,
            placeholder = "e.g. Murugan Stores",
            icon        = Icons.Rounded.Person,
            keyboardType = KeyboardType.Text,
        )
        AddCustomerField(
            label       = "Phone number",
            required    = false,
            value       = phone,
            onChange    = onPhoneChange,
            placeholder = "10-digit mobile number",
            icon        = Icons.Rounded.Phone,
            keyboardType = KeyboardType.Phone,
        )
        AddCustomerField(
            label       = "Location",
            required    = false,
            value       = location,
            onChange    = onLocChange,
            placeholder = "Area / street / landmark",
            icon        = Icons.Rounded.LocationOn,
            keyboardType = KeyboardType.Text,
        )
        AddCustomerField(
            label       = "Other details",
            required    = false,
            value       = other,
            onChange    = onOtherChange,
            placeholder = "Delivery preference, notes, etc.",
            icon        = Icons.Rounded.Notes,
            keyboardType = KeyboardType.Text,
            multiline   = true,
        )
    }

    // Save
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Button(
            onClick  = onSave,
            enabled  = isSaveEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor          = RuwiaColor.TealDark,
                contentColor            = Color.White,
                disabledContainerColor  = RuwiaColor.TextMuted.copy(alpha = 0.30f),
                disabledContentColor    = Color.White.copy(alpha = 0.5f),
            ),
        ) {
            Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add & select customer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddCustomerField(
    label: String,
    required: Boolean,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    multiline: Boolean = false,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextSecondary)
            if (required) Text(" *", fontSize = 12.sp, color = Color.Red.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RuwiaColor.LightGray, RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (value.isNotEmpty()) RuwiaColor.TealLight else RuwiaColor.Divider,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(
                    horizontal = 14.dp,
                    vertical   = if (multiline) 13.dp else 14.dp,
                ),
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
        ) {
            Icon(
                icon, null,
                tint     = RuwiaColor.TextPrimary,
                modifier = Modifier
                    .size(16.dp)
                    .let { if (multiline) it.padding(top = 2.dp) else it },
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 13.sp, color = RuwiaColor.TextMuted)
                }
                BasicTextField(
                    value            = value,
                    onValueChange    = if (keyboardType.isTextField()) { { v -> onChange(capitalizeWords(v)) } } else onChange,
                    textStyle        = TextStyle(
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = RuwiaColor.TextPrimary,
                    ),
                    cursorBrush      = SolidColor(RuwiaColor.TealPrimary),
                    singleLine       = !multiline,
                    maxLines         = if (multiline) 4 else 1,
                    keyboardOptions  = KeyboardOptions(
                        keyboardType  = keyboardType,
                        capitalization = if (keyboardType.isTextField()) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                    ),
                    modifier         = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
