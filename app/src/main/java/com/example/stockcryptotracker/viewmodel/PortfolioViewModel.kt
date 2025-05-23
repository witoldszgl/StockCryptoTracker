package com.example.stockcryptotracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.StockCache
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.data.room.PortfolioItem
import com.example.stockcryptotracker.data.room.StockPortfolioItem
import com.example.stockcryptotracker.repository.CryptoRepository
import com.example.stockcryptotracker.repository.PortfolioRepository
import com.example.stockcryptotracker.repository.StockPortfolioRepository
import com.example.stockcryptotracker.repository.StockRepository
import com.example.stockcryptotracker.network.PolygonRetrofitClient
import com.example.stockcryptotracker.repository.PolygonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val cryptoRepository = CryptoRepository()
    private val stockRepository = StockRepository()
    private val polygonRepository = PolygonRepository(PolygonRetrofitClient.polygonApiService)
    private val stockCache = StockCache(application.applicationContext)
    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var stockPortfolioRepository: StockPortfolioRepository
    
    // Crypto portfolio data
    private val _portfolioItems = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val portfolioItems: StateFlow<List<PortfolioItem>> = _portfolioItems.asStateFlow()
    
    private val _allCryptoList = MutableStateFlow<List<CryptoCurrency>>(emptyList())
    val allCryptoList: StateFlow<List<CryptoCurrency>> = _allCryptoList.asStateFlow()
    
    // Stock portfolio data
    private val _stockPortfolioItems = MutableStateFlow<List<StockPortfolioItem>>(emptyList())
    val stockPortfolioItems: StateFlow<List<StockPortfolioItem>> = _stockPortfolioItems.asStateFlow()
    
    private val _allStocksList = MutableStateFlow<List<Stock>>(emptyList())
    val allStocksList: StateFlow<List<Stock>> = _allStocksList.asStateFlow()
    
    // UI state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Portfolio tab selection
    enum class PortfolioTab { ALL, CRYPTO, STOCKS }
    private val _selectedTab = MutableStateFlow(PortfolioTab.ALL)
    val selectedTab: StateFlow<PortfolioTab> = _selectedTab.asStateFlow()
    
    // Total portfolio value (crypto + stocks)
    private val _portfolioValue = MutableStateFlow(0.0)
    val portfolioValue: StateFlow<String> = _portfolioValue.map { 
        formatCurrency(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = formatCurrency(0.0)
    )
    
    // Combined state for crypto portfolio items with current prices
    val cryptoPortfolioItemsWithValue = combine(
        _portfolioItems,
        _allCryptoList
    ) { portfolioItems, cryptoList ->
        portfolioItems.map { item ->
            val crypto = cryptoList.find { it.id == item.cryptoId }
            val currentPrice = crypto?.currentPrice ?: 0.0
            val value = item.quantity * currentPrice
            val imageUrl = crypto?.image
            
            PortfolioItemUiState(
                id = item.cryptoId,
                name = item.name,
                symbol = item.symbol,
                quantity = item.quantity,
                price = currentPrice,
                value = value,
                imageUrl = imageUrl,
                type = PortfolioItemType.CRYPTO
            )
        }.sortedByDescending { it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Combined state for stock portfolio items with current prices
    val stockPortfolioItemsWithValue = combine(
        _stockPortfolioItems,
        _allStocksList
    ) { portfolioItems, stocksList ->
        portfolioItems.map { item ->
            val stock = stocksList.find { it.symbol == item.symbol }
            val currentPrice = stock?.price ?: 0.0
            val value = item.quantity * currentPrice
            val logoUrl = stock?.logoUrl
            
            PortfolioItemUiState(
                id = item.symbol,
                name = item.name,
                symbol = item.symbol,
                quantity = item.quantity,
                price = currentPrice,
                value = value,
                imageUrl = logoUrl,
                type = PortfolioItemType.STOCK
            )
        }.sortedByDescending { it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Combined list of all portfolio items (crypto + stocks)
    val allPortfolioItems = combine(
        cryptoPortfolioItemsWithValue,
        stockPortfolioItemsWithValue
    ) { cryptoItems, stockItems ->
        (cryptoItems + stockItems).sortedByDescending { it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Filtered portfolio items based on search and selected tab
    val filteredPortfolioItems = combine(
        allPortfolioItems,
        cryptoPortfolioItemsWithValue,
        stockPortfolioItemsWithValue,
        _searchQuery,
        _selectedTab
    ) { all, crypto, stocks, query, tab ->
        // First filter by tab
        val tabFilteredItems = when (tab) {
            PortfolioTab.ALL -> all
            PortfolioTab.CRYPTO -> crypto
            PortfolioTab.STOCKS -> stocks
        }
        
        // Then filter by search query
        if (query.isEmpty()) {
            tabFilteredItems
        } else {
            tabFilteredItems.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.symbol.contains(query, ignoreCase = true) 
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        Log.d("PortfolioViewModel", "Initializing PortfolioViewModel")
        try {
            val database = CryptoDatabase.getDatabase(application)
            portfolioRepository = PortfolioRepository(database.portfolioDao())
            stockPortfolioRepository = StockPortfolioRepository(database.stockPortfolioDao())
            
            loadData()
            
            // Observe crypto portfolio items
            viewModelScope.launch {
                try {
                    portfolioRepository.getAllPortfolioItems().collect { items ->
                        Log.d("PortfolioViewModel", "Collected ${items.size} crypto portfolio items")
                        _portfolioItems.value = items
                        updatePortfolioValue()
                    }
                } catch (e: Exception) {
                    Log.e("PortfolioViewModel", "Error collecting crypto portfolio items", e)
                    _error.value = "Failed to load crypto portfolio: ${e.message}"
                    _portfolioItems.value = emptyList()
                }
            }
            
            // Observe stock portfolio items
            viewModelScope.launch {
                try {
                    stockPortfolioRepository.getAllPortfolioItems().collect { items ->
                        Log.d("PortfolioViewModel", "Collected ${items.size} stock portfolio items")
                        _stockPortfolioItems.value = items
                        updatePortfolioValue()
                    }
                } catch (e: Exception) {
                    Log.e("PortfolioViewModel", "Error collecting stock portfolio items", e)
                    _error.value = "Failed to load stock portfolio: ${e.message}"
                    _stockPortfolioItems.value = emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("PortfolioViewModel", "Error during PortfolioViewModel initialization", e)
            _error.value = "Failed to initialize: ${e.message}"
            _isLoading.value = false
            _portfolioItems.value = emptyList()
            _allCryptoList.value = emptyList()
            _stockPortfolioItems.value = emptyList()
            _allStocksList.value = emptyList()
            _portfolioValue.value = 0.0
        }
    }
    
    fun setSelectedTab(tab: PortfolioTab) {
        _selectedTab.value = tab
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun loadData() {
        loadCryptoList()
        loadStocksList()
    }
    
    private fun loadCryptoList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("PortfolioViewModel", "Loading crypto list")
                val cryptoList = cryptoRepository.getTopCryptos(250)
                _allCryptoList.value = cryptoList
                _isLoading.value = false
                updatePortfolioValue()
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error loading crypto list", e)
                _error.value = "Failed to load cryptocurrencies: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private fun loadStocksList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load stocks from StockRepository
                Log.d("PortfolioViewModel", "Loading stocks from StockRepository")
                val stocksResult = stockRepository.getAllStocks()
                
                if (stocksResult.isSuccess) {
                    val stocks = stocksResult.getOrThrow()
                    _allStocksList.value = stocks
                    _isLoading.value = false
                    updatePortfolioValue()
                    return@launch
                }
                
                // If StockRepository failed, try Polygon directly
                Log.d("PortfolioViewModel", "Loading stocks from Polygon")
                val polygonStocks = polygonRepository.getStocksList()
                
                if (polygonStocks.isNotEmpty()) {
                    _allStocksList.value = polygonStocks
                    _isLoading.value = false
                    updatePortfolioValue()
                } else {
                    Log.e("PortfolioViewModel", "No stocks received from Polygon")
                    _error.value = "Failed to load stocks. Please try again later."
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error loading stocks list", e)
                _error.value = "Failed to load stocks: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Add or update crypto portfolio item
    fun addUpdateCryptoPortfolioItem(cryptoId: String, quantity: Double) {
        if (!::portfolioRepository.isInitialized) {
            Log.e("PortfolioViewModel", "Cannot add item: portfolioRepository not initialized")
            _error.value = "Application not properly initialized"
            return
        }
        
        viewModelScope.launch {
            val crypto = _allCryptoList.value.find { it.id == cryptoId }
            if (crypto != null) {
                portfolioRepository.addOrUpdatePortfolioItem(crypto, quantity)
            }
        }
    }
    
    // Add or update stock portfolio item
    fun addUpdateStockPortfolioItem(symbol: String, quantity: Double) {
        if (!::stockPortfolioRepository.isInitialized) {
            Log.e("PortfolioViewModel", "Cannot add item: stockPortfolioRepository not initialized")
            _error.value = "Application not properly initialized"
            return
        }
        
        viewModelScope.launch {
            val stock = _allStocksList.value.find { it.symbol == symbol }
            if (stock != null) {
                stockPortfolioRepository.addOrUpdatePortfolioItem(stock, quantity)
            }
        }
    }
    
    // Remove crypto portfolio item
    fun removeCryptoPortfolioItem(cryptoId: String) {
        if (!::portfolioRepository.isInitialized) {
            Log.e("PortfolioViewModel", "Cannot remove item: portfolioRepository not initialized")
            _error.value = "Application not properly initialized"
            return
        }
        
        viewModelScope.launch {
            portfolioRepository.removePortfolioItem(cryptoId)
        }
    }
    
    // Remove stock portfolio item
    fun removeStockPortfolioItem(symbol: String) {
        if (!::stockPortfolioRepository.isInitialized) {
            Log.e("PortfolioViewModel", "Cannot remove item: stockPortfolioRepository not initialized")
            _error.value = "Application not properly initialized"
            return
        }
        
        viewModelScope.launch {
            stockPortfolioRepository.removePortfolioItem(symbol)
        }
    }
    
    // Remove portfolio item (either crypto or stock)
    fun removePortfolioItem(id: String, type: PortfolioItemType) {
        when (type) {
            PortfolioItemType.CRYPTO -> removeCryptoPortfolioItem(id)
            PortfolioItemType.STOCK -> removeStockPortfolioItem(id)
        }
    }
    
    private fun updatePortfolioValue() {
        try {
            var totalValue = 0.0
            
            // Calculate crypto portfolio value
            if (::portfolioRepository.isInitialized) {
                Log.d("PortfolioViewModel", "Calculating crypto portfolio value")
                val cryptoItems = _portfolioItems.value
                val cryptos = _allCryptoList.value
                
                if (cryptoItems.isNotEmpty() && cryptos.isNotEmpty()) {
                    val cryptoValue = portfolioRepository.calculatePortfolioValue(cryptoItems, cryptos)
                    Log.d("PortfolioViewModel", "Crypto portfolio value: $cryptoValue")
                    totalValue += cryptoValue
                }
            }
            
            // Calculate stock portfolio value
            if (::stockPortfolioRepository.isInitialized) {
                Log.d("PortfolioViewModel", "Calculating stock portfolio value")
                val stockItems = _stockPortfolioItems.value
                val stocks = _allStocksList.value
                
                if (stockItems.isNotEmpty() && stocks.isNotEmpty()) {
                    val stockValue = stockPortfolioRepository.calculatePortfolioValue(stockItems, stocks)
                    Log.d("PortfolioViewModel", "Stock portfolio value: $stockValue")
                    totalValue += stockValue
                }
            }
            
            Log.d("PortfolioViewModel", "Total portfolio value: $totalValue")
            _portfolioValue.value = totalValue
        } catch (e: Exception) {
            Log.e("PortfolioViewModel", "Error calculating portfolio value", e)
            _error.value = "Failed to update portfolio value: ${e.message}"
            _portfolioValue.value = 0.0
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }
    
    /**
     * Ładuje akcje dla portfolio z cache'u zamiast z API
     * Ta metoda powinna być używana w zakładce Portfolio
     */
    fun loadPortfolioItemsFromCache() {
        viewModelScope.launch {
            // Najpierw pobieramy symbole akcji w portfolio
            if (::stockPortfolioRepository.isInitialized) {
                val portfolioItems = _stockPortfolioItems.value
                if (portfolioItems.isEmpty()) {
                    // Jeśli nie ma akcji w portfolio, nie ma co pobierać
                    return@launch
                }
                
                val symbols = portfolioItems.map { it.symbol }
                
                // Próbujemy pobrać dane z cache'u
                val cachedStocks = stockCache.getStocks(symbols)
                if (cachedStocks.isNotEmpty()) {
                    Log.d("PortfolioViewModel", "Loaded ${cachedStocks.size} stocks from cache for portfolio")
                    _allStocksList.value = cachedStocks
                    updatePortfolioValue()
                    return@launch
                }
                
                // Jeśli nie znaleziono danych w cache, załaduj z API jako fallback
                Log.d("PortfolioViewModel", "No cached data for portfolio stocks, loading from API")
                loadStocksList()
            }
        }
    }
}

// Type of portfolio item
enum class PortfolioItemType {
    CRYPTO,
    STOCK
}

data class PortfolioItemUiState(
    val id: String,
    val name: String,
    val symbol: String,
    val quantity: Double,
    val price: Double,
    val value: Double,
    val imageUrl: String? = null,
    val type: PortfolioItemType
) 