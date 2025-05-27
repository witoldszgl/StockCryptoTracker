package com.example.stockcryptotracker.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.example.stockcryptotracker.network.StockSearchResult
import com.example.stockcryptotracker.ui.screens.DataSourceBanner
import com.example.stockcryptotracker.viewmodel.StockCategory
import com.example.stockcryptotracker.viewmodel.StockViewModel
import java.text.NumberFormat
import java.util.Locale
import com.example.stockcryptotracker.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: StockViewModel = viewModel(),
    context: Context
) {
    val stockList by viewModel.stockList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isUsingMockData by viewModel.isUsingMockData.collectAsState()
    val isUsingSavedData by viewModel.isUsingSavedData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Stock Market") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Add refresh button
                    IconButton(onClick = { viewModel.loadStocks() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text(text = "Search stocks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(StockCategory.values()) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { viewModel.setCategory(category) },
                        label = {
                            Text(
                                when (category) {
                                    StockCategory.ALL -> "All"
                                    StockCategory.MOST_ACTIVE -> "Most Active"
                                    StockCategory.TOP_GAINERS -> "Top Gainers"
                                    StockCategory.TOP_LOSERS -> "Top Losers"
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading && stockList.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading data from API...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    error != null && stockList.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadStocks() }
                            ) {
                                Text(text = "Try Again")
                            }
                        }
                    }
                    searchQuery.isNotEmpty() && searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                DataSourceBanner(
                                    isVisible = isUsingMockData,
                                    isUsingMockData = isUsingMockData,
                                    isUsingSavedData = isUsingSavedData,
                                    onRefreshClick = { viewModel.tryUsingRealData() }
                                )
                            }
                            
                            if (searchQuery.isNotEmpty() && !isLoading) {
                                items(searchResults) { result ->
                                    SearchResultItem(
                                        result = result,
                                        onClick = {
                                            onNavigateToDetail(result.symbol)
                                        }
                                    )
                                }
                            } else {
                                items(stockList) { stock ->
                                    StockListItem(
                                        stock = stock,
                                        onItemClick = { onNavigateToDetail(stock.symbol) },
                                        isFavorite = viewModel.isFavorite(stock.symbol).collectAsState().value,
                                        onFavoriteClick = { viewModel.toggleFavorite(stock) }
                                    )
                                }
                            }
                        }
                    }
                    stockList.isEmpty() && searchQuery.isNotEmpty() -> {
                        Text(
                            text = "No stocks found for: \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    stockList.isEmpty() -> {
                        Text(
                            text = "No stock data available",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                DataSourceBanner(
                                    isVisible = isUsingMockData,
                                    isUsingMockData = isUsingMockData,
                                    isUsingSavedData = isUsingSavedData,
                                    onRefreshClick = { viewModel.tryUsingRealData() }
                                )
                            }
                            
                            items(stockList) { stock ->
                                StockListItem(
                                    stock = stock,
                                    onItemClick = { onNavigateToDetail(stock.symbol) },
                                    isFavorite = viewModel.isFavorite(stock.symbol).collectAsState().value,
                                    onFavoriteClick = { viewModel.toggleFavorite(stock) }
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
fun StockListItem(
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
                model = stock.image,
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
            }
            
            // Price and change
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = formatCurrency(stock.currentPrice),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                val changeColor = if (stock.priceChangePercentage24h >= 0) 
                    Color(0xFF4CAF50) else Color(0xFFE53935)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (stock.priceChangePercentage24h >= 0) 
                            Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Change direction",
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "%.2f%%".format(stock.priceChangePercentage24h),
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
                    tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    result: StockSearchResult,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Text(
                    text = result.securityName,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = result.securityType,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCurrencyPrice(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(value)
}

private fun formatPercentageChange(value: Double): String {
    val prefix = if (value >= 0) "+" else ""
    return "$prefix${String.format("%.2f", value)}%"
} 