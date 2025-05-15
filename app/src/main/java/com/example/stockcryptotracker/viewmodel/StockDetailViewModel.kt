package com.example.stockcryptotracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.repository.StockFavoritesRepository
import com.example.stockcryptotracker.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StockDetailViewModel"

class StockDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val favoritesRepository: StockFavoritesRepository
    
    private val _stockDetail = MutableStateFlow<Stock?>(null)
    val stockDetail: StateFlow<Stock?> = _stockDetail.asStateFlow()
    
    private val _priceHistory = MutableStateFlow<List<PricePoint>>(emptyList())
    val priceHistory: StateFlow<List<PricePoint>> = _priceHistory.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow("30")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()
    
    init {
        val database = CryptoDatabase.getDatabase(application)
        favoritesRepository = StockFavoritesRepository(database.favoriteStockDao())
    }
    
    fun loadStockDetail(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getStockQuote(symbol)
                .onSuccess { stock ->
                    _stockDetail.value = stock
                    _isLoading.value = false
                    checkFavoriteStatus(symbol)
                    // Load price history after successful stock detail load
                    loadPriceHistory(symbol)
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    fun loadPriceHistory(symbol: String, days: String = _selectedTimeRange.value) {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            // Keep existing data visible while loading
            
            try {
                Log.d(TAG, "Loading price history for $symbol with range $days")
                
                // Jeśli wybrany jest zakres 24h, użyj danych intradayowych
                if (days == "1") {
                    loadIntradayData(symbol)
                    return@launch
                }
                
                val outputSize = when (days) {
                    "7", "30" -> "compact" // compact gives last 100 data points (about 3 months)
                    else -> "full" // full gives up to 20 years of data
                }
                
                repository.getStockPriceHistory(symbol, outputSize)
                    .onSuccess { history ->
                        // Filter history based on selected days
                        val filteredHistory = when (days) {
                            "7" -> filterLastNDays(history, 7)
                            "30" -> filterLastNDays(history, 30)
                            "90" -> filterLastNDays(history, 90)
                            "1Y" -> filterLastNDays(history, 365)
                            else -> history
                        }
                        
                        Log.d(TAG, "Loaded ${history.size} price points, filtered to ${filteredHistory.size} for range $days")
                        
                        _priceHistory.value = filteredHistory
                        _selectedTimeRange.value = days
                        _isLoadingHistory.value = false
                        
                        if (filteredHistory.isEmpty()) {
                            Log.w(TAG, "No price history data available after filtering for range $days")
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load price history: ${error.message}")
                        // Just log the error but don't update UI state since this is secondary data
                        _error.value = "Could not load price history: ${error.message}"
                        _isLoadingHistory.value = false
                        
                        // Generate mock data as fallback
                        _priceHistory.value = generateMockPriceHistory(days)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when loading price history", e)
                _error.value = "Exception loading price history: ${e.message}"
                _isLoadingHistory.value = false
                
                // Generate mock data as fallback
                _priceHistory.value = generateMockPriceHistory(days)
            }
        }
    }
    
    private fun loadIntradayData(symbol: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading 24h intraday data for $symbol")
                
                repository.getStockIntraday24h(symbol)
                    .onSuccess { intradayData ->
                        Log.d(TAG, "Successfully loaded ${intradayData.size} intraday data points")
                        
                        _priceHistory.value = intradayData
                        _selectedTimeRange.value = "1"
                        _isLoadingHistory.value = false
                        
                        if (intradayData.isEmpty()) {
                            Log.w(TAG, "No intraday data available for $symbol")
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load intraday data: ${error.message}")
                        _error.value = "Could not load intraday data: ${error.message}"
                        _isLoadingHistory.value = false
                        
                        // Fallback to mock intraday data
                        val mockData = generateMockIntraday24h()
                        _priceHistory.value = mockData
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when loading intraday data", e)
                _error.value = "Exception loading intraday data: ${e.message}"
                _isLoadingHistory.value = false
                
                // Fallback to mock intraday data
                val mockData = generateMockIntraday24h()
                _priceHistory.value = mockData
            }
        }
    }
    
    private fun generateMockIntraday24h(): List<PricePoint> {
        val pricePoints = mutableListOf<PricePoint>()
        val calendar = java.util.Calendar.getInstance()
        val stock = _stockDetail.value
        val basePrice = stock?.price ?: 100.0
        val random = java.util.Random()
        
        // Start from 24 hours ago
        calendar.add(java.util.Calendar.HOUR_OF_DAY, -24)
        
        // Generate price points every 30 minutes (48 points in 24 hours)
        var currentPrice = basePrice * 0.98  // Start slightly below current price
        for (i in 0 until 48) {
            val timestamp = calendar.timeInMillis
            
            // Add randomness to create realistic price movements
            val randomChange = random.nextDouble() * 0.01 - 0.005  // -0.5% to +0.5%
            val timeEffect = i.toDouble() / 96.0  // Slight upward trend towards current price
            
            currentPrice *= (1 + randomChange + timeEffect * 0.01)
            
            // Make sure we end close to the current price
            if (i == 47) {
                currentPrice = basePrice
            }
            
            pricePoints.add(PricePoint(timestamp, currentPrice))
            calendar.add(java.util.Calendar.MINUTE, 30)
        }
        
        Log.d(TAG, "Generated ${pricePoints.size} mock intraday data points")
        
        return pricePoints
    }
    
    private fun filterLastNDays(history: List<PricePoint>, days: Int): List<PricePoint> {
        if (history.isEmpty()) return emptyList()
        
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val filtered = history.filter { it.timestamp >= cutoffTime }
        
        Log.d(TAG, "Filtered from ${history.size} to ${filtered.size} price points for $days days")
        
        return filtered
    }
    
    private fun generateMockPriceHistory(range: String): List<PricePoint> {
        // Generate mock price data based on range
        val days = when(range) {
            "7" -> 7
            "30" -> 30
            "90" -> 90
            "1Y" -> 365
            else -> 30
        }
        
        val pricePoints = mutableListOf<PricePoint>()
        val calendar = java.util.Calendar.getInstance()
        val stock = _stockDetail.value
        val basePrice = stock?.price ?: 100.0
        val random = java.util.Random()
        
        // Start from X days ago
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        
        // Generate daily price points with some trend
        var currentPrice = basePrice * 0.7 // Start lower than current price
        for (i in 0 until days) {
            val timestamp = calendar.timeInMillis
            
            // Add some randomness but with an upward trend toward current price
            val targetRatio = i.toDouble() / days.toDouble() // 0.0 -> 1.0 over time
            val targetPrice = basePrice * (0.7 + (0.3 * targetRatio)) // Move toward 100% of basePrice
            
            // Random walk with bias toward target
            val randomFactor = 0.99 + random.nextDouble() * 0.02 // -1% to +1%
            val trendFactor = 1.0 + (targetPrice - currentPrice) / currentPrice * 0.1 // 10% move toward target
            
            currentPrice *= randomFactor * trendFactor
            
            pricePoints.add(PricePoint(timestamp, currentPrice))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        Log.d(TAG, "Generated ${pricePoints.size} mock price points for range $range")
        
        return pricePoints
    }
    
    private fun checkFavoriteStatus(symbol: String) {
        viewModelScope.launch {
            try {
                favoritesRepository.isFavorite(symbol).collect { isFavorite ->
                    _isFavorite.value = isFavorite
                }
            } catch (e: Exception) {
                // If there's an error checking favorite status, assume it's not a favorite
                _isFavorite.value = false
            }
        }
    }
    
    fun toggleFavorite() {
        val stock = _stockDetail.value ?: return
        
        viewModelScope.launch {
            try {
                val currentlyFavorite = _isFavorite.value
                
                if (currentlyFavorite) {
                    favoritesRepository.removeFromFavorites(stock.symbol)
                } else {
                    favoritesRepository.addToFavorites(stock)
                }
                
                _isFavorite.value = !currentlyFavorite
            } catch (e: Exception) {
                _error.value = "Failed to update favorite status: ${e.message}"
            }
        }
    }
    
    fun changeTimeRange(days: String) {
        val symbol = _stockDetail.value?.symbol ?: return
        Log.d(TAG, "Changing time range to $days days for $symbol")
        loadPriceHistory(symbol, days)
    }
} 