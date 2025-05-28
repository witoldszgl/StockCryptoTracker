package com.example.stockcryptotracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.StockCache
import com.example.stockcryptotracker.data.TimeRange
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.network.PolygonRetrofitClient
import com.example.stockcryptotracker.network.PolygonTickerItem
import com.example.stockcryptotracker.network.StockSearchResult
import com.example.stockcryptotracker.repository.PolygonRepository
import com.example.stockcryptotracker.repository.StockFavoritesRepository
import com.example.stockcryptotracker.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val favoritesRepository: StockFavoritesRepository
    private val polygonRepository = PolygonRepository(PolygonRetrofitClient.polygonApiService)
    private val stockCache = StockCache(application.applicationContext)
    
    // Lista symboli do pobierania z Polygon
    private val polygonSymbols = listOf("AAPL", "GOOGL")
    
    private val _allStockList = MutableStateFlow<List<Stock>>(emptyList())
    val allStockList: StateFlow<List<Stock>> = _allStockList.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow(StockCategory.ALL)
    val selectedCategory: StateFlow<StockCategory> = _selectedCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Add a state flow to expose when mock data is being used
    private val _isUsingMockData = MutableStateFlow(false)
    val isUsingMockData: StateFlow<Boolean> = _isUsingMockData.asStateFlow()
    
    // Add a state flow to expose when using saved data
    private val _isUsingSavedData = MutableStateFlow(false)
    val isUsingSavedData: StateFlow<Boolean> = _isUsingSavedData.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<StockSearchResult>>(emptyList())
    val searchResults: StateFlow<List<StockSearchResult>> = _searchResults.asStateFlow()
    
    // Dodaj StateFlow dla wyników wyszukiwania z Polygon
    private val _polygonSearchResults = MutableStateFlow<List<PolygonTickerItem>>(emptyList())
    val polygonSearchResults: StateFlow<List<PolygonTickerItem>> = _polygonSearchResults.asStateFlow()
    
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteIds: StateFlow<List<String>> = _favoriteIds.asStateFlow()
    
    // Added retry count to track and limit API retry attempts
    private var apiRetryCount = 0
    private val MAX_RETRY_COUNT = 3
    
    // Cache for favorite status to prevent UI flickering
    private val favoriteStatusCache = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    
    private val _stockDetail = MutableStateFlow<Stock?>(null)
    val stockDetail: StateFlow<Stock?> = _stockDetail

    private val _priceHistory = MutableStateFlow<List<PriceHistoryPoint>>(emptyList())
    val priceHistory: StateFlow<List<PriceHistoryPoint>> = _priceHistory

    private val _isLoadingPriceHistory = MutableStateFlow(false)
    val isLoadingPriceHistory: StateFlow<Boolean> = _isLoadingPriceHistory

    private val _selectedTimeRange = MutableStateFlow(TimeRange.DAYS_7)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange
    
    // StateFlow dla ostatniej oferty (bid/ask)
    private val _lastQuote = MutableStateFlow<Pair<Double, Double>?>(null) // Pair(bid, ask)
    val lastQuote: StateFlow<Pair<Double, Double>?> = _lastQuote
    
    // StateFlow dla danych Open/Close
    private val _dailyOpenClose = MutableStateFlow<Triple<Double, Double, Long>?>(null) // Triple(open, close, volume)
    val dailyOpenClose: StateFlow<Triple<Double, Double, Long>?> = _dailyOpenClose
    
    // StateFlow dla ulubionych akcji
    private val _favoriteStocks = MutableStateFlow<List<Stock>>(emptyList())
    val favoriteStocks: StateFlow<List<Stock>> = _favoriteStocks.asStateFlow()
    
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val stockList: StateFlow<List<Stock>> = combine(
        _allStockList,
        _searchQuery,
        _selectedCategory,
        _favoriteIds.debounce(100).distinctUntilChanged() // Add debounce to stabilize updates
    ) { allStocks, query, selectedCategory, favoriteIds ->
        // Apply category filter
        when (selectedCategory) {
            StockCategory.ALL -> allStocks
            StockCategory.MOST_ACTIVE -> allStocks.sortedByDescending { it.totalVolume }.take(15)
            StockCategory.TOP_GAINERS -> allStocks.sortedByDescending { it.priceChangePercentage24h }.take(15)
            StockCategory.TOP_LOSERS -> allStocks.sortedBy { it.priceChangePercentage24h }.take(15)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    init {
        val database = CryptoDatabase.getDatabase(application)
        favoritesRepository = StockFavoritesRepository(database.favoriteStockDao())
        
        loadStocks()
        
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds()
                .distinctUntilChanged()
                .collect {
                    _favoriteIds.value = it
                    // Update cache when favorite list changes
                    updateFavoriteCache(it)
                    // Pobierz ulubione akcje przy każdej zmianie listy ulubionych
                    loadFavoriteStocks(it)
                }
        }
    }
    
    private fun updateFavoriteCache(favoriteIds: List<String>) {
        // Update cached statuses based on the new favoriteIds list
        favoriteStatusCache.forEach { (symbol, flow) ->
            flow.value = favoriteIds.contains(symbol)
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        if (query.length >= 2) {
            searchStocks(query)
        } else if (query.isEmpty()) {
            _searchResults.value = emptyList()
            _polygonSearchResults.value = emptyList()
            loadStocks() // Reload default stocks list
        }
    }
    
    fun setCategory(category: StockCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            _polygonSearchResults.value = emptyList()
            loadStocks()
        }
    }
    
    fun isFavorite(symbol: String): StateFlow<Boolean> {
        // Use cached flow if available, or create a new one
        return favoriteStatusCache.getOrPut(symbol) {
            // Initialize with current value
            val flow = MutableStateFlow(_favoriteIds.value.contains(symbol))
            
            // Subscribe to repository updates in the background
            viewModelScope.launch {
                favoritesRepository.isFavorite(symbol)
                    .distinctUntilChanged()
                    .collect { isFav ->
                        flow.value = isFav
                    }
            }
            
            flow
        }
    }
    
    fun toggleFavorite(stock: Stock) {
        viewModelScope.launch {
            // Get current status from cache if available, or directly from repository
            val cached = favoriteStatusCache[stock.symbol]
            val isFavorite = cached?.value ?: favoritesRepository.isFavorite(stock.symbol).first()
            
            // Toggle favorite status directly
            if (isFavorite) {
                favoritesRepository.removeFromFavorites(stock.symbol)
                // Update cache immediately to prevent UI delay
                favoriteStatusCache[stock.symbol]?.value = false
            } else {
                favoritesRepository.addToFavorites(stock)
                // Update cache immediately to prevent UI delay
                favoriteStatusCache[stock.symbol]?.value = true
                
                // Zapisz dane o akcji do cache'u
                stockCache.saveStocks(listOf(stock))
            }
        }
    }
    
    /**
     * Pobiera ulubione akcje z cache'u lub API
     */
    fun loadFavoriteStocks(favoriteIds: List<String>) {
        viewModelScope.launch {
            if (favoriteIds.isEmpty()) {
                _favoriteStocks.value = emptyList()
                return@launch
            }
            
            // Najpierw spróbuj pobrać dane z cache'u
            val cachedStocks = stockCache.getStocks(favoriteIds)
            
            if (cachedStocks.isNotEmpty()) {
                Log.d("StockViewModel", "Using cached data for favorites: ${cachedStocks.size} stocks")
                _favoriteStocks.value = cachedStocks
                return@launch
            }
            
            // Jeśli cache jest pusty, pobierz dane z API
            Log.d("StockViewModel", "Cache miss for favorites, loading from API")
            val result = mutableListOf<Stock>()
            
            for (symbol in favoriteIds) {
                try {
                    val stock = polygonRepository.getStockDetails(symbol)
                    result.add(stock)
                } catch (e: Exception) {
                    Log.e("StockViewModel", "Error loading favorite stock $symbol: ${e.message}")
                }
            }
            
            if (result.isNotEmpty()) {
                _favoriteStocks.value = result
                // Zapisz pobrane dane do cache'u
                stockCache.saveStocks(result)
            }
        }
    }
    
    fun loadStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Najpierw spróbuj pobrać dane z cache'u
                val cachedStocks = stockCache.getAllStocks()
                
                // Używamy Polygon API jako głównego źródła danych
                try {
                    Log.d("StockViewModel", "Loading stocks from Polygon API")
                    val polygonStocks = polygonRepository.getStocksList()
                    if (polygonStocks.isNotEmpty()) {
                        // Jeśli mamy dane z cache'u, uzupełnij brakujące ceny
                        val updatedStocks = polygonStocks.map { stock ->
                            if (stock.currentPrice <= 0.0 || stock.price <= 0.0) {
                                val cachedStock = cachedStocks.find { it.symbol == stock.symbol }
                                if (cachedStock != null && (cachedStock.currentPrice > 0.0 || cachedStock.price > 0.0)) {
                                    stock.copy(
                                        currentPrice = cachedStock.currentPrice.takeIf { it > 0.0 } ?: cachedStock.price,
                                        price = cachedStock.price.takeIf { it > 0.0 } ?: cachedStock.currentPrice,
                                        change = cachedStock.change,
                                        changePercent = cachedStock.changePercent,
                                        volume = cachedStock.volume,
                                        totalVolume = cachedStock.totalVolume
                                    )
                                } else {
                                    stock
                                }
                            } else {
                                stock
                            }
                        }
                        
                        _allStockList.value = updatedStocks
                        _isLoading.value = false
                        _isUsingSavedData.value = false
                        apiRetryCount = 0
                        
                        // Zapisz pobrane dane do cache'u
                        stockCache.saveStocks(updatedStocks)
                        
                        return@launch
                    } else {
                        Log.e("StockViewModel", "Received empty stock list from Polygon API")
                        
                        // Jeśli mamy dane w cache'u, użyj ich
                        if (cachedStocks.isNotEmpty()) {
                            Log.d("StockViewModel", "Using cached data: ${cachedStocks.size} stocks")
                            _allStockList.value = cachedStocks
                            _isUsingSavedData.value = true
                            _isLoading.value = false
                            return@launch
                        }
                        
                        _error.value = "No stock data available. Please try again later."
                    }
                } catch (e: Exception) {
                    Log.e("StockViewModel", "Error loading stocks from Polygon API", e)
                    
                    // Jeśli mamy dane w cache'u, użyj ich
                    if (cachedStocks.isNotEmpty()) {
                        Log.d("StockViewModel", "Using cached data after API error: ${cachedStocks.size} stocks")
                        _allStockList.value = cachedStocks
                        _isUsingSavedData.value = true
                        _isLoading.value = false
                        return@launch
                    }
                    
                    _error.value = "Failed to load stocks: ${e.message}"
                }
                
                // Fallback do StockRepository jeśli Polygon się nie powiódł i nie ma danych w cache'u
                val stocksResult = repository.getAllStocks()
                
                if (stocksResult.isSuccess) {
                    val stocks = stocksResult.getOrThrow()
                    
                    // Jeśli mamy dane z cache'u, uzupełnij brakujące ceny
                    val updatedStocks = stocks.map { stock ->
                        if (stock.currentPrice <= 0.0 || stock.price <= 0.0) {
                            // Znajdź dane w cache'u
                            val cachedStock = cachedStocks.find { it.symbol == stock.symbol }
                            if (cachedStock != null && (cachedStock.currentPrice > 0.0 || cachedStock.price > 0.0)) {
                                // Użyj danych z cache'u
                                stock.copy(
                                    currentPrice = cachedStock.currentPrice.takeIf { it > 0.0 } ?: cachedStock.price,
                                    price = cachedStock.price.takeIf { it > 0.0 } ?: cachedStock.currentPrice,
                                    change = cachedStock.change,
                                    changePercent = cachedStock.changePercent,
                                    volume = cachedStock.volume,
                                    totalVolume = cachedStock.totalVolume
                                )
                            } else {
                                stock
                            }
                        } else {
                            stock
                        }
                    }
                    
                    _allStockList.value = updatedStocks
                    _isUsingSavedData.value = false
                    
                    // Zapisz pobrane dane do cache'u
                    stockCache.saveStocks(updatedStocks)
                } else {
                    Log.e("StockViewModel", "Failed to load stocks from repository")
                    _error.value = "Failed to load stocks. Please try again later."
                }
                
                _isLoading.value = false
                apiRetryCount = 0
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error loading stocks", e)
                _error.value = "Failed to load stocks: ${e.message}"
                _isUsingSavedData.value = false
                _isLoading.value = false
                
                // Try to reconnect if there are network issues
                if (apiRetryCount < MAX_RETRY_COUNT) {
                    apiRetryCount++
                    Log.d("StockViewModel", "Retrying API call (attempt $apiRetryCount of $MAX_RETRY_COUNT)")
                    delay(2000) // Wait 2 seconds before retrying
                    loadStocks() // Recursively retry
                }
            }
        }
    }
    
    fun searchStocks(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Wyszukaj używając Polygon.io
                val polygonResults = polygonRepository.searchTickers(query)
                _polygonSearchResults.value = polygonResults
                
                // Konwertuj wyniki Polygon na format StockSearchResult dla kompatybilności
                val polygonSearchResults = polygonResults.map { item ->
                    StockSearchResult(
                        symbol = item.ticker,
                        securityName = item.name,
                        securityType = item.type,
                        exchange = item.primaryExchange ?: "Unknown"
                    )
                }
                
                _searchResults.value = polygonSearchResults
                _isLoading.value = false
                
                if (polygonSearchResults.isEmpty()) {
                    _error.value = "No results found for \"$query\""
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error searching with Polygon", e)
                _error.value = "Failed to search stocks: ${e.message}"
                _searchResults.value = emptyList()
                _isLoading.value = false
            }
        }
    }
    
    fun loadStockDetail(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Najpierw spróbuj pobrać dane z cache'u
            val cachedStock = stockCache.getStock(symbol)

            // Tworzymy podstawowy obiekt Stock, który będziemy uzupełniać danymi
            val basicStock = Stock(
                symbol = symbol,
                name = cachedStock?.name ?: symbol, // Użyj nazwy z cache'u jeśli jest dostępna
                currentPrice = cachedStock?.currentPrice ?: 0.0,
                price = cachedStock?.price ?: 0.0,
                priceChangePercentage24h = cachedStock?.priceChangePercentage24h ?: 0.0,
                marketCap = cachedStock?.marketCap ?: 0.0,
                image = cachedStock?.image ?: "",
                logoUrl = cachedStock?.logoUrl ?: ""
            )
            
            // Ustawiamy podstawowy obiekt, aby natychmiast coś pokazać
            _stockDetail.value = basicStock
            
            try {
                // 1. Pobierz podstawowe informacje o spółce
                Log.d("StockViewModel", "Step 1: Loading basic ticker details for $symbol")
                try {
                    val tickerResponse = polygonRepository.polygonApiService.getStockDetails(symbol)
                    Log.d("StockViewModel", "Ticker details response: ${tickerResponse.status}")
                    
                    // Aktualizujemy podstawowe dane
                    val tickerDetails = tickerResponse.results
                    _stockDetail.value = _stockDetail.value?.copy(
                        name = tickerDetails.name,
                        description = tickerDetails.description,
                        exchange = tickerDetails.primary_exchange ?: "",
                        marketCap = tickerDetails.market_cap ?: cachedStock?.marketCap ?: 0.0,
                        image = tickerDetails.branding?.logo_url ?: cachedStock?.image ?: "",
                        logoUrl = tickerDetails.branding?.logo_url ?: cachedStock?.logoUrl ?: ""
                    )
                    
                } catch (e: Exception) {
                    Log.e("StockViewModel", "Error getting ticker details: ${e.message}", e)
                    if (cachedStock == null) {
                        _error.value = "Failed to load stock details: ${e.message}"
                        _isLoading.value = false
                        return@launch
                    }
                }
                
                // 2. Pobierz dane cenowe
                Log.d("StockViewModel", "Step 2: Loading price data for $symbol")
                try {
                    val stock = repository.getStockQuote(symbol).getOrThrow()
                    
                    // Jeśli cena jest zerowa, użyj danych z cache'u
                    val updatedStock = if (stock.currentPrice <= 0.0 || stock.price <= 0.0) {
                        if (cachedStock != null && (cachedStock.currentPrice > 0.0 || cachedStock.price > 0.0)) {
                            stock.copy(
                                currentPrice = cachedStock.currentPrice.takeIf { it > 0.0 } ?: cachedStock.price,
                                price = cachedStock.price.takeIf { it > 0.0 } ?: cachedStock.currentPrice,
                                change = cachedStock.change,
                                changePercent = cachedStock.changePercent,
                                volume = cachedStock.volume,
                                totalVolume = cachedStock.totalVolume
                            )
                        } else {
                            stock
                        }
                    } else {
                        stock
                    }
                    
                    _stockDetail.value = updatedStock.copy(
                        name = _stockDetail.value?.name ?: updatedStock.name,
                        description = _stockDetail.value?.description,
                        exchange = _stockDetail.value?.exchange ?: "",
                        marketCap = _stockDetail.value?.marketCap ?: updatedStock.marketCap,
                        image = _stockDetail.value?.image ?: updatedStock.image,
                        logoUrl = _stockDetail.value?.logoUrl ?: updatedStock.logoUrl
                    )
                    
                    _error.value = null
                    _isLoading.value = false
                    
                    // Load price history after successful stock detail load
                    loadPriceHistory(symbol, _selectedTimeRange.value)
                } catch (e: Exception) {
                    Log.e("StockViewModel", "Error getting stock quote: ${e.message}", e)
                    if (cachedStock == null) {
                        _error.value = "Failed to load price data: ${e.message}"
                        _isLoading.value = false
                    }
                }
                
                // Zapisz pobrane dane do cache'u
                _stockDetail.value?.let { stockCache.saveStocks(listOf(it)) }
                
                _isUsingSavedData.value = false
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error loading stock details: ${e.message}", e)
                if (cachedStock == null) {
                    _error.value = "Failed to load stock data: ${e.message}"
                }
                _isLoading.value = false
            }
        }
    }
    
    fun loadPriceHistory(symbol: String, timeRange: TimeRange) {
        viewModelScope.launch {
            _isLoadingPriceHistory.value = true
            _selectedTimeRange.value = timeRange
            
            try {
                val history = polygonRepository.getStockHistoricalData(symbol, timeRange)
                _priceHistory.value = history
                _isLoadingPriceHistory.value = false
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error loading price history from Polygon", e)
                
                // Próbuj użyć głównego repozytorium
                try {
                    val historyResult = repository.getStockPriceHistory(
                        symbol, 
                        if (timeRange == TimeRange.DAYS_90 || timeRange == TimeRange.YEAR_1) "full" else "compact"
                    )
                    
                    if (historyResult.isSuccess) {
                        // Konwersja z PricePoint do PriceHistoryPoint
                        val pricePoints = historyResult.getOrThrow()
                        val historyPoints = pricePoints.map { point ->
                            PriceHistoryPoint(
                                timestamp = point.timestamp,
                                price = point.price
                            )
                        }
                        _priceHistory.value = historyPoints
                    } else {
                        _priceHistory.value = emptyList()
                        _error.value = "Failed to load price history"
                    }
                } catch (e2: Exception) {
                    Log.e("StockViewModel", "Error loading price history from repository", e2)
                    _priceHistory.value = emptyList()
                }
                _isLoadingPriceHistory.value = false
            }
        }
    }

    fun loadPriceHistory(symbol: String) {
        loadPriceHistory(symbol, _selectedTimeRange.value)
    }

    // Function to reset API and try again with real data
    fun tryUsingRealData() {
        _isUsingMockData.value = false
        _isUsingSavedData.value = false
        loadStocks()
    }

    /**
     * Ładuje akcje z API i aktualizuje cache
     * Ta metoda powinna być wywoływana przy wejściu do zakładki Stocks
     */
    fun loadStocksAndUpdateCache() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("StockViewModel", "Loading stocks from Polygon API and updating cache")
                val polygonStocks = polygonRepository.getStocksList()
                if (polygonStocks.isNotEmpty()) {
                    _allStockList.value = polygonStocks
                    
                    // Zapisz wszystkie pobrane dane do cache'u
                    stockCache.saveStocks(polygonStocks)
                    Log.d("StockViewModel", "Successfully updated cache with ${polygonStocks.size} stocks")
                    
                    _isLoading.value = false
                    _isUsingSavedData.value = false
                    apiRetryCount = 0
                    return@launch
                } else {
                    Log.e("StockViewModel", "Received empty stock list from Polygon API")
                    _error.value = "No stock data available. Please try again later."
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Error loading stocks from Polygon API", e)
                _error.value = "Failed to load stocks: ${e.message}"
                
                // W przypadku błędu, próbujemy użyć danych z cache'u
                val cachedStocks = stockCache.getAllStocks()
                if (cachedStocks.isNotEmpty()) {
                    Log.d("StockViewModel", "Using cached data: ${cachedStocks.size} stocks")
                    _allStockList.value = cachedStocks
                    _isUsingSavedData.value = true
                    _isLoading.value = false
                    return@launch
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Pobiera ulubione akcje tylko z cache'u bez wywoływania API
     * Ta metoda powinna być używana w zakładce Favorites
     */
    fun loadFavoriteStocksFromCache(favoriteIds: List<String>) {
        viewModelScope.launch {
            if (favoriteIds.isEmpty()) {
                _favoriteStocks.value = emptyList()
                return@launch
            }
            
            // Pobierz dane tylko z cache'u
            val cachedStocks = stockCache.getStocks(favoriteIds)
            
            if (cachedStocks.isNotEmpty()) {
                Log.d("StockViewModel", "Using cached data for favorites: ${cachedStocks.size} stocks")
                _favoriteStocks.value = cachedStocks
            } else {
                Log.d("StockViewModel", "No cached data found for favorites. Will use API fallback.")
                // Jeśli nie ma danych w cache, tylko wtedy wywołujemy API
                loadFavoriteStocks(favoriteIds)
            }
        }
    }
} 