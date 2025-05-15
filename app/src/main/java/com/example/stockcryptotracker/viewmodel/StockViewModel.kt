package com.example.stockcryptotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.network.StockSearchResult
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

@OptIn(kotlinx.coroutines.FlowPreview::class)
class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository()
    private val favoritesRepository: StockFavoritesRepository
    
    private val _allStockList = MutableStateFlow<List<Stock>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow(StockCategory.ALL)
    val selectedCategory: StateFlow<StockCategory> = _selectedCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<StockSearchResult>>(emptyList())
    val searchResults: StateFlow<List<StockSearchResult>> = _searchResults.asStateFlow()
    
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteIds: StateFlow<List<String>> = _favoriteIds.asStateFlow()
    
    // Cache for favorite status to prevent UI flickering
    private val favoriteStatusCache = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    
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
            StockCategory.MOST_ACTIVE -> allStocks.sortedByDescending { it.volume ?: 0 }.take(15)
            StockCategory.TOP_GAINERS -> allStocks.sortedByDescending { it.changePercent }.take(15)
            StockCategory.TOP_LOSERS -> allStocks.sortedBy { it.changePercent }.take(15)
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
            loadStocks() // Reload default stocks list
        }
    }
    
    fun setCategory(category: StockCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            _searchQuery.value = ""
            _searchResults.value = emptyList()
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
            }
        }
    }
    
    fun loadStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = when (_selectedCategory.value) {
                StockCategory.ALL -> repository.getAllStocks()
                StockCategory.MOST_ACTIVE -> repository.getMostActiveStocks()
                StockCategory.TOP_GAINERS -> repository.getTopGainers()
                StockCategory.TOP_LOSERS -> repository.getTopLosers()
            }
            
            result.onSuccess { stocks ->
                _allStockList.value = stocks
                _isLoading.value = false
            }.onFailure { error ->
                _error.value = error.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }
    
    private fun searchStocks(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.searchStocks(query)
                .onSuccess { results ->
                    _searchResults.value = results
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    fun getStockDetail(symbol: String, onResult: (Result<Stock>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getStockQuote(symbol))
        }
    }
} 