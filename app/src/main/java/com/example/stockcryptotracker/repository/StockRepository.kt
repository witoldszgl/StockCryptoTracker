package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.PricePoint
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.TimeRange
import com.example.stockcryptotracker.network.PolygonRetrofitClient

class StockRepository {
    companion object {
        private const val TAG = "StockRepository"
    }
    
    private val polygonRepository = PolygonRepository(PolygonRetrofitClient.polygonApiService)
    
    // Lista symboli obsługiwanych przez Polygon
    private val polygonSymbols = listOf("AAPL", "GOOGL")
    
    suspend fun getMostActiveStocks(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching most active stocks")
            
            // Pobierz dane z Polygon
            val stocks = polygonRepository.getStocksList()
            if (stocks.isNotEmpty()) {
                // Sortuj według wolumenu
                val sortedStocks = stocks.sortedByDescending { it.totalVolume }
                Log.d(TAG, "Successfully fetched ${sortedStocks.size} most active stocks from Polygon")
                Result.success(sortedStocks)
            } else {
                Log.e(TAG, "No active stocks found from Polygon")
                Result.failure(Exception("No active stocks found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching most active stocks", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTopGainers(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching top gainers")
            
            // Pobierz dane z Polygon
            val stocks = polygonRepository.getStocksList()
            if (stocks.isNotEmpty()) {
                // Sortuj według procentowej zmiany ceny (malejąco)
                val sortedStocks = stocks.sortedByDescending { it.priceChangePercentage24h }
                Log.d(TAG, "Successfully fetched ${sortedStocks.size} top gainers from Polygon")
                Result.success(sortedStocks)
            } else {
                Log.e(TAG, "No top gainers found from Polygon")
                Result.failure(Exception("No top gainers found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching top gainers", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTopLosers(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching top losers")
            
            // Pobierz dane z Polygon
            val stocks = polygonRepository.getStocksList()
            if (stocks.isNotEmpty()) {
                // Sortuj według procentowej zmiany ceny (rosnąco)
                val sortedStocks = stocks.sortedBy { it.priceChangePercentage24h }
                Log.d(TAG, "Successfully fetched ${sortedStocks.size} top losers from Polygon")
                Result.success(sortedStocks)
            } else {
                Log.e(TAG, "No top losers found from Polygon")
                Result.failure(Exception("No top losers found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching top losers", e)
            Result.failure(e)
        }
    }
    
    suspend fun getStockQuote(symbol: String): Result<Stock> {
        return try {
            Log.d(TAG, "Fetching stock quote for $symbol")
            
            try {
                val stock = polygonRepository.getStockDetails(symbol)
                Log.d(TAG, "Successfully fetched quote for $symbol from Polygon")
                Result.success(stock)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch stock from Polygon for $symbol", e)
                Result.failure(Exception("Failed to fetch stock quote for $symbol: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching stock quote for $symbol", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMultipleStocks(symbols: List<String>): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching multiple stocks: ${symbols.joinToString()}")
            
            val result = mutableListOf<Stock>()
            
            // Fetch all stocks from Polygon
            try {
                Log.d(TAG, "Fetching stocks from Polygon")
                val polygonStocks = polygonRepository.getStocksList()
                
                // Filter for requested symbols
                val filteredStocks = polygonStocks.filter { stock -> 
                    symbols.contains(stock.symbol) 
                }
                
                result.addAll(filteredStocks)
                Log.d(TAG, "Successfully fetched ${filteredStocks.size} stocks from Polygon")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stocks from Polygon", e)
            }
            
            if (result.isEmpty()) {
                Log.e(TAG, "Failed to fetch any stocks")
                Result.failure(Exception("Failed to fetch any stocks"))
            } else {
                Log.d(TAG, "Successfully fetched ${result.size} stocks in total")
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching multiple stocks", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchStocks(query: String): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Searching stocks with query: $query")
            
            // Use Polygon to search
            try {
                val tickers = polygonRepository.searchTickers(query)
                
                // For each ticker, try to get full stock details
                val stocks = tickers.mapNotNull { ticker ->
                    try {
                        polygonRepository.getStockDetails(ticker.ticker)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching details for ${ticker.ticker}", e)
                        null
                    }
                }
                
                if (stocks.isNotEmpty()) {
                    Log.d(TAG, "Successfully found ${stocks.size} search results for: $query")
                    return Result.success(stocks)
                } else {
                    Log.e(TAG, "No results found for query: $query")
                    return Result.failure(Exception("No results found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to search stocks with Polygon", e)
                return Result.failure(Exception("Failed to search stocks"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when searching stocks with query: $query", e)
            Result.failure(e)
        }
    }

    suspend fun getAllStocks(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching all stocks")
            
            // Fetch stocks from Polygon
            try {
                Log.d(TAG, "Fetching stocks from Polygon")
                val polygonStocks = polygonRepository.getStocksList()
                
                if (polygonStocks.isNotEmpty()) {
                    Log.d(TAG, "Successfully fetched ${polygonStocks.size} stocks from Polygon")
                    return Result.success(polygonStocks)
                } else {
                    Log.e(TAG, "Polygon returned empty stock list")
                    
                    // Create basic stock data for some popular symbols as fallback
                    val basicStocks = listOf(
                        Stock(symbol = "AAPL", name = "Apple Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 3000000000000.0),
                        Stock(symbol = "GOOGL", name = "Alphabet Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1500000000000.0),
                        Stock(symbol = "MSFT", name = "Microsoft Corporation", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 2800000000000.0),
                        Stock(symbol = "AMZN", name = "Amazon.com Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1700000000000.0),
                        Stock(symbol = "META", name = "Meta Platforms Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1000000000000.0)
                    )
                    
                    Log.d(TAG, "Returning basic stock data as fallback")
                    return Result.success(basicStocks)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stocks from Polygon", e)
                
                // Create basic stock data for some popular symbols as fallback
                val basicStocks = listOf(
                    Stock(symbol = "AAPL", name = "Apple Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 3000000000000.0),
                    Stock(symbol = "GOOGL", name = "Alphabet Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1500000000000.0),
                    Stock(symbol = "MSFT", name = "Microsoft Corporation", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 2800000000000.0),
                    Stock(symbol = "AMZN", name = "Amazon.com Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1700000000000.0),
                    Stock(symbol = "META", name = "Meta Platforms Inc.", currentPrice = 0.0, priceChangePercentage24h = 0.0, marketCap = 1000000000000.0)
                )
                
                Log.d(TAG, "Returning basic stock data as fallback after error")
                return Result.success(basicStocks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching all stocks", e)
            Result.failure(e)
        }
    }

    suspend fun getStockPriceHistory(symbol: String, outputSize: String = "compact"): Result<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching price history for $symbol")
            
            // Use Polygon for all symbols
            try {
                Log.d(TAG, "Using Polygon API for $symbol price history")
                val timeRange = if (outputSize == "full") TimeRange.YEAR_1 else TimeRange.DAYS_30
                val priceHistory = polygonRepository.getStockHistoricalData(symbol, timeRange)
                
                // Convert PriceHistoryPoint to PricePoint
                val pricePoints = priceHistory.map { 
                    PricePoint(timestamp = it.timestamp, price = it.price) 
                }
                
                return Result.success(pricePoints)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch price history from Polygon for $symbol", e)
                return Result.failure(Exception("Failed to fetch price history"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching price history for $symbol", e)
            Result.failure(e)
        }
    }

    suspend fun getStockIntraday24h(symbol: String): Result<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching 24h intraday data for $symbol")
            
            // Use Polygon for all symbols
            try {
                Log.d(TAG, "Using Polygon API for $symbol intraday data")
                val priceHistory = polygonRepository.getStockHistoricalData(symbol, TimeRange.HOUR_24)
                
                // Convert PriceHistoryPoint to PricePoint
                val pricePoints = priceHistory.map { 
                    PricePoint(timestamp = it.timestamp, price = it.price) 
                }
                
                return Result.success(pricePoints)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch intraday data from Polygon for $symbol", e)
                return Result.failure(Exception("Failed to fetch intraday data"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching intraday data for $symbol", e)
            Result.failure(e)
        }
    }
} 