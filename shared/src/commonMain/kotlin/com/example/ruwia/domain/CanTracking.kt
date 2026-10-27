package com.example.ruwia.domain

/** Canonical product size parsed from a SKU or display name. */
enum class SizeUnit { MILLILITRE, LITRE }

data class ProductSize(val value: Double, val unit: SizeUnit)

private val SIZE_PATTERN = Regex(
    pattern = """(\d+(?:[.,]\d+)?)\s*(ml|millilitre|milliliter|millilitres|milliliters|l|lt|ltr|litre|liter|litres|liters)\b""",
    option = RegexOption.IGNORE_CASE,
)

/**
 * Parses the first size token in [raw], e.g. "20L", "Neer Thuli - 20 L",
 * "500ml". Returns null when no recognised size token exists.
 *
 * Deliberately token-based: "200ml" parses as 200 ml (never 20 L), and
 * "201L" parses as 201 L, so physical 20L cans are never confused with
 * litre quantities or other pack sizes.
 */
fun canonicalProductSize(raw: String): ProductSize? {
    val match = SIZE_PATTERN.find(raw) ?: return null
    val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val unit = when (match.groupValues[2].lowercase()) {
        "ml", "millilitre", "milliliter", "millilitres", "milliliters" -> SizeUnit.MILLILITRE
        else -> SizeUnit.LITRE
    }
    return ProductSize(value, unit)
}

/**
 * True only for physical 20-litre can SKUs. The canonical SKU [name] wins;
 * the branded [displayName] is only consulted when the SKU has no size token.
 */
fun ProductCategory.is20LitreCan(): Boolean {
    val size = canonicalProductSize(name) ?: canonicalProductSize(displayName) ?: return false
    return size.unit == SizeUnit.LITRE && size.value == 20.0
}

/** Derived preview for the Sale Entry / Return Case UI. */
data class CanBalancePreview(
    val previouslyOutstanding: Int,
    val newCansIssued: Int,
    val emptyCansReceived: Int,
) {
    /**
     * Upper bound for a return in this transaction. Only the balance carried
     * into the sale is returnable; cans issued by this same entry are new
     * stock and cannot be handed straight back.
     */
    val maxReturnable: Int
        get() = previouslyOutstanding.coerceAtLeast(0)

    /** Balance shown after this transaction; never below zero. */
    val outstandingAfter: Int
        get() = (previouslyOutstanding + newCansIssued - emptyCansReceived).coerceAtLeast(0)

    val isWithinLimit: Boolean
        get() = emptyCansReceived in 0..maxReturnable
}

fun canBalancePreview(
    previouslyOutstanding: Int,
    newCansIssued: Int,
    emptyCansReceived: Int,
): CanBalancePreview = CanBalancePreview(
    previouslyOutstanding = previouslyOutstanding.coerceAtLeast(0),
    newCansIssued = newCansIssued.coerceAtLeast(0),
    emptyCansReceived = emptyCansReceived.coerceAtLeast(0),
)

/** Parses a digits-only can count. Null when blank, negative or not a number. */
fun parseCanCount(raw: String): Int? =
    raw.trim().toIntOrNull()?.takeIf { it >= 0 }
