package com.example.ruwia.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors

private object InvColors {
    val Background    get() = NTColors.Background
}

@Composable
fun InventoryScreen(
    state: AdminState,
    onBack: () -> Unit,
    onAddProductRequest: () -> Unit,
    onAddProductCategory: (ProductCategory, Int, String) -> Unit,
    onUpdateProduct: (ProductCategory) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAddMovement: (source: String, qty: Int, type: String, shopName: String, productId: String?) -> Unit,
    onAddStockPurchase: (product: ProductCategory?, currentStock: Int) -> Unit,
    onToggleProductStatus: (ProductCategory) -> Unit = {},
    onOpenStockHistory: () -> Unit = {},
    onSaveCustomerPrice: (CustomerProductPrice) -> Unit = {},
    onLoadCustomerPrices: (productId: String) -> Unit = {},
    onDeleteCustomerPrice: (customerId: String, productId: String) -> Unit = { _, _ -> },
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(modifier = Modifier.fillMaxSize().background(InvColors.Background)) {
        com.example.ruwia.ui.StockInventoryScreen(
            state = state,
            onBack = onBack,
            onAddMovement = onAddMovement,
            onAddStock = onAddStockPurchase,
            onAddProduct = onAddProductRequest,
            onDeleteProduct = onDeleteProduct,
            onToggleProductStatus = onToggleProductStatus,
            onOpenStockHistory = onOpenStockHistory,
            onSaveCustomerPrice = onSaveCustomerPrice,
            onLoadCustomerPrices = onLoadCustomerPrices,
            onDeleteCustomerPrice = onDeleteCustomerPrice,
            contentPadding = contentPadding
        )
    }
}