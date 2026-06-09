package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.StockItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder

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
            StockItem("1", "5L Purified Can", 5, 230, 40.0),
            StockItem("2", "10L Purified Can", 10, 150, 70.0),
            StockItem("3", "20L Purified Can", 20, 520, 120.0)
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
            EmployeeInfo("e1", "Arul S.", "9123456780", "on_delivery", 24, 32, 18000.0, 4.8),
            EmployeeInfo("e2", "Senthil K.", "9123456781", "active", 18, 20, 16000.0, 4.6),
            EmployeeInfo("e3", "Meena R.", "9123456782", "active", 0, 0, 15000.0, 5.0)
        )
    }

    suspend fun assignEmployee(orderId: String, employeeId: String) {
        // Assign logic
    }

    suspend fun addStockPurchase(name: String, liters: Int, qty: Int, rate: Double) {
        // Purchase entry logic
    }
}
