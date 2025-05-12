package com.example.stockcryptotracker.data

data class CryptoCurrency(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val currentPrice: Double,
    val priceChangePercentage24h: Double,
    val marketCap: Long = 0,
    val totalVolume: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val priceChangePercentage7d: Double = 0.0,
    val priceChangePercentage30d: Double = 0.0
)

data class CryptoResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val current_price: Double,
    val price_change_percentage_24h: Double,
    val market_cap: Long = 0,
    val total_volume: Double = 0.0,
    val high_24h: Double = 0.0,
    val low_24h: Double = 0.0,
    val price_change_percentage_7d_in_currency: Double? = null,
    val price_change_percentage_30d_in_currency: Double? = null
)

fun CryptoResponse.toCryptoCurrency(): CryptoCurrency {
    return CryptoCurrency(
        id = id,
        symbol = symbol,
        name = name,
        image = image,
        currentPrice = current_price,
        priceChangePercentage24h = price_change_percentage_24h,
        marketCap = market_cap,
        totalVolume = total_volume,
        high24h = high_24h,
        low24h = low_24h,
        priceChangePercentage7d = price_change_percentage_7d_in_currency ?: 0.0,
        priceChangePercentage30d = price_change_percentage_30d_in_currency ?: 0.0
    )
}

data class PriceHistoryResponse(
    val prices: List<List<Double>>
)

data class PricePoint(
    val timestamp: Long,
    val price: Double
)

fun PriceHistoryResponse.toPricePoints(): List<PricePoint> {
    return prices.map { point ->
        PricePoint(
            timestamp = point[0].toLong(),
            price = point[1]
        )
    }
} 