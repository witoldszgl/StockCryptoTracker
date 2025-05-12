package com.example.stockcryptotracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.network.CryptoDetailResponse
import com.example.stockcryptotracker.repository.CryptoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CryptoDetailViewModel : ViewModel() {
    private val repository = CryptoRepository()
    
    private val _cryptoDetail = MutableStateFlow<CryptoDetailResponse?>(null)
    val cryptoDetail: StateFlow<CryptoDetailResponse?> = _cryptoDetail.asStateFlow()
    
    private val _priceHistory = MutableStateFlow<List<PricePoint>>(emptyList())
    val priceHistory: StateFlow<List<PricePoint>> = _priceHistory.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow("7")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadCryptoDetail(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getCryptoDetail(id)
                .onSuccess { detail ->
                    _cryptoDetail.value = detail
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    fun loadPriceHistory(id: String, days: String = _selectedTimeRange.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getPriceHistory(id, days)
                .onSuccess { history ->
                    _priceHistory.value = history
                    _selectedTimeRange.value = days
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
} 