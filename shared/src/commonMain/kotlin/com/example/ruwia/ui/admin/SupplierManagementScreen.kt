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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.Supplier
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp

/**
 * Supplier management screen — admin can add new suppliers, view the list and
 * soft-delete entries that are no longer in use.
 *
 * The DB schema already enforces RLS so the rows added here are visible to
 * employees through their own [com.example.ruwia.data.EmployeeRepository.getSuppliers]
 * call — adding a supplier here propagates to every employee's Add Inward
 * picker on next dashboard refresh.
 */
@Composable
fun SupplierManagementScreen(
    suppliers: List<Supplier>,
    onAddSupplier: (name: String, location: String?) -> Unit,
    onDeleteSupplier: (id: String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var deletingSupplier by remember { mutableStateOf<Supplier?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Scaffold(
            containerColor = NTColors.Background,
            topBar = { SupplierTopBar(onBack = onBack, count = suppliers.size) },
        ) { padding ->
            if (suppliers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(NTColors.PrimaryLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Store, null, tint = NTColors.Primary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(NTDp.lg))
                    Text("No suppliers yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                    Spacer(Modifier.height(NTDp.sm))
                    Text(
                        "Add your first supplier so employees can pick them when logging stock inward.",
                        fontSize = 13.sp, color = NTColors.TextTertiary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        horizontal = NTDp.screenPad,
                        vertical   = NTDp.md,
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
                                "Suppliers added here are visible to all employees in the Add Inward picker.",
                                fontSize = 12.sp, color = NTColors.Primary, lineHeight = 16.sp,
                            )
                        }
                    }
                    items(suppliers, key = { it.id ?: it.name.hashCode().toString() }) { sup ->
                        SupplierRow(
                            supplier = sup,
                            onDelete = { deletingSupplier = sup },
                        )
                    }
                    item {
                        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 80.dp))
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
            Icon(Icons.Rounded.Add, "Add supplier", modifier = Modifier.size(26.dp))
        }

        if (showAddSheet) {
            SupplierFormDialog(
                onDismiss = { showAddSheet = false },
                onSave    = { name, loc ->
                    onAddSupplier(name, loc)
                    showAddSheet = false
                },
            )
        }

        deletingSupplier?.let { sup ->
            AlertDialog(
                onDismissRequest = { deletingSupplier = null },
                icon  = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error) },
                title = { Text("Remove ${sup.name}?", fontWeight = FontWeight.Bold) },
                text  = {
                    Text(
                        "This supplier will no longer appear in pickers. Existing stock movements " +
                        "that reference them keep working.",
                        fontSize = 13.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sup.id?.let(onDeleteSupplier)
                            deletingSupplier = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Error),
                    ) { Text("Remove", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletingSupplier = null }) { Text("Cancel") }
                },
                containerColor = NTColors.Surface,
            )
        }
    }
}

@Composable
private fun SupplierTopBar(onBack: () -> Unit, count: Int) {
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
            "Suppliers", fontSize = 17.sp, fontWeight = FontWeight.Bold,
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
private fun SupplierRow(supplier: Supplier, onDelete: () -> Unit) {
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
            Icon(Icons.Rounded.Store, null, tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconMd))
        }
        Spacer(Modifier.width(NTDp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                supplier.name,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!supplier.location.isNullOrBlank()) {
                Text(
                    supplier.location,
                    fontSize = 12.sp, color = NTColors.TextTertiary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NTColors.ErrorLight)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Delete, "Delete", tint = NTColors.Error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SupplierFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, location: String?) -> Unit,
) {
    var name     by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NTColors.Surface, RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Text("Add supplier", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Both admin and employees will see this supplier in pickers.",
                fontSize = 12.sp, color = NTColors.TextTertiary,
            )

            Spacer(Modifier.height(NTDp.md))
            FormFieldLabel("Supplier name *")
            SupplierFormInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. Aqua Pure",
            )

            Spacer(Modifier.height(NTDp.md))
            FormFieldLabel("Location")
            SupplierFormInput(
                value = location,
                onValueChange = { location = it },
                placeholder = "e.g. Coimbatore",
            )

            Spacer(Modifier.height(NTDp.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(name.trim(), location.trim().takeIf { it.isNotBlank() }) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
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
private fun FormFieldLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = NTColors.TextTertiary, letterSpacing = 0.4.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SupplierFormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

