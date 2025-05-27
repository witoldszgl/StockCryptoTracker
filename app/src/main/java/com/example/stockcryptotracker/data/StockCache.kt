package com.example.stockcryptotracker.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

/**
 * Klasa odpowiedzialna za cache'owanie danych o akcjach
 */
class StockCache(private val context: Context) {
    companion object {
        private const val TAG = "StockCache"
        private const val CACHE_FILENAME = "stock_cache.json"
        private const val CACHE_EXPIRY_MS = 3600000 // 1 godzina
    }

    private val gson = Gson()
    private val cacheFile: File = File(context.cacheDir, CACHE_FILENAME)
    
    // Wewnętrzna klasa dla informacji o cache'u
    private data class CacheData(
        val stocks: Map<String, Stock>,
        val timestamp: Long
    )
    
    /**
     * Zapisuje dane o akcjach do cache'u
     */
    fun saveStocks(stocks: List<Stock>) {
        try {
            // Pobierz istniejące dane, jeśli są
            val existingData = readCacheData()
            
            // Stwórz nową mapę z istniejących danych
            val stockMap = existingData?.stocks?.toMutableMap() ?: mutableMapOf()
            
            // Dodaj/aktualizuj dane o akcjach
            stocks.forEach { stock ->
                // Upewnij się, że wszystkie pola są prawidłowo ustawione
                val updatedStock = stock.copy(
                    price = stock.currentPrice.takeIf { it > 0 } ?: stock.price,
                    currentPrice = stock.currentPrice.takeIf { it > 0 } ?: stock.price,
                    change = stock.change.takeIf { it != 0.0 } ?: (stock.currentPrice * stock.priceChangePercentage24h / 100.0),
                    changePercent = stock.changePercent.takeIf { it != 0.0 } ?: stock.priceChangePercentage24h / 100.0,
                    volume = stock.volume.takeIf { it > 0 } ?: stock.totalVolume.toLong(),
                    totalVolume = stock.totalVolume.takeIf { it > 0 } ?: stock.volume.toDouble(),
                    logoUrl = stock.logoUrl.takeIf { it.isNotEmpty() } ?: stock.image,
                    image = stock.image.takeIf { it.isNotEmpty() } ?: stock.logoUrl
                )
                stockMap[stock.symbol] = updatedStock
            }
            
            // Zapisz zaktualizowane dane
            val cacheData = CacheData(
                stocks = stockMap,
                timestamp = System.currentTimeMillis()
            )
            
            cacheFile.writeText(gson.toJson(cacheData))
            Log.d(TAG, "Saved ${stocks.size} stocks to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stocks to cache", e)
        }
    }
    
    /**
     * Pobiera dane o akcjach z cache'u
     */
    fun getStocks(symbols: List<String>): List<Stock> {
        try {
            val cacheData = readCacheData() ?: return emptyList()
            
            // Sprawdź, czy cache nie wygasł
            if (isCacheExpired(cacheData.timestamp)) {
                Log.d(TAG, "Cache expired, not using cached data")
                return emptyList()
            }
            
            // Pobierz akcje, które są w liście symboli
            return symbols.mapNotNull { symbol ->
                cacheData.stocks[symbol]
            }.also {
                Log.d(TAG, "Retrieved ${it.size} stocks from cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stocks from cache", e)
            return emptyList()
        }
    }
    
    /**
     * Pobiera dane o pojedynczej akcji z cache'u
     */
    fun getStock(symbol: String): Stock? {
        try {
            val cacheData = readCacheData() ?: return null
            
            // Sprawdź, czy cache nie wygasł
            if (isCacheExpired(cacheData.timestamp)) {
                Log.d(TAG, "Cache expired, not using cached data")
                return null
            }
            
            return cacheData.stocks[symbol].also {
                Log.d(TAG, "Retrieved stock $symbol from cache: ${it != null}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stock from cache", e)
            return null
        }
    }
    
    /**
     * Pobiera wszystkie akcje z cache'u
     */
    fun getAllStocks(): List<Stock> {
        try {
            val cacheData = readCacheData() ?: return emptyList()
            
            // Sprawdź, czy cache nie wygasł
            if (isCacheExpired(cacheData.timestamp)) {
                Log.d(TAG, "Cache expired, not using cached data")
                return emptyList()
            }
            
            return cacheData.stocks.values.toList().also {
                Log.d(TAG, "Retrieved all ${it.size} stocks from cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all stocks from cache", e)
            return emptyList()
        }
    }
    
    /**
     * Czyści cache
     */
    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.d(TAG, "Cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
    
    /**
     * Sprawdza, czy cache wygasł
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val age = currentTime - timestamp
        return age > CACHE_EXPIRY_MS
    }
    
    /**
     * Odczytuje dane z pliku cache'u
     */
    private fun readCacheData(): CacheData? {
        return try {
            if (!cacheFile.exists()) {
                return null
            }
            
            val json = cacheFile.readText()
            val type = object : TypeToken<CacheData>() {}.type
            gson.fromJson(json, type)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading cache file", e)
            null
        }
    }
} 