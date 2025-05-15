package com.example.stockcryptotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.viewmodel.PortfolioItemType
import com.example.stockcryptotracker.viewmodel.PortfolioItemUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PortfolioListItem(
    portfolioItem: PortfolioItemUiState,
    activeAlerts: List<PriceAlert> = emptyList(),
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAlertClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Asset logo
                AsyncImage(
                    model = portfolioItem.imageUrl,
                    contentDescription = "${portfolioItem.name} logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Asset name, symbol, type and quantity
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = portfolioItem.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${portfolioItem.symbol.uppercase()} • ${formatQuantity(portfolioItem.quantity)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Small badge to indicate asset type
                        Text(
                            text = when (portfolioItem.type) {
                                PortfolioItemType.CRYPTO -> "CRYPTO"
                                PortfolioItemType.STOCK -> "STOCK"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Value and price
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = formatCurrency(portfolioItem.value),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    
                    Text(
                        text = formatCurrency(portfolioItem.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                
                // Action buttons
                Column {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit portfolio item",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onAlertClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Set price alert",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove portfolio item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Display active alerts if there are any
            if (activeAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "Active Price Alerts:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Column(
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    activeAlerts.forEach { alert ->
                        val direction = if (alert.isAboveTarget) "above" else "below"
                        Text(
                            text = "• Alert when price is $direction ${formatCurrency(alert.targetPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        String.format("%.8f", quantity)
    }
} 