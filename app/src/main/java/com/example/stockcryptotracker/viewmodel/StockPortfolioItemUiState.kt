package com.example.stockcryptotracker.viewmodel

data class StockPortfolioItemUiState(
    val symbol: String,
    val name: String,
    val exchange: String,
    val quantity: Double,
    val price: Double,
    val value: Double,
    val logoUrl: String? = null
) 