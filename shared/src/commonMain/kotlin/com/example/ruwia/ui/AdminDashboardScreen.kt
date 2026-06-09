package com.example.ruwia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.presentation.AdminViewModel
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onLogout: () -> Unit
) {
    val state by vm.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Home/Dashboard, 1: Orders, 2: Stocks, 3: Employees, 4: Reports

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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(end = 0.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Neer Thuli Admin", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006D77))
                            Text("Enterprise Control Portal", fontSize = 11.sp, color = Color(0xFF6C757D))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Text("🚪", fontSize = 20.sp)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Dashboard", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📋", fontSize = 20.sp) },
                    label = { Text("Orders", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📦", fontSize = 20.sp) },
                    label = { Text("Stock", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("👥", fontSize = 20.sp) },
                    label = { Text("Staff", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Config", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> SaaSAnalyticsDashboard(state.mrr, state.csat, state.fleetActiveCount, padding)
            1 -> AdminOrdersTab(state.orders, vm, padding)
            2 -> AdminStockTab(state.stockItems, vm, padding)
            3 -> AdminEmployeesTab(state.employees, vm, padding)
            4 -> AdminConfigTab(padding)
        }
    }
}

@Composable
fun SaaSAnalyticsDashboard(
    mrr: Double,
    csat: Double,
    fleetActive: Int,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Metrics
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SaaSCard(modifier = Modifier.weight(1f)) {
                    Text("TOTAL MRR", fontSize = 10.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${mrr.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                    Text("+8.2% vs last mo", fontSize = 10.sp, color = Color(0xFF2A9D8F))
                }

                SaaSCard(modifier = Modifier.weight(1f)) {
                    Text("FLEET ON ROAD", fontSize = 10.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$fleetActive active", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                    Text("92% complete", fontSize = 10.sp, color = Color(0xFF2A9D8F))
                }

                SaaSCard(modifier = Modifier.weight(1f)) {
                    Text("CSAT RATING", fontSize = 10.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$csat / 5.0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                    Text("91% Positive", fontSize = 10.sp, color = Color(0xFF2A9D8F))
                }
            }
        }

        // Charts: Monthly Volume
        item {
            SaaSCard {
                Text("STOCK ANALYTICS", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
                Text("Monthly Volume (Liters)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                Spacer(modifier = Modifier.height(16.dp))

                // Custom bar chart rendering with canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val bars = listOf(90f, 130f, 150f, 115f, 125f, 145f)
                    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                    val space = size.width / (bars.size)
                    val barWidth = 30.dp.toPx()

                    bars.forEachIndexed { i, h ->
                        val x = i * space + (space - barWidth) / 2
                        val progress = h / 160f
                        val barHeight = size.height * progress

                        // Draw background bar
                        drawRoundRect(
                            color = Color(0xFFE9ECEF),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )

                        // Draw active volume bar
                        drawRoundRect(
                            color = Color(0xFF2A9D8F),
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun").forEach {
                        Text(it, fontSize = 11.sp, color = Color(0xFF6C757D), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Active Delivery Fleet Tracking Canvas
        item {
            SaaSCard {
                Text("ACTIVE FLEET TRACKER", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFE9ECEF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple interactive map telemetry graphics
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw simulated roads
                        drawLine(Color.White, Offset(10f, 50f), Offset(size.width - 10f, 50f), strokeWidth = 10f)
                        drawLine(Color.White, Offset(100f, 10f), Offset(100f, size.height - 10f), strokeWidth = 10f)
                        drawLine(Color.White, Offset(size.width - 120f, 10f), Offset(size.width - 120f, size.height - 10f), strokeWidth = 10f)

                        // Draw driver hubs
                        drawCircle(Color(0xFF006D77), radius = 12f, center = Offset(100f, 50f))
                        drawCircle(Color(0xFF2A9D8F), radius = 12f, center = Offset(size.width - 120f, 90f))
                        drawCircle(Color(0xFFF4A261), radius = 12f, center = Offset(100f, 110f))
                    }
                    Text("Live Fleet Map Active (32 Vehicles)", color = Color(0xFF006D77), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚚 Active: 32", fontSize = 12.sp, color = Color(0xFF2A9D8F), fontWeight = FontWeight.Bold)
                    Text("✅ Delivered: 12", fontSize = 12.sp, color = Color(0xFF6C757D))
                    Text("⚠️ Idle: 4", fontSize = 12.sp, color = Color(0xFFF4A261))
                }
            }
        }

        // Quick Admin Controls Grid
        item {
            Text("QUICK OPERATIONAL ACTIONS", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickActionButton("➕ Order", Color(0xFF006D77), Modifier.weight(1f))
                QuickActionButton("👤 Customer", Color(0xFF2A9D8F), Modifier.weight(1f))
                QuickActionButton("🚚 Route", Color(0xFFF4A261), Modifier.weight(1f))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SaaSCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9ECEF)),
        modifier = modifier.fillMaxWidth()
    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            content = content
//        ) {
//
//        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
