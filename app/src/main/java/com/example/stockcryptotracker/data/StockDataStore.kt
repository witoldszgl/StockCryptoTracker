package com.example.stockcryptotracker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Klasa odpowiedzialna za przechowywanie ostatnich pobranych danych z API
 */
class StockDataStore(context: Context) {
    private val TAG = "StockDataStore"
    private val PREF_NAME = "stock_data_store"
    private val KEY_STOCKS = "saved_stocks"
    private val KEY_LAST_UPDATE = "last_update_time"
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Zapisuje dane pobranych akcji
     */
    fun saveStocks(stocks: List<Stock>) {
        try {
            val stocksJson = gson.toJson(stocks)
            prefs.edit()
                .putString(KEY_STOCKS, stocksJson)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Zapisano ${stocks.size} akcji do lokalnego magazynu")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas zapisywania danych akcji", e)
        }
    }
    
    /**
     * Zapisuje dane pojedynczej akcji
     */
    fun saveStock(stock: Stock) {
        try {
            // Pobierz obecną listę
            val currentStocks = getStocks().toMutableList()
            
            // Znajdź i zaktualizuj lub dodaj nową akcję
            val index = currentStocks.indexOfFirst { it.symbol == stock.symbol }
            if (index != -1) {
                currentStocks[index] = stock
            } else {
                currentStocks.add(stock)
            }
            
            // Zapisz zaktualizowaną listę
            saveStocks(currentStocks)
            
            Log.d(TAG, "Zapisano akcję ${stock.symbol} do lokalnego magazynu")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas zapisywania danych akcji ${stock.symbol}", e)
        }
    }
    
    /**
     * Pobiera wszystkie zapisane akcje
     */
    fun getStocks(): List<Stock> {
        val stocksJson = prefs.getString(KEY_STOCKS, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<Stock>>() {}.type
            gson.fromJson(stocksJson, type)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas odczytu danych akcji", e)
            emptyList()
        }
    }
    
    /**
     * Pobiera pojedynczą akcję po symbolu
     */
    fun getStock(symbol: String): Stock? {
        return getStocks().find { it.symbol == symbol }
    }
    
    /**
     * Sprawdza czy mamy zapisane dane dla określonego symbolu
     */
    fun hasStockData(symbol: String): Boolean {
        return getStock(symbol) != null
    }
    
    /**
     * Pobiera czas ostatniej aktualizacji
     */
    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0)
    }
    
    /**
     * Formatuje czas ostatniej aktualizacji jako String
     */
    fun getLastUpdateTimeFormatted(): String {
        val lastUpdate = getLastUpdateTime()
        if (lastUpdate == 0L) {
            return "Brak danych"
        }
        
        return Date(lastUpdate).toString()
    }
    
    /**
     * Czyści wszystkie zapisane dane
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Wyczyszczono wszystkie zapisane dane")
    }
} 