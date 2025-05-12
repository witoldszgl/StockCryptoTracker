package com.example.stockcryptotracker.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cryptos")
data class FavoriteCrypto(
    @PrimaryKey
    val cryptoId: String,
    val symbol: String,
    val name: String,
    val dateAdded: Long = System.currentTimeMillis()
) 