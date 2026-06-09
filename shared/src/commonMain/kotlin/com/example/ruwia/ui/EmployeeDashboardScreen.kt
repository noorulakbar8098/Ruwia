package com.example.ruwia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.presentation.EmployeeViewModel
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    vm: EmployeeViewModel,
    onLogout: () -> Unit
) {
    val state by vm.state.collectAsState()
    var showingCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadDashboard("e1") // Load default driver Arul S.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.app_logo),
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Neer Thuli Driver", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Active Shift: Arul S.", fontSize = 11.sp, color = Color(0xFF888888))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Text("🚪", fontSize = 20.sp)
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (state.selectedTask != null) {
            EmployeeDeliveryDetailsScreen(
                task = state.selectedTask!!,
                onBack = { vm.selectTask(null) },
                onComplete = { showingCompleteDialog = true },
                padding = padding
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Shift Telemetry Card
                item {
                    EmployeeStateHUD(
                        earnings = state.dailyEarnings,
                        completed = state.filledDelivered,
                        total = state.filledDelivered + 8
                    )
                }

                // Can Stock Counter
                item {
                    CanInventoryHUD(state.filledDelivered, state.emptyReturned)
                }

                // Live routes list
                item {
                    Text("LIVE SHIFT ROUTE RUNS", fontSize = 12.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                }

                if (state.tasks.isEmpty()) {
                    item {
                        Text("No active delivery runs loaded.", color = Color(0xFF888888))
                    }
                } else {
                    items(state.tasks) { task ->
                        EmployeeRouteCard(task, onClick = { vm.selectTask(task) })
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        if (showingCompleteDialog && state.selectedTask != null) {
            EmployeeDeliveryCompletionScreen(
                task = state.selectedTask!!,
                onDismiss = { showingCompleteDialog = false },
                onSubmit = { delivered, emptyCans, amount, mode ->
                    vm.completeDelivery(state.selectedTask!!.id, "e1", delivered, emptyCans, amount, mode)
                    showingCompleteDialog = false
                }
            )
        }
    }
}

@Composable
fun EmployeeStateHUD(
    earnings: Double,
    completed: Int,
    total: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color(0xFF2A9D8F).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SHIFT EARNINGS", fontSize = 10.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                Text("₹${earnings.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A9D8F))
                Text("Shift duration: 6h 15m", fontSize = 11.sp, color = Color(0xFF888888))
            }

            // Custom Circular Progress Wheel
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFF2B2B2B), style = Stroke(width = 8.dp.toPx()))
                    val progress = completed.toFloat() / total.toFloat()
                    drawArc(
                        color = Color(0xFF2A9D8F),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$completed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("/$total", fontSize = 10.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}

@Composable
fun CanInventoryHUD(filled: Int, empty: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("FILLED CANS REMAINING", fontSize = 9.sp, color = Color(0xFF888888))
                Text("$filled Cans", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A9D8F))
            }
            Divider(modifier = Modifier.width(1.dp).height(35.dp).background(Color(0xFF2B2B2B)))
            Column {
                Text("EMPTY CANS RETURNED", fontSize = 9.sp, color = Color(0xFF888888))
                Text("$empty Cans", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4A261))
            }
        }
    }
}

@Composable
fun EmployeeRouteCard(
    task: DeliveryTask,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color(0xFF2B2B2B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.customerName, fontWeight = FontWeight.Bold, color = Color.White)
                Text(task.etaText, fontSize = 11.sp, color = Color(0xFF2A9D8F))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(task.address, fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF2A9D8F).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📦 ${task.canQty} Cans (20L)",
                        color = Color(0xFF2A9D8F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = when (task.status) {
                        "en_route" -> Color(0xFF2A9D8F).copy(alpha = 0.12f)
                        else -> Color(0xFFF4A261).copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.status.replace("_", " ").uppercase(),
                        color = when (task.status) {
                            "en_route" -> Color(0xFF2A9D8F)
                            else -> Color(0xFFF4A261)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
