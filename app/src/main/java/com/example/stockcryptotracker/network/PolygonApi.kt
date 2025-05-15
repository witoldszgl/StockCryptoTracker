package com.example.stockcryptotracker.network

import android.util.Log
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.data.StockApiResponse
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Interfejs API dla Polygon.io
interface PolygonApiService {
    // Pobieranie szczegółów o pojedyńczej akcji
    @GET("v2/aggs/ticker/{ticker}/prev")
    suspend fun getStockQuote(
        @Path("ticker") ticker: String,
        @Query("apiKey") apiKey: String
    ): Response<PolygonStockQuoteResponse>

    // Wyszukiwanie tickerów
    @GET("v3/reference/tickers")
    suspend fun searchStocks(
        @Query("search") query: String,
        @Query("active") active: Boolean = true,
        @Query("market") market: String = "stocks",
        @Query("limit") limit: Int = 10,
        @Query("apiKey") apiKey: String
    ): Response<PolygonTickerSearchResponse>

    // Pobieranie historycznych danych o cenach
    @GET("v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}")
    suspend fun getPriceHistory(
        @Path("ticker") ticker: String,
        @Path("multiplier") multiplier: Int = 1,
        @Path("timespan") timespan: String = "day",
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("limit") limit: Int = 120,
        @Query("apiKey") apiKey: String
    ): Response<PolygonAggregatesResponse>

    // Pobieranie danych intraday
    @GET("v2/aggs/ticker/{ticker}/range/30/minute/{from}/{to}")
    suspend fun getIntradayData(
        @Path("ticker") ticker: String,
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("limit") limit: Int = 50,
        @Query("apiKey") apiKey: String
    ): Response<PolygonAggregatesResponse>
}

// Klasy odpowiedzi dla Polygon API
data class PolygonStockQuoteResponse(
    val status: String,
    val ticker: String?,
    val results: List<PolygonResultQuote>?
)

data class PolygonResultQuote(
    val T: String,  // Ticker
    val c: Double,  // Close price
    val h: Double,  // High price
    val l: Double,  // Low price
    val o: Double,  // Open price
    val v: Long     // Volume
)

data class PolygonTickerSearchResponse(
    val status: String,
    val count: Int,
    val results: List<PolygonTicker>?
)

data class PolygonTicker(
    val ticker: String,
    val name: String,
    val market: String,
    val locale: String,
    val primary_exchange: String?,
    val type: String,
    val active: Boolean,
    val currency_name: String?
)

data class PolygonAggregatesResponse(
    val status: String,
    val ticker: String?,
    val queryCount: Int?,
    val resultsCount: Int?,
    val adjusted: Boolean?,
    val results: List<PolygonAggregateBar>?
)

data class PolygonAggregateBar(
    val v: Long,    // Volume
    val vw: Double, // Volume weighted price
    val o: Double,  // Open price
    val c: Double,  // Close price
    val h: Double,  // High price
    val l: Double,  // Low price
    val t: Long,    // Timestamp
    val n: Int?     // Number of transactions
)

// Klasa reprezentująca wynik wyszukiwania akcji
data class StockSearchResult(
    val symbol: String,
    val securityName: String,
    val securityType: String,
    val exchange: String
)

// Główna klasa API
object PolygonApi {
    private const val BASE_URL = "https://api.polygon.io/"
    private const val API_KEY = "hsAEjYe0UgbPIpBXNKAL_MEYukJglBZn"
    private const val TAG = "PolygonApi"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: PolygonApiService = retrofit.create(PolygonApiService::class.java)
    
    // Pobieranie szczegółów akcji
    suspend fun getStockQuote(symbol: String): Response<StockApiResponse> {
        return try {
            Log.d(TAG, "Fetching stock quote for $symbol")
            val response = service.getStockQuote(ticker = symbol, apiKey = API_KEY)
            
            if (response.isSuccessful) {
                val polygonResponse = response.body()
                if (polygonResponse != null && polygonResponse.results?.isNotEmpty() == true) {
                    val result = polygonResponse.results[0]
                    
                    val stockResponse = StockApiResponse(
                        symbol = symbol,
                        companyName = getCompanyNameForSymbol(symbol),
                        latestPrice = result.c,
                        change = 0.0,  // Brak danych o zmianie
                        changePercent = 0.0,  // Brak danych o zmianie procentowej
                        primaryExchange = "NASDAQ",
                        marketCap = null,
                        volume = result.v
                    )
                    
                    Response.success(stockResponse)
                } else {
                    Log.e(TAG, "Empty or null results for $symbol")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.errorBody()?.string()}")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching stock quote: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Wyszukiwanie akcji
    suspend fun searchStocks(query: String): Response<List<StockSearchResult>> {
        return try {
            Log.d(TAG, "Searching stocks with query: $query")
            val response = service.searchStocks(query = query, apiKey = API_KEY)
            
            if (response.isSuccessful) {
                val searchResponse = response.body()
                if (searchResponse != null && searchResponse.results != null) {
                    val results = searchResponse.results.map { ticker ->
                        StockSearchResult(
                            symbol = ticker.ticker,
                            securityName = ticker.name,
                            securityType = ticker.type,
                            exchange = ticker.primary_exchange ?: "N/A"
                        )
                    }
                    Response.success(results)
                } else {
                    Response.success(emptyList())
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.errorBody()?.string()}")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when searching stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Lista popularnych symboli akcji
    private val POPULAR_SYMBOLS = listOf(
        "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", 
        "META", "NVDA", "JPM", "JNJ", "V", 
        "PG", "MA", "UNH", "HD", "BAC",
        "DIS", "PYPL", "ADBE", "CMCSA", "XOM"
    )
    
    // Pobieranie listy najpopularniejszych akcji
    suspend fun getTopStocks(): Response<List<StockApiResponse>> {
        return try {
            Log.d(TAG, "Fetching top stocks using individual API calls")
            
            val stocksList = mutableListOf<StockApiResponse>()
            val failedSymbols = mutableListOf<String>()
            
            // Zwiększamy do 15 symboli
            val limitedSymbols = POPULAR_SYMBOLS.take(15)
            
            Log.d(TAG, "Attempting to fetch data for: ${limitedSymbols.joinToString()}")
            
            // Podziel symbole na grupy po 3, aby przyspieszyć ładowanie
            val symbolGroups = limitedSymbols.chunked(3)
            
            for (group in symbolGroups) {
                Log.d(TAG, "Processing group: ${group.joinToString()}")
                
                // Utwórz listę zadań do kolejnego wykonania
                val responses = group.map { symbol ->
                    try {
                        Log.d(TAG, "Requesting data for: $symbol")
                        val response = getStockQuote(symbol)
                        
                        if (response.isSuccessful && response.body() != null) {
                            Log.d(TAG, "Successfully fetched data for: $symbol")
                            Pair(symbol, response.body()!!)
                        } else {
                            Log.e(TAG, "Failed to fetch data for $symbol: ${response.code()} - ${response.message()}")
                            failedSymbols.add(symbol)
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception fetching data for $symbol: ${e.message}")
                        failedSymbols.add(symbol)
                        null
                    }
                }
                
                // Dodaj udane odpowiedzi do głównej listy
                responses.filterNotNull().forEach { (_, stock) ->
                    stocksList.add(stock)
                }
                
                // Poczekaj przed pobraniem następnej grupy
                if (group != symbolGroups.last()) {
                    Log.d(TAG, "Waiting 1000ms before next group")
                    Thread.sleep(1000) // 1000ms między grupami
                }
            }
            
            Log.d(TAG, "Successfully fetched ${stocksList.size} stock quotes")
            if (failedSymbols.isNotEmpty()) {
                Log.d(TAG, "Failed to fetch data for: ${failedSymbols.joinToString()}")
            }
            
            if (stocksList.isEmpty()) {
                Log.e(TAG, "Could not fetch any stock data")
                Response.error(500, "Could not fetch any stock data".toResponseBody("text/plain".toMediaTypeOrNull()))
            } else {
                Response.success(stocksList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching top stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie historii cen
    suspend fun getStockPriceHistory(symbol: String, days: Int = 30): Response<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching price history for $symbol, days: $days")
            
            val calendar = Calendar.getInstance()
            val endDate = dateToFormattedString(calendar.time)
            
            // Ustaw datę początkową
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateToFormattedString(calendar.time)
            
            val response = service.getPriceHistory(
                ticker = symbol,
                from = startDate,
                to = endDate,
                timespan = "day",
                apiKey = API_KEY
            )
            
            if (response.isSuccessful) {
                val historyResponse = response.body()
                if (historyResponse != null && historyResponse.results != null) {
                    val pricePoints = historyResponse.results.map { bar ->
                        PricePoint(
                            timestamp = bar.t,
                            price = bar.c
                        )
                    }
                    Response.success(pricePoints)
                } else {
                    Log.e(TAG, "Empty or null results in price history response")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.errorBody()?.string()}")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching price history: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie danych intraday (ostatnie 24 godziny)
    suspend fun getStockIntraday24h(symbol: String): Response<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching intraday data for $symbol")
            
            val calendar = Calendar.getInstance()
            val endDate = dateToFormattedString(calendar.time)
            
            // Ustaw datę początkową na 24 godziny wstecz
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val startDate = dateToFormattedString(calendar.time)
            
            val response = service.getIntradayData(
                ticker = symbol,
                from = startDate,
                to = endDate,
                apiKey = API_KEY
            )
            
            if (response.isSuccessful) {
                val intradayResponse = response.body()
                if (intradayResponse != null && intradayResponse.results != null) {
                    val pricePoints = intradayResponse.results.map { bar ->
                        PricePoint(
                            timestamp = bar.t,
                            price = bar.c
                        )
                    }
                    Response.success(pricePoints)
                } else {
                    Log.e(TAG, "Empty or null results in intraday response")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.errorBody()?.string()}")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching intraday data: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie wielu akcji jednocześnie
    suspend fun getMultipleStocks(symbols: List<String>): Response<List<StockApiResponse>> {
        return try {
            Log.d(TAG, "Fetching multiple stocks: ${symbols.joinToString(", ")}")
            
            val stocksList = mutableListOf<StockApiResponse>()
            
            // Podziel symbole na grupy, aby uniknąć zbyt wielu równoczesnych żądań
            val symbolGroups = symbols.chunked(3)
            
            for (group in symbolGroups) {
                Log.d(TAG, "Processing group: ${group.joinToString()}")
                
                val responses = group.map { symbol ->
                    try {
                        val response = getStockQuote(symbol)
                        if (response.isSuccessful && response.body() != null) {
                            Pair(symbol, response.body()!!)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception fetching data for $symbol: ${e.message}")
                        null
                    }
                }
                
                responses.filterNotNull().forEach { (_, stock) ->
                    stocksList.add(stock)
                }
                
                // Poczekaj przed pobraniem następnej grupy
                if (group != symbolGroups.last()) {
                    Thread.sleep(800) // 800ms między grupami
                }
            }
            
            if (stocksList.isEmpty()) {
                Log.e(TAG, "Could not fetch any stock data")
                Response.error(500, "Could not fetch any stock data".toResponseBody("text/plain".toMediaTypeOrNull()))
            } else {
                Response.success(stocksList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching multiple stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Metody pomocnicze
    
    // Funkcja do konwersji Date na string w formacie YYYY-MM-DD
    private fun dateToFormattedString(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(date)
    }
    
    // Funkcja do pobierania nazw firm na podstawie symbolu (dla popularnych akcji)
    private fun getCompanyNameForSymbol(symbol: String): String {
        return when (symbol.uppercase()) {
            "AAPL" -> "Apple Inc."
            "MSFT" -> "Microsoft Corporation"
            "GOOGL" -> "Alphabet Inc. (Google)"
            "GOOG" -> "Alphabet Inc. (Google)"
            "AMZN" -> "Amazon.com Inc."
            "TSLA" -> "Tesla, Inc."
            "META" -> "Meta Platforms, Inc."
            "NVDA" -> "NVIDIA Corporation"
            "JPM" -> "JPMorgan Chase & Co."
            "JNJ" -> "Johnson & Johnson"
            "V" -> "Visa Inc."
            "PG" -> "Procter & Gamble Co."
            "MA" -> "Mastercard Incorporated"
            "UNH" -> "UnitedHealth Group Inc."
            "HD" -> "Home Depot Inc."
            "BAC" -> "Bank of America Corp."
            "DIS" -> "Walt Disney Co."
            "PYPL" -> "PayPal Holdings Inc."
            "ADBE" -> "Adobe Inc."
            "CMCSA" -> "Comcast Corporation"
            "XOM" -> "Exxon Mobil Corporation"
            "PFE" -> "Pfizer Inc."
            "NFLX" -> "Netflix Inc."
            "VZ" -> "Verizon Communications Inc."
            "CSCO" -> "Cisco Systems Inc."
            "ABT" -> "Abbott Laboratories"
            "PEP" -> "PepsiCo Inc."
            "KO" -> "Coca-Cola Company"
            "CRM" -> "Salesforce.com Inc."
            "WMT" -> "Walmart Inc."
            "TMO" -> "Thermo Fisher Scientific Inc."
            else -> "$symbol"
        }
    }
    
    // Metody proxy do kompatybilności z resztą aplikacji
    suspend fun getMostActiveStocks(): Response<List<StockApiResponse>> = getTopStocks()
    suspend fun getTopGainers(): Response<List<StockApiResponse>> = getTopStocks()
    suspend fun getTopLosers(): Response<List<StockApiResponse>> = getTopStocks()
    suspend fun getAllStocks(): Response<List<StockApiResponse>> = getTopStocks()
} 