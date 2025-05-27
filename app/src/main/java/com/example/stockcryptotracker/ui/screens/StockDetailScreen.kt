package com.example.stockcryptotracker.ui.screens

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.stockcryptotracker.data.TimeRange
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.ui.components.PriceAlertDialog
import com.example.stockcryptotracker.ui.screens.DataSourceBanner
import com.example.stockcryptotracker.viewmodel.PriceAlertViewModel
import com.example.stockcryptotracker.viewmodel.StockViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    onBackClick: () -> Unit,
    viewModel: StockViewModel = viewModel(),
    alertViewModel: PriceAlertViewModel = viewModel(),
    onSetAlert: (String, Double) -> Unit = { _, _ -> }
) {
    val stockDetail by viewModel.stockDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val isUsingMockData by viewModel.isUsingMockData.collectAsState()
    val isUsingSavedData by viewModel.isUsingSavedData.collectAsState()
    var showSetAlertDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(symbol) {
        viewModel.loadStockDetail(symbol)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = symbol) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Usuwamy przycisk Set Alert z górnego paska
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error ?: "An unknown error occurred",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadStockDetail(symbol) }
                    ) {
                        Text("Retry")
                    }
                }
            } else if (stockDetail != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Add MockDataBanner at the top
                    DataSourceBanner(
                        isVisible = isUsingMockData,
                        isUsingMockData = isUsingMockData,
                        isUsingSavedData = isUsingSavedData,
                        onRefreshClick = { viewModel.tryUsingRealData() }
                    )
                    
                    stockDetail?.let { stock ->
                        // Header with stock info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stock logo and name
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stock.image.isNotEmpty()) {
                                    AsyncImage(
                                        model = stock.image,
                                        contentDescription = "${stock.name} logo",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        text = stock.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stock.symbol,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Price and change
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = formatCurrency(stock.currentPrice),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val priceChangeColor = if (stock.priceChangePercentage24h >= 0) {
                                    Color(0xFF4CAF50) // Green for positive change
                                } else {
                                    Color(0xFFF44336) // Red for negative change
                                }
                                
                                Text(
                                    text = formatPercentage(stock.priceChangePercentage24h),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = priceChangeColor
                                )
                            }
                        }
                        
                        // Price Alert Button
                        OutlinedButton(
                            onClick = { showSetAlertDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Set Price Alert")
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
                                
                                // Time range selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TimeRangeButton(
                                        text = "1H",
                                        isSelected = selectedTimeRange == TimeRange.HOUR_1,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.HOUR_1) }
                                    )
                                    TimeRangeButton(
                                        text = "24H",
                                        isSelected = selectedTimeRange == TimeRange.HOUR_24,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.HOUR_24) }
                                    )
                                    TimeRangeButton(
                                        text = "7D",
                                        isSelected = selectedTimeRange == TimeRange.DAYS_7,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.DAYS_7) }
                                    )
                                    TimeRangeButton(
                                        text = "30D",
                                        isSelected = selectedTimeRange == TimeRange.DAYS_30,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.DAYS_30) }
                                    )
                                    TimeRangeButton(
                                        text = "90D",
                                        isSelected = selectedTimeRange == TimeRange.DAYS_90,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.DAYS_90) }
                                    )
                                    TimeRangeButton(
                                        text = "1Y",
                                        isSelected = selectedTimeRange == TimeRange.YEAR_1,
                                        onClick = { viewModel.loadPriceHistory(stock.symbol, TimeRange.YEAR_1) }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingPriceHistory) {
                                        CircularProgressIndicator()
                                    } else if (priceHistory.isEmpty()) {
                                        Text(
                                            text = "No price data available",
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        // Convert data for chart
                                        val entries = priceHistory.mapIndexed { index, data ->
                                            FloatEntry(index.toFloat(), data.price.toFloat())
                                        }
                                        
                                        Chart(
                                            chart = lineChart(),
                                            model = entryModelOf(entries),
                                            startAxis = rememberStartAxis(),
                                            bottomAxis = rememberBottomAxis()
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Stock details
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
                                    text = "Stock Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                DetailRow(label = "Market Cap", value = formatCurrency(stock.marketCap))
                                DetailRow(label = "24h High", value = formatCurrency(stock.high24h))
                                DetailRow(label = "24h Low", value = formatCurrency(stock.low24h))
                                DetailRow(label = "24h Volume", value = formatNumber(stock.totalVolume))
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showSetAlertDialog) {
        PriceAlertDialog(
            symbol = symbol,
            currentPrice = stockDetail?.currentPrice ?: 0.0,
            onDismiss = { showSetAlertDialog = false },
            onSetAlert = { price ->
                showSetAlertDialog = false
                onSetAlert(symbol, price)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Price alert set for $symbol at ${formatCurrency(price)}")
                }
            }
        )
    }
}

@Composable
fun TimeRangeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(value)
}

fun formatPercentage(value: Double): String {
    val prefix = if (value >= 0) "+" else ""
    return "$prefix${String.format("%.2f", value)}%"
}

fun formatNumber(value: Double): String {
    val suffix = when {
        value >= 1_000_000_000 -> "B"
        value >= 1_000_000 -> "M"
        value >= 1_000 -> "K"
        else -> ""
    }
    
    val divValue = when {
        value >= 1_000_000_000 -> value / 1_000_000_000
        value >= 1_000_000 -> value / 1_000_000
        value >= 1_000 -> value / 1_000
        else -> value
    }
    
    return String.format("%.2f%s", divValue, suffix)
} 