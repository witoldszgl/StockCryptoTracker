package com.example.stockcryptotracker.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteCrypto::class, PortfolioItem::class], 
    version = 2, 
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun favoriteCryptoDao(): FavoriteCryptoDao
    abstract fun portfolioDao(): PortfolioDao
    
    companion object {
        @Volatile
        private var INSTANCE: CryptoDatabase? = null
        
        fun getDatabase(context: Context): CryptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CryptoDatabase::class.java,
                    "crypto_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 