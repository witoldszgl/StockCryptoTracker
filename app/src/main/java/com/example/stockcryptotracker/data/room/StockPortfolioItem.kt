package com.example.stockcryptotracker.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_portfolio_items")
data class StockPortfolioItem(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val exchange: String,
    val quantity: Double,
    val lastUpdated: Long = System.currentTimeMillis()
) 