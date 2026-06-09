package com.example.ruwia.data

import com.example.ruwia.domain.DeliveryTask

class EmployeeRepository {

    suspend fun getTodayRouteTasks(employeeId: String): List<DeliveryTask> {
        return listOf(
            DeliveryTask("t1", "Senthil K.", "9876543201", "Sector 4, Block B-102", 4, "10:28 AM (5 mins)", "en_route"),
            DeliveryTask("t2", "Meena R.", "9876543202", "Ganga Apartments, Flat 304", 2, "10:45 AM", "queued"),
            DeliveryTask("t3", "Vignesh W.", "9876543203", "Nehru St, No. 14", 5, "11:15 AM", "queued"),
            DeliveryTask("t4", "Kavitha L.", "9876543204", "Shanthi Nagar, Cross Road 3", 1, "12:00 PM", "queued")
        )
    }

    suspend fun recordDeliveryCompletion(
        taskId: String,
        deliveredQty: Int,
        returnedEmptyQty: Int,
        collectedAmount: Double,
        paymentMode: String
    ) {
        // Log telemetry
    }

    suspend fun getDailyEarningsSummary(employeeId: String): Double {
        return 1850.0
    }

    suspend fun getDailyCansSummary(employeeId: String): Pair<Int, Int> {
        return Pair(68, 42) // Filled cans delivered, Empty cans returned
    }
}
