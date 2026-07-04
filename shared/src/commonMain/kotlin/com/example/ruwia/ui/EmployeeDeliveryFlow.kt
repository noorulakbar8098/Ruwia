package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask

@Composable
fun EmployeeDeliveryDetailsScreen(
    task: DeliveryTask,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1214))
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
            }
            Text("Delivery Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111C1F))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(task.customerName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phone, null, tint = Color(0xFF24C7B7), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(task.phone, color = Color.White.copy(alpha = 0.7f))
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(task.address, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111C1F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quantity", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${task.canQty} Cans", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111C1F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Status", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(task.status.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF24C7B7))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24C7B7))
            ) {
                Text("MARK AS DELIVERED", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun EmployeeDeliveryCompletionScreen(
    task: DeliveryTask,
    onDismiss: () -> Unit,
    onSubmit: (delivered: Int, emptyReturned: Int, collectedAmount: Double, paymentMode: String) -> Unit
) {
    var cansDelivered by remember { mutableStateOf(task.canQty.toString()) }
    var emptyCansReturned by remember { mutableStateOf(task.canQty.toString()) }
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
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        title = { Text("Complete Delivery run", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cansDelivered,
                    onValueChange = { cansDelivered = it },
                    label = { Text("Cans Delivered", color = Color.White.copy(alpha = 0.8f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFF2A9D8F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emptyCansReturned,
                    onValueChange = { emptyCansReturned = it },
                    label = { Text("Empty Cans Returned", color = Color.White.copy(alpha = 0.8f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFF2A9D8F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = collectedAmt,
                    onValueChange = { collectedAmt = it },
                    label = { Text("Amount Collected (₹)", color = Color.White.copy(alpha = 0.8f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2A9D8F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFF2A9D8F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Reconciliation Mode", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("UPI", "Cash", "Due").forEach { mode ->
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
        containerColor = Color(0xFF111C1F),
        shape = RoundedCornerShape(16.dp)
    )
}
