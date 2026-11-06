package com.example.ruwia.data

import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Requests sent to the `admin-users` Edge Function.
 *
 * These operations need the service-role key (creating auth users and banning
 * them), so they cannot run in the app. The function verifies the caller's JWT
 * and admin role before touching the database.
 */
@Serializable
internal data class CreateEmployeeRequest(
    val action: String,
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: String,
    val shopName: String,
    val salary: Double,
)

@Serializable
internal data class DeactivateEmployeeRequest(
    val action: String,
    val employeeId: String,
)

@Serializable
internal data class AdminEmployee(
    val id: String,
    val name: String,
    val phone: String = "",
    val role: String = "driver",
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("monthly_salary") val monthlySalary: Double = 0.0,
    val status: String = "active",
)

@Serializable
internal data class AdminFunctionResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val employee: AdminEmployee? = null,
)

/**
 * Invokes a privileged admin function. The Supabase Functions plugin attaches
 * the signed-in user's JWT automatically; the server decides whether that user
 * is allowed to perform the action.
 */
internal suspend inline fun <reified T : Any> invokeAdminFunction(body: T): AdminFunctionResponse {
    val response = try {
        supabase.functions.invoke("admin-users", body = body)
    } catch (e: Exception) {
        throw IllegalStateException(
            "Could not reach the secure admin service. Sign in again and retry.",
            e,
        )
    }
    val text = response.bodyAsText()
    return try {
        Json { ignoreUnknownKeys = true }.decodeFromString<AdminFunctionResponse>(text)
    } catch (e: Exception) {
        throw IllegalStateException("Unexpected response from the secure admin service.", e)
    }
}
