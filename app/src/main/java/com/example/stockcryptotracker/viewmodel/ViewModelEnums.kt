package com.example.stockcryptotracker.viewmodel

/**
 * Common enum classes for ViewModels
 */

/**
 * Tab enum for navigation between All, Crypto, Stocks and Favorites screens
 */
enum class Tab {
    ALL, CRYPTO, STOCKS, FAVORITES
}

/**
 * CryptoCategory enum for filtering cryptocurrency lists
 */
enum class CryptoCategory {
    ALL, TOP_GAINERS, TOP_LOSERS, MOST_ACTIVE
}

/**
 * StockCategory enum for filtering stock lists
 */
enum class StockCategory {
    ALL, MOST_ACTIVE, TOP_GAINERS, TOP_LOSERS
} 