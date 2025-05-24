package com.example.stockcryptotracker.data

data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val priceChangePercentage24h: Double,
    val marketCap: Double,
    val image: String = "",
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val totalVolume: Double = 0.0,
    val exchange: String = "",
    val price: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0,
    val logoUrl: String = "",
    val change: Double = 0.0,
    val description: String? = null,
    val employees: Int? = null,
    val listDate: String? = null,
    val bidPrice: Double = 0.0,
    val askPrice: Double = 0.0
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
        currentPrice = latestPrice,
        priceChangePercentage24h = changePercent,
        marketCap = marketCap ?: 0.0,
        image = "https://financialmodelingprep.com/image-stock/$symbol.png", // przykładowy URL for logo
        high24h = 0.0,
        low24h = 0.0,
        totalVolume = volume?.toDouble() ?: 0.0,
        exchange = primaryExchange,
        price = latestPrice,
        changePercent = changePercent,
        volume = volume ?: 0,
        logoUrl = "https://financialmodelingprep.com/image-stock/$symbol.png", // Copy of image field
        change = change
    )
} 