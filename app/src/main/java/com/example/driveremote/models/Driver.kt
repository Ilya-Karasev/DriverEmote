package com.example.driveremote.models

import androidx.room.*

@Entity
data class Driver(
    @PrimaryKey val id: Int,
    val isCompleted: Boolean,
    val testingTime: List<String>?,
    val quantity: Int, // Может быть только 1 или 2
    var status: String = "Норма" // Поле для статуса ("Норма", "Внимание", "Критическое")
) {
    init {
        require(quantity in listOf(1, 2)) { "Количество может быть 1 или 2" }
    }

    constructor(user: User, isCompleted: Boolean, testingTime: List<String>, quantity: Int) : this(
        id = user.id,
        isCompleted = isCompleted,
        testingTime = testingTime,
        quantity = quantity,
        status = "Норма"
    )

    companion object {
        private const val MAX_SCORE = 132
        private const val THRESHOLD_ATTENTION = 0.5 * MAX_SCORE  // 50% от 132
        private const val THRESHOLD_CRITICAL = 0.8 * MAX_SCORE   // 80% от 132

        fun calculateStatus(results: List<Results>): String {
            if (results.size < 3) return "Норма"

            val recentResults = results.takeLast(7) // Берем последние 7 или меньше
            val total = recentResults.size

            val above50 = recentResults.count { it.totalScore >= THRESHOLD_ATTENTION }
            val above80 = recentResults.count { it.totalScore >= THRESHOLD_CRITICAL }

            return when {
                above80 > total / 2 -> "Критическое"
                above50 > total / 2 -> "Внимание"
                else -> "Норма"
            }
        }
    }
}

@Dao
interface DriverDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver)

    @Query("SELECT * FROM Driver WHERE id = :id LIMIT 1")
    suspend fun getDriverById(id: Int): Driver?

    @Query("UPDATE Driver SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean)

    @Query("UPDATE Driver SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Int, quantity: Int)

    @Query("UPDATE Driver SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
}