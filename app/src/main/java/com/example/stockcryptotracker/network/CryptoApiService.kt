package com.example.stockcryptotracker.network

import com.example.stockcryptotracker.data.CryptoResponse
import com.example.stockcryptotracker.data.PriceHistoryResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CryptoApiService {
    @GET("api/v3/coins/markets")
    suspend fun getCryptocurrencies(
        @Query("vs_currency") currency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("price_change_percentage") priceChangePercentage: String = "7d,30d"
    ): List<CryptoResponse>
    
    @GET("api/v3/coins/{id}")
    suspend fun getCryptoDetail(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = false,
        @Query("market_data") marketData: Boolean = true,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developerData: Boolean = false
    ): CryptoDetailResponse
    
    @GET("api/v3/coins/{id}/market_chart")
    suspend fun getPriceHistory(
        @Path("id") id: String,
        @Query("vs_currency") currency: String = "usd",
        @Query("days") days: String = "7",
        @Query("interval") interval: String? = null
    ): PriceHistoryResponse
}

data class CryptoDetailResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val description: Map<String, String>,
    val image: ImageData,
    val market_data: MarketData
)

data class ImageData(
    val thumb: String,
    val small: String,
    val large: String
)

data class MarketData(
    val current_price: Map<String, Double>,
    val market_cap: Map<String, Long>,
    val total_volume: Map<String, Double>,
    val high_24h: Map<String, Double>,
    val low_24h: Map<String, Double>,
    val price_change_percentage_24h: Double,
    val price_change_percentage_7d: Double,
    val price_change_percentage_30d: Double
)

object RetrofitClient {
    private const val BASE_URL = "https://api.coingecko.com/"

    val cryptoApiService: CryptoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CryptoApiService::class.java)
    }
} 