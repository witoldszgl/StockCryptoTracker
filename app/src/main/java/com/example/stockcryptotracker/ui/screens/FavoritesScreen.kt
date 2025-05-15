package com.example.stockcryptotracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.ui.components.CryptoListItem
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import com.example.stockcryptotracker.viewmodel.StockViewModel
import com.example.stockcryptotracker.viewmodel.Tab
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToCryptoDetail: (String) -> Unit,
    onNavigateToStockDetail: (String) -> Unit,
    cryptoViewModel: CryptoViewModel = viewModel(),
    stockViewModel: StockViewModel = viewModel()
) {
    // Set Favorites tab in CryptoViewModel
    cryptoViewModel.setSelectedTab(Tab.FAVORITES)
    
    val cryptoList by cryptoViewModel.cryptoList.collectAsState()
    val stockFavoriteIds by stockViewModel.favoriteIds.collectAsState(initial = emptyList())
    val stockList by stockViewModel.stockList.collectAsState()
    
    // Filter favorite stocks
    val favoriteStocks = stockList.filter { stock ->
        stockFavoriteIds.contains(stock.symbol)
    }
    
    val cryptoIsLoading by cryptoViewModel.isLoading.collectAsState()
    val stockIsLoading by stockViewModel.isLoading.collectAsState()
    val isLoading = cryptoIsLoading || stockIsLoading
    
    val cryptoError by cryptoViewModel.error.collectAsState()
    val stockError by stockViewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading && cryptoList.isEmpty() && favoriteStocks.isEmpty() -> {
                    CircularProgressIndicator()
                }
                
                cryptoError != null && stockError != null -> {
                    Text(
                        text = "Error: Failed to load data",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                cryptoList.isEmpty() && favoriteStocks.isEmpty() -> {
                    Text(
                        text = "You don't have any favorites yet.\n" +
                              "Add cryptocurrencies or stocks to your favorites using the star icon.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Cryptocurrency section
                        if (cryptoList.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Cryptocurrencies",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            items(cryptoList) { crypto ->
                                val isFavorite by cryptoViewModel.isFavorite(crypto.id).collectAsState(initial = true)
                                
                                CryptoListItem(
                                    cryptocurrency = crypto,
                                    onItemClick = { cryptoId ->
                                        onNavigateToCryptoDetail(cryptoId)
                                    },
                                    isFavorite = isFavorite,
                                    onFavoriteClick = {
                                        cryptoViewModel.toggleFavorite(crypto)
                                    }
                                )
                            }
                        }
                        
                        // Stocks section
                        if (favoriteStocks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Stocks",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            items(favoriteStocks) { stock ->
                                val isFavorite by stockViewModel.isFavorite(stock.symbol).collectAsState(initial = true)
                                
                                FavoriteStockItem(
                                    stock = stock,
                                    onItemClick = { onNavigateToStockDetail(stock.symbol) },
                                    isFavorite = isFavorite,
                                    onFavoriteClick = { stockViewModel.toggleFavorite(stock) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteStockItem(
    stock: Stock,
    onItemClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onItemClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stock logo
            AsyncImage(
                model = stock.logoUrl,
                contentDescription = "${stock.name} logo",
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Stock info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stock.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Text(
                    text = stock.name,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = stock.exchange,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            // Price and change
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = formatCurrency(stock.price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val changeColor = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val changeIcon = if (stock.change >= 0) 
                        Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    
                    Icon(
                        imageVector = changeIcon,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.height(16.dp)
                    )
                    
                    Text(
                        text = "${String.format("%.2f", stock.change)} (${String.format("%.2f", stock.changePercent * 100)}%)",
                        color = changeColor,
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

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(value)
} 