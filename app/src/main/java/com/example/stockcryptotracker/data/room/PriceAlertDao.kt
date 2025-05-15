package com.example.stockcryptotracker.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert): Long
    
    @Update
    suspend fun updateAlert(alert: PriceAlert)
    
    @Delete
    suspend fun deleteAlert(alert: PriceAlert)
    
    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlertById(alertId: Int)
    
    @Query("SELECT * FROM price_alerts WHERE id = :alertId")
    fun getAlertById(alertId: Int): Flow<PriceAlert?>
    
    @Query("SELECT * FROM price_alerts WHERE assetId = :assetId AND isCrypto = :isCrypto")
    fun getAlertsByAsset(assetId: String, isCrypto: Boolean): Flow<List<PriceAlert>>
    
    @Query("SELECT * FROM price_alerts WHERE isCrypto = :isCrypto")
    fun getAllAlertsByType(isCrypto: Boolean): Flow<List<PriceAlert>>
    
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>
    
    @Query("SELECT * FROM price_alerts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveAlerts(): Flow<List<PriceAlert>>
    
    @Query("UPDATE price_alerts SET isActive = :isActive WHERE id = :alertId")
    suspend fun setAlertActive(alertId: Int, isActive: Boolean)
    
    @Query("SELECT EXISTS(SELECT 1 FROM price_alerts WHERE assetId = :assetId AND targetPrice = :targetPrice AND isCrypto = :isCrypto AND isAboveTarget = :isAboveTarget)")
    fun alertExists(assetId: String, targetPrice: Double, isCrypto: Boolean, isAboveTarget: Boolean): Flow<Boolean>
} 