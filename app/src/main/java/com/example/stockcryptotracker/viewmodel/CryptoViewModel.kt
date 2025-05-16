package com.example.stockcryptotracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.repository.CryptoCompareRepository
import com.example.stockcryptotracker.repository.CryptoRepository
import com.example.stockcryptotracker.repository.FavoritesRepository
import com.example.stockcryptotracker.repository.TimeRange
import com.example.stockcryptotracker.viewmodel.CryptoCategory
import com.example.stockcryptotracker.viewmodel.Tab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.io.IOException
import retrofit2.HttpException

private const val TAG = "CryptoViewModel"
private const val MAX_RETRIES = 3
private const val DEFAULT_PAGE_SIZE = 50
private const val RETRY_DELAY_MS = 1000L

@OptIn(kotlinx.coroutines.FlowPreview::class)
class CryptoViewModel(application: Application) : AndroidViewModel(application) {
    private val cryptoRepository = CryptoRepository()
    private val cryptoCompareRepository = CryptoCompareRepository()
    private val favoritesRepository: FavoritesRepository
    
    private val _allCryptoList = MutableStateFlow<List<CryptoCurrency>>(emptyList())
    
    // Pagination variables
    private val _visibleItemsCount = MutableStateFlow(DEFAULT_PAGE_SIZE)
    val visibleItemsCount: StateFlow<Int> = _visibleItemsCount.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(Tab.ALL)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow(CryptoCategory.ALL)
    val selectedCategory: StateFlow<CryptoCategory> = _selectedCategory.asStateFlow()
    
    private val favoriteIds = MutableStateFlow<List<String>>(emptyList())
    
    // Cache for favorite status to prevent UI flickering
    private val favoriteStatusCache = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.DAYS_7)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val filteredCryptoList: StateFlow<List<CryptoCurrency>> = combine(
        _allCryptoList,
        _searchQuery,
        _selectedTab,
        _selectedCategory,
        favoriteIds.debounce(100).distinctUntilChanged() // Add debounce to stabilize updates
    ) { allCrypto, query, selectedTab, selectedCategory, favoriteIds ->
        // Filter by query
        val queriedList = if (query.isBlank()) {
            allCrypto
        } else {
            allCrypto.filter { crypto ->
                crypto.name.contains(query, ignoreCase = true) || 
                crypto.symbol.contains(query, ignoreCase = true)
            }
        }
        
        // Apply category filter
        val categoryFilteredList = applySortingAndFiltering(queriedList, selectedCategory)
        
        // Filter by tab
        val finalList = if (selectedTab == Tab.ALL) {
            categoryFilteredList
        } else {
            categoryFilteredList.filter { crypto -> favoriteIds.contains(crypto.id) }
        }
        
        finalList
    }.distinctUntilChanged()
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Paginated crypto list that only shows visible items
    val cryptoList: StateFlow<List<CryptoCurrency>> = combine(
        filteredCryptoList,
        _visibleItemsCount
    ) { list, count ->
        list.take(count)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Indicates whether there are more items to load
    val canLoadMore: StateFlow<Boolean> = combine(
        filteredCryptoList,
        _visibleItemsCount
    ) { list, count ->
        count < list.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Detail view properties
    private val _cryptoDetail = MutableStateFlow<CryptoCurrency?>(null)
    val cryptoDetail: StateFlow<CryptoCurrency?> = _cryptoDetail.asStateFlow()
    
    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()
    
    private val _errorDetail = MutableStateFlow<String?>(null)
    val errorDetail: StateFlow<String?> = _errorDetail.asStateFlow()
    
    private val _priceHistory = MutableStateFlow<List<PriceHistoryPoint>>(emptyList())
    val priceHistory: StateFlow<List<PriceHistoryPoint>> = _priceHistory.asStateFlow()
    
    private val _isLoadingPriceHistory = MutableStateFlow(false)
    val isLoadingPriceHistory: StateFlow<Boolean> = _isLoadingPriceHistory.asStateFlow()
    
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()
    
    init {
        val database = CryptoDatabase.getDatabase(application)
        favoritesRepository = FavoritesRepository(database.favoriteCryptoDao())
        
        loadCryptocurrencies()
        
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds()
                .distinctUntilChanged()
                .collect {
                    favoriteIds.value = it
                    // Update cache when favorite list changes
                    updateFavoriteCache(it)
                }
        }
    }
    
    // Load more items when scrolling
    fun loadMoreItems() {
        val currentCount = _visibleItemsCount.value
        val filteredListSize = filteredCryptoList.value.size
        
        if (currentCount < filteredListSize && !_isLoadingMore.value) {
            viewModelScope.launch {
                _isLoadingMore.value = true
                
                // Simulate network delay for smoother loading
                delay(100)
                
                // Increase visible items by page size
                val newCount = (currentCount + DEFAULT_PAGE_SIZE).coerceAtMost(filteredListSize)
                _visibleItemsCount.value = newCount
                
                _isLoadingMore.value = false
            }
        }
    }
    
    // Reset pagination when filters change
    fun resetPagination() {
        _visibleItemsCount.value = DEFAULT_PAGE_SIZE
    }
    
    private fun updateFavoriteCache(favoriteIds: List<String>) {
        // Update cached statuses based on the new favoriteIds list
        favoriteStatusCache.forEach { (cryptoId, flow) ->
            flow.value = favoriteIds.contains(cryptoId)
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        resetPagination()
    }
    
    fun setSelectedTab(tab: Tab) {
        _selectedTab.value = tab
        resetPagination()
    }
    
    fun setCategory(category: CryptoCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            resetPagination()
        }
    }
    
    fun isFavorite(cryptoId: String): StateFlow<Boolean> {
        // Use cached flow if available, or create a new one
        return favoriteStatusCache.getOrPut(cryptoId) {
            // Initialize with current value
            val flow = MutableStateFlow(favoriteIds.value.contains(cryptoId))
            
            // Subscribe to repository updates in the background
            viewModelScope.launch {
                favoritesRepository.isFavorite(cryptoId)
                    .distinctUntilChanged()
                    .collect { isFav ->
                        flow.value = isFav
                    }
            }
            
            flow
        }
    }
    
    fun toggleFavorite(crypto: CryptoCurrency) {
        viewModelScope.launch {
            // Get current status from cache if available, or directly from repository
            val cached = favoriteStatusCache[crypto.id]
            val isFavorite = cached?.value ?: favoritesRepository.isFavorite(crypto.id).first()
            
            // Toggle favorite status directly
            if (isFavorite) {
                favoritesRepository.removeFromFavorites(crypto.id)
                // Update cache immediately to prevent UI delay
                favoriteStatusCache[crypto.id]?.value = false
            } else {
                favoritesRepository.addToFavorites(crypto)
                // Update cache immediately to prevent UI delay
                favoriteStatusCache[crypto.id]?.value = true
            }
            
            // If we're in favorites tab and unfavoriting an item
            if (_selectedTab.value == Tab.FAVORITES && isFavorite) {
                // Force a crypto list update after a short delay
                delay(100)
                loadCryptocurrencies()
            }
        }
    }

    fun loadCryptocurrencies() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            fetchCryptocurrencies()
        }
    }
    
    private suspend fun fetchCryptocurrencies(attempt: Int = 1): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            Log.d(TAG, "Fetching cryptocurrencies, attempt $attempt")
            val cryptocurrencies = cryptoCompareRepository.getAllCryptos()
            
            Log.d(TAG, "Fetched ${cryptocurrencies.size} cryptocurrencies from repository")
            
            if (cryptocurrencies.isNotEmpty()) {
                _allCryptoList.value = cryptocurrencies
                Log.d(TAG, "Successfully updated cryptocurrency list with ${cryptocurrencies.size} items")
                true
            } else {
                Log.e(TAG, "Repository returned empty cryptocurrency list")
                if (attempt < MAX_RETRIES) {
                    Log.d(TAG, "Retrying cryptocurrency fetch, attempt ${attempt + 1} of $MAX_RETRIES")
                    delay(RETRY_DELAY_MS)
                    fetchCryptocurrencies(attempt + 1)
                } else {
                    Log.e(TAG, "Max retries reached, showing error message to user")
                    _error.value = "Unable to load cryptocurrencies. Please try again later."
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cryptocurrencies", e)
            if (attempt < MAX_RETRIES) {
                Log.d(TAG, "Retrying after exception, attempt ${attempt + 1} of $MAX_RETRIES")
                delay(RETRY_DELAY_MS)
                fetchCryptocurrencies(attempt + 1)
            } else {
                Log.e(TAG, "Max retries reached after exception, showing error message to user")
                _error.value = "Network error: ${e.message}"
                false
            }
        } finally {
            _isLoading.value = false
        }
    }
    
    fun fetchCryptoDetail(cryptoId: String) {
        _isLoadingDetail.value = true
        _errorDetail.value = null
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching crypto detail for ID: $cryptoId")
                
                // Rozszerzone mapowanie ID na symbole dla popularnych kryptowalut
                val effectiveId = when {
                    // Top 10
                    cryptoId.lowercase() == "bitcoin" -> "BTC"
                    cryptoId.lowercase() == "ethereum" -> "ETH"
                    cryptoId.lowercase() == "tether" -> "USDT"
                    cryptoId.lowercase() == "binancecoin" -> "BNB"
                    cryptoId.lowercase() == "solana" -> "SOL"
                    cryptoId.lowercase() == "ripple" || cryptoId.lowercase() == "xrp" -> "XRP"
                    cryptoId.lowercase() == "usd-coin" -> "USDC"
                    cryptoId.lowercase() == "staked-ether" -> "STETH"
                    cryptoId.lowercase() == "avalanche-2" || cryptoId.lowercase() == "avalanche" -> "AVAX"
                    cryptoId.lowercase() == "dogecoin" -> "DOGE"
                    // 11-20
                    cryptoId.lowercase() == "tron" -> "TRX"
                    cryptoId.lowercase() == "chainlink" -> "LINK"
                    cryptoId.lowercase() == "toncoin" -> "TON"
                    cryptoId.lowercase() == "polkadot" -> "DOT"
                    cryptoId.lowercase() == "polygon" -> "MATIC"
                    cryptoId.lowercase() == "shiba-inu" -> "SHIB"
                    cryptoId.lowercase() == "dai" -> "DAI"
                    cryptoId.lowercase() == "wrapped-bitcoin" -> "WBTC"
                    cryptoId.lowercase() == "litecoin" -> "LTC"
                    cryptoId.lowercase() == "bitcoin-cash" -> "BCH"
                    // 21-30
                    cryptoId.lowercase() == "uniswap" -> "UNI"
                    cryptoId.lowercase() == "cardano" -> "ADA"
                    cryptoId.lowercase() == "leo-token" -> "LEO"
                    cryptoId.lowercase() == "cosmos" -> "ATOM"
                    cryptoId.lowercase() == "ethereum-classic" -> "ETC"
                    cryptoId.lowercase() == "okb" -> "OKB"
                    cryptoId.lowercase() == "stellar" -> "XLM"
                    cryptoId.lowercase() == "near" -> "NEAR"
                    cryptoId.lowercase() == "internet-computer" -> "ICP"
                    cryptoId.lowercase() == "injective-protocol" -> "INJ"
                    else -> cryptoId
                }
                
                val crypto = cryptoCompareRepository.getCryptoById(effectiveId)
                if (crypto != null) {
                    Log.d(TAG, "Successfully fetched crypto detail: ${crypto.name}")
                    _cryptoDetail.value = crypto
                    
                    // Also fetch price history
                    fetchPriceHistory(effectiveId)
                    
                    // Check if it's a favorite
                    val isFavorite = favoritesRepository.isFavorite(cryptoId).first()
                    _isFavorite.value = isFavorite
                } else {
                    Log.e(TAG, "Failed to fetch crypto detail: Returned null")
                    _errorDetail.value = "Could not find cryptocurrency details. Please check your internet connection."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching crypto detail", e)
                _errorDetail.value = "Error loading details: ${e.message}"
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }
    
    fun fetchBitcoinForTesting() {
        fetchCryptoDetail("BTC")
    }
    
    private fun fetchPriceHistory(cryptoId: String) {
        _isLoadingPriceHistory.value = true
        viewModelScope.launch {
            try {
                val history = cryptoCompareRepository.getCryptoHistoricalData(
                    cryptoId, 
                    _selectedTimeRange.value
                )
                _priceHistory.value = history
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching price history", e)
                // We don't set error here as it's a secondary data
            } finally {
                _isLoadingPriceHistory.value = false
            }
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        if (_selectedTimeRange.value != timeRange) {
            _selectedTimeRange.value = timeRange
            _cryptoDetail.value?.let { crypto ->
                fetchPriceHistory(crypto.id)
            }
        }
    }

    // Helper function to apply sorting and filtering
    private fun applySortingAndFiltering(cryptos: List<CryptoCurrency>, category: CryptoCategory): List<CryptoCurrency> {
        return when (category) {
            CryptoCategory.ALL -> cryptos  // Zachowujemy oryginalną kolejność, która już ma popularne kryptowaluty na górze
            CryptoCategory.TOP_GAINERS -> cryptos.sortedByDescending { it.priceChangePercentage24h }.take(20)
            CryptoCategory.TOP_LOSERS -> cryptos.sortedBy { it.priceChangePercentage24h }.take(20)
            CryptoCategory.MOST_ACTIVE -> cryptos.sortedByDescending { it.totalVolume }.take(20)
        }
    }
}

private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)

