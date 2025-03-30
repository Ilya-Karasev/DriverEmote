package com.example.driveremote.models

import android.content.Context
import androidx.room.*

@Database(entities = [User::class, Results::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun resultsDao(): ResultsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                context.deleteDatabase("app_database") // Очистка старой базы перед созданием
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // Позволяет пересоздавать базу при изменении схемы
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}