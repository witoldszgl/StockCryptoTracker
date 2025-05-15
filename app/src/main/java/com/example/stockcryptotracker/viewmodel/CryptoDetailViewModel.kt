package com.example.stockcryptotracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.network.CryptoDetailResponse
import com.example.stockcryptotracker.repository.CryptoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.IOException
import retrofit2.HttpException

private const val TAG = "CryptoDetailViewModel"
private const val MAX_RETRIES = 3

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
    
    // Mapowanie parametru UI do wartości API dla kryptowalut
    private val timeRangeToApiParam = mapOf(
        "1" to "1",     // 1 dzień
        "7" to "7",     // 7 dni
        "30" to "30",   // 30 dni
        "365" to "365"  // 1 rok
    )
    
    fun loadCryptoDetail(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            fetchCryptoDetailWithRetry(id)
        }
    }
    
    private suspend fun fetchCryptoDetailWithRetry(id: String, attempt: Int = 1) {
        try {
            Log.d(TAG, "Fetching crypto detail for $id, attempt $attempt")
            
            repository.getCryptoDetail(id)
                .onSuccess { detail ->
                    _cryptoDetail.value = detail
                    _isLoading.value = false
                    _error.value = null
                    
                    // Automatically load price history after successful detail load
                    loadPriceHistory(id)
                }
                .onFailure { error ->
                    handleError(error, attempt) { fetchCryptoDetailWithRetry(id, it) }
                }
        } catch (e: Exception) {
            handleError(e, attempt) { fetchCryptoDetailWithRetry(id, it) }
        }
    }
    
    fun loadPriceHistory(id: String, days: String = _selectedTimeRange.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            fetchPriceHistoryWithRetry(id, days)
        }
    }
    
    private suspend fun fetchPriceHistoryWithRetry(id: String, days: String, attempt: Int = 1) {
        try {
            // Transform UI parameter to API value
            val apiParam = timeRangeToApiParam[days] ?: "7"
            
            Log.d(TAG, "Loading price history for $id with range $days (API param: $apiParam), attempt $attempt")
            
            repository.getPriceHistory(id, apiParam)
                .onSuccess { history ->
                    _priceHistory.value = history
                    _selectedTimeRange.value = days
                    _isLoading.value = false
                    _error.value = null
                    
                    Log.d(TAG, "Loaded ${history.size} price points for range $days")
                }
                .onFailure { error ->
                    handleError(error, attempt) { fetchPriceHistoryWithRetry(id, days, it) }
                }
        } catch (e: Exception) {
            handleError(e, attempt) { fetchPriceHistoryWithRetry(id, days, it) }
        }
    }
    
    private suspend fun handleError(error: Throwable, attempt: Int, retryAction: suspend (Int) -> Unit) {
        val errorMsg = when (error) {
            is HttpException -> {
                when (error.code()) {
                    429 -> "Rate limit exceeded (429). Retrying..."
                    else -> "HTTP Error: ${error.code()} - ${error.message()}"
                }
            }
            is IOException -> "Network error: ${error.message}"
            else -> error.message ?: "Unknown error occurred"
        }
        
        Log.e(TAG, "Error: $errorMsg", error)
        
        if (attempt < MAX_RETRIES && (error is HttpException && error.code() == 429 || error is IOException)) {
            val backoffDelay = (2.0.pow(attempt.toDouble()) * 1000).toLong()
            Log.d(TAG, "Retrying in $backoffDelay ms (attempt $attempt of $MAX_RETRIES)")
            _error.value = "Rate limit exceeded. Retrying... ($attempt/$MAX_RETRIES)"
            
            delay(backoffDelay)
            retryAction(attempt + 1)
        } else {
            _error.value = errorMsg
            _isLoading.value = false
        }
    }
}

private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent) 