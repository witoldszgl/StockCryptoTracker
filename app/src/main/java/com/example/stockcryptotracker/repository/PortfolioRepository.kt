package com.example.stockcryptotracker.repository

import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.room.PortfolioDao
import com.example.stockcryptotracker.data.room.PortfolioItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PortfolioRepository(private val portfolioDao: PortfolioDao) {
    
    fun getAllPortfolioItems(): Flow<List<PortfolioItem>> {
        return portfolioDao.getAllPortfolioItems()
    }
    
    fun getPortfolioItem(cryptoId: String): Flow<PortfolioItem?> {
        return portfolioDao.getPortfolioItem(cryptoId)
    }
    
    fun hasPortfolioItem(cryptoId: String): Flow<Boolean> {
        return portfolioDao.hasPortfolioItem(cryptoId)
    }
    
    suspend fun addOrUpdatePortfolioItem(crypto: CryptoCurrency, quantity: Double) {
        val currentItem = portfolioDao.getPortfolioItem(crypto.id).first()
        
        if (currentItem != null) {
            // Update existing item
            val updatedItem = PortfolioItem(
                cryptoId = currentItem.cryptoId,
                symbol = currentItem.symbol,
                name = currentItem.name,
                quantity = quantity,
                lastUpdated = System.currentTimeMillis()
            )
            portfolioDao.updatePortfolioItem(updatedItem)
        } else {
            // Add new item
            val newItem = PortfolioItem(
                cryptoId = crypto.id,
                symbol = crypto.symbol,
                name = crypto.name,
                quantity = quantity
            )
            portfolioDao.addPortfolioItem(newItem)
        }
    }
    
    suspend fun removePortfolioItem(cryptoId: String) {
        portfolioDao.removePortfolioItem(cryptoId)
    }
    
    // Calculate portfolio value based on current prices
    fun calculatePortfolioValue(portfolioItems: List<PortfolioItem>, cryptoCurrencies: List<CryptoCurrency>): Double {
        var totalValue = 0.0
        
        portfolioItems.forEach { item ->
            val crypto = cryptoCurrencies.find { it.id == item.cryptoId }
            if (crypto != null) {
                totalValue += crypto.currentPrice * item.quantity
            }
        }
        
        return totalValue
    }
} 