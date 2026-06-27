package com.example.ruwia.data

import com.example.ruwia.domain.Profile
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    suspend fun signUp(email: String, password: String, fullName: String, phone: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildMap {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                put("phone", kotlinx.serialization.json.JsonPrimitive(phone))
            }.let { kotlinx.serialization.json.JsonObject(it) }
        }
    }

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Sign out and wipe the local session from disk.
     *
     * Supabase's `signOut()` makes a network call to revoke the token FIRST
     * and clears local storage AFTER the response. If this coroutine is
     * cancelled mid-flight (or the app is killed), the local session stays
     * on disk and gets auto-restored on next launch — meaning the user
     * appears to still be "logged in" as the previous account.
     *
     * Wrapping in [NonCancellable] guarantees the whole signOut runs to
     * completion regardless of parent cancellation, so the on-disk session
     * is always cleared.
     */
    suspend fun logout() = withContext(NonCancellable) {
        runCatching { supabase.auth.signOut() }
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun currentAdminId(): String? = myProfile()?.adminId ?: currentUserId()

    fun currentUserEmail(): String = supabase.auth.currentUserOrNull()?.email ?: ""

    suspend fun myProfile(): Profile? {
        val uid = currentUserId() ?: return null
        return supabase.from("profiles")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull()
    }

    suspend fun isAdmin(): Boolean = myProfile()?.role == UserRole.admin

    suspend fun promoteToAdmin(uid: String) {
        supabase.from("profiles").update(
            buildJsonObject {
                put("role", "admin")
            }
        ) {
            filter { eq("id", uid) }
        }
    }
}
