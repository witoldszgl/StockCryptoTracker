package com.example.stockcryptotracker

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class StockCryptoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicjalizacja WorkManager
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
            
        WorkManager.initialize(this, config)
    }
} 