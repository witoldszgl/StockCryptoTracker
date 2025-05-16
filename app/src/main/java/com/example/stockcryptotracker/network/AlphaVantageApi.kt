package com.example.stockcryptotracker.network

import android.util.Log
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.StockApiResponse
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Simple rate limiter for Alpha Vantage API
 * Alpha Vantage limits to 5 requests per minute for free API keys
 */
class ApiRateLimiter(private val requestsPerMinute: Int = 5) {
    private val mutex = Mutex()
    private val requestTimestamps = mutableListOf<Long>()
    
    suspend fun acquirePermit(): Boolean {
        return mutex.withLock {
            val currentTime = System.currentTimeMillis()
            // Remove timestamps older than 1 minute
            val oneMinuteAgo = currentTime - 60000
            requestTimestamps.removeAll { it < oneMinuteAgo }
            
            // Check if we can make another request
            if (requestTimestamps.size < requestsPerMinute) {
                requestTimestamps.add(currentTime)
                true
            } else {
                // Return false if we're rate limited
                false
            }
        }
    }
    
    suspend fun waitForPermit(timeout: Long = 10000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            if (acquirePermit()) return true
            kotlinx.coroutines.delay(1000) // Wait a second before trying again
        }
        return false
    }
}

// Interfejs API dla Alpha Vantage
interface AlphaVantageApiService {
    // Pobieranie informacji o akcji
    @GET("query")
    suspend fun getStockQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageQuoteResponse>

    // Wyszukiwanie tickerów
    @GET("query")
    suspend fun searchStocks(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageSearchResponse>

    // Pobieranie danych historycznych
    @GET("query")
    suspend fun getTimeSeriesDaily(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String, 
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageTimeSeriesResponse>

    // Pobieranie danych intraday
    @GET("query")
    suspend fun getTimeSeriesIntraday(
        @Query("function") function: String = "TIME_SERIES_INTRADAY",
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "60min",
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageTimeSeriesResponse>
    
    // Pobieranie listy top spółek z S&P 500
    @GET("query")
    suspend fun getSP500List(
        @Query("function") function: String = "LISTING_STATUS",
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageListingResponse>
}

// Klasy odpowiedzi dla Alpha Vantage API
data class AlphaVantageQuoteResponse(
    @SerializedName("Global Quote")
    val globalQuote: GlobalQuote?
)

data class GlobalQuote(
    @SerializedName("01. symbol")
    val symbol: String,
    @SerializedName("02. open")
    val open: String,
    @SerializedName("03. high")
    val high: String,
    @SerializedName("04. low")
    val low: String,
    @SerializedName("05. price")
    val price: String,
    @SerializedName("06. volume")
    val volume: String,
    @SerializedName("07. latest trading day")
    val latestTradingDay: String,
    @SerializedName("08. previous close")
    val previousClose: String,
    @SerializedName("09. change")
    val change: String,
    @SerializedName("10. change percent")
    val changePercent: String
)

data class AlphaVantageSearchResponse(
    @SerializedName("bestMatches")
    val bestMatches: List<StockSearchMatch>?
)

data class StockSearchMatch(
    @SerializedName("1. symbol")
    val symbol: String,
    @SerializedName("2. name")
    val name: String,
    @SerializedName("3. type")
    val type: String,
    @SerializedName("4. region")
    val region: String,
    @SerializedName("5. marketOpen")
    val marketOpen: String,
    @SerializedName("6. marketClose")
    val marketClose: String,
    @SerializedName("7. timezone")
    val timezone: String,
    @SerializedName("8. currency")
    val currency: String,
    @SerializedName("9. matchScore")
    val matchScore: String
)

data class AlphaVantageTimeSeriesResponse(
    @SerializedName("Meta Data")
    val metaData: MetaData?,
    @SerializedName("Time Series (Daily)")
    val timeSeriesDaily: Map<String, DailyData>?,
    @SerializedName("Time Series (60min)")
    val timeSeriesIntraday: Map<String, DailyData>?
)

data class MetaData(
    @SerializedName("1. Information")
    val information: String,
    @SerializedName("2. Symbol")
    val symbol: String,
    @SerializedName("3. Last Refreshed")
    val lastRefreshed: String
)

data class DailyData(
    @SerializedName("1. open")
    val open: String,
    @SerializedName("2. high")
    val high: String,
    @SerializedName("3. low")
    val low: String,
    @SerializedName("4. close")
    val close: String,
    @SerializedName("5. volume")
    val volume: String
)

data class AlphaVantageListingResponse(
    val data: List<List<String>>?
)

// Klasa reprezentująca wynik wyszukiwania akcji
data class StockSearchResult(
    val symbol: String,
    val securityName: String,
    val securityType: String,
    val exchange: String
)

// Główna klasa API
object AlphaVantageApi {
    private const val BASE_URL = "https://www.alphavantage.co/"
    private const val API_KEY = "M6O5FPVVYBL3AMJY"
    private const val TAG = "AlphaVantageApi"
    
    // Add cache for stock data
    private val stockCache = mutableMapOf<String, Pair<StockApiResponse, Long>>()
    private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes cache duration
    
    // Create rate limiter
    private val rateLimiter = ApiRateLimiter(5) // 5 requests per minute
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS) // Decreased from 30 to 5 seconds
        .readTimeout(5, TimeUnit.SECONDS)    // Decreased from 30 to 5 seconds
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: AlphaVantageApiService = retrofit.create(AlphaVantageApiService::class.java)
    
    // Lista najpopularniejszych akcji z S&P 500
    private val popularStocks = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA", "JPM", "JNJ", "V", "PG", "UNH", "HD", "BAC", "MA", "INTC", "VZ", "ADBE", "CRM", "CMCSA")
    
    // Pobieranie szczegółów pojedynczej akcji
    suspend fun getStockQuote(symbol: String): Response<StockApiResponse> {
        return try {
            Log.d(TAG, "Fetching stock quote for $symbol")
            
            // Check cache first
            val cachedData = stockCache[symbol]
            if (cachedData != null) {
                val (cachedResponse, timestamp) = cachedData
                if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                    Log.d(TAG, "Returning cached data for $symbol")
                    return Response.success(cachedResponse)
                }
            }
            
            // Try to acquire a permit with short timeout
            val hasPermit = rateLimiter.waitForPermit(3000)
            if (!hasPermit) {
                Log.w(TAG, "Rate limited for $symbol")
                return Response.error(429, "Rate limit exceeded".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
            
            // Add timeout to API call
            val response = kotlinx.coroutines.withTimeoutOrNull(5000) {
                service.getStockQuote(symbol = symbol, apiKey = API_KEY)
            }
            
            if (response != null && response.isSuccessful) {
                val quoteResponse = response.body()
                if (quoteResponse != null && quoteResponse.globalQuote != null) {
                    val quote = quoteResponse.globalQuote
                    val changePercent = quote.changePercent.replace("%", "").trim().toDoubleOrNull() ?: 0.0
                    
                    val stockResponse = StockApiResponse(
                        symbol = quote.symbol,
                        companyName = getCompanyNameForSymbol(quote.symbol),
                        latestPrice = quote.price.toDoubleOrNull() ?: 0.0,
                        change = quote.change.toDoubleOrNull() ?: 0.0,
                        changePercent = changePercent / 100.0,
                        primaryExchange = "NYSE/NASDAQ",
                        marketCap = null,
                        volume = quote.volume.toLongOrNull()
                    )
                    
                    // Cache the response
                    stockCache[symbol] = Pair(stockResponse, System.currentTimeMillis())
                    
                    Response.success(stockResponse)
                } else {
                    Log.e(TAG, "API error: Quote data is null for $symbol")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response?.code() ?: "Timeout"} for $symbol")
                Response.error(response?.code() ?: 408, 
                    (response?.errorBody() ?: "Request timeout".toResponseBody("text/plain".toMediaTypeOrNull())))
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
            val response = service.searchStocks(keywords = query, apiKey = API_KEY)
            
            if (response.isSuccessful) {
                val searchResponse = response.body()
                if (searchResponse != null && searchResponse.bestMatches != null) {
                    val results = searchResponse.bestMatches.map { match ->
                        StockSearchResult(
                            symbol = match.symbol,
                            securityName = match.name,
                            securityType = match.type,
                            exchange = match.region
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
    
    // Pobieranie najpopularniejszych akcji (top 20 z S&P 500)
    suspend fun getMostActiveStocks(): Response<List<StockApiResponse>> {
        return try {
            Log.d(TAG, "Fetching most active stocks")
            val result = mutableListOf<StockApiResponse>()
            
            // Pobieramy dane dla najpopularniejszych akcji z naszej predefiniowanej listy
            for (symbol in popularStocks.take(10)) {
                try {
                    val response = getStockQuote(symbol)
                    if (response.isSuccessful && response.body() != null) {
                        result.add(response.body()!!)
                    }
                    // Dodajemy opóźnienie, aby nie przekroczyć limitu zapytań API
                    kotlinx.coroutines.delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching data for $symbol: ${e.message}")
                    // Kontynuujemy z następną akcją
                }
            }
            
            if (result.isNotEmpty()) {
                Response.success(result)
            } else {
                Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching most active stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie najlepszych akcji (będzie używane zarówno dla zyskujących jak i tracących)
    suspend fun getTopStocks(): Response<List<StockApiResponse>> {
        return getMostActiveStocks() // Używamy tej samej metody, sortowanie odbywa się w repozytorium
    }
    
    // Pobieranie wszystkich akcji
    suspend fun getAllStocks(): Response<List<StockApiResponse>> {
        return try {
            Log.d(TAG, "Fetching all stocks from API")
            val result = mutableListOf<StockApiResponse>()
            
            // We'll try to get more stocks, up to 10
            for (symbol in popularStocks.take(10)) {
                try {
                    // Check rate limiter
                    if (!rateLimiter.acquirePermit()) {
                        Log.w(TAG, "Rate limited, skipping API call for $symbol")
                        continue
                    }
                    
                    Log.d(TAG, "Fetching data for $symbol")
                    val response = service.getStockQuote(symbol = symbol, apiKey = API_KEY)
                    
                    if (response.isSuccessful) {
                        val quoteResponse = response.body()
                        if (quoteResponse != null && quoteResponse.globalQuote != null) {
                            val quote = quoteResponse.globalQuote
                            val changePercent = quote.changePercent.replace("%", "").trim().toDoubleOrNull() ?: 0.0
                            
                            val stockResponse = StockApiResponse(
                                symbol = quote.symbol,
                                companyName = getCompanyNameForSymbol(quote.symbol),
                                latestPrice = quote.price.toDoubleOrNull() ?: 0.0,
                                change = quote.change.toDoubleOrNull() ?: 0.0,
                                changePercent = changePercent / 100.0,
                                primaryExchange = "NYSE/NASDAQ",
                                marketCap = null,
                                volume = quote.volume.toLongOrNull()
                            )
                            result.add(stockResponse)
                            Log.d(TAG, "Successfully added $symbol data")
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch $symbol: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                    
                    // Shorter delay between API calls
                    kotlinx.coroutines.delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception for $symbol: ${e.message}", e)
                }
            }
            
            // If we have data, return it
            if (result.isNotEmpty()) {
                Log.d(TAG, "Returning ${result.size} stocks from API")
                Response.success(result)
            } else {
                // Return error if no data could be retrieved
                Log.e(TAG, "No data could be retrieved from API")
                Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching all stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie wielu akcji jednocześnie
    suspend fun getMultipleStocks(symbols: List<String>): Response<List<StockApiResponse>> {
        return try {
            Log.d(TAG, "Fetching multiple stocks: ${symbols.joinToString(", ")}")
            val result = mutableListOf<StockApiResponse>()
            val symbolsToFetch = mutableListOf<String>()
            
            // Check cache first for each symbol
            for (symbol in symbols) {
                val cachedData = stockCache[symbol]
                if (cachedData != null) {
                    val (cachedResponse, timestamp) = cachedData
                    if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                        Log.d(TAG, "Using cached data for $symbol")
                        result.add(cachedResponse)
                        continue
                    }
                }
                symbolsToFetch.add(symbol)
            }
            
            // Fetch only the symbols that aren't in cache or are expired
            for (symbol in symbolsToFetch) {
                try {
                    // Check rate limiter
                    if (!rateLimiter.acquirePermit()) {
                        Log.w(TAG, "Rate limited, skipping API call for $symbol")
                        continue
                    }
                    
                    Log.d(TAG, "Fetching data for $symbol")
                    val response = service.getStockQuote(symbol = symbol, apiKey = API_KEY)
                    
                    if (response.isSuccessful) {
                        val quoteResponse = response.body()
                        if (quoteResponse != null && quoteResponse.globalQuote != null) {
                            val quote = quoteResponse.globalQuote
                            val changePercent = quote.changePercent.replace("%", "").trim().toDoubleOrNull() ?: 0.0
                            
                            val stockResponse = StockApiResponse(
                                symbol = quote.symbol,
                                companyName = getCompanyNameForSymbol(quote.symbol),
                                latestPrice = quote.price.toDoubleOrNull() ?: 0.0,
                                change = quote.change.toDoubleOrNull() ?: 0.0,
                                changePercent = changePercent / 100.0,
                                primaryExchange = "NYSE/NASDAQ",
                                marketCap = null,
                                volume = quote.volume.toLongOrNull()
                            )
                            
                            // Cache the response
                            stockCache[symbol] = Pair(stockResponse, System.currentTimeMillis())
                            
                            result.add(stockResponse)
                            Log.d(TAG, "Successfully added $symbol data")
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch $symbol: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                    
                    // Use shorter delay
                    kotlinx.coroutines.delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception for $symbol: ${e.message}", e)
                }
            }
            
            if (result.isNotEmpty()) {
                Log.d(TAG, "Returning ${result.size} stocks from API")
                Response.success(result)
            } else {
                Log.e(TAG, "No results from API fetching multiple stocks")
                Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching multiple stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie historii cen
    suspend fun getStockPriceHistory(symbol: String, days: Int): Response<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching price history for $symbol, days: $days")
            
            val outputSize = if (days > 100) "full" else "compact"
            val response = service.getTimeSeriesDaily(
                symbol = symbol,
                outputSize = outputSize,
                apiKey = API_KEY
            )
            
            if (response.isSuccessful) {
                val timeSeriesResponse = response.body()
                if (timeSeriesResponse != null && timeSeriesResponse.timeSeriesDaily != null) {
                    val pricePoints = timeSeriesResponse.timeSeriesDaily.entries
                        .sortedByDescending { it.key }
                        .take(days)
                        .map { (dateStr, data) ->
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val date = dateFormat.parse(dateStr)
                            val timestamp = date?.time ?: 0
                            
                            PricePoint(
                                timestamp = timestamp / 1000, // Konwersja na sekundy
                                price = data.close.toDoubleOrNull() ?: 0.0
                            )
                        }
                    
                    Response.success(pricePoints)
                } else {
                    Log.e(TAG, "Empty or null time series data for $symbol")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} for $symbol")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching price history: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie danych intraday (24h)
    suspend fun getStockIntraday24h(symbol: String): Response<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching 24h intraday data for $symbol")
            
            val response = service.getTimeSeriesIntraday(
                symbol = symbol,
                interval = "60min", // Godzinne interwały dają dobry przegląd dnia
                apiKey = API_KEY
            )
            
            if (response.isSuccessful) {
                val timeSeriesResponse = response.body()
                if (timeSeriesResponse != null && timeSeriesResponse.timeSeriesIntraday != null) {
                    val pricePoints = timeSeriesResponse.timeSeriesIntraday.entries
                        .sortedByDescending { it.key }
                        .take(24) // Ostatnie 24 godziny
                        .map { (dateTimeStr, data) ->
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            val dateTime = dateFormat.parse(dateTimeStr)
                            val timestamp = dateTime?.time ?: 0
                            
                            PricePoint(
                                timestamp = timestamp / 1000, // Konwersja na sekundy
                                price = data.close.toDoubleOrNull() ?: 0.0
                            )
                        }
                    
                    Response.success(pricePoints)
                } else {
                    Log.e(TAG, "Empty or null intraday data for $symbol")
                    Response.error(404, "No data found".toResponseBody("text/plain".toMediaTypeOrNull()))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} for $symbol")
                Response.error(response.code(), response.errorBody() ?: "Error".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching intraday data: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Pobieranie listy top spółek
    private suspend fun getListedStocks(): Response<List<StockSearchResult>> {
        return try {
            Log.d(TAG, "Fetching listed stocks")
            val response = service.getSP500List(apiKey = API_KEY)
            
            if (response.isSuccessful) {
                val listingResponse = response.body()
                if (listingResponse != null && listingResponse.data != null) {
                    val results = listingResponse.data.map { row ->
                        StockSearchResult(
                            symbol = row.getOrNull(0) ?: "",
                            securityName = row.getOrNull(1) ?: "",
                            securityType = row.getOrNull(3) ?: "",
                            exchange = row.getOrNull(2) ?: ""
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
            Log.e(TAG, "Exception when fetching listed stocks: ${e.message}", e)
            Response.error(500, "Exception: ${e.message}".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }
    
    // Funkcja pomocnicza do pobierania nazw firm na podstawie symbolu (dla popularnych akcji)
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
            "INTC" -> "Intel Corporation"
            "AMD" -> "Advanced Micro Devices, Inc."
            "IBM" -> "International Business Machines Corp."
            "JPM" -> "JPMorgan Chase & Co."
            "BAC" -> "Bank of America Corp."
            "WFC" -> "Wells Fargo & Co."
            "GS" -> "Goldman Sachs Group Inc."
            "C" -> "Citigroup Inc."
            "JNJ" -> "Johnson & Johnson"
            "PFE" -> "Pfizer Inc."
            "UNH" -> "UnitedHealth Group Inc."
            "MRK" -> "Merck & Co."
            "ABBV" -> "AbbVie Inc."
            "PG" -> "Procter & Gamble Co."
            "KO" -> "Coca-Cola Company"
            "PEP" -> "PepsiCo Inc."
            "WMT" -> "Walmart Inc."
            "COST" -> "Costco Wholesale Corporation"
            "MCD" -> "McDonald's Corporation"
            "DIS" -> "Walt Disney Co."
            "NFLX" -> "Netflix Inc."
            else -> "$symbol" // Just return the symbol if we don't have the company name
        }
    }
} 