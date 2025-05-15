package com.example.stockcryptotracker.repository

import android.util.Log
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.data.room.PriceAlertDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private const val TAG = "PriceAlertRepository"

class PriceAlertRepository(private val priceAlertDao: PriceAlertDao) {
    
    fun getAllAlerts(): Flow<List<PriceAlert>> {
        return priceAlertDao.getAllAlerts()
    }
    
    fun getAllActiveAlerts(): Flow<List<PriceAlert>> {
        return priceAlertDao.getAllActiveAlerts()
    }
    
    fun getAlertsByAsset(assetId: String, isCrypto: Boolean): Flow<List<PriceAlert>> {
        return priceAlertDao.getAlertsByAsset(assetId, isCrypto)
    }
    
    fun getAllCryptoAlerts(): Flow<List<PriceAlert>> {
        return priceAlertDao.getAllAlertsByType(isCrypto = true)
    }
    
    fun getAllStockAlerts(): Flow<List<PriceAlert>> {
        return priceAlertDao.getAllAlertsByType(isCrypto = false)
    }
    
    suspend fun addCryptoAlert(crypto: CryptoCurrency, targetPrice: Double, isAboveTarget: Boolean): Boolean {
        return try {
            // Check if the alert already exists
            val exists = priceAlertDao.alertExists(
                assetId = crypto.id,
                targetPrice = targetPrice,
                isCrypto = true,
                isAboveTarget = isAboveTarget
            ).first()
            
            if (!exists) {
                val alert = PriceAlert(
                    assetId = crypto.id,
                    assetName = crypto.name,
                    assetSymbol = crypto.symbol.uppercase(),
                    targetPrice = targetPrice,
                    isAboveTarget = isAboveTarget,
                    isCrypto = true
                )
                priceAlertDao.insertAlert(alert)
                Log.d(TAG, "Added crypto price alert for ${crypto.name} at $targetPrice")
                true
            } else {
                Log.d(TAG, "Crypto price alert for ${crypto.name} at $targetPrice already exists")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding crypto price alert", e)
            false
        }
    }
    
    suspend fun addStockAlert(stock: Stock, targetPrice: Double, isAboveTarget: Boolean): Boolean {
        return try {
            // Check if the alert already exists
            val exists = priceAlertDao.alertExists(
                assetId = stock.symbol,
                targetPrice = targetPrice,
                isCrypto = false,
                isAboveTarget = isAboveTarget
            ).first()
            
            if (!exists) {
                val alert = PriceAlert(
                    assetId = stock.symbol,
                    assetName = stock.name,
                    assetSymbol = stock.symbol.uppercase(),
                    targetPrice = targetPrice,
                    isAboveTarget = isAboveTarget,
                    isCrypto = false
                )
                priceAlertDao.insertAlert(alert)
                Log.d(TAG, "Added stock price alert for ${stock.name} at $targetPrice")
                true
            } else {
                Log.d(TAG, "Stock price alert for ${stock.name} at $targetPrice already exists")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding stock price alert", e)
            false
        }
    }
    
    suspend fun deleteAlert(alertId: Int) {
        try {
            priceAlertDao.deleteAlertById(alertId)
            Log.d(TAG, "Deleted price alert with ID: $alertId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting price alert", e)
        }
    }
    
    suspend fun toggleAlertActive(alertId: Int, isActive: Boolean) {
        try {
            priceAlertDao.setAlertActive(alertId, isActive)
            Log.d(TAG, "Set price alert $alertId active status to: $isActive")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling price alert active status", e)
        }
    }
    
    // Check if any alerts have been triggered
    fun checkAlertsForCrypto(crypto: CryptoCurrency, currentPrice: Double): List<PriceAlert> {
        val triggeredAlerts = mutableListOf<PriceAlert>()
        // Implementation will be done in a service or worker
        return triggeredAlerts
    }
    
    fun checkAlertsForStock(stock: Stock, currentPrice: Double): List<PriceAlert> {
        val triggeredAlerts = mutableListOf<PriceAlert>()
        // Implementation will be done in a service or worker
        return triggeredAlerts
    }
} 