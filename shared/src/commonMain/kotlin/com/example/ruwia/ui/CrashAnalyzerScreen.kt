package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.CrashReport
import com.example.ruwia.domain.CrashSeverity
import com.example.ruwia.domain.CrashStatus
import com.example.ruwia.presentation.CrashAnalyzerState
import com.example.ruwia.presentation.CrashAnalyzerViewModel
import com.example.ruwia.presentation.CrashStats
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import org.koin.compose.koinInject

@Composable
fun CrashAnalyzerScreen(
    onBack: () -> Unit,
    vm: CrashAnalyzerViewModel = koinInject(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val selectedReport = state.selectedReport
    if (selectedReport != null) {
        SystemBackHandler { vm.onSelectReport(null) }
        CrashDetailsScreen(
            report = selectedReport,
            onBack = { vm.onSelectReport(null) },
            onStatusChange = { newStatus ->
                vm.onUpdateStatus(selectedReport.id, newStatus)
            },
        )
    } else {
        SystemBackHandler(onBack = onBack)
        CrashListScreen(
            state = state,
            onBack = onBack,
            onSearchQueryChange = vm::onSearchQueryChange,
            onSeverityFilterChange = vm::onSeverityFilterChange,
            onSelectReport = { vm.onSelectReport(it.id) },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 1. Crash Reports List Screen
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrashListScreen(
    state: CrashAnalyzerState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSeverityFilterChange: (CrashSeverity?) -> Unit,
    onSelectReport: (CrashReport) -> Unit,
) {
    Scaffold(
        containerColor = NTColors.Background,
        topBar = {
            CrashAnalyzerTopBar(
                title = "Crash Analyzer",
                subtitle = "Diagnostic telemetry & error logs",
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = NTDp.screenPad, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Dashboard KPI Statistics
            item {
                CrashStatsDashboard(stats = state.stats)
            }

            // Search and Severity Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CrashSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange,
                    )
                    SeverityFilterChips(
                        selectedSeverity = state.selectedSeverity,
                        onSelectSeverity = onSeverityFilterChange,
                    )
                }
            }

            // Header for reports count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Crash Reports",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.TextPrimary,
                    )
                    Text(
                        text = "${state.filteredReports.size} found",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NTColors.TextSecondary,
                    )
                }
            }

            // Report items
            if (state.filteredReports.isEmpty()) {
                item {
                    EmptyReportsState(query = state.searchQuery)
                }
            } else {
                items(state.filteredReports, key = { it.id }) { report ->
                    CrashReportCard(
                        report = report,
                        onClick = { onSelectReport(report) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 2. Dashboard KPI Statistics (4 Basic Stats)
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrashStatsDashboard(stats: CrashStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                title = "Total Reports",
                value = stats.total.toString(),
                icon = Icons.Rounded.Analytics,
                accentColor = NTColors.Primary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Critical Issues",
                value = stats.critical.toString(),
                icon = Icons.Rounded.ErrorOutline,
                accentColor = NTColors.Error,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                title = "Open Issues",
                value = stats.open.toString(),
                icon = Icons.Rounded.WarningAmber,
                accentColor = NTColors.Warning,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Resolved Issues",
                value = stats.resolved.toString(),
                icon = Icons.Rounded.CheckCircleOutline,
                accentColor = NTColors.Success,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(NTDp.radLg),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NTColors.TextSecondary,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NTColors.TextPrimary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 3. Search Bar & Severity Filters
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrashSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radMd))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radMd))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = "Search",
            tint = NTColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search by title or crash ID (e.g. CR-1001)...",
                    fontSize = 14.sp,
                    color = NTColors.TextDisabled,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = NTColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(NTColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = NTColors.TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun SeverityFilterChips(
    selectedSeverity: CrashSeverity?,
    onSelectSeverity: (CrashSeverity?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipItem(
            label = "All",
            selected = selectedSeverity == null,
            onClick = { onSelectSeverity(null) },
        )
        CrashSeverity.entries.forEach { severity ->
            FilterChipItem(
                label = severity.label,
                selected = selectedSeverity == severity,
                accentColor = severityColor(severity),
                onClick = {
                    if (selectedSeverity == severity) onSelectSeverity(null) else onSelectSeverity(severity)
                },
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    accentColor: Color = NTColors.Primary,
    onClick: () -> Unit,
) {
    val bg = if (selected) accentColor.copy(alpha = 0.15f) else NTColors.Surface
    val border = if (selected) accentColor else NTColors.Border
    val text = if (selected) accentColor else NTColors.TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(NTDp.radFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = text,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 4. Crash Report Card Item
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrashReportCard(
    report: CrashReport,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radLg),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: ID, Badges, Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NTColors.SurfaceVar)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = report.id,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NTColors.TextSecondary,
                        )
                    }
                    SeverityBadge(severity = report.severity)
                    StatusBadge(status = report.status)
                }

                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NTColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Title
            Text(
                text = report.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Affected component + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = NTColors.TextTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = report.component,
                        fontSize = 12.sp,
                        color = NTColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = report.timestamp,
                    fontSize = 11.sp,
                    color = NTColors.TextTertiary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 5. Crash Details Screen
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrashDetailsScreen(
    report: CrashReport,
    onBack: () -> Unit,
    onStatusChange: (CrashStatus) -> Unit,
) {
    Scaffold(
        containerColor = NTColors.Background,
        topBar = {
            CrashAnalyzerTopBar(
                title = report.id,
                subtitle = report.severity.label + " severity crash report",
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NTDp.screenPad, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Overview Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NTDp.radLg),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SeverityBadge(severity = report.severity)
                        Text(
                            text = report.timestamp,
                            fontSize = 12.sp,
                            color = NTColors.TextTertiary,
                        )
                    }

                    Text(
                        text = report.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.TextPrimary,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Affected Component:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NTColors.TextSecondary,
                        )
                        Text(
                            text = report.component,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.Primary,
                        )
                    }
                }
            }

            // Status Management Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NTDp.radLg),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Investigation Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                        )
                        StatusBadge(status = report.status)
                    }

                    Text(
                        text = "Update the current status of this issue. Changes will save automatically and update dashboard metrics.",
                        fontSize = 12.sp,
                        color = NTColors.TextSecondary,
                    )

                    // Status segmented buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CrashStatus.entries.forEach { status ->
                            val isSelected = report.status == status
                            val color = statusColor(status)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(NTDp.radMd))
                                    .background(if (isSelected) color.copy(alpha = 0.15f) else NTColors.SurfaceVar)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) color else Color.Transparent,
                                        RoundedCornerShape(NTDp.radMd),
                                    )
                                    .clickable { onStatusChange(status) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        text = status.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else NTColors.TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Message (Stack trace preview)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NTDp.radLg),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = NTColors.Error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Error Message",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NTDp.radSm))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = report.errorMessage,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            // Suspected Root Cause
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NTDp.radLg),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = NTColors.Warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Suspected Root Cause",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                        )
                    }

                    Text(
                        text = report.suspectedRootCause,
                        fontSize = 13.sp,
                        color = NTColors.TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }

            // Recommended Debugging Steps
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NTDp.radLg),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Build,
                            contentDescription = null,
                            tint = NTColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Recommended Debugging Steps",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                        )
                    }

                    Text(
                        text = report.recommendedSteps,
                        fontSize = 13.sp,
                        color = NTColors.TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 6. Badges & Helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun SeverityBadge(severity: CrashSeverity) {
    val color = severityColor(severity)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = severity.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun StatusBadge(status: CrashStatus) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = status.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

private fun severityColor(severity: CrashSeverity): Color = when (severity) {
    CrashSeverity.CRITICAL -> Color(0xFFDC2626)
    CrashSeverity.HIGH     -> Color(0xFFEA580C)
    CrashSeverity.MEDIUM   -> Color(0xFFD97706)
    CrashSeverity.LOW      -> Color(0xFF2563EB)
}

private fun statusColor(status: CrashStatus): Color = when (status) {
    CrashStatus.OPEN          -> Color(0xFFD97706)
    CrashStatus.INVESTIGATING -> Color(0xFF0284C7)
    CrashStatus.RESOLVED      -> Color(0xFF16A34A)
}

@Composable
private fun EmptyReportsState(query: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radLg),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NTColors.SurfaceVar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = NTColors.TextTertiary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "No crash reports found",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
            )
            Text(
                text = if (query.isNotBlank()) {
                    "No results match \"$query\". Try adjusting your search query or clear severity filters."
                } else {
                    "No reports match the selected severity filter."
                },
                fontSize = 13.sp,
                color = NTColors.TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun CrashAnalyzerTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface)
            .border(width = 1.dp, color = NTColors.Border)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NTColors.SurfaceVar),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = NTColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NTColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
