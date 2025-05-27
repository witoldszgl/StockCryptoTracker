package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.PriceHistoryPoint
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.TimeRange
import com.example.stockcryptotracker.network.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PolygonRepository(
    val polygonApiService: PolygonApiService
) {
    companion object {
        private const val TAG = "PolygonRepository"
    }

    // Lista wspieranych akcji - ograniczona ze względu na limity API
    private val supportedStocks = listOf("AAPL", "GOOGL")
    
    // Pomocnicza funkcja do uzyskania nazwy firmy na podstawie symbolu
    private fun getDefaultCompanyName(symbol: String): String {
        return when (symbol) {
            "AAPL" -> "Apple Inc."
            "GOOGL" -> "Alphabet Inc."
            "MSFT" -> "Microsoft Corporation"
            "AMZN" -> "Amazon.com Inc."
            "META" -> "Meta Platforms Inc."
            "TSLA" -> "Tesla Inc."
            "NVDA" -> "NVIDIA Corporation"
            else -> "$symbol Inc."
        }
    }
    
    // Pomocnicza funkcja do uzyskania domyślnej ceny akcji
    private fun getDefaultPrice(symbol: String): Double {
        return when (symbol) {
            "AAPL" -> 187.68
            "GOOGL" -> 152.03
            else -> 100.0
        }
    }
    
    // Pomocnicza funkcja do uzyskania domyślnego wolumenu
    private fun getDefaultVolume(symbol: String): Long {
        return when (symbol) {
            "AAPL" -> 58642300
            "GOOGL" -> 26752400
            else -> 1000000
        }
    }
    
    // Pomocnicza funkcja do uzyskania domyślnej kapitalizacji rynkowej
    private fun getDefaultMarketCap(symbol: String): Double {
        return when (symbol) {
            "AAPL" -> 2.89e12  // 2.89 bln USD
            "GOOGL" -> 1.89e12 // 1.89 bln USD
            else -> 1.0e9      // 1 mld USD
        }
    }
    
    // 1. Pobierz listę tickerów akcji (z możliwością wyszukiwania)
    suspend fun getStocksList(
        searchQuery: String? = null, 
        limit: Int = 20
    ): List<Stock> {
        try {
            Log.d(TAG, "Fetching stocks list from Polygon" + (searchQuery?.let { " with query: $it" } ?: ""))
            
            // Jeśli nie ma zapytania wyszukiwania, pobierz tylko wspierane akcje
            if (searchQuery.isNullOrBlank()) {
                // Tworzymy listę na wyniki
                val results = mutableListOf<Stock>()
                
                // Pobierz dane dla każdego wspieranego symbolu
                for (symbol in supportedStocks) {
                    try {
                        val stock = getStockDetails(symbol)
                        results.add(stock)
                        Log.d(TAG, "Successfully fetched details for $symbol")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching details for $symbol", e)
                        // Tworzymy podstawowy obiekt Stock z tymczasowymi danymi
                        val fallbackStock = Stock(
                            symbol = symbol,
                            name = getDefaultCompanyName(symbol),
                            currentPrice = 0.0,
                            priceChangePercentage24h = 0.0,
                            marketCap = 0.0,
                            image = "",
                            exchange = "NASDAQ"
                        )
                        results.add(fallbackStock)
                        Log.d(TAG, "Added fallback data for $symbol")
                    }
                }
                
                // Zwróć wszystkie dane, nawet jeśli niektóre są podstawowe
                return results
            }
            
            // Wyszukiwanie tickerów
            val response = polygonApiService.getTickersList(
                market = "stocks",
                active = true,
                ticker = searchQuery,
                limit = limit
            )
            
            Log.d(TAG, "Received ${response.results.size} tickers from Polygon")
            
            // Konwertuj wyniki na listę Stock
            return response.results.map { tickerItem ->
                // Dla każdego tickera próbujemy pobrać więcej danych
                try {
                    getStockDetails(tickerItem.ticker)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching details for ${tickerItem.ticker}, using basic data", e)
                    
                    // Jeśli nie udało się pobrać szczegółów, zwróć podstawowe dane
                    Stock(
                        symbol = tickerItem.ticker,
                        name = tickerItem.name,
                        currentPrice = 0.0,
                        priceChangePercentage24h = 0.0,
                        marketCap = 0.0,
                        image = "",
                        exchange = tickerItem.primaryExchange ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stocks list", e)
            throw e
        }
    }

    // 2. Pobierz szczegóły spółki + dane cenowe
    suspend fun getStockDetails(symbol: String): Stock {
        try {
            Log.d(TAG, "Getting stock details for $symbol")
            
            // 1. Pobierz podstawowe informacje o spółce
            val detailsResponse = polygonApiService.getStockDetails(symbol)
            Log.d(TAG, "Stock details response status: ${detailsResponse.status}")
            val details = detailsResponse.results
            
            // Create base stock object with details
            var stock = Stock(
                symbol = details.ticker,
                name = details.name,
                currentPrice = 0.0,
                priceChangePercentage24h = 0.0,
                marketCap = details.market_cap ?: 0.0,
                image = details.branding?.logo_url ?: "",
                description = details.description,
                employees = details.totalEmployees ?: 0,
                listDate = details.listDate,
                exchange = details.primary_exchange ?: ""
            )
            
            // 2. Try to get price data
            try {
                val prevDayResponse = polygonApiService.getStockAggregates(
                    symbol = symbol,
                    multiplier = 1,
                    timespan = "day",
                    from = "2024-01-01",
                    to = java.time.LocalDate.now().plusDays(1).format(java.time.format.DateTimeFormatter.ISO_DATE)
                )
                
                Log.d(TAG, "Stock aggregates response received. Results size: ${prevDayResponse.results?.size ?: 0}")
                
                // Get the latest data point
                val latestBar = prevDayResponse.results?.lastOrNull()
                if (latestBar != null) {
                    stock = stock.copy(
                        currentPrice = latestBar.c,
                        high24h = latestBar.h,
                        low24h = latestBar.l,
                        totalVolume = latestBar.v.toDouble(),
                        price = latestBar.c,
                        // Calculate price change percentage
                        priceChangePercentage24h = if (latestBar.o > 0) {
                            ((latestBar.c - latestBar.o) / latestBar.o) * 100
                        } else 0.0
                    )
                } else {
                    Log.w(TAG, "No price data available for $symbol")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching price data for $symbol: ${e.message}", e)
                // Continue with basic stock data even if price data fails
            }
            
            Log.d(TAG, "Returning stock details for $symbol: $stock")
            return stock
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock details for $symbol: ${e.message}", e)
            throw e
        }
    }

    // 3. Pobierz historyczne dane cenowe dla wykresu
    suspend fun getStockHistoricalData(symbol: String, timeRange: TimeRange): List<PriceHistoryPoint> {
        try {
            val now = LocalDateTime.now()
            
            // Określ parametry zapytania na podstawie przedziału czasowego
            val (multiplier, timespan, fromDate) = when (timeRange) {
                TimeRange.HOUR_1 -> Triple(5, "minute", now.minusHours(1))
                TimeRange.HOUR_24 -> Triple(1, "hour", now.minusDays(1))
                TimeRange.DAYS_7 -> Triple(1, "day", now.minusDays(7))
                TimeRange.DAYS_30 -> Triple(1, "day", now.minusDays(30))
                TimeRange.DAYS_90 -> Triple(1, "day", now.minusDays(90))
                TimeRange.YEAR_1 -> Triple(1, "day", now.minusYears(1))
            }

            // Format dat zgodny z API Polygon
            val toDateFormatted = now.format(DateTimeFormatter.ISO_DATE)
            val fromDateFormatted = fromDate.format(DateTimeFormatter.ISO_DATE)
            
            Log.d(TAG, "Fetching historical data for $symbol from $fromDateFormatted to $toDateFormatted with timespan $timespan")
            
            val response = polygonApiService.getStockAggregates(
                symbol = symbol,
                multiplier = multiplier,
                timespan = timespan,
                from = fromDateFormatted,
                to = toDateFormatted
            )
            
            Log.d(TAG, "Received ${response.results?.size ?: 0} historical data points")

            // Konwertuj wyniki na listę PriceHistoryPoint
            return response.results?.map { bar ->
                PriceHistoryPoint(
                    timestamp = bar.t,
                    price = bar.c
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock historical data for $symbol", e)
            throw e
        }
    }
    
    // 4. Pobierz dane o otwarciu/zamknięciu sesji dla konkretnego dnia
    suspend fun getDailyOpenClose(symbol: String, date: LocalDate = LocalDate.now().minusDays(1)): PolygonDailyOpenCloseResponse {
        try {
            val dateFormatted = date.format(DateTimeFormatter.ISO_DATE)
            Log.d(TAG, "Fetching daily open/close data for $symbol on $dateFormatted")
            
            val response = polygonApiService.getDailyOpenClose(
                symbol = symbol,
                date = dateFormatted
            )
            
            Log.d(TAG, "Successfully fetched daily open/close data: open=${response.open}, close=${response.close}")
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching daily open/close data for $symbol", e)
            throw e
        }
    }
    
    // 5. Pobierz ostatnią ofertę (bid/ask)
    suspend fun getLastQuote(symbol: String): PolygonLastQuoteResponse {
        try {
            Log.d(TAG, "Fetching last quote for $symbol")
            val response = polygonApiService.getLastQuote(symbol)
            Log.d(TAG, "Successfully fetched last quote: bid=${response.results.bidPrice}, ask=${response.results.askPrice}")
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching last quote for $symbol", e)
            throw e
        }
    }
    
    // 6. Pobierz ostatnią transakcję
    suspend fun getLastTrade(symbol: String): PolygonLastTradeResponse {
        try {
            Log.d(TAG, "Fetching last trade for $symbol")
            val response = polygonApiService.getLastTrade(symbol)
            Log.d(TAG, "Successfully fetched last trade: price=${response.results.price}")
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching last trade for $symbol", e)
            throw e
        }
    }
    
    // 7. Wyszukiwanie tickerów
    suspend fun searchTickers(query: String): List<PolygonTickerItem> {
        try {
            Log.d(TAG, "Searching tickers with query: $query")
            val response = polygonApiService.getTickersList(
                market = "stocks",
                active = true,
                ticker = query,
                limit = 10
            )
            Log.d(TAG, "Found ${response.results.size} matching tickers")
            return response.results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching tickers", e)
            throw e
        }
    }
} 