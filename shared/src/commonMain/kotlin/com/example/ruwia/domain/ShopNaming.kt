package com.example.ruwia.domain

const val DEFAULT_SHOP_ONE = "Shop 1"
const val DEFAULT_SHOP_TWO = "Shop 2"

/** Canonical fallback labels for a business that has exactly two shops. */
fun defaultShopNames(): List<String> = listOf(DEFAULT_SHOP_ONE, DEFAULT_SHOP_TWO)

private fun isDefaultShopName(name: String): Boolean {
    val key = shopMatchKey(name)
    return key == shopMatchKey(DEFAULT_SHOP_ONE) || key == shopMatchKey(DEFAULT_SHOP_TWO)
}

/**
 * Resolves persisted shop rows into the two stable slots used everywhere in
 * the app (0 = primary, 1 = secondary).
 *
 * Custom names win over legacy "Shop 1" / "Shop 2" placeholders: if at least
 * two real names exist, stray placeholder rows are ignored instead of being
 * shown as a third shop. Rows carrying an explicit primary/secondary
 * location are mapped to that slot first so a rename never swaps shops.
 */
fun orderedShopSlots(rows: List<ShopStockInfo>): List<ShopStockInfo> {
    val unique = rows
        .filter { it.name.trim().isNotEmpty() }
        .distinctBy { shopMatchKey(it.name) }
    val custom = unique.filterNot { isDefaultShopName(it.name) }
    val source = if (custom.size >= 2) custom else unique

    val slots = arrayOfNulls<ShopStockInfo>(2)
    fun explicitSlot(row: ShopStockInfo): Int? {
        val location = row.location.trim().lowercase()
        val key = shopMatchKey(row.name)
        return when {
            location.contains("primary") -> 0
            location.contains("secondary") -> 1
            key == shopMatchKey(DEFAULT_SHOP_ONE) -> 0
            key == shopMatchKey(DEFAULT_SHOP_TWO) -> 1
            else -> null
        }
    }

    val placed = mutableSetOf<ShopStockInfo>()
    source.forEach { row ->
        val slot = explicitSlot(row) ?: return@forEach
        if (slots[slot] == null) {
            slots[slot] = row
            placed += row
        }
    }
    source.forEach { row ->
        if (row in placed) return@forEach
        val slot = slots.indexOfFirst { it == null }
        if (slot >= 0) {
            slots[slot] = row
            placed += row
        }
    }
    return slots.filterNotNull()
}

/** Always returns exactly two display names, custom where configured. */
fun resolveShopNames(rows: List<ShopStockInfo>): List<String> {
    val defaults = defaultShopNames()
    val ordered = orderedShopSlots(rows)
    return List(2) { index ->
        ordered.getOrNull(index)?.name?.trim()?.takeIf { it.isNotEmpty() } ?: defaults[index]
    }
}
