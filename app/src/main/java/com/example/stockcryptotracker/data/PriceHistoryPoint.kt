package com.example.stockcryptotracker.data

/**
 * Reprezentuje pojedynczy punkt w historii cen kryptowaluty
 */
data class PriceHistoryPoint(
    val timestamp: Long,
    val date: String,
    val price: Double
) 