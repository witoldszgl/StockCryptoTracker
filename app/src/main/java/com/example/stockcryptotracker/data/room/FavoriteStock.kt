package com.example.stockcryptotracker.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stocks")
data class FavoriteStock(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val exchange: String,
    val dateAdded: Long = System.currentTimeMillis()
) 