package com.example.stockcryptotracker.network

/**
 * Klasa reprezentująca wynik wyszukiwania akcji
 */
data class StockSearchResult(
    val symbol: String,
    val securityName: String,
    val securityType: String,
    val exchange: String
) 