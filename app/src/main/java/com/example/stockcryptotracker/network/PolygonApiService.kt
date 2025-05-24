package com.example.stockcryptotracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PolygonApiService {
    
    companion object {
        const val API_KEY = "DziVq92Q03nfqMPaeppx1Vx22LkxAZcR" // Updated API key
    }
    
    // 1. Lista i wyszukiwanie tickerów
    @GET("v3/reference/tickers")
    suspend fun getTickersList(
        @Query("market") market: String = "stocks",
        @Query("active") active: Boolean = true,
        @Query("ticker") ticker: String? = null,
        @Query("sort") sort: String = "ticker",
        @Query("order") order: String = "asc",
        @Query("limit") limit: Int = 100,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonTickersListResponse
    
    // 2. Szczegóły spółki
    @GET("v3/reference/tickers/{symbol}")
    suspend fun getStockDetails(
        @Path("symbol") symbol: String,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonTickerResponse
    
    // 3. Ostatnia transakcja
    @GET("v2/last/trade/{stocksTicker}")
    suspend fun getLastTrade(
        @Path("stocksTicker") symbol: String,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonLastTradeResponse
    
    // 4. Ostatnia oferta (bid/ask)
    @GET("v1/last_quote/stocks/{ticker}")
    suspend fun getLastQuote(
        @Path("ticker") symbol: String,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonLastQuoteResponse
    
    // 5. Agregaty cenowe (OHLC) dla wykresów
    @GET("v2/aggs/ticker/{symbol}/range/{multiplier}/{timespan}/{from}/{to}")
    suspend fun getStockAggregates(
        @Path("symbol") symbol: String,
        @Path("multiplier") multiplier: Int,
        @Path("timespan") timespan: String, // minute, hour, day, week, month, quarter, year
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("adjusted") adjusted: Boolean = true,
        @Query("limit") limit: Int = 5000,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonAggregatesResponse
    
    // 6. Otwarcie i zamknięcie sesji
    @GET("v1/open-close/{ticker}/{date}")
    suspend fun getDailyOpenClose(
        @Path("ticker") symbol: String,
        @Path("date") date: String, // YYYY-MM-DD
        @Query("adjusted") adjusted: Boolean = true,
        @Query("apiKey") apiKey: String = API_KEY
    ): PolygonDailyOpenCloseResponse
}

// Klasy odpowiedzi dla poszczególnych endpointów

// 1. Lista tickerów
data class PolygonTickersListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: List<PolygonTickerItem>,
    @SerializedName("count") val count: Int,
    @SerializedName("next_url") val nextUrl: String?
)

data class PolygonTickerItem(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("name") val name: String,
    @SerializedName("market") val market: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("primary_exchange") val primaryExchange: String?,
    @SerializedName("type") val type: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("currency_name") val currencyName: String,
    @SerializedName("last_updated_utc") val lastUpdatedUtc: String?
)

// 2. Szczegóły spółki
data class PolygonTickerResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: PolygonTickerDetails
)

data class PolygonTickerDetails(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("name") val name: String,
    @SerializedName("market_cap") val market_cap: Double? = null,
    @SerializedName("primary_exchange") val primary_exchange: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("list_date") val listDate: String? = null,
    @SerializedName("total_employees") val totalEmployees: Int? = null,
    @SerializedName("branding") val branding: PolygonBranding? = null,
    @SerializedName("weighted_shares_outstanding") val weightedSharesOutstanding: Long? = null
)

data class PolygonBranding(
    @SerializedName("logo_url") val logo_url: String? = null,
    @SerializedName("icon_url") val icon_url: String? = null
)

// 3. Ostatnia transakcja
data class PolygonLastTradeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: PolygonLastTrade
)

data class PolygonLastTrade(
    @SerializedName("T") val symbol: String,
    @SerializedName("t") val timestamp: Long,
    @SerializedName("p") val price: Double,
    @SerializedName("s") val size: Int,
    @SerializedName("c") val conditions: List<Int>,
    @SerializedName("x") val exchange: Int
)

// 4. Ostatnia oferta (bid/ask)
data class PolygonLastQuoteResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: PolygonLastQuote
)

data class PolygonLastQuote(
    @SerializedName("T") val symbol: String,
    @SerializedName("t") val timestamp: Long,
    @SerializedName("p") val bidPrice: Double,
    @SerializedName("s") val bidSize: Int,
    @SerializedName("P") val askPrice: Double,
    @SerializedName("S") val askSize: Int
)

// 5. Agregaty cenowe
data class PolygonAggregatesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: List<PolygonAggregate>? = null,
    @SerializedName("ticker") val ticker: String,
    @SerializedName("queryCount") val queryCount: Int,
    @SerializedName("resultsCount") val resultsCount: Int,
    @SerializedName("adjusted") val adjusted: Boolean
)

data class PolygonAggregate(
    @SerializedName("t") val t: Long, // timestamp
    @SerializedName("o") val o: Double, // open price
    @SerializedName("h") val h: Double, // high price
    @SerializedName("l") val l: Double, // low price
    @SerializedName("c") val c: Double, // close price
    @SerializedName("v") val v: Long, // volume
    @SerializedName("vw") val vw: Double? = null, // volume weighted average price
    @SerializedName("n") val n: Int? = null // number of transactions
)

// 6. Otwarcie i zamknięcie sesji
data class PolygonDailyOpenCloseResponse(
    @SerializedName("status") val status: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("from") val from: String,
    @SerializedName("open") val open: Double,
    @SerializedName("high") val high: Double,
    @SerializedName("low") val low: Double,
    @SerializedName("close") val close: Double,
    @SerializedName("volume") val volume: Long,
    @SerializedName("afterHours") val afterHours: Double?,
    @SerializedName("preMarket") val preMarket: Double?
) 