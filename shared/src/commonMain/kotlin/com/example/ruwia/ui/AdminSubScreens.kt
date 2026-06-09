package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.StockItem
import com.example.ruwia.presentation.AdminViewModel

@Composable
fun AdminOrdersTab(
    orders: List<Order>,
    vm: AdminViewModel,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("MANAGE ACTIVE ORDERS", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
        }

        if (orders.isEmpty()) {
            item {
                Text("No active orders found.", color = Color(0xFF6C757D))
            }
        } else {
            items(orders) { order ->
                SaaSCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Order ID: #${order.id ?: "Pending"}", fontWeight = FontWeight.Bold)
                            Text("Customer ID: ${order.customerId}", fontSize = 12.sp, color = Color(0xFF6C757D))
                            Text("Volume: ${order.qty} Cans (20L)", fontSize = 12.sp, color = Color(0xFF006D77))
                        }
                        
                        val badgeColor = when (order.status) {
                            "pending" -> Color(0xFFF4A261)
                            "approved" -> Color(0xFF006D77)
                            else -> Color(0xFF2A9D8F)
                        }

                        Surface(
                            color = badgeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = order.status.uppercase(),
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFFE9ECEF))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dispatch Status", fontSize = 11.sp, color = Color(0xFF6C757D))
                        TextButton(
                            onClick = { /* Open Assign Driver Dialog */ },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Assign Fleet Driver ➔", color = Color(0xFF006D77), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AdminStockTab(
    stocks: List<StockItem>,
    vm: AdminViewModel,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("WAREHOUSE INVENTORY LEVELS", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
        }

        items(stocks) { stock ->
            SaaSCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stock.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Capacity: ${stock.capacityLiters} Liters per can", fontSize = 12.sp, color = Color(0xFF6C757D))
                        Text("Base Rate: ₹${stock.pricePerCan}", fontSize = 12.sp, color = Color(0xFF006D77))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("${stock.stockAvailable}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A9D8F))
                        Text("Available Cans", fontSize = 10.sp, color = Color(0xFF6C757D))
                    }
                }
            }
        }

        item {
            Button(
                onClick = { /* Add stock purchase flow */ },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D77)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Log Supplier Stock Purchase", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AdminEmployeesTab(
    employees: List<EmployeeInfo>,
    vm: AdminViewModel,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("DISTRIBUTION FLEET STATUS", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
        }

        items(employees) { emp ->
            SaaSCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Phone: ${emp.phone}", fontSize = 12.sp, color = Color(0xFF6C757D))
                        Text("Shift Performance Rating: ⭐ ${emp.rating}", fontSize = 12.sp, color = Color(0xFFF4A261))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val statusColor = when (emp.status) {
                            "on_delivery" -> Color(0xFFF4A261)
                            "active" -> Color(0xFF2A9D8F)
                            else -> Color(0xFF6C757D)
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = emp.status.replace("_", " ").uppercase(),
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AdminConfigTab(
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("SAAS CONFIGURATION & SUBSCRIPTION", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
        }

        item {
            SaaSCard {
                Text("Enterprise Workspace", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Domain: acmeco.neerthuli.com", color = Color(0xFF6C757D), fontSize = 12.sp)
                Text("Plan: Enterprise Multi-Franchise Plus", color = Color(0xFF006D77), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SaaSCard {
                Text("Distributed Webhook Integrations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Google Maps Matrix API: Status Connected", color = Color(0xFF2A9D8F), fontSize = 12.sp)
                Text("SMS Dispatch Gateway: Connected", color = Color(0xFF2A9D8F), fontSize = 12.sp)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
