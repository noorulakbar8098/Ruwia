package com.example.ruwia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ruwia.presentation.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceOrderScreen(
    vm: UserViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var qty by remember { mutableStateOf("") }

    LaunchedEffect(state.orderPlaced) {
        if (state.orderPlaced) {
            vm.clearOrderPlaced()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place Order") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                "How many cans do you need?",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = qty,
                onValueChange = { v -> if (v.all(Char::isDigit)) qty = v },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            val n = qty.toIntOrNull() ?: 0
            Button(
                onClick = { vm.placeOrder(n) },
                enabled = !state.loading && n > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Place Order")
            }

            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
