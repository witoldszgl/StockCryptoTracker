package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.room.CryptoPortfolioDao
import com.example.stockcryptotracker.data.room.CryptoPortfolioItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CryptoPortfolioRepository(private val cryptoPortfolioDao: CryptoPortfolioDao) {
    
    fun getAllPortfolioItems(): Flow<List<CryptoPortfolioItem>> {
        return cryptoPortfolioDao.getAllPortfolioItems()
    }
    
    fun getPortfolioItem(cryptoId: String): Flow<CryptoPortfolioItem?> {
        return cryptoPortfolioDao.getPortfolioItem(cryptoId)
    }
    
    fun hasPortfolioItem(cryptoId: String): Flow<Boolean> {
        return cryptoPortfolioDao.hasPortfolioItem(cryptoId)
    }
    
    suspend fun addOrUpdatePortfolioItem(crypto: CryptoCurrency, quantity: Double) {
        val currentItem = cryptoPortfolioDao.getPortfolioItem(crypto.id).first()
        
        if (currentItem != null) {
            // Update existing item
            val updatedItem = CryptoPortfolioItem(
                cryptoId = currentItem.cryptoId,
                symbol = currentItem.symbol,
                name = currentItem.name,
                quantity = quantity,
                lastUpdated = System.currentTimeMillis()
            )
            cryptoPortfolioDao.updatePortfolioItem(updatedItem)
        } else {
            // Add new item
            val newItem = CryptoPortfolioItem(
                cryptoId = crypto.id,
                symbol = crypto.symbol,
                name = crypto.name,
                quantity = quantity
            )
            cryptoPortfolioDao.addPortfolioItem(newItem)
        }
    }
    
    suspend fun removePortfolioItem(cryptoId: String) {
        cryptoPortfolioDao.removePortfolioItem(cryptoId)
    }
    
    // Calculate portfolio value based on current prices
    fun calculatePortfolioValue(portfolioItems: List<CryptoPortfolioItem>, cryptoCurrencies: List<CryptoCurrency>): Double {
        var totalValue = 0.0
        
        portfolioItems.forEach { item ->
            val crypto = cryptoCurrencies.find { it.id == item.cryptoId }
            if (crypto != null) {
                val itemValue = crypto.currentPrice * item.quantity
                Log.d("PortfolioRepository", "Crypto ${item.symbol}: ${item.quantity} x ${crypto.currentPrice} = $itemValue")
                totalValue += itemValue
            }
        }
        
        Log.d("PortfolioRepository", "Total portfolio value: $totalValue")
        return totalValue
    }
} 