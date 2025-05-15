package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockPortfolioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPortfolioItem(stockPortfolioItem: StockPortfolioItem)
    
    @Update
    suspend fun updatePortfolioItem(stockPortfolioItem: StockPortfolioItem)
    
    @Query("DELETE FROM stock_portfolio_items WHERE symbol = :symbol")
    suspend fun removePortfolioItem(symbol: String)
    
    @Query("SELECT * FROM stock_portfolio_items ORDER BY lastUpdated DESC")
    fun getAllPortfolioItems(): Flow<List<StockPortfolioItem>>
    
    @Query("SELECT * FROM stock_portfolio_items WHERE symbol = :symbol")
    fun getPortfolioItem(symbol: String): Flow<StockPortfolioItem?>
    
    @Query("SELECT EXISTS(SELECT 1 FROM stock_portfolio_items WHERE symbol = :symbol LIMIT 1)")
    fun hasPortfolioItem(symbol: String): Flow<Boolean>
    
    @Query("SELECT SUM(quantity) FROM stock_portfolio_items WHERE symbol = :symbol")
    fun getTotalQuantity(symbol: String): Flow<Double?>
} 