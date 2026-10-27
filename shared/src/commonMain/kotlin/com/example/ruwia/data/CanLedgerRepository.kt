package com.example.ruwia.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Single entry point for customer-wise 20L can balance changes. Both the
 * employee and admin apps call the same Postgres function so sale-time
 * returns and standalone returns share one atomic ledger.
 */
internal object CanLedgerRepository {

    suspend fun apply(
        customerId: String,
        issued: Int,
        returned: Int,
        referenceKey: String,
        movementType: String,
        employeeId: String?,
        shopName: String,
        recordInwardStock: Boolean,
    ): Int {
        require(customerId.isNotBlank()) { "Select a saved customer before recording 20L cans." }
        require(issued >= 0 && returned >= 0) { "Can counts cannot be negative." }
        require(referenceKey.isNotBlank()) { "A ledger reference key is required." }

        return try {
            supabase.postgrest.rpc(
                "apply_customer_can_ledger",
                buildJsonObject {
                    put("p_customer_id", customerId)
                    put("p_issued", issued)
                    put("p_returned", returned)
                    put("p_reference_key", referenceKey)
                    put("p_movement_type", movementType)
                    if (!employeeId.isNullOrBlank()) put("p_employee_id", employeeId)
                    put("p_shop_name", shopName.ifBlank { "Shop 1" })
                    put("p_record_inward_stock", recordInwardStock)
                }
            ).decodeAs<Int>()
        } catch (e: Exception) {
            if (isMissingLedgerFunction(e)) {
                throw IllegalStateException(
                    "20L can tracking is not configured on the database. " +
                        "Run supabase_migration_can_ledger.sql in the Supabase SQL Editor, then try again.",
                    e,
                )
            }
            throw e
        }
    }

    private fun isMissingLedgerFunction(e: Exception): Boolean {
        val message = e.message.orEmpty()
        return message.contains("apply_customer_can_ledger", ignoreCase = true) &&
            (message.contains("does not exist", ignoreCase = true) ||
                message.contains("could not find the function", ignoreCase = true) ||
                message.contains("PGRST202", ignoreCase = true))
    }
}
