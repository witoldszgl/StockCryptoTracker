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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.stockcryptotracker.data.Stock
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStockPortfolioDialog(
    stock: Stock,
    currentQuantity: Double? = null,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var quantityText by remember { 
        mutableStateOf(
            if (currentQuantity != null && currentQuantity > 0) {
                currentQuantity.toString()
            } else {
                ""
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (currentQuantity != null) {
                    "Edit ${stock.symbol} holding"
                } else {
                    "Add ${stock.symbol} to portfolio"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Stock info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = stock.image,
                        contentDescription = "${stock.name} logo",
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
                            text = stock.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = stock.symbol.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = formatPortfolioPrice(stock.price),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { 
                        quantityText = it
                        error = null
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                
                // Total value calculation
                val quantity = quantityText.toDoubleOrNull() ?: 0.0
                if (quantity > 0) {
                    val totalValue = quantity * stock.price
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Value:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = formatPortfolioPrice(totalValue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantity = quantityText.toDoubleOrNull()
                    if (quantity == null || quantity <= 0) {
                        error = "Please enter a valid quantity"
                    } else {
                        onSave(quantity)
                    }
                }
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

private fun formatPortfolioPrice(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
} 