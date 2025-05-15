package com.example.stockcryptotracker.repository

import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.FavoriteStock
import com.example.stockcryptotracker.data.room.FavoriteStockDao
import kotlinx.coroutines.flow.Flow

class StockFavoritesRepository(private val favoriteStockDao: FavoriteStockDao) {
    
    fun getAllFavorites(): Flow<List<FavoriteStock>> {
        return favoriteStockDao.getAllFavorites()
    }
    
    fun getAllFavoriteIds(): Flow<List<String>> {
        return favoriteStockDao.getAllFavoriteIds()
    }
    
    fun isFavorite(symbol: String): Flow<Boolean> {
        return favoriteStockDao.isFavorite(symbol)
    }
    
    suspend fun addToFavorites(stock: Stock) {
        val favoriteStock = FavoriteStock(
            symbol = stock.symbol,
            name = stock.name,
            exchange = stock.exchange
        )
        favoriteStockDao.addToFavorites(favoriteStock)
    }
    
    suspend fun removeFromFavorites(symbol: String) {
        favoriteStockDao.removeFromFavorites(symbol)
    }
    
    suspend fun toggleFavorite(stock: Stock, isFavorite: Boolean) {
        if (isFavorite) {
            removeFromFavorites(stock.symbol)
        } else {
            addToFavorites(stock)
        }
    }
} 