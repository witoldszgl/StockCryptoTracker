package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.StockPortfolioDao
import com.example.stockcryptotracker.data.room.StockPortfolioItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StockPortfolioRepository(private val stockPortfolioDao: StockPortfolioDao) {
    
    fun getAllPortfolioItems(): Flow<List<StockPortfolioItem>> {
        return stockPortfolioDao.getAllPortfolioItems()
    }
    
    fun getPortfolioItem(symbol: String): Flow<StockPortfolioItem?> {
        return stockPortfolioDao.getPortfolioItem(symbol)
    }
    
    fun hasPortfolioItem(symbol: String): Flow<Boolean> {
        return stockPortfolioDao.hasPortfolioItem(symbol)
    }
    
    suspend fun addOrUpdatePortfolioItem(stock: Stock, quantity: Double) {
        val currentItem = stockPortfolioDao.getPortfolioItem(stock.symbol).first()
        
        if (currentItem != null) {
            // Update existing item
            val updatedItem = StockPortfolioItem(
                symbol = currentItem.symbol,
                name = currentItem.name,
                exchange = currentItem.exchange,
                quantity = quantity,
                lastUpdated = System.currentTimeMillis()
            )
            stockPortfolioDao.updatePortfolioItem(updatedItem)
        } else {
            // Add new item
            val newItem = StockPortfolioItem(
                symbol = stock.symbol,
                name = stock.name,
                exchange = stock.exchange,
                quantity = quantity
            )
            stockPortfolioDao.addPortfolioItem(newItem)
        }
    }
    
    suspend fun removePortfolioItem(symbol: String) {
        stockPortfolioDao.removePortfolioItem(symbol)
    }
    
    // Calculate portfolio value based on current prices
    fun calculatePortfolioValue(portfolioItems: List<StockPortfolioItem>, stocks: List<Stock>): Double {
        var totalValue = 0.0
        
        portfolioItems.forEach { item ->
            val stock = stocks.find { it.symbol == item.symbol }
            if (stock != null) {
                val itemValue = stock.price * item.quantity
                Log.d("StockPortfolioRepository", "Stock ${item.symbol}: ${item.quantity} x ${stock.price} = $itemValue")
                totalValue += itemValue
            }
        }
        
        Log.d("StockPortfolioRepository", "Total stock portfolio value: $totalValue")
        return totalValue
    }
} 