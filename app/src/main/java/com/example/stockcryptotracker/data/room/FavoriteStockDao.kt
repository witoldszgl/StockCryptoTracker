package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favoriteStock: FavoriteStock)
    
    @Query("DELETE FROM favorite_stocks WHERE symbol = :symbol")
    suspend fun removeFromFavorites(symbol: String)
    
    @Query("SELECT * FROM favorite_stocks ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<FavoriteStock>>
    
    @Query("SELECT symbol FROM favorite_stocks")
    fun getAllFavoriteIds(): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stocks WHERE symbol = :symbol LIMIT 1)")
    fun isFavorite(symbol: String): Flow<Boolean>
} 