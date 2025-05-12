package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCryptoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favoriteCrypto: FavoriteCrypto)
    
    @Query("DELETE FROM favorite_cryptos WHERE cryptoId = :cryptoId")
    suspend fun removeFromFavorites(cryptoId: String)
    
    @Query("SELECT * FROM favorite_cryptos ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<FavoriteCrypto>>
    
    @Query("SELECT cryptoId FROM favorite_cryptos")
    fun getAllFavoriteIds(): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cryptos WHERE cryptoId = :cryptoId LIMIT 1)")
    fun isFavorite(cryptoId: String): Flow<Boolean>
} 