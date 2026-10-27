package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Recycling
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.theme.RuwiaColor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Customer-aware "receive empty cans" flow shared by the employee and admin
 * apps. Lists only customers with an outstanding 20L balance, supports a full
 * settle shortcut, and uses a stable reference key so retries/double taps
 * cannot apply the same return twice.
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun ReceiveEmptyCansDialog(
    customers: List<Customer>,
    shopOptions: List<String>,
    initialShop: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (customerId: String, qty: Int, referenceKey: String, shopName: String) -> Unit,
) {
    val eligible = remember(customers) {
        customers
            .filter { !it.id.isNullOrBlank() && it.cansHeld > 0 }
            .sortedBy { it.name.lowercase() }
    }
    val shops = remember(shopOptions, initialShop) {
        (shopOptions + initialShop)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    val selected = remember(selectedCustomerId, eligible) {
        eligible.firstOrNull { it.id == selectedCustomerId }
    }
    var qtyText by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var selectedShop by remember {
        mutableStateOf(initialShop.trim().ifBlank { shops.firstOrNull().orEmpty() })
    }
    val referenceKey = remember { "return:" + Uuid.random().toString() }

    // Shops can arrive after the dialog opens (backend refresh). Keep the
    // selector pointing at a real option instead of silently saving to the
    // repository fallback.
    LaunchedEffect(shops) {
        if (selectedShop.isBlank() || selectedShop !in shops) {
            selectedShop = shops.firstOrNull().orEmpty()
        }
    }

    // A background refresh may remove the selected customer (another device
    // settled it). Never keep a stale selection that could over-return.
    LaunchedEffect(eligible) {
        if (selectedCustomerId != null && eligible.none { it.id == selectedCustomerId }) {
            selectedCustomerId = null
            qtyText = ""
        }
    }

    val qty = qtyText.toIntOrNull()
    val outstanding = selected?.cansHeld ?: 0
    val remaining = (outstanding - (qty ?: 0)).coerceAtLeast(0)
    val isValid = selected != null &&
        qty != null &&
        qty in 1..outstanding &&
        !isSaving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = RuwiaColor.Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    "Empty Cans Received",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RuwiaColor.TextPrimary,
                )
                Text(
                    "Customer 20L can balance",
                    fontSize = 12.sp,
                    color = RuwiaColor.TextMuted,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (shops.size > 1) {
                    Text("Stock returned at", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        shops.forEach { shop ->
                            val isSelected = shop == selectedShop
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) RuwiaColor.TealPrimary else RuwiaColor.LightGray)
                                    .border(
                                        1.dp,
                                        if (isSelected) RuwiaColor.TealPrimary else RuwiaColor.Divider,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable(enabled = !isSaving) { selectedShop = shop }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    shop,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else RuwiaColor.TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RuwiaColor.LightGray)
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSaving) { menuOpen = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = RuwiaColor.TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selected?.name ?: "Select customer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected != null) RuwiaColor.TextPrimary else RuwiaColor.TextMuted,
                            )
                            Text(
                                selected?.let { "${it.cansHeld} outstanding 20L cans" }
                                    ?: "Only customers holding 20L cans",
                                fontSize = 11.sp,
                                color = RuwiaColor.TextMuted,
                            )
                        }
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = RuwiaColor.TextSecondary)
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        if (eligible.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No customers have outstanding 20L cans", color = RuwiaColor.TextMuted) },
                                onClick = {},
                                enabled = false,
                            )
                        } else {
                            eligible.forEach { customer ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(customer.name, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                                            Text(
                                                "${customer.cansHeld} outstanding",
                                                fontSize = 11.sp,
                                                color = RuwiaColor.TextMuted,
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomerId = customer.id
                                        qtyText = ""
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (eligible.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No customers currently hold 20L cans. Balances refresh automatically.",
                        fontSize = 12.sp,
                        color = RuwiaColor.TextMuted,
                    )
                }

                if (selected != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RuwiaColor.TealExtraLight, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Outstanding 20L cans", fontSize = 12.sp, color = RuwiaColor.TextSecondary, modifier = Modifier.weight(1f))
                        Text("$outstanding", fontSize = 20.sp, fontWeight = FontWeight.Black, color = RuwiaColor.TealDark)
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Empty 20L Cans Received",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RuwiaColor.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { qtyText = outstanding.toString() },
                            enabled = !isSaving && outstanding > 0,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("Settle full balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TealPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RuwiaColor.LightGray, RoundedCornerShape(12.dp))
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Recycling, null, tint = RuwiaColor.TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (qtyText.isEmpty()) {
                                Text("0", fontSize = 16.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Bold)
                            }
                            BasicTextField(
                                value = qtyText,
                                onValueChange = { newVal ->
                                    if (newVal.length <= 5 && newVal.all { it.isDigit() }) {
                                        qtyText = newVal
                                    }
                                },
                                enabled = !isSaving,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RuwiaColor.TextPrimary,
                                ),
                                cursorBrush = SolidColor(RuwiaColor.TealPrimary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text("Cans", fontSize = 11.sp, color = RuwiaColor.TextSecondary)
                    }

                    if (qty != null && qty > outstanding) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cannot return more than $outstanding outstanding cans.",
                            fontSize = 11.sp,
                            color = Color(0xFFC0392B),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Remaining outstanding",
                            fontSize = 12.sp,
                            color = RuwiaColor.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "$remaining cans",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = RuwiaColor.TealDark,
                        )
                    }

                    if (selectedShop.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Returned cans are added to $selectedShop's empty stock.",
                            fontSize = 11.sp,
                            color = RuwiaColor.TextMuted,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val customer = selected ?: return@TextButton
                    val id = customer.id ?: return@TextButton
                    val count = qty ?: return@TextButton
                    onConfirm(id, count, referenceKey, selectedShop)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = RuwiaColor.TealPrimary),
            ) {
                Text(if (isSaving) "Saving…" else "Record Return", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancel", color = RuwiaColor.TextSecondary)
            }
        },
    )
}
