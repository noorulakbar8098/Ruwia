package com.example.ruwia.domain

enum class CrashSeverity(val label: String) {
    CRITICAL("Critical"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

enum class CrashStatus(val label: String) {
    OPEN("Open"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved")
}

data class CrashReport(
    val id: String,
    val title: String,
    val component: String,
    val severity: CrashSeverity,
    val status: CrashStatus,
    val errorMessage: String,
    val suspectedRootCause: String,
    val recommendedSteps: String,
    val timestamp: String = "Today, 10:42 AM",
)

fun defaultCrashReports(): List<CrashReport> = listOf(
    CrashReport(
        id = "CR-1001",
        title = "NullPointerException in CustomerAdapter binding",
        component = "CustomerAdapter.kt / DeliveryList",
        severity = CrashSeverity.CRITICAL,
        status = CrashStatus.OPEN,
        errorMessage = "java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String com.example.ruwia.domain.Customer.getName()' on a null object reference\n    at com.example.ruwia.ui.CustomerAdapter.onBindViewHolder(CustomerAdapter.kt:48)\n    at androidx.recyclerview.widget.RecyclerView\$Adapter.bindViewHolder(RecyclerView.java:7337)",
        suspectedRootCause = "Customer item retrieved from local database before background sync finished, causing an unpopulated null model in the UI adapter list.",
        recommendedSteps = "1. Add a non-null check and fallback placeholder model in CustomerAdapter.\n2. Ensure UI StateFlow only emits non-null Customer objects.\n3. Validate DB hydration before passing list to RecyclerView / LazyColumn.",
        timestamp = "Today, 10:42 AM",
    ),
    CrashReport(
        id = "CR-1002",
        title = "IllegalStateException: Fragment not attached to Context",
        component = "OrderConfirmationDialog.kt",
        severity = CrashSeverity.HIGH,
        status = CrashStatus.INVESTIGATING,
        errorMessage = "java.lang.IllegalStateException: Fragment OrderConfirmationDialog{a8f2b3} not attached to a context.\n    at androidx.fragment.app.Fragment.requireContext(Fragment.java:996)\n    at com.example.ruwia.ui.OrderConfirmationDialog\$onConfirm\$1.invokeSuspend(OrderConfirmationDialog.kt:82)",
        suspectedRootCause = "Coroutine callback accessed getString() or context after the user dismissed the dialog during a screen rotation/configuration change.",
        recommendedSteps = "1. Scope the asynchronous coroutine to viewLifecycleOwner.lifecycleScope.\n2. Guard context access with isAdded and view != null.\n3. Refactor dialog to Jetpack Compose AlertDialog to avoid Fragment lifecycle mismatch.",
        timestamp = "Today, 09:15 AM",
    ),
    CrashReport(
        id = "CR-1003",
        title = "SocketTimeoutException on Supabase order sync",
        component = "SupabaseClient.kt / SyncWorker",
        severity = CrashSeverity.HIGH,
        status = CrashStatus.OPEN,
        errorMessage = "java.net.SocketTimeoutException: failed to connect to srvertclbjcvlfdectsl.supabase.co/104.18.32.7 (port 443) after 15000ms\n    at okhttp3.internal.connection.RealCall.connect(RealCall.kt:182)\n    at io.github.jan.supabase.postgrest.Postgrest.execute(Postgrest.kt:94)",
        suspectedRootCause = "Offline-to-online transition with unstable cellular data triggered batched sync without connection pre-check or retry backoff.",
        recommendedSteps = "1. Check ConnectivityObserver before executing remote sync.\n2. Configure exponential backoff retry in Ktor/OkHttp client.\n3. Cache pending transactions locally in SQLite until network is stable.",
        timestamp = "Yesterday, 04:30 PM",
    ),
    CrashReport(
        id = "CR-1004",
        title = "BluetoothGatt 133 GATT_ERROR during thermal printer pairing",
        component = "BluetoothPrinterManager.kt",
        severity = CrashSeverity.MEDIUM,
        status = CrashStatus.OPEN,
        errorMessage = "com.example.ruwia.bluetooth.BluetoothException: GATT connection failed with status 133 (GATT_ERROR) during device handshake\n    at com.example.ruwia.bluetooth.BluetoothPrinterManager.connect(BluetoothPrinterManager.kt:114)\n    at android.bluetooth.BluetoothGatt\$1.onConnectionStateChange(BluetoothGatt.java:420)",
        suspectedRootCause = "Previous connection slot was not cleanly released, exhausting the Android Bluetooth stack GATT connection table on repeated printer reconnection.",
        recommendedSteps = "1. Explicitly invoke gatt.disconnect() followed by gatt.close() on disconnect.\n2. Add a 600ms debounce delay before opening a new GATT connection.\n3. Verify BLUETOOTH_CONNECT runtime permission is granted on Android 12+.",
        timestamp = "Oct 28, 02:10 PM",
    ),
    CrashReport(
        id = "CR-1005",
        title = "OutOfMemoryError: Failed to allocate bitmap in ProductCatalog",
        component = "ProductGrid.kt / ImageLoader",
        severity = CrashSeverity.LOW,
        status = CrashStatus.RESOLVED,
        errorMessage = "java.lang.OutOfMemoryError: Failed to allocate a 48000000 byte allocation with 16777216 free bytes and 24MB until OOM\n    at android.graphics.BitmapFactory.nativeDecodeStream(Native Method)\n    at android.graphics.BitmapFactory.decodeStream(BitmapFactory.java:542)\n    at com.example.ruwia.ui.ProductGridKt.ProductImage(ProductGrid.kt:128)",
        suspectedRootCause = "High-resolution product banners loaded into memory at full native resolution without downsampling or memory cache constraints.",
        recommendedSteps = "1. Enable automatic image downsampling matching target display bounds.\n2. Set bitmapConfig to RGB_565 or hardware bitmap acceleration.\n3. Configure max memory cache size to 25% of available app heap.",
        timestamp = "Oct 27, 11:20 AM",
    ),
)
