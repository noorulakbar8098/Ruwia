package com.example.ruwia.util

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTH_NAMES = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/** Full month name from 1-based month number (1=January). */
fun monthName(monthNumber: Int): String =
    MONTH_NAMES.getOrNull(monthNumber - 1) ?: "Unknown"

/** Abbreviated month name from 1-based month number. */
fun monthAbbr(monthNumber: Int): String =
    monthName(monthNumber).take(3)

// ── Instant/Clock-based formatters ─────────────────────────────

/** Format as "DD/MM/YYYY" e.g. "29/09/2026". */
fun toDisplayDate(
    instant: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val local = instant.toLocalDateTime(timeZone)
    val dd = local.dayOfMonth.toString().padStart(2, '0')
    val mm = local.monthNumber.toString().padStart(2, '0')
    return "$dd/$mm/${local.year}"
}

/** Current time formatted as "DD/MM/YYYY". */
fun todayDisplayDate(): String =
    toDisplayDate(Clock.System.now())

// ── String-based formatters (for DB/ISO strings) ────────────────

/**
 * Parses a stored ISO timestamp into wall-clock time at [timeZone].
 *
 * Accepts both TZ-aware ISO-8601 values (e.g. `2026-09-06T10:30:00Z` or
 * `...+05:30`) and legacy offset-less local values (`2026-09-06T10:30:00`)
 * that older writes stored in the DB.
 */
private fun parseToLocal(
    s: String?,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime? {
    if (s.isNullOrBlank()) return null
    val afterT = s.trim().substringAfter('T', "")
    val hasOffset = s.trim().endsWith('Z') || afterT.contains('+') || afterT.contains('-')
    return if (hasOffset) {
        try {
            Instant.parse(s).toLocalDateTime(timeZone)
        } catch (_: Exception) {
            parseLocalNoOffset(s)
        }
    } else {
        // No offset → treat as stored wall-clock time. Parsing it as UTC would
        // shift the displayed hour away from what was actually saved.
        parseLocalNoOffset(s)
    }
}

/** Parse `YYYY-MM-DD[THH:MM[:SS]]` (no offset) as a wall-clock time. */
private fun parseLocalNoOffset(raw: String): LocalDateTime? {
    val s = raw.trim()
    val tIdx = s.indexOf('T')
    val datePart = if (tIdx >= 0) s.substring(0, tIdx) else s
    val timePart = if (tIdx >= 0) s.substring(tIdx + 1) else ""
    val d = datePart.split("-")
    if (d.size != 3) return null
    val y = d[0].toIntOrNull() ?: return null
    val m = d[1].toIntOrNull() ?: return null
    val day = d[2].takeWhile { it.isDigit() }.toIntOrNull() ?: return null
    val hm = timePart.split(":").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
    return try {
        LocalDateTime(y, m, day, hm.getOrNull(0) ?: 0, hm.getOrNull(1) ?: 0, hm.getOrNull(2) ?: 0, 0)
    } catch (_: Exception) { null }
}

/** Parse an ISO-8601 timestamp and format as "DD/MM/YYYY". */
fun isoToDisplayDate(s: String?): String {
    if (s.isNullOrBlank()) return "—"
    val local = parseToLocal(s) ?: return s.take(10)
    val dd = local.dayOfMonth.toString().padStart(2, '0')
    val mm = local.monthNumber.toString().padStart(2, '0')
    return "$dd/$mm/${local.year}"
}

/** Parse an ISO-8601 timestamp and format as "HH:MM AM". */
fun isoToDisplayTime(s: String?): String {
    if (s.isNullOrBlank()) return ""
    if (s.indexOf('T') < 0) return ""
    val local = parseToLocal(s) ?: return ""
    val h = local.hour
    val hh = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    val ampm = if (h >= 12) "PM" else "AM"
    val mm = local.minute.toString().padStart(2, '0')
    return "$hh:$mm $ampm"
}

/** Parse an ISO-8601 timestamp and format as "DD/MM/YYYY · HH:MM AM". */
fun isoToDisplayDateTime(s: String?): String {
    if (s.isNullOrBlank()) return "—"
    val date = isoToDisplayDate(s)
    val time = isoToDisplayTime(s)
    return if (time.isEmpty()) date else "$date · $time"
}

/** Parse "YYYY-MM-DD" DB date and format as "DD/MM/YYYY". */
fun dbToDisplayDate(s: String): String {
    if (s.isNullOrBlank()) return "—"
    return try {
        val parts = s.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: return s
            val m = parts[1].toIntOrNull() ?: return s
            val d = parts[2].takeWhile { it.isDigit() }.toIntOrNull() ?: return s.take(10)
            "${d.toString().padStart(2, '0')}/${m.toString().padStart(2, '0')}/$y"
        } else s
    } catch (_: Exception) { s }
}

/** Parse "DD/MM/YYYY" display date back to "YYYY-MM-DD" for DB storage. */
fun displayDateToDb(s: String): String? {
    if (s.isNullOrBlank()) return null
    return try {
        val parts = s.trim().split("/")
        if (parts.size == 3) {
            val d = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val y = parts[2].toIntOrNull() ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            "${y}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
        } else null
    } catch (_: Exception) { null }
}

/** Current instant as a TZ-aware ISO-8601 string (e.g. "2026-09-06T10:30:00Z") for DB persistence. */
fun currentDateTimeIso(): String =
    Clock.System.now().toString()

// ── Transaction timestamp guards ────────────────────────────────────
//  Admins may backdate entries (past date/time) but must never postdate:
//  future timestamps corrupt "today" figures, week/month windows and audit
//  history. Enforced here (data layer, every write path) AND in the pickers.

/** True when the ISO instant is more than [toleranceMinutes] ahead of now. */
fun isFutureTimestamp(iso: String?, toleranceMinutes: Long = 1): Boolean {
    if (iso.isNullOrBlank()) return false
    return try {
        val instant = Instant.parse(iso.trim())
        val nowMs = Clock.System.now().toEpochMilliseconds()
        instant.toEpochMilliseconds() > nowMs + toleranceMinutes * 60_000
    } catch (_: Exception) {
        false
    }
}

/** True when the "YYYY-MM-DD" day (time suffixes tolerated) is after today. */
fun isFutureDay(day: String?): Boolean {
    if (day.isNullOrBlank()) return false
    return try {
        val clean = day.trim().take(10)
        val p = clean.split("-")
        if (p.size != 3) return false
        val y = p[0].toIntOrNull() ?: return false
        val m = p[1].toIntOrNull() ?: return false
        val d = p[2].takeWhile { it.isDigit() }.toIntOrNull() ?: return false
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayStr = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        clean > todayStr && y > 0 && m in 1..12 && d in 1..31
    } catch (_: Exception) {
        false
    }
}

/** Throws when [iso] is in the future. Call before every timestamped write. */
fun requireNotFutureTimestamp(label: String, iso: String?) {
    if (isFutureTimestamp(iso)) {
        throw IllegalStateException("$label cannot be in the future (got $iso). Pick today or a past date/time.")
    }
}

/** Throws when [day] ("YYYY-MM-DD") is after today. Call before every dated write. */
fun requireNotFutureDay(label: String, day: String?) {
    if (isFutureDay(day)) {
        throw IllegalStateException("$label cannot be a future date (got $day). Pick today or a past date.")
    }
}
