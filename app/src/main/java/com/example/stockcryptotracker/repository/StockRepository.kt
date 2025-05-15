package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.toStock
import com.example.stockcryptotracker.network.PolygonApi
import com.example.stockcryptotracker.network.StockSearchResult
import com.example.stockcryptotracker.data.PricePoint

private const val TAG = "StockRepository"

class StockRepository {
    suspend fun getMostActiveStocks(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching most active stocks")
            val response = PolygonApi.getMostActiveStocks()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stocks = body.map { it.toStock() }
                    Log.d(TAG, "Successfully fetched ${stocks.size} most active stocks")
                    Result.success(stocks.sortedByDescending { it.volume ?: 0 }.take(10))
                } else {
                    Log.e(TAG, "Empty response body when fetching most active stocks")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch most active stocks: ${response.message()}")
                Result.failure(Exception("Failed to fetch active stocks: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching most active stocks", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTopGainers(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching top gainers")
            val response = PolygonApi.getTopStocks()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stocks = body.map { it.toStock() }
                    Log.d(TAG, "Successfully fetched ${stocks.size} stocks")
                    Result.success(stocks.sortedByDescending { it.changePercent }.take(10))
                } else {
                    Log.e(TAG, "Empty response body when fetching top gainers")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch top gainers: ${response.message()}")
                Result.failure(Exception("Failed to fetch top gainers: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching top gainers", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTopLosers(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching top losers")
            val response = PolygonApi.getTopStocks()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stocks = body.map { it.toStock() }
                    Log.d(TAG, "Successfully fetched ${stocks.size} stocks")
                    Result.success(stocks.sortedBy { it.changePercent }.take(10))
                } else {
                    Log.e(TAG, "Empty response body when fetching top losers")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch top losers: ${response.message()}")
                Result.failure(Exception("Failed to fetch top losers: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching top losers", e)
            Result.failure(e)
        }
    }
    
    suspend fun getStockQuote(symbol: String): Result<Stock> {
        return try {
            Log.d(TAG, "Fetching stock quote for $symbol")
            val response = PolygonApi.getStockQuote(symbol)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stock = body.toStock()
                    Log.d(TAG, "Successfully fetched quote for $symbol")
                    Result.success(stock)
                } else {
                    Log.e(TAG, "Empty response body when fetching quote for $symbol")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch stock quote for $symbol: ${response.message()}")
                Result.failure(Exception("Failed to fetch stock quote: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching stock quote for $symbol", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMultipleStocks(symbols: List<String>): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching multiple stocks: ${symbols.joinToString(", ")}")
            val response = PolygonApi.getMultipleStocks(symbols)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stocks = body.map { it.toStock() }
                    Log.d(TAG, "Successfully fetched ${stocks.size} stocks")
                    Result.success(stocks)
                } else {
                    Log.e(TAG, "Empty response body when fetching multiple stocks")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch multiple stocks: ${response.message()}")
                Result.failure(Exception("Failed to fetch multiple stocks: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching multiple stocks", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchStocks(query: String): Result<List<StockSearchResult>> {
        return try {
            Log.d(TAG, "Searching stocks with query: $query")
            val response = PolygonApi.searchStocks(query)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Successfully found ${body.size} search results for: $query")
                    Result.success(body)
                } else {
                    Log.e(TAG, "Empty response body when searching stocks with query: $query")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to search stocks with query $query: ${response.message()}")
                Result.failure(Exception("Failed to search stocks: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when searching stocks with query: $query", e)
            Result.failure(e)
        }
    }

    suspend fun getAllStocks(): Result<List<Stock>> {
        return try {
            Log.d(TAG, "Fetching all stocks")
            val response = PolygonApi.getAllStocks()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val stocks = body.map { it.toStock() }
                    Log.d(TAG, "Successfully fetched ${stocks.size} stocks")
                    Result.success(stocks)
                } else {
                    Log.e(TAG, "Empty response body when fetching all stocks")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorCode = response.code()
                Log.e(TAG, "Failed to fetch all stocks: HTTP $errorCode - $errorBody")
                Result.failure(Exception("Failed to fetch all stocks: HTTP $errorCode - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching all stocks", e)
            Result.failure(e)
        }
    }

    suspend fun getStockPriceHistory(symbol: String, outputSize: String = "compact"): Result<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching price history for $symbol")
            val days = when (outputSize) {
                "full" -> 365
                else -> 30
            }
            val response = PolygonApi.getStockPriceHistory(symbol, days)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Successfully fetched ${body.size} price points for $symbol")
                    Result.success(body)
                } else {
                    Log.e(TAG, "Empty response body when fetching price history for $symbol")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch price history for $symbol: ${response.message()}")
                Result.failure(Exception("Failed to fetch price history: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching price history for $symbol", e)
            Result.failure(e)
        }
    }

    suspend fun getStockIntraday24h(symbol: String): Result<List<PricePoint>> {
        return try {
            Log.d(TAG, "Fetching 24h intraday data for $symbol")
            val response = PolygonApi.getStockIntraday24h(symbol)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Successfully fetched ${body.size} intraday points for $symbol")
                    Result.success(body)
                } else {
                    Log.e(TAG, "Empty response body when fetching intraday data for $symbol")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "Failed to fetch intraday data for $symbol: ${response.message()}")
                Result.failure(Exception("Failed to fetch intraday data: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when fetching intraday data for $symbol", e)
            Result.failure(e)
        }
    }
} 