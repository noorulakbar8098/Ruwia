package com.example.ruwia.data

import com.example.ruwia.domain.NotificationItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * In-app notification feed shared by the admin and employee apps
 * (see supabase_migration_notifications.sql).
 *
 * Directions:
 * - admin → all employees: [broadcastToEmployees] (`to_role = "employee"`)
 * - employee → admin: [sendToAdmin] (`to_role = "admin"`)
 *
 * Reads are per-user (`notification_reads`) so one broadcast row serves every
 * recipient. Everything is tenant-stamped with `admin_id`, matching the RLS
 * pattern of every other table. All functions swallow transport errors and
 * return empty/false — notifications must never break the flow that raised
 * them (a sale or stock entry).
 */
class NotificationRepository {

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** The admin whose tenant the current user belongs to (own id for admins). */
    suspend fun tenantAdminId(): String? {
        val uid = currentUserId() ?: return null
        return try {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingleOrNull<com.example.ruwia.domain.Profile>()
            profile?.adminId ?: uid
        } catch (_: Exception) {
            uid
        }
    }

    /** "admin" for admins, "employee" for everyone else. */
    suspend fun myRole(): String {
        val uid = currentUserId() ?: return "employee"
        return try {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingleOrNull<com.example.ruwia.domain.Profile>()
            if (profile?.role == com.example.ruwia.domain.UserRole.admin) "admin" else "employee"
        } catch (_: Exception) {
            "employee"
        }
    }

    /** Employee → admin (e.g. "New sale recorded"). */
    suspend fun sendToAdmin(title: String, body: String, kind: String = "general", relatedId: String? = null) {
        val me = currentUserId() ?: return
        val adminId = tenantAdminId() ?: return
        insertRow(adminId, me, toUserId = null, toRole = "admin", title, body, kind, relatedId)
    }

    /** Admin → every employee of the tenant (e.g. "New stock added"). */
    suspend fun broadcastToEmployees(title: String, body: String, kind: String = "general", relatedId: String? = null) {
        val me = currentUserId() ?: return
        val adminId = tenantAdminId() ?: return
        insertRow(adminId, me, toUserId = null, toRole = "employee", title, body, kind, relatedId)
    }

    /** Admin → one employee, such as a newly assigned delivery task. */
    suspend fun sendToEmployee(
        employeeId: String,
        title: String,
        body: String,
        kind: String = "general",
        relatedId: String? = null,
    ) {
        val me = currentUserId() ?: return
        val adminId = tenantAdminId() ?: return
        if (employeeId.isBlank()) return
        insertRow(adminId, me, employeeId, toRole = null, title, body, kind, relatedId)
    }

    private suspend fun insertRow(
        adminId: String,
        fromUserId: String,
        toUserId: String?,
        toRole: String?,
        title: String,
        body: String,
        kind: String,
        relatedId: String?,
    ) {
        try {
            supabase.from("notifications").insert(
                buildJsonObject {
                    put("admin_id", adminId)
                    put("from_user_id", fromUserId)
                    if (toUserId != null) put("to_user_id", toUserId)
                    if (toRole != null) put("to_role", toRole)
                    put("title", title.take(200))
                    put("body", body.take(500))
                    put("kind", kind)
                    if (relatedId != null) put("related_id", relatedId)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Everything visible to the current user (direct + role broadcasts + own
     * sent), newest first, with [NotificationItem.isRead] resolved.
     */
    suspend fun getFeed(limit: Int = 200): List<NotificationItem> {
        val me = currentUserId() ?: return emptyList()
        val adminId = tenantAdminId() ?: return emptyList()
        return try {
            val role = myRole()
            val rows = supabase.from("notifications")
                .select {
                    filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<NotificationItem>()
            val reads = supabase.from("notification_reads")
                .select { filter { eq("user_id", me) } }
                .decodeList<ReadMarker>()
                .map { it.notificationId }
                .toSet()
            rows.filter { n ->
                n.toUserId == me ||
                    (n.toRole == "employee" && role == "employee") ||
                    (n.toRole == "admin" && role == "admin") ||
                    n.fromUserId == me
            }.map { n -> n.copy(isRead = n.id in reads) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getUnreadCount(): Int {
        return try {
            getFeed().count { !it.isRead }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun markRead(notificationId: String) {
        val me = currentUserId() ?: return
        if (notificationId.isBlank()) return
        try {
            supabase.from("notification_reads").insert(
                buildJsonObject {
                    put("notification_id", notificationId)
                    put("user_id", me)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllRead(ids: List<String>) {
        ids.filter { it.isNotBlank() }.forEach { markRead(it) }
    }

    /**
     * Registers/refreshes this device's FCM token for outside-the-app push.
     * Insert-then-update by token allows one user to register multiple devices.
     */
    suspend fun upsertDeviceToken(token: String, platform: String = "android") {
        val me = currentUserId() ?: return
        if (token.isBlank()) return
        val adminId = tenantAdminId() ?: return
        fun payload() = buildJsonObject {
            put("user_id", me)
            put("admin_id", adminId)
            put("token", token)
            put("platform", platform)
            put("updated_at", kotlin.time.Clock.System.now().toString())
        }
        try {
            supabase.from("device_tokens").insert(payload())
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("duplicate", ignoreCase = true) || msg.contains("conflict", ignoreCase = true)) {
                try {
                    supabase.from("device_tokens").update(payload()) {
                        filter { eq("token", token) }
                    }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            } else {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeDeviceToken(token: String) {
        val me = currentUserId() ?: return
        if (token.isBlank()) return
        try {
            supabase.from("device_tokens").delete {
                filter {
                    eq("user_id", me)
                    eq("token", token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @kotlinx.serialization.Serializable
    private data class ReadMarker(
        @kotlinx.serialization.SerialName("notification_id") val notificationId: String = "",
    )
}
