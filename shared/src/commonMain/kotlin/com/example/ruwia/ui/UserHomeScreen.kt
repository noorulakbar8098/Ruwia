package com.example.ruwia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ruwia.domain.Order
import com.example.ruwia.presentation.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    vm: UserViewModel,
    onPlaceOrder: () -> Unit,
    onLogout: () -> Unit,
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account") },
                actions = {
                    TextButton(onClick = { vm.logout(); onLogout() }) {
                        Text("Logout")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.loadData() }) { Text("Retry") }
                }
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    val c = state.customer
                    if (c != null) {
                        Text("Welcome, ${c.name}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SummaryCard("Cans Held", "${c.cansHeld}", Modifier.weight(1f))
                            SummaryCard("Balance Due", "₹${c.balance}", Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Button(
                        onClick = onPlaceOrder,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.customer != null,
                    ) { Text("Place New Order") }
                }

                item {
                    Text("Recent Orders", style = MaterialTheme.typography.titleSmall)
                }

                if (state.orders.isEmpty()) {
                    item {
                        Text(
                            "No orders yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.orders.take(10)) { order -> OrderRow(order) }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OrderRow(order: Order) {
    val statusColor = when (order.status) {
        "pending" -> Color(0xFFE65100)
        "approved" -> Color(0xFF1565C0)
        "delivered" -> Color(0xFF2E7D32)
        "cancelled" -> Color(0xFFC62828)
        else -> Color.Gray
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "${order.qty} can${if (order.qty != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                order.createdAt?.take(10)?.let { date ->
                    Text(
                        date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    order.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
        }
    }
}
