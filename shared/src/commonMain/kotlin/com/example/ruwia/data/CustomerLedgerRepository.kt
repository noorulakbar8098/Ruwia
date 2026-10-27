package com.example.ruwia.data

import com.example.ruwia.domain.CustomerLedgerEntry
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Single entry point for a customer's money account.
 *
 * Every sale is a debit and every payment is a credit. The Postgres functions
 * lock the customer row and update `customers.balance` in the same transaction,
 * so the statement and the cached balance can never disagree. Callers pass a
 * stable reference key, making retries and double taps safe.
 */
internal object CustomerLedgerRepository {

    suspend fun applySale(
        customerId: String,
        saleKey: String,
        invoiceTotal: Double,
        paidNow: Double,
        paymentMode: String,
        note: String? = null,
        employeeId: String? = null,
        shopName: String? = null,
    ): Double {
        require(customerId.isNotBlank()) { "Select a saved customer before recording a payment." }
        require(saleKey.isNotBlank()) { "A sale key is required to record the sale." }
        require(invoiceTotal >= 0.0) { "Sale total cannot be negative." }
        require(paidNow >= 0.0) { "Payment received cannot be negative." }
        require(paidNow <= invoiceTotal + 0.001) {
            "Payment received cannot be more than the sale total."
        }

        return try {
            supabase.postgrest.rpc(
                "apply_customer_sale",
                buildJsonObject {
                    put("p_customer_id", customerId)
                    put("p_sale_key", saleKey)
                    put("p_total", invoiceTotal)
                    put("p_paid_now", paidNow)
                    put("p_payment_mode", paymentMode.ifBlank { "cash" })
                    if (!note.isNullOrBlank()) put("p_note", note)
                    if (!employeeId.isNullOrBlank()) put("p_employee_id", employeeId)
                    if (!shopName.isNullOrBlank()) put("p_shop_name", shopName)
                },
            ).decodeAs<Double>()
        } catch (e: Exception) {
            throw missingFunctionAsActionable(e)
        }
    }

    suspend fun applyPayment(
        customerId: String,
        amount: Double,
        paymentMode: String,
        referenceKey: String,
        saleClientKey: String? = null,
        note: String? = null,
    ): Double {
        require(customerId.isNotBlank()) { "Select a saved customer before recording a payment." }
        require(amount > 0.0) { "Enter a payment amount greater than zero." }
        require(referenceKey.isNotBlank()) { "A payment key is required." }

        return try {
            supabase.postgrest.rpc(
                "apply_customer_payment",
                buildJsonObject {
                    put("p_customer_id", customerId)
                    put("p_amount", amount)
                    put("p_payment_mode", paymentMode.ifBlank { "cash" })
                    put("p_reference_key", referenceKey)
                    if (!saleClientKey.isNullOrBlank()) put("p_sale_client_key", saleClientKey)
                    if (!note.isNullOrBlank()) put("p_note", note)
                },
            ).decodeAs<Double>()
        } catch (e: Exception) {
            throw missingFunctionAsActionable(e)
        }
    }

    suspend fun getLedger(customerId: String): List<CustomerLedgerEntry> {
        if (customerId.isBlank()) return emptyList()
        val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.from("customer_account_ledger")
            .select {
                filter {
                    eq("admin_id", adminId)
                    eq("customer_id", customerId)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()
    }

    private fun missingFunctionAsActionable(e: Exception): Exception {
        val message = e.message.orEmpty()
        val missing = (
            message.contains("apply_customer_sale", ignoreCase = true) ||
                message.contains("apply_customer_payment", ignoreCase = true)
            ) && (
            message.contains("does not exist", ignoreCase = true) ||
                message.contains("could not find the function", ignoreCase = true) ||
                message.contains("PGRST202", ignoreCase = true)
            )
        return if (missing) {
            IllegalStateException(
                "Customer payment tracking is not configured on the database. " +
                    "Run supabase_migration_customer_payments.sql in the Supabase SQL Editor, then try again.",
                e,
            )
        } else {
            e
        }
    }
}
