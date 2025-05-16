package com.example.stockcryptotracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoCompareApiService {
    
    @GET("data/top/mktcapfull")
    suspend fun getTopCryptos(
        @Query("limit") limit: Int = 100,
        @Query("tsym") currency: String = "USD",
        @Query("api_key") apiKey: String? = null
    ): CryptoCompareResponse
    
    @GET("data/pricemultifull")
    suspend fun getCryptoDetails(
        @Query("fsyms") symbol: String,
        @Query("tsyms") currency: String = "USD",
        @Query("api_key") apiKey: String? = null
    ): CryptoCompareDetailResponse
    
    @GET("data/v2/histoday")
    suspend fun getCryptoHistoricalDaily(
        @Query("fsym") symbol: String,
        @Query("tsym") currency: String = "USD",
        @Query("limit") limit: Int = 30,
        @Query("aggregate") aggregate: Int = 1,
        @Query("api_key") apiKey: String? = null
    ): CryptoCompareHistoricalResponse
    
    @GET("data/v2/histohour")
    suspend fun getCryptoHistoricalHourly(
        @Query("fsym") symbol: String,
        @Query("tsym") currency: String = "USD",
        @Query("limit") limit: Int = 24,
        @Query("aggregate") aggregate: Int = 1,
        @Query("api_key") apiKey: String? = null
    ): CryptoCompareHistoricalResponse
    
    @GET("data/v2/histominute")
    suspend fun getCryptoHistoricalMinute(
        @Query("fsym") symbol: String,
        @Query("tsym") currency: String = "USD",
        @Query("limit") limit: Int = 60,
        @Query("aggregate") aggregate: Int = 1,
        @Query("api_key") apiKey: String? = null
    ): CryptoCompareHistoricalResponse
}

data class CryptoCompareResponse(
    @SerializedName("Data") val data: List<CryptoCompareData>
)

data class CryptoCompareData(
    @SerializedName("CoinInfo") val coinInfo: CoinInfo,
    @SerializedName("RAW") val raw: RawData?,
    @SerializedName("DISPLAY") val display: DisplayData?
)

data class CoinInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val symbol: String,
    @SerializedName("FullName") val name: String,
    @SerializedName("ImageUrl") val imageUrl: String
)

data class RawData(
    @SerializedName("USD") val usd: UsdRawData?
)

data class UsdRawData(
    @SerializedName("PRICE") val price: Double,
    @SerializedName("MKTCAP") val marketCap: Double,
    @SerializedName("CHANGEPCT24HOUR") val priceChangePercentage24h: Double,
    @SerializedName("VOLUME24HOUR") val volume24h: Double,
    @SerializedName("HIGH24HOUR") val high24h: Double,
    @SerializedName("LOW24HOUR") val low24h: Double,
    @SerializedName("SUPPLY") val circulatingSupply: Double
)

data class DisplayData(
    @SerializedName("USD") val usd: UsdDisplayData?
)

data class UsdDisplayData(
    @SerializedName("PRICE") val price: String,
    @SerializedName("MKTCAP") val marketCap: String,
    @SerializedName("CHANGEPCT24HOUR") val priceChangePercentage24h: String,
    @SerializedName("VOLUME24HOUR") val volume24h: String
)

// Detail response
data class CryptoCompareDetailResponse(
    @SerializedName("RAW") val raw: Map<String, Map<String, UsdRawDetailData>>?,
    @SerializedName("DISPLAY") val display: Map<String, Map<String, UsdDisplayDetailData>>?
)

data class UsdRawDetailData(
    @SerializedName("PRICE") val price: Double,
    @SerializedName("MKTCAP") val marketCap: Double,
    @SerializedName("CHANGEPCT24HOUR") val priceChangePercentage24h: Double,
    @SerializedName("VOLUME24HOUR") val volume24h: Double,
    @SerializedName("HIGH24HOUR") val high24h: Double,
    @SerializedName("LOW24HOUR") val low24h: Double,
    @SerializedName("SUPPLY") val circulatingSupply: Double,
    @SerializedName("TOTALVOLUME24H") val totalVolume24h: Double,
    @SerializedName("IMAGEURL") val imageUrl: String
)

data class UsdDisplayDetailData(
    @SerializedName("PRICE") val price: String,
    @SerializedName("MKTCAP") val marketCap: String,
    @SerializedName("CHANGEPCT24HOUR") val priceChangePercentage24h: String,
    @SerializedName("VOLUME24HOUR") val volume24h: String,
    @SerializedName("HIGH24HOUR") val high24h: String,
    @SerializedName("LOW24HOUR") val low24h: String,
    @SerializedName("SUPPLY") val circulatingSupply: String
)

// Historical data response
data class CryptoCompareHistoricalResponse(
    @SerializedName("Data") val data: HistoricalData
)

data class HistoricalData(
    @SerializedName("Data") val points: List<HistoricalDataPoint>
)

data class HistoricalDataPoint(
    @SerializedName("time") val time: Long,
    @SerializedName("close") val close: Double,
    @SerializedName("high") val high: Double,
    @SerializedName("low") val low: Double,
    @SerializedName("open") val open: Double,
    @SerializedName("volumefrom") val volumeFrom: Double,
    @SerializedName("volumeto") val volumeTo: Double
) 