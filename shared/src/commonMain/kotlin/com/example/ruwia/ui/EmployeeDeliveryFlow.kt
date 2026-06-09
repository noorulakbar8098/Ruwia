package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ruwia.domain.DeliveryTask

@Composable
fun EmployeeDeliveryDetailsScreen(
    task: DeliveryTask,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("⬅️ BACK TO ROUTE", color = Color(0xFF2A9D8F))
            }
            Surface(
                color = Color(0xFF2A9D8F).copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    task.status.uppercase(),
                    color = Color(0xFF2A9D8F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CUSTOMER ACCOUNT", fontSize = 10.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                Text(task.customerName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("📞 Phone: ${task.phone}", fontSize = 14.sp, color = Color.White)
                Text("📍 Address: ${task.address}", fontSize = 14.sp, color = Color(0xFF888888))
            }
        }

        // Map visual placeholder
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Google Maps Real-Time Telemetry Activated\n[Estimated Route Outline]", color = Color(0xFF888888), textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { /* Navigation map link trigger */ },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B)),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Open Maps ➔", color = Color.White)
            }

            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F)),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Confirm Delivery", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDeliveryCompletionScreen(
    task: DeliveryTask,
    onDismiss: () -> Unit,
    onSubmit: (delivered: Int, emptyReturned: Int, collectedAmount: Double, paymentMode: String) -> Unit
) {
    var cansDelivered by remember { mutableStateOf("${task.canQty}") }
    var emptyCansReturned by remember { mutableStateOf("${task.canQty}") }
    var collectedAmt by remember { mutableStateOf("480.0") } // Auto calculated base rate: 4 * 120
    var payMode by remember { mutableStateOf("UPI") } // UPI | Cash | Due

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val del = cansDelivered.toIntOrNull() ?: task.canQty
                    val ret = emptyCansReturned.toIntOrNull() ?: task.canQty
                    val amt = collectedAmt.toDoubleOrNull() ?: 0.0
                    onSubmit(del, ret, amt, payMode)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Settlement", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF888888))
            }
        },
        title = { Text("Complete Delivery run", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cansDelivered,
                    onValueChange = { cansDelivered = it },
                    label = { Text("Cans Delivered", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emptyCansReturned,
                    onValueChange = { emptyCansReturned = it },
                    label = { Text("Empty Cans Collected", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = collectedAmt,
                    onValueChange = { collectedAmt = it },
                    label = { Text("Amount Collected (₹)", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Reconciliation Mode", fontSize = 11.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("UPI", "Cash", "Mark Due").forEach { mode ->
                        val active = payMode == mode
                        Button(
                            onClick = { payMode = mode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Color(0xFF2A9D8F) else Color(0xFF2B2B2B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}
