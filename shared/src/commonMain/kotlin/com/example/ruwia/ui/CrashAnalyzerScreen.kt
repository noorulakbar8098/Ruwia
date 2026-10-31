package com.example.ruwia.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.*
import com.example.ruwia.presentation.*
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import org.koin.compose.koinInject

@Composable
fun CrashAnalyzerScreen(onBack: () -> Unit, vm: CrashAnalyzerViewModel = koinInject()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val report = state.selectedReport
    if (report != null) {
        SystemBackHandler { vm.onSelectReport(null) }
        CrashDetailsScreen(report, { vm.onSelectReport(null) }) { vm.onUpdateStatus(report.id, it) }
    } else {
        SystemBackHandler(onBack = onBack)
        CrashListScreen(state, onBack, vm::onSearchQueryChange, vm::onSeverityFilterChange) {
            vm.onSelectReport(it.id)
        }
    }
}
@Composable
private fun CrashListScreen(state: CrashAnalyzerState, onBack: () -> Unit, onSearch: (String) -> Unit, onFilter: (CrashSeverity?) -> Unit, onSelect: (CrashReport) -> Unit) {
    Scaffold(containerColor = NTColors.Background, topBar = { TopBar("Crash Analyzer", "Diagnostic telemetry & error logs", onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(NTDp.screenPad, 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { StatsDashboard(state.stats) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchBar(state.searchQuery, onSearch); SeverityFilters(state.selectedSeverity, onFilter)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Crash Reports", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                    Text("${state.filteredReports.size} found", fontSize = 13.sp, color = NTColors.TextSecondary)
                }
            }
            if (state.filteredReports.isEmpty()) item { EmptyReports(state.searchQuery) }
            else items(state.filteredReports, key = { it.id }) { CrashReportCard(it) { onSelect(it) } }
        }
    }
}

@Composable
private fun StatsDashboard(stats: CrashStats) {
    val cards = listOf(Triple("Total Reports", stats.total, Icons.Rounded.Analytics) to NTColors.Primary,
        Triple("Critical Issues", stats.critical, Icons.Rounded.ErrorOutline) to NTColors.Error,
        Triple("Open Issues", stats.open, Icons.Rounded.WarningAmber) to NTColors.Warning,
        Triple("Resolved Issues", stats.resolved, Icons.Rounded.CheckCircleOutline) to NTColors.Success)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (data, color) -> StatCard(data.first, data.second.toString(), data.third, color, Modifier.weight(1f)) }
        } }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    SurfaceCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, fontSize = 12.sp, color = NTColors.TextSecondary)
                Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = .12f)), Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NTColors.TextPrimary)
        }
    }
}
@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(NTDp.radMd)).background(NTColors.Surface)
        .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radMd)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, "Search", tint = NTColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search by title or crash ID (e.g. CR-1001)...", fontSize = 14.sp, color = NTColors.TextDisabled)
            BasicTextField(query, onChange, singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = NTColors.TextPrimary, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(NTColors.Primary), modifier = Modifier.fillMaxWidth())
        }
        if (query.isNotEmpty()) { Spacer(Modifier.width(8.dp)); Icon(Icons.Rounded.Close, "Clear search",
            tint = NTColors.TextSecondary, modifier = Modifier.size(18.dp).clickable { onChange("") }) }
    }
}
@Composable
private fun SeverityFilters(selected: CrashSeverity?, onSelect: (CrashSeverity?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("All", selected == null) { onSelect(null) }
        CrashSeverity.entries.forEach { severity ->
            FilterChip(severity.label, selected == severity, severityColor(severity)) {
                onSelect(if (selected == severity) null else severity)
            }
        }
    }
}
@Composable
private fun FilterChip(label: String, selected: Boolean, color: Color = NTColors.Primary, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(NTDp.radFull)).background(if (selected) color.copy(alpha = .15f) else NTColors.Surface)
        .border(1.dp, if (selected) color else NTColors.Border, RoundedCornerShape(NTDp.radFull))
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp), Alignment.Center) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) color else NTColors.TextSecondary)
    }
}
@Composable
private fun CrashReportCard(report: CrashReport, onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(report.id, Modifier.clip(RoundedCornerShape(6.dp)).background(NTColors.SurfaceVar)
                        .padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = NTColors.TextSecondary)
                    SeverityBadge(report.severity)
                    StatusBadge(report.status)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = NTColors.TextTertiary)
            }
            Text(report.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Memory, null, tint = NTColors.TextTertiary, modifier = Modifier.size(14.dp))
                    Text(report.component, fontSize = 12.sp, color = NTColors.TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(report.timestamp, fontSize = 11.sp, color = NTColors.TextTertiary)
            }
        }
    }
}
@Composable
private fun CrashDetailsScreen(report: CrashReport, onBack: () -> Unit, onStatusChange: (CrashStatus) -> Unit) {
    Scaffold(containerColor = NTColors.Background, topBar = { TopBar(report.id, "${report.severity.label} severity crash report", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = NTDp.screenPad, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        SeverityBadge(report.severity)
                        Text(report.timestamp, fontSize = 12.sp, color = NTColors.TextTertiary)
                    }
                    Text(report.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Affected Component:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
                        Text(report.component, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NTColors.Primary)
                    }
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Investigation Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                        StatusBadge(report.status)
                    }
                    Text("Update the current status of this issue. Changes will save automatically and update dashboard metrics.",
                        fontSize = 12.sp, color = NTColors.TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrashStatus.entries.forEach { status ->
                            val selected = report.status == status
                            val color = statusColor(status)
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(NTDp.radMd))
                                    .background(if (selected) color.copy(alpha = .15f) else NTColors.SurfaceVar)
                                    .border(1.5.dp, if (selected) color else Color.Transparent, RoundedCornerShape(NTDp.radMd))
                                    .clickable { onStatusChange(status) }.padding(vertical = 10.dp),
                                Alignment.Center,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(14.dp))
                                    Text(status.label, fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) color else NTColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            DetailSection("Error Message", Icons.Rounded.Terminal, NTColors.Error) {
                Text(report.errorMessage, Modifier.fillMaxWidth().clip(RoundedCornerShape(NTDp.radSm))
                    .background(Color(0xFF1E293B)).padding(12.dp), fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace, color = Color(0xFFF1F5F9), lineHeight = 18.sp)
            }
            DetailSection("Suspected Root Cause", Icons.Rounded.HelpOutline, NTColors.Warning) {
                Text(report.suspectedRootCause, fontSize = 13.sp, color = NTColors.TextSecondary, lineHeight = 20.sp)
            }
            DetailSection("Recommended Debugging Steps", Icons.Rounded.Build, NTColors.Primary) {
                Text(report.recommendedSteps, fontSize = 13.sp, color = NTColors.TextSecondary, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
@Composable
private fun DetailSection(title: String, icon: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            }
            content()
        }
    }
}
@Composable
private fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(NTDp.radLg)
    val colors = CardDefaults.cardColors(containerColor = NTColors.Surface)
    val border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(NTColors.Border))
    if (onClick == null) Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    else Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}
@Composable
private fun SeverityBadge(severity: CrashSeverity) = Badge(severity.label, severityColor(severity))

@Composable
private fun StatusBadge(status: CrashStatus) = Badge(status.label, statusColor(status))
@Composable
private fun Badge(label: String, color: Color) {
    Text(label, Modifier.clip(RoundedCornerShape(NTDp.radFull)).background(color.copy(alpha = .12f))
        .padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
}
private fun severityColor(severity: CrashSeverity) = when (severity) {
    CrashSeverity.CRITICAL -> Color(0xFFDC2626)
    CrashSeverity.HIGH -> Color(0xFFEA580C)
    CrashSeverity.MEDIUM -> Color(0xFFD97706)
    CrashSeverity.LOW -> Color(0xFF2563EB)
}
private fun statusColor(status: CrashStatus) = when (status) {
    CrashStatus.OPEN -> Color(0xFFD97706)
    CrashStatus.INVESTIGATING -> Color(0xFF0284C7)
    CrashStatus.RESOLVED -> Color(0xFF16A34A)
}
@Composable
private fun EmptyReports(query: String) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(NTColors.SurfaceVar), Alignment.Center) {
                Icon(Icons.Rounded.SearchOff, null, tint = NTColors.TextTertiary, modifier = Modifier.size(28.dp))
            }
            Text("No crash reports found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            Text(
                if (query.isNotBlank()) "No results match \"$query\". Try adjusting your search query or clear severity filters."
                else "No reports match the selected severity filter.",
                fontSize = 13.sp, color = NTColors.TextSecondary, lineHeight = 18.sp,
            )
        }
    }
}
@Composable
private fun TopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(NTColors.Surface).border(1.dp, NTColors.Border).statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onBack, Modifier.size(40.dp).clip(CircleShape).background(NTColors.SurfaceVar)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = NTColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
