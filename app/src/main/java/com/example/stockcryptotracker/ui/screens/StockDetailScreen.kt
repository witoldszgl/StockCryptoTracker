package com.example.stockcryptotracker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.viewmodel.StockDetailViewModel
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
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    onBackClick: () -> Unit,
    viewModel: StockDetailViewModel = viewModel()
) {
    val stockDetail by viewModel.stockDetail.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()
    val error by viewModel.error.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()

    LaunchedEffect(key1 = symbol) {
        viewModel.loadStockDetail(symbol)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stockDetail?.name ?: "Stock Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Favorite action button
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                isLoading && stockDetail == null -> {
                    CircularProgressIndicator()
                }
                error != null && stockDetail == null -> {
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
                stockDetail != null -> {
                    val stock = stockDetail!!
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header with image and basic info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stock logo
                            AsyncImage(
                                model = stock.logoUrl,
                                contentDescription = "${stock.name} logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Fit
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = stock.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = stock.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                Text(
                                    text = stock.exchange,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Price and change
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
                                    text = "Current Price",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatCurrency(stock.price),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    val changeColor = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    
                                    Text(
                                        text = "${formatCurrency(stock.change)} (${formatPercentage(stock.changePercent)})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = changeColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                                    text = "Price History",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Time range selection buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    StockTimeRangeButton(
                                        text = "1H",
                                        selected = selectedTimeRange == "1",
                                        onClick = { viewModel.changeTimeRange("1") }
                                    )
                                    StockTimeRangeButton(
                                        text = "24H",
                                        selected = selectedTimeRange == "24",
                                        onClick = { viewModel.changeTimeRange("24") }
                                    )
                                    StockTimeRangeButton(
                                        text = "7D",
                                        selected = selectedTimeRange == "7",
                                        onClick = { viewModel.changeTimeRange("7") }
                                    )
                                    StockTimeRangeButton(
                                        text = "30D",
                                        selected = selectedTimeRange == "30",
                                        onClick = { viewModel.changeTimeRange("30") }
                                    )
                                    StockTimeRangeButton(
                                        text = "90D",
                                        selected = selectedTimeRange == "90",
                                        onClick = { viewModel.changeTimeRange("90") }
                                    )
                                    StockTimeRangeButton(
                                        text = "1Y",
                                        selected = selectedTimeRange == "1Y",
                                        onClick = { viewModel.changeTimeRange("1Y") }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Price chart
                                if (isLoadingHistory) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else if (priceHistory.isNotEmpty()) {
                                    // Podsumowanie cenowe i wykres
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Podsumowanie dla wybranego zakresu
                                        PriceSummary(
                                            stock = stock,
                                            priceHistory = priceHistory,
                                            timeRange = selectedTimeRange
                                        )
                                        
                                        // Wykres
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                        ) {
                                            // Convert price history to chart entries
                                            val entries = priceHistory.mapIndexed { index, pricePoint ->
                                                FloatEntry(index.toFloat(), pricePoint.price.toFloat())
                                            }
                                            
                                            // Display chart
                                            Chart(
                                                chart = lineChart(),
                                                model = entryModelOf(entries),
                                                startAxis = rememberStartAxis(),
                                                bottomAxis = rememberBottomAxis(
                                                    valueFormatter = { value, _ ->
                                                        if (value.toInt() < priceHistory.size) {
                                                            formatAxisDate(priceHistory[value.toInt()].timestamp)
                                                        } else {
                                                            ""
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No price history data available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Additional details
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
                                    text = "Details",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Volume
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Volume",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = formatVolume(stock.volume ?: 0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Market Cap
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Market Cap",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = if (stock.marketCap != null) formatMarketCap(stock.marketCap) else "N/A",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Exchange
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Exchange",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = stock.exchange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Disclaimer
                        Text(
                            text = "Data provided by Polygon.io",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.StockTimeRangeButton(
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
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(value)
}

private fun formatPercentage(value: Double): String {
    return String.format("%.2f%%", value * 100)
}

private fun formatVolume(volume: Long): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.2fB", volume / 1_000_000_000.0)
        volume >= 1_000_000 -> String.format("%.2fM", volume / 1_000_000.0)
        volume >= 1_000 -> String.format("%.2fK", volume / 1_000.0)
        else -> volume.toString()
    }
}

private fun formatMarketCap(marketCap: Double): String {
    return when {
        marketCap >= 1_000_000_000_000 -> String.format("$%.2fT", marketCap / 1_000_000_000_000)
        marketCap >= 1_000_000_000 -> String.format("$%.2fB", marketCap / 1_000_000_000)
        marketCap >= 1_000_000 -> String.format("$%.2fM", marketCap / 1_000_000)
        else -> formatCurrency(marketCap)
    }
}

private fun formatAxisDate(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance()
    val now = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    
    // Sprawdź czy timestamp jest z tego samego dnia
    val isSameDay = calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) && 
                   calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
    
    // Dla danych intraday pokazuj godziny
    if (isSameDay || now.timeInMillis - timestamp < 24 * 60 * 60 * 1000) {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(timestamp)
    }
    
    // Dla codziennych danych pokazuj datę
    return java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(timestamp)
}

@Composable
fun PriceSummary(
    stock: com.example.stockcryptotracker.data.Stock,
    priceHistory: List<com.example.stockcryptotracker.data.PricePoint>,
    timeRange: String
) {
    if (priceHistory.isEmpty()) return
    
    // Oblicz zmianę procentową dla wybranego zakresu
    val firstPrice = priceHistory.firstOrNull()?.price ?: 0.0
    val lastPrice = priceHistory.lastOrNull()?.price ?: 0.0
    val currentPrice = stock.price
    
    // Jeśli mamy zbyt małą ilość danych, zwłaszcza dla krótkiego okresu, użyj standardowej zmiany
    val priceChangePercent = if (priceHistory.size < 3 && timeRange == "1") {
        // Dla bardzo krótkiego zakresu z małą ilością danych użyj zmiany 24h
        stock.changePercent * 100
    } else if (firstPrice > 0) {
        val rawChange = ((lastPrice - firstPrice) / firstPrice) * 100
        // Ogranicz maksymalną wartość zmiany procentowej dla bardzo krótkich okresów
        if (timeRange == "1" && kotlin.math.abs(rawChange) > 10.0) {
            if (rawChange > 0) 9.99 else -9.99
        } else {
            // Dodatkowa weryfikacja: jeśli zmiana jest nieprawdopodobnie duża, ogranicz ją
            val reasonableMaxChange = when(timeRange) {
                "1" -> 10.0  // 10% dla 1h
                "24" -> 20.0 // 20% dla 24h
                "7" -> 50.0  // 50% dla tygodnia
                "30" -> 100.0 // 100% dla miesiąca
                "90", "1Y" -> 200.0 // 200% dla dłuższych okresów
                else -> 300.0
            }
            if (kotlin.math.abs(rawChange) > reasonableMaxChange) {
                if (rawChange > 0) reasonableMaxChange else -reasonableMaxChange
            } else {
                rawChange
            }
        }
    } else {
        0.0
    }
    
    // Określ kolor na podstawie trendu
    val trendColor = if (priceChangePercent >= 0) {
        Color(0xFF4CAF50) // Zielony dla rosnącego trendu
    } else {
        Color(0xFFF44336) // Czerwony dla spadającego trendu
    }
    
    // Oblicz minimalną i maksymalną cenę z zakresu, uwzględniając aktualną cenę
    var minPrice = priceHistory.minOfOrNull { it.price } ?: currentPrice
    var maxPrice = priceHistory.maxOfOrNull { it.price } ?: currentPrice
    
    // Upewnij się, że aktualna cena mieści się w zakresie min-max
    // Jeśli nie, to dane historyczne mogą być niepełne lub nieprawidłowe
    if (currentPrice < minPrice) minPrice = currentPrice
    if (currentPrice > maxPrice) maxPrice = currentPrice
    
    // Weryfikacja wartości min/max - nie powinny się różnić o więcej niż X% od aktualnej ceny
    val maxDiffFactor = when(timeRange) {
        "1" -> 0.15  // 15% dla 1h
        "24" -> 0.25 // 25% dla 24h
        "7" -> 0.50  // 50% dla tygodnia
        "30" -> 1.0  // 100% dla miesiąca
        "90" -> 1.5  // 150% dla 3 miesięcy
        "1Y" -> 2.0  // 200% dla roku
        else -> 3.0  // 300% dla innych okresów
    }
    
    // Jeśli min/max wartości są zbyt odległe od aktualnej ceny, ogranicz je
    val lowerBound = currentPrice * (1 - maxDiffFactor)
    val upperBound = currentPrice * (1 + maxDiffFactor)
    
    if (minPrice < lowerBound) minPrice = lowerBound
    if (maxPrice > upperBound) maxPrice = upperBound
    
    // Zakres cenowy dla wybranego okresu
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Min
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(minPrice),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Zmiana w okresie
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = getTimeRangeLabel(timeRange),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPriceChange(priceChangePercent),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = trendColor
            )
        }
        
        // Max
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Max",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(maxPrice),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Funkcja pomocnicza do formatowania zmiany ceny
private fun formatPriceChange(percentage: Double): String {
    return if (percentage >= 0) {
        "+${String.format("%.2f", percentage)}%"
    } else {
        "${String.format("%.2f", percentage)}%"
    }
}

// Funkcja pomocnicza do wyświetlania opisu zakresu czasowego
private fun getTimeRangeLabel(timeRange: String): String {
    return when(timeRange) {
        "1" -> "Last hour"
        "24" -> "Last 24 hours"
        "7" -> "Last 7 days"
        "30" -> "Last 30 days"
        "90" -> "Last 90 days"
        "1Y" -> "Last year"
        else -> timeRange
    }
} 