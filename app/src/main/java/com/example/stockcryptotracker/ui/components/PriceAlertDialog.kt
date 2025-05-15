package com.example.stockcryptotracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertDialog(
    cryptoCurrency: CryptoCurrency,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onSetAlert: (targetPrice: Double, isAboveTarget: Boolean) -> Unit
) {
    var targetPrice by remember { mutableStateOf(currentPrice.toString()) }
    var isAboveTarget by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Price Alert for ${cryptoCurrency.symbol.uppercase()}") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Current price: ${formatCurrency(currentPrice)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { 
                        targetPrice = it
                        showError = false
                    },
                    label = { Text("Target Price (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Text(
                        text = "Please enter a valid price",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Alert me when price is:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = isAboveTarget,
                        onClick = { isAboveTarget = true }
                    )
                    Text(
                        text = "Above target price",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = !isAboveTarget,
                        onClick = { isAboveTarget = false }
                    )
                    Text(
                        text = "Below target price",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val price = targetPrice.toDouble()
                        if (price > 0) {
                            onSetAlert(price, isAboveTarget)
                        } else {
                            showError = true
                        }
                    } catch (e: NumberFormatException) {
                        showError = true
                    }
                }
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPriceAlertDialog(
    stock: Stock,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onSetAlert: (targetPrice: Double, isAboveTarget: Boolean) -> Unit
) {
    var targetPrice by remember { mutableStateOf(currentPrice.toString()) }
    var isAboveTarget by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Price Alert for ${stock.symbol.uppercase()}") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Current price: ${formatCurrency(currentPrice)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { 
                        targetPrice = it
                        showError = false
                    },
                    label = { Text("Target Price (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Text(
                        text = "Please enter a valid price",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Alert me when price is:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = isAboveTarget,
                        onClick = { isAboveTarget = true }
                    )
                    Text(
                        text = "Above target price",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = !isAboveTarget,
                        onClick = { isAboveTarget = false }
                    )
                    Text(
                        text = "Below target price",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val price = targetPrice.toDouble()
                        if (price > 0) {
                            onSetAlert(price, isAboveTarget)
                        } else {
                            showError = true
                        }
                    } catch (e: NumberFormatException) {
                        showError = true
                    }
                }
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
} 