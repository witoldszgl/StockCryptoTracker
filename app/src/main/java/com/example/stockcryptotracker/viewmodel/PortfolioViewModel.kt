package com.example.stockcryptotracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.data.room.PortfolioItem
import com.example.stockcryptotracker.repository.CryptoRepository
import com.example.stockcryptotracker.repository.PortfolioRepository
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
    private val portfolioRepository: PortfolioRepository
    
    private val _portfolioItems = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val portfolioItems: StateFlow<List<PortfolioItem>> = _portfolioItems.asStateFlow()
    
    private val _allCryptoList = MutableStateFlow<List<CryptoCurrency>>(emptyList())
    val allCryptoList: StateFlow<List<CryptoCurrency>> = _allCryptoList.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _portfolioValue = MutableStateFlow(0.0)
    val portfolioValue: StateFlow<String> = _portfolioValue.map { 
        formatCurrency(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = formatCurrency(0.0)
    )
    
    // Combined state for portfolio items with current prices
    val portfolioItemsWithValue = combine(
        _portfolioItems,
        _allCryptoList
    ) { portfolioItems, cryptoList ->
        portfolioItems.map { item ->
            val crypto = cryptoList.find { it.id == item.cryptoId }
            val currentPrice = crypto?.currentPrice ?: 0.0
            val value = item.quantity * currentPrice
            
            PortfolioItemUiState(
                id = item.cryptoId,
                name = item.name,
                symbol = item.symbol,
                quantity = item.quantity,
                price = currentPrice,
                value = value
            )
        }.sortedByDescending { it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Filtered portfolio items based on search
    val filteredPortfolioItems = combine(
        portfolioItemsWithValue,
        _searchQuery
    ) { items, query ->
        if (query.isEmpty()) {
            items
        } else {
            items.filter { 
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
        val database = CryptoDatabase.getDatabase(application)
        portfolioRepository = PortfolioRepository(database.portfolioDao())
        
        loadData()
        
        // Observe portfolio items
        viewModelScope.launch {
            portfolioRepository.getAllPortfolioItems().collect { items ->
                _portfolioItems.value = items
                updatePortfolioValue()
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            cryptoRepository.getCryptocurrencies()
                .onSuccess { cryptocurrencies ->
                    _allCryptoList.value = cryptocurrencies
                    _isLoading.value = false
                    updatePortfolioValue()
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    fun addUpdatePortfolioItem(cryptoId: String, quantity: Double) {
        viewModelScope.launch {
            val crypto = _allCryptoList.value.find { it.id == cryptoId }
            if (crypto != null) {
                portfolioRepository.addOrUpdatePortfolioItem(crypto, quantity)
            }
        }
    }
    
    fun removePortfolioItem(cryptoId: String) {
        viewModelScope.launch {
            portfolioRepository.removePortfolioItem(cryptoId)
        }
    }
    
    private fun updatePortfolioValue() {
        val items = _portfolioItems.value
        val cryptos = _allCryptoList.value
        
        if (items.isNotEmpty() && cryptos.isNotEmpty()) {
            _portfolioValue.value = portfolioRepository.calculatePortfolioValue(items, cryptos)
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }
}

data class PortfolioItemUiState(
    val id: String,
    val name: String,
    val symbol: String,
    val quantity: Double,
    val price: Double,
    val value: Double
) 