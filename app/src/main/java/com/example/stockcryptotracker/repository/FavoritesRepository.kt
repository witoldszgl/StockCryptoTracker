package com.example.stockcryptotracker.repository

import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.room.FavoriteCrypto
import com.example.stockcryptotracker.data.room.FavoriteCryptoDao
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val favoriteCryptoDao: FavoriteCryptoDao) {
    
    fun getAllFavorites(): Flow<List<FavoriteCrypto>> {
        return favoriteCryptoDao.getAllFavorites()
    }
    
    fun getAllFavoriteIds(): Flow<List<String>> {
        return favoriteCryptoDao.getAllFavoriteIds()
    }
    
    fun isFavorite(cryptoId: String): Flow<Boolean> {
        return favoriteCryptoDao.isFavorite(cryptoId)
    }
    
    suspend fun addToFavorites(crypto: CryptoCurrency) {
        val favoriteCrypto = FavoriteCrypto(
            cryptoId = crypto.id,
            symbol = crypto.symbol,
            name = crypto.name
        )
        favoriteCryptoDao.addToFavorites(favoriteCrypto)
    }
    
    suspend fun removeFromFavorites(cryptoId: String) {
        favoriteCryptoDao.removeFromFavorites(cryptoId)
    }
    
    suspend fun toggleFavorite(crypto: CryptoCurrency, isFavorite: Boolean) {
        if (isFavorite) {
            removeFromFavorites(crypto.id)
        } else {
            addToFavorites(crypto)
        }
    }
} 