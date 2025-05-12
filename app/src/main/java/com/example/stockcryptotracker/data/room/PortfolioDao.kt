package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPortfolioItem(portfolioItem: PortfolioItem)
    
    @Update
    suspend fun updatePortfolioItem(portfolioItem: PortfolioItem)
    
    @Query("DELETE FROM portfolio_items WHERE cryptoId = :cryptoId")
    suspend fun removePortfolioItem(cryptoId: String)
    
    @Query("SELECT * FROM portfolio_items ORDER BY lastUpdated DESC")
    fun getAllPortfolioItems(): Flow<List<PortfolioItem>>
    
    @Query("SELECT * FROM portfolio_items WHERE cryptoId = :cryptoId")
    fun getPortfolioItem(cryptoId: String): Flow<PortfolioItem?>
    
    @Query("SELECT EXISTS(SELECT 1 FROM portfolio_items WHERE cryptoId = :cryptoId LIMIT 1)")
    fun hasPortfolioItem(cryptoId: String): Flow<Boolean>
    
    @Query("SELECT SUM(quantity) FROM portfolio_items WHERE cryptoId = :cryptoId")
    fun getTotalQuantity(cryptoId: String): Flow<Double?>
} 