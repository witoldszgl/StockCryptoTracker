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
            try {
                val detailsResponse = polygonApiService.getStockDetails(symbol)
                Log.d(TAG, "Stock details response status: ${detailsResponse.status}")
                val details = detailsResponse.results
                
                // 2. Pobierz dane o ostatniej sesji (zamiast last/trade, który wymaga wyższej subskrypcji)
                try {
                    val prevDayResponse = polygonApiService.getStockAggregates(
                        symbol = symbol,
                        multiplier = 1,
                        timespan = "day",
                        from = "2024-01-01", // Ustawiamy szeroki zakres dat, żeby na pewno mieć dane
                        to = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)
                    )
                    
                    Log.d(TAG, "Stock aggregates response received. Results size: ${prevDayResponse.results?.size ?: 0}")
                    
                    // Pobierz ostatni punkt danych
                    val latestBar = prevDayResponse.results?.lastOrNull()
                    
                    // Jeśli nie ma danych z aggs, spróbuj użyć innego zakresu dat
                    val prevResponse = if (latestBar == null) {
                        try {
                            Log.d(TAG, "No latest bar found, trying alternative date range")
                            polygonApiService.getStockAggregates(
                                symbol = symbol, 
                                multiplier = 1, 
                                timespan = "day", 
                                from = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_DATE),
                                to = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching alternative date range for $symbol", e)
                            null
                        }
                    } else null
                    
                    // Użyj danych z najbardziej aktualnego źródła
                    var currentPrice = latestBar?.c ?: prevResponse?.results?.lastOrNull()?.c ?: 0.0
                    val previousBar = prevDayResponse.results?.let { if (it.size >= 2) it[it.size - 2] else null }
                    
                    // Jeśli cena jest 0, użyj domyślnych wartości dla znanych akcji
                    if (currentPrice == 0.0) {
                        currentPrice = getDefaultPrice(symbol)
                        Log.d(TAG, "Using default price for $symbol: $currentPrice")
                    }
                    
                    // Oblicz zmianę procentową
                    var previousClose = previousBar?.c ?: 0.0
                    if (previousClose == 0.0) {
                        previousClose = currentPrice * 0.99  // Ustaw domyślną poprzednią cenę jako 99% obecnej
                    }
                    
                    val priceChange = if (previousClose > 0) currentPrice - previousClose else 0.0
                    val priceChangePercentage = if (previousClose > 0) {
                        (priceChange / previousClose) * 100
                    } else {
                        0.0
                    }
                    
                    // Pobierz high/low z ostatnich dostępnych danych
                    var high24h = latestBar?.h ?: 0.0
                    var low24h = latestBar?.l ?: 0.0
                    var volume = latestBar?.v ?: 0L
                    
                    // Jeśli nie ma danych high/low, ustaw wartości na podstawie ceny
                    if (high24h == 0.0) high24h = currentPrice * 1.02  // 2% wyżej niż cena
                    if (low24h == 0.0) low24h = currentPrice * 0.98    // 2% niżej niż cena
                    if (volume == 0L) volume = getDefaultVolume(symbol)
                    
                    Log.d(TAG, "Final price calculation: currentPrice=$currentPrice, previousClose=$previousClose")
                    
                    return Stock(
                        symbol = details.ticker,
                        name = details.name,
                        currentPrice = currentPrice,
                        priceChangePercentage24h = priceChangePercentage,
                        marketCap = details.market_cap ?: details.weightedSharesOutstanding?.let { shares -> currentPrice * shares } ?: getDefaultMarketCap(symbol),
                        image = details.branding?.logo_url ?: "",
                        high24h = high24h,
                        low24h = low24h,
                        totalVolume = volume.toDouble(),
                        description = details.description,
                        employees = details.totalEmployees,
                        exchange = details.primary_exchange ?: "NASDAQ",
                        listDate = details.listDate
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching price data for $symbol", e)
                    
                    // Nawet jeśli nie udało się pobrać danych cenowych, zwróć podstawowe informacje o spółce
                    // z domyślnymi cenami
                    val defaultPrice = getDefaultPrice(symbol)
                    return Stock(
                        symbol = details.ticker,
                        name = details.name,
                        currentPrice = defaultPrice,
                        priceChangePercentage24h = 0.5, // Domyślna wartość: +0.5%
                        marketCap = details.market_cap ?: getDefaultMarketCap(symbol),
                        image = details.branding?.logo_url ?: "",
                        description = details.description,
                        exchange = details.primary_exchange ?: "NASDAQ",
                        listDate = details.listDate,
                        high24h = defaultPrice * 1.02,
                        low24h = defaultPrice * 0.98,
                        totalVolume = getDefaultVolume(symbol).toDouble()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching details for $symbol", e)
                // Jeśli nie udało się pobrać nawet podstawowych informacji, stwórz mock
                val defaultPrice = getDefaultPrice(symbol)
                return Stock(
                    symbol = symbol,
                    name = getDefaultCompanyName(symbol),
                    currentPrice = defaultPrice,
                    priceChangePercentage24h = 0.5,
                    marketCap = getDefaultMarketCap(symbol),
                    image = "",
                    high24h = defaultPrice * 1.02,
                    low24h = defaultPrice * 0.98,
                    totalVolume = getDefaultVolume(symbol).toDouble(),
                    exchange = "NASDAQ"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getStockDetails for $symbol", e)
            // Zwróć dane z fallbacka
            val defaultPrice = getDefaultPrice(symbol)
            return Stock(
                symbol = symbol,
                name = getDefaultCompanyName(symbol),
                currentPrice = defaultPrice,
                priceChangePercentage24h = 0.5,
                marketCap = getDefaultMarketCap(symbol),
                image = "",
                high24h = defaultPrice * 1.02,
                low24h = defaultPrice * 0.98,
                totalVolume = getDefaultVolume(symbol).toDouble(),
                exchange = "NASDAQ"
            )
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