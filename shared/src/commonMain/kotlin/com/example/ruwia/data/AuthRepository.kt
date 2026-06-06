package com.example.ruwia.data

import com.example.ruwia.domain.Profile
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository {

    suspend fun signUp(email: String, password: String, fullName: String, phone: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildMap {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
            }.let { kotlinx.serialization.json.JsonObject(it) }
        }
    }

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun logout() = supabase.auth.signOut()

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun myProfile(): Profile? {
        val uid = currentUserId() ?: return null
        return supabase.from("profiles")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull()
    }

    suspend fun isAdmin(): Boolean = myProfile()?.role == UserRole.admin
}
