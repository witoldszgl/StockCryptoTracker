package com.example.stockcryptotracker.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_items")
data class PortfolioItem(
    @PrimaryKey
    val cryptoId: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val lastUpdated: Long = System.currentTimeMillis()
) 