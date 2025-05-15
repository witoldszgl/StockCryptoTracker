package com.example.stockcryptotracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stockcryptotracker.MainActivity
import com.example.stockcryptotracker.R
import com.example.stockcryptotracker.data.CryptoCurrency
import com.example.stockcryptotracker.data.Stock
import com.example.stockcryptotracker.data.room.CryptoDatabase
import com.example.stockcryptotracker.data.room.PriceAlert
import com.example.stockcryptotracker.repository.CryptoRepository
import com.example.stockcryptotracker.repository.PriceAlertRepository
import com.example.stockcryptotracker.repository.StockRepository
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "PriceAlertService"
private const val CHANNEL_ID = "price_alerts"
private const val PRICE_ALERT_WORK = "price_alert_work"

class PriceAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Starting price alert check")
            
            val database = CryptoDatabase.getDatabase(context)
            val priceAlertRepository = PriceAlertRepository(database.priceAlertDao())
            val cryptoRepository = CryptoRepository()
            val stockRepository = StockRepository()
            
            // Get all active alerts
            val alerts = priceAlertRepository.getAllActiveAlerts().first()
            if (alerts.isEmpty()) {
                Log.d(TAG, "No active price alerts found")
                return Result.success()
            }
            
            Log.d(TAG, "Found ${alerts.size} active price alerts")
            
            // Group alerts by type (crypto/stock)
            val cryptoAlerts = alerts.filter { it.isCrypto }
            val stockAlerts = alerts.filter { !it.isCrypto }
            
            // Process crypto alerts
            if (cryptoAlerts.isNotEmpty()) {
                processCryptoAlerts(cryptoAlerts, cryptoRepository)
            }
            
            // Process stock alerts
            if (stockAlerts.isNotEmpty()) {
                processStockAlerts(stockAlerts, stockRepository)
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking price alerts", e)
            return Result.retry()
        }
    }
    
    private suspend fun processCryptoAlerts(alerts: List<PriceAlert>, repository: CryptoRepository) {
        try {
            // Get unique crypto IDs
            val cryptoIds = alerts.map { it.assetId }.distinct()
            
            // Fetch current prices for all needed cryptocurrencies
            val cryptoResult = repository.getCryptocurrencies()
            if (cryptoResult.isSuccess) {
                val cryptos = cryptoResult.getOrNull() ?: emptyList()
                
                // Check each alert
                for (alert in alerts) {
                    val crypto = cryptos.find { it.id == alert.assetId }
                    if (crypto != null) {
                        checkCryptoAlert(alert, crypto)
                    }
                }
            } else {
                Log.e(TAG, "Failed to fetch crypto prices", cryptoResult.exceptionOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing crypto alerts", e)
        }
    }
    
    private suspend fun processStockAlerts(alerts: List<PriceAlert>, repository: StockRepository) {
        try {
            // Get unique stock symbols
            val symbols = alerts.map { it.assetId }.distinct()
            
            // Fetch current prices for all needed stocks
            val stocksResult = repository.getAllStocks()
            if (stocksResult.isSuccess) {
                val stocks = stocksResult.getOrNull() ?: emptyList()
                
                // Check each alert
                for (alert in alerts) {
                    val stock = stocks.find { it.symbol == alert.assetId }
                    if (stock != null) {
                        checkStockAlert(alert, stock)
                    }
                }
            } else {
                Log.e(TAG, "Failed to fetch stock prices", stocksResult.exceptionOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing stock alerts", e)
        }
    }
    
    private fun checkCryptoAlert(alert: PriceAlert, crypto: CryptoCurrency) {
        val currentPrice = crypto.currentPrice
        
        val isTriggered = if (alert.isAboveTarget) {
            currentPrice >= alert.targetPrice
        } else {
            currentPrice <= alert.targetPrice
        }
        
        if (isTriggered) {
            val direction = if (alert.isAboveTarget) "above" else "below"
            val message = "${crypto.symbol.uppercase()} price is now ${formatCurrency(currentPrice)}, " +
                   "$direction your target of ${formatCurrency(alert.targetPrice)}"
            
            sendNotification(
                title = "${crypto.symbol.uppercase()} Price Alert",
                message = message,
                alertId = alert.id
            )
            
            // TODO: Optionally mark alert as inactive after triggering
            // This would require calling the repository from here
        }
    }
    
    private fun checkStockAlert(alert: PriceAlert, stock: Stock) {
        val currentPrice = stock.price ?: 0.0
        
        val isTriggered = if (alert.isAboveTarget) {
            currentPrice >= alert.targetPrice
        } else {
            currentPrice <= alert.targetPrice
        }
        
        if (isTriggered) {
            val direction = if (alert.isAboveTarget) "above" else "below"
            val message = "${stock.symbol.uppercase()} price is now ${formatCurrency(currentPrice)}, " +
                   "$direction your target of ${formatCurrency(alert.targetPrice)}"
            
            sendNotification(
                title = "${stock.symbol.uppercase()} Price Alert",
                message = message,
                alertId = alert.id
            )
            
            // TODO: Optionally mark alert as inactive after triggering
        }
    }
    
    private fun sendNotification(title: String, message: String, alertId: Int) {
        try {
            // Create notification channel for Android O and above
            createNotificationChannel()
            
            // Create an intent for tapping on notification
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                alertId, 
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with appropriate icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                notify(alertId, builder.build())
                Log.d(TAG, "Notification sent: $title - $message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Price Alerts"
            val descriptionText = "Notifications when asset prices reach target levels"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }
    
    companion object {
        fun schedulePriceAlertChecks(context: Context) {
            val work = PeriodicWorkRequestBuilder<PriceAlertWorker>(
                15, TimeUnit.MINUTES,  // Check prices every 15 minutes
                5, TimeUnit.MINUTES    // With flex interval of 5 minutes
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PRICE_ALERT_WORK,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
                work
            )
            
            Log.d(TAG, "Price alert checks scheduled")
        }
    }
} 