package com.example.stockcryptotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.repository.CryptoRepository
import com.example.stockcryptotracker.repository.FavoritesRepository
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
import java.util.concurrent.ConcurrentHashMap

class CryptoViewModel(application: Application) : AndroidViewModel(application) {
    private val cryptoRepository = CryptoRepository()
    private val favoritesRepository: FavoritesRepository
    
    private val _allCryptoList = MutableStateFlow<List<CryptoCurrency>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(Tab.ALL)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    
    private val favoriteIds = MutableStateFlow<List<String>>(emptyList())
    
    // Cache for favorite status to prevent UI flickering
    private val favoriteStatusCache = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val cryptoList: StateFlow<List<CryptoCurrency>> = combine(
        _allCryptoList,
        _searchQuery,
        _selectedTab,
        favoriteIds.debounce(100).distinctUntilChanged() // Add debounce to stabilize updates
    ) { allCrypto, query, selectedTab, favoriteIds ->
        // Filter by query
        val queriedList = if (query.isBlank()) {
            allCrypto
        } else {
            allCrypto.filter { crypto ->
                crypto.name.contains(query, ignoreCase = true) || 
                crypto.symbol.contains(query, ignoreCase = true)
            }
        }
        
        // Filter by tab
        when (selectedTab) {
            Tab.ALL -> queriedList
            Tab.FAVORITES -> queriedList.filter { crypto -> favoriteIds.contains(crypto.id) }
        }
    }.distinctUntilChanged()
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
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
    
    private fun updateFavoriteCache(favoriteIds: List<String>) {
        // Update cached statuses based on the new favoriteIds list
        favoriteStatusCache.forEach { (cryptoId, flow) ->
            flow.value = favoriteIds.contains(cryptoId)
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSelectedTab(tab: Tab) {
        _selectedTab.value = tab
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
                kotlinx.coroutines.delay(100)
                loadCryptocurrencies()
            }
        }
    }

    fun loadCryptocurrencies() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            cryptoRepository.getCryptocurrencies()
                .onSuccess { cryptocurrencies ->
                    _allCryptoList.value = cryptocurrencies
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
}

enum class Tab {
    ALL, FAVORITES
} 