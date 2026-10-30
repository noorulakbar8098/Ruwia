package com.example.ruwia.data

import com.example.ruwia.domain.CrashReport
import com.example.ruwia.domain.CrashStatus
import com.example.ruwia.domain.defaultCrashReports
import com.russhwolf.settings.Settings

/**
 * Repository for managing crash reports and persisting their investigation
 * status locally using the existing project Settings persistence.
 */
class CrashReportRepository(private val settings: Settings) {

    fun getCrashReports(): List<CrashReport> {
        val defaults = defaultCrashReports()
        return defaults.map { report ->
            val savedStatusName = settings.getStringOrNull(KEY_STATUS_PREFIX + report.id)
            if (savedStatusName != null) {
                val savedStatus = runCatching { CrashStatus.valueOf(savedStatusName) }.getOrNull()
                if (savedStatus != null) report.copy(status = savedStatus) else report
            } else {
                report
            }
        }
    }

    fun updateCrashStatus(reportId: String, newStatus: CrashStatus): List<CrashReport> {
        settings.putString(KEY_STATUS_PREFIX + reportId, newStatus.name)
        return getCrashReports()
    }

    private companion object {
        const val KEY_STATUS_PREFIX = "crash_analyzer.status."
    }
}
