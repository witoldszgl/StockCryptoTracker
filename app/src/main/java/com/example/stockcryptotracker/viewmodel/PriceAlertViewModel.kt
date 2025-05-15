package com.example.stockcryptotracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.repository.PriceAlertRepository
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

class PriceAlertViewModel(application: Application) : AndroidViewModel(application) {
    
    private val priceAlertRepository: PriceAlertRepository
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Filter options for alerts view
    enum class AlertFilter { ALL, CRYPTO, STOCKS, ACTIVE }
    private val _currentFilter = MutableStateFlow(AlertFilter.ALL)
    val currentFilter: StateFlow<AlertFilter> = _currentFilter.asStateFlow()
    
    // Access to alert data
    val allAlerts: StateFlow<List<PriceAlert>>
    val filteredAlerts: StateFlow<List<PriceAlert>>
    
    init {
        Log.d("PriceAlertViewModel", "Initializing")
        val database = CryptoDatabase.getDatabase(application)
        priceAlertRepository = PriceAlertRepository(database.priceAlertDao())
        
        // Initialize alert data streams
        allAlerts = priceAlertRepository.getAllAlerts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        // Add filtered alerts based on selected filter
        filteredAlerts = _currentFilter
            .map { filter -> filter }
            .combine(allAlerts) { filter, alerts ->
                when (filter) {
                    AlertFilter.ALL -> alerts
                    AlertFilter.CRYPTO -> alerts.filter { it.isCrypto }
                    AlertFilter.STOCKS -> alerts.filter { !it.isCrypto }
                    AlertFilter.ACTIVE -> alerts.filter { it.isActive }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun setFilter(filter: AlertFilter) {
        _currentFilter.value = filter
    }
    
    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
    
    fun addCryptoAlert(crypto: CryptoCurrency, targetPrice: Double, isAboveTarget: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = priceAlertRepository.addCryptoAlert(crypto, targetPrice, isAboveTarget)
                
                if (success) {
                    val direction = if (isAboveTarget) "above" else "below"
                    _successMessage.value = "Price alert for ${crypto.symbol.uppercase()} ${formatPrice(targetPrice)} $direction added"
                } else {
                    _error.value = "A similar alert already exists"
                }
            } catch (e: Exception) {
                Log.e("PriceAlertViewModel", "Error adding crypto alert", e)
                _error.value = "Failed to add alert: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun addStockAlert(stock: Stock, targetPrice: Double, isAboveTarget: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = priceAlertRepository.addStockAlert(stock, targetPrice, isAboveTarget)
                
                if (success) {
                    val direction = if (isAboveTarget) "above" else "below"
                    _successMessage.value = "Price alert for ${stock.symbol.uppercase()} ${formatPrice(targetPrice)} $direction added"
                } else {
                    _error.value = "A similar alert already exists"
                }
            } catch (e: Exception) {
                Log.e("PriceAlertViewModel", "Error adding stock alert", e)
                _error.value = "Failed to add alert: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                priceAlertRepository.deleteAlert(alertId)
                _successMessage.value = "Alert removed successfully"
            } catch (e: Exception) {
                Log.e("PriceAlertViewModel", "Error deleting alert", e)
                _error.value = "Failed to delete alert: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun toggleAlertActive(alertId: Int, isActive: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            try {
                priceAlertRepository.toggleAlertActive(alertId, isActive)
                val status = if (isActive) "enabled" else "disabled"
                _successMessage.value = "Alert ${status} successfully"
            } catch (e: Exception) {
                Log.e("PriceAlertViewModel", "Error toggling alert status", e)
                _error.value = "Failed to update alert: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    private fun formatPrice(price: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price)
    }
} 