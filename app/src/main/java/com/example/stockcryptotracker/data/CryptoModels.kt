package com.example.stockcryptotracker.data

import com.example.stockcryptotracker.network.CryptoDetailResponse

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
    val priceChangePercentage30d: Double = 0.0,
    val marketCapRank: Int = 0
) {
    // Add companion object with a factory method to convert from CryptoDetailResponse
    companion object {
        fun fromCryptoDetailResponse(detail: CryptoDetailResponse): CryptoCurrency {
            val currentPrice = detail.market_data.current_price["usd"] ?: 0.0
            val marketCap = detail.market_data.market_cap["usd"] ?: 0L
            
            return CryptoCurrency(
                id = detail.id,
                symbol = detail.symbol,
                name = detail.name,
                image = detail.image.large,
                currentPrice = currentPrice,
                priceChangePercentage24h = detail.market_data.price_change_percentage_24h,
                marketCap = marketCap,
                totalVolume = detail.market_data.total_volume["usd"] ?: 0.0,
                high24h = detail.market_data.high_24h["usd"] ?: 0.0,
                low24h = detail.market_data.low_24h["usd"] ?: 0.0,
                priceChangePercentage7d = detail.market_data.price_change_percentage_7d,
                priceChangePercentage30d = detail.market_data.price_change_percentage_30d,
                marketCapRank = detail.market_cap_rank?.toInt() ?: 0
            )
        }
    }
}

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