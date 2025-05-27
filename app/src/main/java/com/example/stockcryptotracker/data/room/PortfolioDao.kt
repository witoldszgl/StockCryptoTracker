package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoPortfolioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPortfolioItem(portfolioItem: CryptoPortfolioItem)
    
    @Update
    suspend fun updatePortfolioItem(portfolioItem: CryptoPortfolioItem)
    
    @Query("DELETE FROM crypto_portfolio_items WHERE cryptoId = :cryptoId")
    suspend fun removePortfolioItem(cryptoId: String)
    
    @Query("SELECT * FROM crypto_portfolio_items ORDER BY lastUpdated DESC")
    fun getAllPortfolioItems(): Flow<List<CryptoPortfolioItem>>
    
    @Query("SELECT * FROM crypto_portfolio_items WHERE cryptoId = :cryptoId")
    fun getPortfolioItem(cryptoId: String): Flow<CryptoPortfolioItem?>
    
    @Query("SELECT EXISTS(SELECT 1 FROM crypto_portfolio_items WHERE cryptoId = :cryptoId LIMIT 1)")
    fun hasPortfolioItem(cryptoId: String): Flow<Boolean>
    
    @Query("SELECT SUM(quantity) FROM crypto_portfolio_items WHERE cryptoId = :cryptoId")
    fun getTotalQuantity(cryptoId: String): Flow<Double?>
} 