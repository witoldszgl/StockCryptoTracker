package com.example.stockcryptotracker.ui.screens

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.viewmodel.CryptoDetailViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    cryptoId: String,
    onBackClick: () -> Unit,
    viewModel: CryptoDetailViewModel = viewModel()
) {
    val cryptoDetail by viewModel.cryptoDetail.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()

    LaunchedEffect(key1 = cryptoId) {
        viewModel.loadCryptoDetail(cryptoId)
        viewModel.loadPriceHistory(cryptoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cryptoDetail?.name ?: "Cryptocurrency Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
                isLoading && cryptoDetail == null -> {
                    CircularProgressIndicator()
                }
                error != null && cryptoDetail == null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                cryptoDetail != null -> {
                    val detail = cryptoDetail!!
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header with image and basic info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            AsyncImage(
                                model = detail.image.large,
                                contentDescription = "${detail.name} logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = detail.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = detail.symbol.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                val currentPrice = detail.market_data.current_price["usd"]?.toDouble() ?: 0.0
                                Text(
                                    text = formatCurrency(currentPrice),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val priceChangeColor = if (detail.market_data.price_change_percentage_24h >= 0) {
                                    Color(0xFF4CAF50) // Green for positive change
                                } else {
                                    Color(0xFFF44336) // Red for negative change
                                }
                                
                                Text(
                                    text = formatPercentage(detail.market_data.price_change_percentage_24h),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = priceChangeColor
                                )
                            }
                        }
                        
                        // Price chart
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Price Chart",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Time range selection
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    TimeRangeButton(
                                        text = "24H",
                                        selected = selectedTimeRange == "1",
                                        onClick = { viewModel.loadPriceHistory(cryptoId, "1") }
                                    )
                                    TimeRangeButton(
                                        text = "7D",
                                        selected = selectedTimeRange == "7",
                                        onClick = { viewModel.loadPriceHistory(cryptoId, "7") }
                                    )
                                    TimeRangeButton(
                                        text = "30D",
                                        selected = selectedTimeRange == "30",
                                        onClick = { viewModel.loadPriceHistory(cryptoId, "30") }
                                    )
                                    TimeRangeButton(
                                        text = "1Y",
                                        selected = selectedTimeRange == "365",
                                        onClick = { viewModel.loadPriceHistory(cryptoId, "365") }
                                    )
                                }
                                
                                // Chart
                                if (priceHistory.isNotEmpty()) {
                                    PriceChart(
                                        pricePoints = priceHistory,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                        
                        // Market stats
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Market Statistics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                val marketData = detail.market_data
                                
                                val marketCap = marketData.market_cap["usd"]?.toDouble() ?: 0.0
                                val totalVolume = marketData.total_volume["usd"]?.toDouble() ?: 0.0
                                val high24h = marketData.high_24h["usd"]?.toDouble() ?: 0.0
                                val low24h = marketData.low_24h["usd"]?.toDouble() ?: 0.0
                                
                                StatRow(
                                    label = "Market Cap", 
                                    value = formatCurrency(marketCap)
                                )
                                
                                StatRow(
                                    label = "24h Volume", 
                                    value = formatCurrency(totalVolume)
                                )
                                
                                StatRow(
                                    label = "24h High", 
                                    value = formatCurrency(high24h)
                                )
                                
                                StatRow(
                                    label = "24h Low", 
                                    value = formatCurrency(low24h)
                                )
                                
                                StatRow(
                                    label = "7d Change", 
                                    value = formatPercentage(marketData.price_change_percentage_7d),
                                    valueColor = if (marketData.price_change_percentage_7d >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                                
                                StatRow(
                                    label = "30d Change", 
                                    value = formatPercentage(marketData.price_change_percentage_30d),
                                    valueColor = if (marketData.price_change_percentage_30d >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TimeRangeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.weight(1f)
    ) {
        Text(text = text)
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
    
    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun PriceChart(
    pricePoints: List<PricePoint>,
    modifier: Modifier = Modifier
) {
    // Prepare chart data
    val entries = pricePoints.mapIndexed { index, pricePoint ->
        val price = pricePoint.price.toFloat()
        FloatEntry(index.toFloat(), price)
    }
    
    val chartEntryModel = entryModelOf(entries)
    
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Chart(
            chart = lineChart(),
            model = chartEntryModel,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
        )
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 