package com.example.stockcryptotracker.data

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val exchange: String,
    val marketCap: Double?,
    val volume: Long?,
    val logoUrl: String? = null
)

// Klasa do mapowania odpowiedzi z API na model Stock
data class StockApiResponse(
    val symbol: String,
    val companyName: String,
    val latestPrice: Double,
    val change: Double,
    val changePercent: Double,
    val primaryExchange: String,
    val marketCap: Double?,
    val volume: Long?
)

fun StockApiResponse.toStock(): Stock {
    return Stock(
        symbol = symbol,
        name = companyName,
        price = latestPrice,
        change = change,
        changePercent = changePercent,
        exchange = primaryExchange,
        marketCap = marketCap,
        volume = volume,
        logoUrl = "https://financialmodelingprep.com/image-stock/$symbol.png" // przykładowy URL dla logo
    )
} 