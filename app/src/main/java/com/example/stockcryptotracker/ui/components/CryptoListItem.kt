package com.example.stockcryptotracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.CryptoCurrency
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CryptoListItem(
    cryptocurrency: CryptoCurrency,
    onItemClick: (String) -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clickable { onItemClick(cryptocurrency.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cryptocurrency logo
            AsyncImage(
                model = cryptocurrency.image,
                contentDescription = "${cryptocurrency.name} logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Cryptocurrency name and symbol
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cryptocurrency.symbol.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Text(
                    text = cryptocurrency.name,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Add market cap
                Text(
                    text = "Market Cap: ${formatMarketCap(cryptocurrency.marketCap)}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            // Cryptocurrency price and price change
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = formatCurrency(cryptocurrency.currentPrice),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val priceChangeColor = if (cryptocurrency.priceChangePercentage24h >= 0) {
                        Color(0xFF4CAF50) // Green for positive change
                    } else {
                        Color(0xFFF44336) // Red for negative change
                    }
                    
                    val changeIcon = if (cryptocurrency.priceChangePercentage24h >= 0) 
                        Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    
                    Icon(
                        imageVector = changeIcon,
                        contentDescription = null,
                        tint = priceChangeColor,
                        modifier = Modifier.height(16.dp)
                    )
                    
                    Text(
                        text = formatPercentage(cryptocurrency.priceChangePercentage24h),
                        color = priceChangeColor,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Favorite button
            IconButton(
                onClick = onFavoriteClick
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

private fun formatPercentage(percentage: Double): String {
    return if (percentage >= 0) {
        "+${String.format("%.2f", percentage)}%"
    } else {
        "${String.format("%.2f", percentage)}%"
    }
}

private fun formatMarketCap(marketCap: Long): String {
    return when {
        marketCap >= 1_000_000_000_000 -> String.format("$%.2fT", marketCap / 1_000_000_000_000.0)
        marketCap >= 1_000_000_000 -> String.format("$%.2fB", marketCap / 1_000_000_000.0)
        marketCap >= 1_000_000 -> String.format("$%.2fM", marketCap / 1_000_000.0)
        else -> "$${marketCap}"
    }
} 