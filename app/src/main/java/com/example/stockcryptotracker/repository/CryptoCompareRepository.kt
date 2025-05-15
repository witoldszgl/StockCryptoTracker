package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.network.CryptoCompareApiService
import com.example.stockcryptotracker.network.CryptoCompareData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "CryptoCompareRepository"

enum class TimeRange {
    HOUR_1, HOUR_24, DAYS_7, DAYS_30, DAYS_90, YEAR_1
}

class CryptoCompareRepository {

    private val apiKey = "aab3c67a05413db4ca8310a0c31f37745574ec1cbf800d197be3004373275c55"
    private val baseUrl = "https://min-api.cryptocompare.com/"
    
    // Symbol to ID mapping for common cryptocurrencies
    private val symbolMap = mapOf(
        "bitcoin" to "BTC",
        "ethereum" to "ETH",
        "tether" to "USDT",
        "binancecoin" to "BNB",
        "ripple" to "XRP",
        "solana" to "SOL",
        "cardano" to "ADA",
        "dogecoin" to "DOGE",
        "polkadot" to "DOT",
        "tron" to "TRX"
    )
    
    // Create OkHttpClient with logging
    private val okHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val cryptoCompareService = retrofit.create(CryptoCompareApiService::class.java)
    
    suspend fun getAllCryptos(): List<CryptoCurrency> {
        return withContext(Dispatchers.IO) {
            try {
                val response = cryptoCompareService.getTopCryptos(apiKey = apiKey)
                mapCryptoCompareDataListToCryptoCurrencyList(response.data)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching cryptos: ${e.message}")
                emptyList()
            }
        }
    }
    
    suspend fun getCryptoById(cryptoId: String): CryptoCurrency? {
        return withContext(Dispatchers.IO) {
            try {
                // Convert ID to symbol if necessary
                val symbol = getSymbol(cryptoId)
                
                Log.d(TAG, "Fetching details for crypto: $cryptoId (symbol: $symbol)")
                val response = cryptoCompareService.getCryptoDetails(symbol = symbol, apiKey = apiKey)
                
                // Dump raw response for debugging
                Log.d(TAG, "Response received: RAW fields present: ${response.raw != null}")
                if (response.raw == null) {
                    Log.e(TAG, "Raw data is null in response")
                    return@withContext null
                }
                
                // Use uppercase symbol for lookup as the API returns uppercase keys
                val upperSymbol = symbol.uppercase()
                Log.d(TAG, "Looking for symbol: $upperSymbol in response")
                
                val rawData = response.raw?.get(upperSymbol)?.get("USD")
                if (rawData == null) {
                    Log.e(TAG, "No USD data found for symbol: $upperSymbol")
                    
                    // Dump available keys for debugging
                    val availableKeys = response.raw?.keys?.joinToString(", ") ?: "none"
                    Log.e(TAG, "Available symbols in response: $availableKeys")
                    
                    return@withContext null
                }
                
                Log.d(TAG, "Successfully fetched details for $cryptoId (symbol: $symbol)")
                CryptoCurrency(
                    id = cryptoId,
                    symbol = symbol,
                    name = getReadableName(cryptoId, symbol), // Use more readable name
                    currentPrice = rawData.price,
                    marketCap = rawData.marketCap.toLong(),
                    marketCapRank = 0, // Not provided by CryptoCompare
                    priceChangePercentage24h = rawData.priceChangePercentage24h,
                    image = "https://cryptocompare.com${rawData.imageUrl}",
                    high24h = rawData.high24h,
                    low24h = rawData.low24h,
                    totalVolume = rawData.totalVolume24h,
                    priceChangePercentage7d = 0.0, // Not available in detail endpoint
                    priceChangePercentage30d = 0.0 // Not available in detail endpoint
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching crypto detail: ${e.message}", e)
                null
            }
        }
    }
    
    suspend fun getCryptoHistoricalData(symbol: String, timeRange: TimeRange = TimeRange.DAYS_7): List<PriceHistoryPoint> {
        return when (timeRange) {
            TimeRange.HOUR_1 -> getCryptoHistoricalMinute(symbol, 60) // Last hour with minute precision
            TimeRange.HOUR_24 -> getCryptoHistoricalHourly(symbol, 24) // Last 24 hours
            TimeRange.DAYS_7 -> getCryptoHistoricalDaily(symbol, 7) // Last 7 days
            TimeRange.DAYS_30 -> getCryptoHistoricalDaily(symbol, 30) // Last 30 days
            TimeRange.DAYS_90 -> getCryptoHistoricalDaily(symbol, 90) // Last 90 days
            TimeRange.YEAR_1 -> getCryptoHistoricalDaily(symbol, 365) // Last year
        }
    }
    
    private suspend fun getCryptoHistoricalMinute(symbol: String, limit: Int): List<PriceHistoryPoint> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching minute historical data for $symbol, limit: $limit")
                val response = cryptoCompareService.getCryptoHistoricalMinute(
                    symbol = symbol,
                    limit = limit,
                    apiKey = apiKey
                )
                
                mapHistoricalDataToPoints(response, "HH:mm")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching minute historical data: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    private suspend fun getCryptoHistoricalHourly(symbol: String, limit: Int): List<PriceHistoryPoint> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching hourly historical data for $symbol, limit: $limit")
                val response = cryptoCompareService.getCryptoHistoricalHourly(
                    symbol = symbol,
                    limit = limit,
                    apiKey = apiKey
                )
                
                mapHistoricalDataToPoints(response, "HH:mm dd/MM")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching hourly historical data: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    private suspend fun getCryptoHistoricalDaily(symbol: String, limit: Int): List<PriceHistoryPoint> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching daily historical data for $symbol, limit: $limit")
                val response = cryptoCompareService.getCryptoHistoricalDaily(
                    symbol = symbol,
                    limit = limit,
                    apiKey = apiKey
                )
                
                mapHistoricalDataToPoints(response, "dd/MM/yyyy")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching daily historical data: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    private fun mapHistoricalDataToPoints(response: com.example.stockcryptotracker.network.CryptoCompareHistoricalResponse, dateFormat: String): List<PriceHistoryPoint> {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        
        return response.data.points.map { point ->
            val date = Date(point.time * 1000)
            val formattedDate = formatter.format(date)
            
            PriceHistoryPoint(
                timestamp = point.time,
                date = formattedDate,
                price = point.close
            )
        }
    }
    
    private fun mapCryptoCompareDataListToCryptoCurrencyList(data: List<CryptoCompareData>): List<CryptoCurrency> {
        return data.mapNotNull { cryptoData ->
            val usdData = cryptoData.raw?.usd
            if (usdData != null) {
                CryptoCurrency(
                    id = cryptoData.coinInfo.symbol.lowercase(),
                    symbol = cryptoData.coinInfo.symbol.lowercase(),
                    name = cryptoData.coinInfo.name,
                    currentPrice = usdData.price,
                    marketCap = usdData.marketCap.toLong(),
                    marketCapRank = 0, // Not provided directly by CryptoCompare
                    priceChangePercentage24h = usdData.priceChangePercentage24h,
                    image = "https://cryptocompare.com${cryptoData.coinInfo.imageUrl}",
                    high24h = usdData.high24h,
                    low24h = usdData.low24h,
                    totalVolume = usdData.volume24h,
                    priceChangePercentage7d = 0.0, // Not available in list endpoint
                    priceChangePercentage30d = 0.0 // Not available in list endpoint
                )
            } else {
                null
            }
        }
    }
    
    // Helper function to convert ID to symbol
    private fun getSymbol(cryptoId: String): String {
        // If input is already a symbol (3-5 chars uppercase), use it directly
        if (cryptoId.length in 2..5 && cryptoId.uppercase() == cryptoId) {
            return cryptoId
        }
        
        // Try to find in symbol map
        return symbolMap[cryptoId.lowercase()] ?: cryptoId
    }
    
    // Helper to get readable name
    private fun getReadableName(cryptoId: String, symbol: String): String {
        return when (cryptoId.lowercase()) {
            "bitcoin" -> "Bitcoin"
            "ethereum" -> "Ethereum"
            "tether" -> "Tether"
            "binancecoin" -> "Binance Coin"
            "ripple" -> "XRP"
            "solana" -> "Solana"
            "cardano" -> "Cardano"
            "dogecoin" -> "Dogecoin"
            "polkadot" -> "Polkadot"
            "tron" -> "TRON"
            else -> symbol.uppercase() // Fallback to symbol
        }
    }
} 