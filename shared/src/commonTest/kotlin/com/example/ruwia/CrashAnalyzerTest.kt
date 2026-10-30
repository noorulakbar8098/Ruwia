package com.example.ruwia

import com.example.ruwia.data.CrashReportRepository
import com.example.ruwia.domain.CrashSeverity
import com.example.ruwia.domain.CrashStatus
import com.example.ruwia.presentation.CrashAnalyzerViewModel
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class InMemorySettings : Settings {
    private val map = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size

    override fun clear() = map.clear()
    override fun remove(key: String) { map.remove(key) }
    override fun hasKey(key: String): Boolean = map.containsKey(key)

    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key] as? Int

    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key] as? Long

    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key] as? String

    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key] as? Float

    override fun putDouble(key: String, value: Double) { map[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key] as? Double

    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
}

class CrashAnalyzerTest {

    @Test
    fun sampleReportsAreLoadedAndCategorizedCorrectly() {
        val settings = InMemorySettings()
        val repository = CrashReportRepository(settings)
        val vm = CrashAnalyzerViewModel(repository)

        val state = vm.state.value
        assertEquals(5, state.allReports.size)
        assertEquals(5, state.filteredReports.size)

        // Verify initial dashboard stats
        assertEquals(5, state.stats.total)
        assertEquals(1, state.stats.critical) // CR-1001 (NPE)
        assertEquals(3, state.stats.open)     // CR-1001, CR-1003, CR-1004
        assertEquals(1, state.stats.resolved) // CR-1005 (OOM)

        // Verify the 5 required crash scenarios exist
        val titles = state.allReports.map { it.title }.joinToString()
        assertTrue(titles.contains("NullPointerException"))
        assertTrue(titles.contains("lifecycle") || titles.contains("Fragment"))
        assertTrue(titles.contains("SocketTimeout") || titles.contains("sync"))
        assertTrue(titles.contains("Bluetooth"))
        assertTrue(titles.contains("OutOfMemory") || titles.contains("bitmap"))
    }

    @Test
    fun searchFilteringWorksByTitleAndId() {
        val settings = InMemorySettings()
        val repository = CrashReportRepository(settings)
        val vm = CrashAnalyzerViewModel(repository)

        // Search by ID
        vm.onSearchQueryChange("CR-1001")
        assertEquals(1, vm.state.value.filteredReports.size)
        assertEquals("CR-1001", vm.state.value.filteredReports.first().id)

        // Search by title keyword (case-insensitive)
        vm.onSearchQueryChange("bluetooth")
        assertEquals(1, vm.state.value.filteredReports.size)
        assertEquals("CR-1004", vm.state.value.filteredReports.first().id)

        // Search non-existent
        vm.onSearchQueryChange("xyzNonExistent")
        assertEquals(0, vm.state.value.filteredReports.size)

        // Clear query
        vm.onSearchQueryChange("")
        assertEquals(5, vm.state.value.filteredReports.size)
    }

    @Test
    fun severityFilteringWorksCorrectly() {
        val settings = InMemorySettings()
        val repository = CrashReportRepository(settings)
        val vm = CrashAnalyzerViewModel(repository)

        vm.onSeverityFilterChange(CrashSeverity.CRITICAL)
        assertEquals(1, vm.state.value.filteredReports.size)
        assertEquals(CrashSeverity.CRITICAL, vm.state.value.filteredReports.first().severity)

        vm.onSeverityFilterChange(CrashSeverity.HIGH)
        assertEquals(2, vm.state.value.filteredReports.size)

        vm.onSeverityFilterChange(null)
        assertEquals(5, vm.state.value.filteredReports.size)
    }

    @Test
    fun statusUpdateReflectsImmediatelyAndPersists() {
        val settings = InMemorySettings()
        val repository = CrashReportRepository(settings)
        val vm = CrashAnalyzerViewModel(repository)

        // Initially CR-1001 is OPEN
        val initial = vm.state.value.allReports.first { it.id == "CR-1001" }
        assertEquals(CrashStatus.OPEN, initial.status)
        assertEquals(3, vm.state.value.stats.open)
        assertEquals(1, vm.state.value.stats.resolved)

        // Update CR-1001 to RESOLVED
        vm.onUpdateStatus("CR-1001", CrashStatus.RESOLVED)

        val updated = vm.state.value.allReports.first { it.id == "CR-1001" }
        assertEquals(CrashStatus.RESOLVED, updated.status)

        // Stats should update dynamically
        assertEquals(2, vm.state.value.stats.open)
        assertEquals(2, vm.state.value.stats.resolved)

        // Verify persistence: create a new ViewModel/Repository using same Settings
        val reloadedRepo = CrashReportRepository(settings)
        val newVm = CrashAnalyzerViewModel(reloadedRepo)
        val persistedReport = newVm.state.value.allReports.first { it.id == "CR-1001" }
        assertEquals(CrashStatus.RESOLVED, persistedReport.status)
        assertEquals(2, newVm.state.value.stats.resolved)
    }

    @Test
    fun reportDetailsSelectionWorks() {
        val settings = InMemorySettings()
        val repository = CrashReportRepository(settings)
        val vm = CrashAnalyzerViewModel(repository)

        vm.onSelectReport("CR-1003")
        assertNotNull(vm.state.value.selectedReport)
        assertEquals("CR-1003", vm.state.value.selectedReport?.id)
        assertTrue(vm.state.value.selectedReport?.errorMessage?.contains("SocketTimeoutException") == true)
        assertTrue(vm.state.value.selectedReport?.suspectedRootCause?.isNotBlank() == true)
        assertTrue(vm.state.value.selectedReport?.recommendedSteps?.isNotBlank() == true)

        vm.onSelectReport(null)
        assertEquals(null, vm.state.value.selectedReport)
    }
}
