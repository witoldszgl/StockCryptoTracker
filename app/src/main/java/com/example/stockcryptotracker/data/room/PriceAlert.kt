package com.example.stockcryptotracker.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a price alert for a cryptocurrency or stock
 */
@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val assetId: String, // Crypto ID or stock symbol
    val assetName: String,
    val assetSymbol: String,
    val targetPrice: Double,
    val isAboveTarget: Boolean, // true if alert for price above target, false for below
    val isActive: Boolean = true,
    val isCrypto: Boolean, // true for crypto, false for stock
    val createdAt: Long = System.currentTimeMillis()
) 