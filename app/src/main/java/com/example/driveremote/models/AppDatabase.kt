package com.example.driveremote.models
import android.content.Context
import androidx.room.*
import com.example.driveremote.adapters.Converters
@Database(entities = [User::class, Results::class, Driver::class, Manager::class, Request::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun resultsDao(): ResultsDao
    abstract fun driverDao(): DriverDao
    abstract fun managerDao(): ManagerDao
    abstract fun requestDao(): RequestDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                context.deleteDatabase("app_database")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}