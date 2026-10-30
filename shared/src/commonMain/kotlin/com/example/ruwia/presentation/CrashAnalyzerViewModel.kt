package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import com.example.ruwia.data.CrashReportRepository
import com.example.ruwia.domain.CrashReport
import com.example.ruwia.domain.CrashSeverity
import com.example.ruwia.domain.CrashStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CrashStats(
    val total: Int = 0,
    val critical: Int = 0,
    val open: Int = 0,
    val resolved: Int = 0,
)

data class CrashAnalyzerState(
    val allReports: List<CrashReport> = emptyList(),
    val filteredReports: List<CrashReport> = emptyList(),
    val searchQuery: String = "",
    val selectedSeverity: CrashSeverity? = null,
    val selectedReportId: String? = null,
    val stats: CrashStats = CrashStats(),
) {
    val selectedReport: CrashReport?
        get() = selectedReportId?.let { id -> allReports.find { it.id == id } }
}

class CrashAnalyzerViewModel(
    private val repository: CrashReportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CrashAnalyzerState())
    val state: StateFlow<CrashAnalyzerState> = _state.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        val reports = repository.getCrashReports()
        _state.update { current ->
            applyFilters(
                allReports = reports,
                query = current.searchQuery,
                severity = current.selectedSeverity,
                selectedReportId = current.selectedReportId,
            )
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _state.update { current ->
            applyFilters(
                allReports = current.allReports,
                query = newQuery,
                severity = current.selectedSeverity,
                selectedReportId = current.selectedReportId,
            )
        }
    }

    fun onSeverityFilterChange(severity: CrashSeverity?) {
        _state.update { current ->
            applyFilters(
                allReports = current.allReports,
                query = current.searchQuery,
                severity = severity,
                selectedReportId = current.selectedReportId,
            )
        }
    }

    fun onSelectReport(reportId: String?) {
        _state.update { it.copy(selectedReportId = reportId) }
    }

    fun onUpdateStatus(reportId: String, newStatus: CrashStatus) {
        val updatedReports = repository.updateCrashStatus(reportId, newStatus)
        _state.update { current ->
            applyFilters(
                allReports = updatedReports,
                query = current.searchQuery,
                severity = current.selectedSeverity,
                selectedReportId = current.selectedReportId,
            )
        }
    }

    private fun applyFilters(
        allReports: List<CrashReport>,
        query: String,
        severity: CrashSeverity?,
        selectedReportId: String?,
    ): CrashAnalyzerState {
        val trimmedQuery = query.trim()
        val filtered = allReports.filter { report ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                report.title.contains(trimmedQuery, ignoreCase = true) ||
                    report.id.contains(trimmedQuery, ignoreCase = true) ||
                    report.component.contains(trimmedQuery, ignoreCase = true)
            }
            val matchesSeverity = severity == null || report.severity == severity
            matchesQuery && matchesSeverity
        }

        val stats = CrashStats(
            total = allReports.size,
            critical = allReports.count { it.severity == CrashSeverity.CRITICAL },
            open = allReports.count { it.status == CrashStatus.OPEN },
            resolved = allReports.count { it.status == CrashStatus.RESOLVED },
        )

        return CrashAnalyzerState(
            allReports = allReports,
            filteredReports = filtered,
            searchQuery = query,
            selectedSeverity = severity,
            selectedReportId = selectedReportId,
            stats = stats,
        )
    }
}
