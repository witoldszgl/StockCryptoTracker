package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.data.toCryptoCurrency
import com.example.stockcryptotracker.data.toPricePoints
import com.example.stockcryptotracker.network.CryptoDetailResponse
import com.example.stockcryptotracker.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CryptoRepository {
    private val apiService = RetrofitClient.cryptoApiService

    suspend fun getCryptocurrencies(): Result<List<CryptoCurrency>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCryptocurrencies()
                Result.success(response.map { it.toCryptoCurrency() })
            } catch (e: Exception) {
                Log.e("CryptoRepository", "Error fetching cryptocurrencies", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getCryptoDetail(id: String): Result<CryptoDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCryptoDetail(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getPriceHistory(id: String, days: String = "7"): Result<List<PricePoint>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPriceHistory(id = id, days = days)
                Result.success(response.toPricePoints())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 