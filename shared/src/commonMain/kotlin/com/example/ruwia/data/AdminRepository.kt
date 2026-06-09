package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Profile
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AdminRepository {

    suspend fun getMRR(): Double {
        // High level SaaS metric
        return 24500.0
    }

    suspend fun getCSAT(): Double {
        // High level SaaS CSAT score
        return 4.7
    }

    suspend fun getActiveFleetCount(): Int = 32

    suspend fun getStockSummaryList(): List<StockItem> {
        return listOf(
            StockItem("1", "20L Water can", 20, 520, 60.0, costPrice = 35.0, tags = "DAILY · STAPLE"),
            StockItem("2", "10L Water can", 10, 150, 50.0, costPrice = 25.0, tags = "FAMILY · MID-SIZE"),
            StockItem("3", "5L Water can",   5, 230, 30.0, costPrice = 15.0, tags = "TRAVEL · SMALL"),
        )
    }

    suspend fun getAllOrders(): List<Order> {
        return try {
            supabase.from("orders")
                .select {
                    order("created_at", SortOrder.DESCENDING)
                    limit(20)
                }
                .decodeList()
        } catch (e: Exception) {
            // Fallback mock data if table is not seeded
            listOf(
                Order("o1", "c1", 4, "pending", "2026-06-06T10:00:00Z"),
                Order("o2", "c2", 2, "approved", "2026-06-06T09:15:00Z"),
                Order("o3", "c3", 10, "delivered", "2026-06-06T08:30:00Z")
            )
        }
    }

    suspend fun getAllCustomers(): List<Customer> {
        return try {
            supabase.from("customers").select().decodeList()
        } catch (e: Exception) {
            listOf(
                Customer("c1", null, "Rajesh Kumar", "9876543210", "12, Park Ave", 5, 120.0),
                Customer("c2", null, "Priya Sharma", "9876543211", "45, Ring Rd", 2, 0.0),
                Customer("c3", null, "Sneha Gupta", "9876543212", "78, Crescent Blvd", 12, 450.0)
            )
        }
    }

    suspend fun getEmployees(): List<EmployeeInfo> {
        return listOf(
            EmployeeInfo("e1", "Ravi Velu",  "+91 98432 11290", "active",      "manager", "Shop 1", 24, 32, 22000.0, 4.9, 312, "TODAY"),
            EmployeeInfo("e2", "Dinesh M.",  "+91 96008 78123", "on_delivery", "stock",   "Shop 1", 12, 20, 16000.0, 4.7,  12, "DELIVERIES"),
            EmployeeInfo("e3", "Prakash K.", "+91 89512 34890", "active",      "manager", "Shop 2", 18, 28, 21000.0, 4.8, 428, "TODAY"),
            EmployeeInfo("e4", "Suresh R.",  "+91 97809 44321", "active",      "stock",   "Shop 2",  8, 15, 15000.0, 4.5,   8, "ACTIVE"),
            EmployeeInfo("e5", "Jaya K.",    "+91 93456 78901", "inactive",    "cashier", "Shop 1",  0,  0, 14000.0, 5.0,   0, "OFFLINE"),
        )
    }

    suspend fun getShopStocks(): List<ShopStockInfo> = listOf(
        ShopStockInfo("s1", "Shop 1 · Main",   "SAIBABA COLONY", 252, 142,  87, 23, true),
        ShopStockInfo("s2", "Shop 2 · Branch", "RS PURAM",       340, 198, 110, 32, true),
    )

    suspend fun getRecentMovements(): List<StockMovement> = listOf(
        StockMovement("m1", "Aqua Pure Plant",  50, "inward",      "Shop 1 · Main",   "2026-06-09"),
        StockMovement("m2", "Customer Delivery",12, "outward",     "Shop 1 · Main",   "2026-06-09"),
        StockMovement("m3", "Supplier XL",     100, "inward",      "Shop 2 · Branch", "2026-06-08"),
        StockMovement("m4", "Empty Return",     18, "adjustment",  "Shop 1 · Main",   "2026-06-08"),
    )

    /** Returns normalised 0f-1f points (Mon–Sun) + formatted label for the weekly revenue chart. */
    suspend fun getWeeklyRevenueSummary(): Pair<List<Float>, String> {
        // In production: query a daily_revenue view from Supabase for the current week.
        val raw = listOf(18_400f, 21_200f, 19_600f, 24_800f, 26_400f, 29_100f, 31_500f)
        val max = raw.max()
        val points = raw.map { it / max }
        val total = raw.sum()
        val label = when {
            total >= 1_00_000 -> "₹%.2f L".format(total / 1_00_000f)
            else              -> "₹%.0f".format(total)
        }
        return points to label
    }

    /**
     * Creates a new employee account end-to-end:
     *  1. Supabase Auth → creates auth user with email + password (no confirmation email)
     *  2. profiles table → sets role = employee, full_name, phone
     *  3. employees table → stores role, shop, salary, status
     *
     * Uses [supabaseAdmin] (service-role key) so the admin's own session is untouched.
     */
    suspend fun addEmployee(
        name: String,
        phone: String,
        role: String,
        shopName: String,
        salary: Double,
        email: String,
        password: String
    ): EmployeeInfo {
        // ── Step 0: Load service-role JWT as Bearer token ────────────────
        //   The admin API requires Authorization: Bearer <service-role-jwt>.
        //   Initialising the client with the key only sets the `apikey` header;
        //   importAuthToken also sets the session access token → Bearer header.
        initAdminSession()

        // ── Step 1: Create auth user (skips confirmation email) ──────────
        //   createUserWithEmail  →  AdminUserBuilder.Email lambda
        //   autoConfirm = true   →  serialised as "email_confirm": true
        val newUser = supabaseAdmin.auth.admin.createUserWithEmail {
            this.email       = email
            this.password    = password
            this.autoConfirm = true
        }
        val userId = newUser.id

        // ── Step 2: Upsert profile row with employee role ─────────────────
        supabaseAdmin.from("profiles").upsert(
            Profile(id = userId, role = UserRole.employee, fullName = name, phone = phone)
        )

        // ── Step 3: Insert into employees table ───────────────────────────
        supabaseAdmin.from("employees").insert(
            buildJsonObject {
                put("id",             userId)
                put("role",           role.lowercase())
                put("shop_name",      shopName)
                put("monthly_salary", salary)
                put("status",         "active")
            }
        )

        return EmployeeInfo(
            id            = userId,
            name          = name,
            phone         = phone,
            role          = role.lowercase(),
            shopName      = shopName,
            monthlySalary = salary,
            status        = "active"
        )
    }

    suspend fun assignEmployee(orderId: String, employeeId: String) {
        // Assign logic
    }

    suspend fun addStockPurchase(name: String, liters: Int, qty: Int, rate: Double) {
        // Purchase entry logic
    }
}
