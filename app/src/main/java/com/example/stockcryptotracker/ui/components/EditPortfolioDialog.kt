package com.example.stockcryptotracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.viewmodel.PortfolioItemUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EditPortfolioDialog(
    crypto: CryptoCurrency,
    currentQuantity: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var quantity by remember { mutableStateOf((currentQuantity ?: 0.0).toString()) }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentQuantity != null) "Edit Quantity" else "Add to Portfolio",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Crypto info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = crypto.image,
                        contentDescription = "${crypto.name} logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = crypto.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = crypto.symbol.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = formatCurrency(crypto.currentPrice),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        quantity = it 
                        isError = !isValidQuantity(it)
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Enter a valid number greater than zero")
                        } else {
                            val parsedQuantity = quantity.toDoubleOrNull() ?: 0.0
                            val value = parsedQuantity * crypto.currentPrice
                            Text("Value: ${formatCurrency(value)}")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQuantity = quantity.toDoubleOrNull()
                    if (parsedQuantity != null && parsedQuantity > 0) {
                        onSave(parsedQuantity)
                    }
                },
                enabled = !isError && quantity.toDoubleOrNull() != null && quantity.toDoubleOrNull()!! > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun isValidQuantity(quantity: String): Boolean {
    return try {
        val value = quantity.toDouble()
        value > 0
    } catch (e: NumberFormatException) {
        false
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
} 