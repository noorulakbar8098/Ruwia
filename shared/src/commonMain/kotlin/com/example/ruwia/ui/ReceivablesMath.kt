package com.example.ruwia.ui

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Payment
import kotlin.math.abs
import kotlin.math.round

/** Aggregated money picture shown on the Payments & Dues screen. */
internal data class ReceivableTotals(
    val billed: Double,
    val collected: Double,
    val outstanding: Double,
)

/**
 * Derives the headline figures once so the UI never re-implements money math.
 * Legacy payments without a reference key are excluded because they pre-date
 * the ledger and would double count collected amounts.
 */
internal fun computeReceivableTotals(
    customers: List<Customer>,
    payments: List<Payment>,
): ReceivableTotals {
    val collected = payments
        .filter { !it.referenceKey.isNullOrBlank() }
        .sumOf { it.amount }
    val outstanding = customers.sumOf { it.balance.coerceAtLeast(0.0) }
    return ReceivableTotals(
        billed = outstanding + collected,
        collected = collected,
        outstanding = outstanding,
    )
}

/** Formats an amount with Indian digit grouping, e.g. 123456.5 -> ₹1,23,456.50. */
internal fun formatReceivableAmount(value: Double): String {
    if (!value.isFinite()) return "₹0"
    val negative = value < 0
    val paiseTotal = round(abs(value) * 100).toLong()
    val rupees = paiseTotal / 100
    val paise = (paiseTotal % 100).toInt()
    val decimals = if (paise == 0) "" else "." + paise.toString().padStart(2, '0')
    val sign = if (negative) "-₹" else "₹"
    return sign + groupIndianDigits(rupees.toString()) + decimals
}

private fun groupIndianDigits(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    var rest = digits.dropLast(3)
    val groups = mutableListOf<String>()
    while (rest.length > 2) {
        groups.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) groups.add(0, rest)
    return (groups + lastThree).joinToString(",")
}

/**
 * Keeps the amount field numeric and readable while typing: at most one dot
 * and at most two paise digits. Empty stays empty so the placeholder shows.
 */
internal fun sanitizePaymentAmountInput(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    if (cleaned.isEmpty()) return ""
    val dot = cleaned.indexOf('.')
    if (dot < 0) {
        return cleaned.trimStart('0').ifEmpty { "0" }
    }
    val whole = cleaned.substring(0, dot)
        .filter { it.isDigit() }
        .trimStart('0')
        .ifEmpty { "0" }
    val paise = cleaned.substring(dot + 1)
        .filter { it.isDigit() }
        .take(2)
    return whole + "." + paise
}

internal fun parsePaymentAmount(raw: String): Double = raw.toDoubleOrNull() ?: 0.0

internal fun isValidPaymentAmount(amount: Double, outstanding: Double): Boolean =
    amount.isFinite() && amount > 0.0 && amount <= outstanding + 0.001

/** Pre-fills the amount field with the full balance, without currency noise. */
internal fun paymentAmountInput(outstanding: Double): String =
    formatReceivableAmount(outstanding)
        .removePrefix("₹")
        .removePrefix("-₹")
        .replace(",", "")

/** Collected share of billed money, clamped to 0..1 for progress rendering. */
internal fun collectionProgress(collected: Double, billed: Double): Float {
    if (!billed.isFinite() || billed <= 0.0) return 0f
    return (collected / billed).coerceIn(0.0, 1.0).toFloat()
}
