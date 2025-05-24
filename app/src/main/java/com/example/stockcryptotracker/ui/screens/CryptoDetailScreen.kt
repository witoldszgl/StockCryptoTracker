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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.ui.components.PriceAlertDialog
import com.example.stockcryptotracker.ui.components.CryptoPriceAlertDialog
import com.example.stockcryptotracker.viewmodel.PriceAlertViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.SnackbarDuration
import com.example.stockcryptotracker.repository.TimeRange
import com.example.stockcryptotracker.viewmodel.CryptoViewModel
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    cryptoId: String,
    onBackClick: () -> Unit,
    viewModel: CryptoViewModel = viewModel(),
    priceAlertViewModel: PriceAlertViewModel = viewModel()
) {
    val cryptoDetail by viewModel.cryptoDetail.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val isLoading by viewModel.isLoadingDetail.collectAsState()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsState()
    val error by viewModel.errorDetail.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    
    // Price alert states
    val alertLoading by priceAlertViewModel.loading.collectAsState()
    val alertError by priceAlertViewModel.error.collectAsState()
    val alertSuccess by priceAlertViewModel.successMessage.collectAsState()
    var showSetAlertDialog by remember { mutableStateOf(false) }
    
    // Snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show alert success or error messages
    LaunchedEffect(alertSuccess, alertError) {
        when {
            alertSuccess != null -> {
                snackbarHostState.showSnackbar(
                    message = alertSuccess ?: "Success",
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                priceAlertViewModel.clearMessages()
            }
            alertError != null -> {
                snackbarHostState.showSnackbar(
                    message = alertError ?: "Error occurred",
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                priceAlertViewModel.clearMessages()
            }
        }
    }

    // Remember the error state to display it better
    val formattedError = remember(error) {
        if (error?.contains("429") == true) {
            "Rate limit exceeded. Please wait a moment and try again."
        } else {
            error
        }
    }

    LaunchedEffect(key1 = cryptoId) {
        viewModel.fetchCryptoDetail(cryptoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = cryptoDetail?.name ?: "Cryptocurrency Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                            text = "Error: $formattedError",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.fetchCryptoDetail(cryptoId) }
                        ) {
                            Text(text = "Try Again")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Przycisk testowy do pobrania danych BTC
                        Button(
                            onClick = { viewModel.fetchBitcoinForTesting() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(text = "Test with Bitcoin (BTC)")
                        }
                    }
                }
                cryptoDetail != null -> {
                    val crypto = cryptoDetail!!
                    
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
                                model = crypto.image,
                                contentDescription = "${crypto.name} logo",
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
                                    text = crypto.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = crypto.symbol.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = formatCryptoPrice(crypto.currentPrice),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val priceChangeColor = if (crypto.priceChangePercentage24h >= 0) {
                                    Color(0xFF4CAF50) // Green for positive change
                                } else {
                                    Color(0xFFF44336) // Red for negative change
                                }
                                
                                Text(
                                    text = formatCryptoPercentage(crypto.priceChangePercentage24h),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = priceChangeColor
                                )
                            }
                        }
                        
                        // Price Alert Button
                        OutlinedButton(
                            onClick = { showSetAlertDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "Set Price Alert")
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
                                        text = "1H",
                                        selected = selectedTimeRange == TimeRange.HOUR_1,
                                        onClick = { viewModel.setTimeRange(TimeRange.HOUR_1) }
                                    )
                                    TimeRangeButton(
                                        text = "24H",
                                        selected = selectedTimeRange == TimeRange.HOUR_24,
                                        onClick = { viewModel.setTimeRange(TimeRange.HOUR_24) }
                                    )
                                    TimeRangeButton(
                                        text = "7D",
                                        selected = selectedTimeRange == TimeRange.DAYS_7,
                                        onClick = { viewModel.setTimeRange(TimeRange.DAYS_7) }
                                    )
                                    TimeRangeButton(
                                        text = "30D",
                                        selected = selectedTimeRange == TimeRange.DAYS_30,
                                        onClick = { viewModel.setTimeRange(TimeRange.DAYS_30) }
                                    )
                                    TimeRangeButton(
                                        text = "90D",
                                        selected = selectedTimeRange == TimeRange.DAYS_90,
                                        onClick = { viewModel.setTimeRange(TimeRange.DAYS_90) }
                                    )
                                    TimeRangeButton(
                                        text = "1Y",
                                        selected = selectedTimeRange == TimeRange.YEAR_1,
                                        onClick = { viewModel.setTimeRange(TimeRange.YEAR_1) }
                                    )
                                }
                                
                                // Chart or loading indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp), // Zwiększ wysokość dla wykresów
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingPriceHistory) {
                                        CircularProgressIndicator()
                                    } else if (priceHistory.isNotEmpty()) {
                                        // Podsumowanie cenowe i wykres
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Podsumowanie dla wybranego zakresu
                                            PriceSummary(
                                                crypto = crypto,
                                                priceHistory = priceHistory,
                                                timeRange = selectedTimeRange
                                            )
                                            
                                            // Wykres
                                            PriceChart(
                                                pricePoints = priceHistory, 
                                                timeRange = selectedTimeRange
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No price data available",
                                            color = MaterialTheme.colorScheme.error
                                        )
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
                                
                                StatRow(
                                    label = "Market Cap", 
                                    value = formatCryptoPrice(crypto.marketCap.toDouble())
                                )
                                
                                StatRow(
                                    label = "24h Volume", 
                                    value = formatCryptoPrice(crypto.totalVolume)
                                )
                                
                                StatRow(
                                    label = "24h High", 
                                    value = formatCryptoPrice(crypto.high24h)
                                )
                                
                                StatRow(
                                    label = "24h Low", 
                                    value = formatCryptoPrice(crypto.low24h)
                                )
                            }
                        }
                    }
                    
                    // Show price alert dialog
                    if (showSetAlertDialog && cryptoDetail != null) {
                        CryptoPriceAlertDialog(
                            cryptoCurrency = crypto,
                            currentPrice = crypto.currentPrice,
                            onDismiss = { showSetAlertDialog = false },
                            onSetAlert = { targetPrice, isAboveTarget ->
                                priceAlertViewModel.addCryptoAlert(
                                    crypto = crypto,
                                    targetPrice = targetPrice,
                                    isAboveTarget = isAboveTarget
                                )
                                showSetAlertDialog = false
                            }
                        )
                    }
                }
            }
            
            // Show loading indicator when setting alerts
            if (alertLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
    
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun PriceChart(pricePoints: List<PriceHistoryPoint>, timeRange: TimeRange) {
    if (pricePoints.isEmpty()) return
    
    // Convert price history to chart entries
    val entries = pricePoints.mapIndexed { index, point ->
        FloatEntry(index.toFloat(), point.price.toFloat())
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Display chart
        Chart(
            chart = lineChart(),
            model = entryModelOf(entries),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ ->
                    if (value.toInt() < pricePoints.size) {
                        // Używamy podobnego formatowania jak w StockDetailScreen
                        val idx = value.toInt()
                        if (idx % max(1, pricePoints.size / 5) == 0) {
                            pricePoints[idx].date
                        } else {
                            ""
                        }
                    } else {
                        ""
                    }
                }
            )
        )
    }
}

// Funkcja pomocnicza do wyświetlania opisu zakresu czasowego
private fun getTimeRangeLabel(timeRange: TimeRange): String {
    return when(timeRange) {
        TimeRange.HOUR_1 -> "Last hour"
        TimeRange.HOUR_24 -> "Last 24 hours"
        TimeRange.DAYS_7 -> "Last 7 days"
        TimeRange.DAYS_30 -> "Last 30 days"
        TimeRange.DAYS_90 -> "Last 90 days"
        TimeRange.YEAR_1 -> "Last year"
    }
}

private fun formatCryptoPrice(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

private fun formatCryptoPercentage(percentage: Double): String {
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

@Composable
fun PriceSummary(
    crypto: CryptoCurrency,
    priceHistory: List<PriceHistoryPoint>,
    timeRange: TimeRange
) {
    if (priceHistory.isEmpty()) return
    
    // Oblicz zmianę procentową dla wybranego zakresu
    val firstPrice = priceHistory.firstOrNull()?.price ?: 0.0
    val lastPrice = priceHistory.lastOrNull()?.price ?: 0.0
    val currentPrice = crypto.currentPrice
    
    // Jeśli mamy zbyt małą ilość danych, zwłaszcza dla krótkiego okresu, użyj standardowej zmiany
    val priceChangePercent = if (priceHistory.size < 3 && timeRange == TimeRange.HOUR_1) {
        // Dla bardzo krótkiego zakresu z małą ilością danych użyj zmiany 24h
        crypto.priceChangePercentage24h
    } else if (firstPrice > 0) {
        val rawChange = ((lastPrice - firstPrice) / firstPrice) * 100
        // Ogranicz maksymalną wartość zmiany procentowej dla bardzo krótkich okresów
        if (timeRange == TimeRange.HOUR_1 && kotlin.math.abs(rawChange) > 10.0) {
            if (rawChange > 0) 9.99 else -9.99
        } else {
            // Dodatkowa weryfikacja: jeśli zmiana jest nieprawdopodobnie duża, ogranicz ją
            val reasonableMaxChange = when(timeRange) {
                TimeRange.HOUR_1 -> 10.0  // 10% dla 1h
                TimeRange.HOUR_24 -> 20.0 // 20% dla 24h
                TimeRange.DAYS_7 -> 50.0  // 50% dla tygodnia
                TimeRange.DAYS_30 -> 100.0 // 100% dla miesiąca
                TimeRange.DAYS_90, TimeRange.YEAR_1 -> 200.0 // 200% dla dłuższych okresów
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
        TimeRange.HOUR_1 -> 0.15  // 15% dla 1h
        TimeRange.HOUR_24 -> 0.25 // 25% dla 24h
        TimeRange.DAYS_7 -> 0.50  // 50% dla tygodnia
        TimeRange.DAYS_30 -> 1.0  // 100% dla miesiąca
        TimeRange.DAYS_90 -> 1.5  // 150% dla 3 miesięcy
        TimeRange.YEAR_1 -> 2.0  // 200% dla roku
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
                text = formatCryptoPrice(minPrice),
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
                text = formatCryptoPercentage(priceChangePercent),
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
                text = formatCryptoPrice(maxPrice),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
} 