package com.example.driveremote.models

import androidx.room.*
import java.util.*

@Entity(
    tableName = "Results",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Results(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val testDate: String,
    val emotionalExhaustionScore: Int,
    val depersonalizationScore: Int,
    val personalAchievementScore: Int,
    val totalScore: Int
) {
    val status: String
        get() = when {
            totalScore >= 106 -> "Критическое" // 80% от 132
            totalScore >= 66 -> "Внимание"     // 50% от 132
            else -> "Норма"
        }
}

@Dao
interface ResultsDao {
    @Insert
    suspend fun insertResult(result: Results)

    @Query("SELECT * FROM results WHERE userId = :userId ORDER BY testDate DESC")
    suspend fun getResultsByUser(userId: Int): List<Results>

    @Query("SELECT * FROM results WHERE userId = :userId ORDER BY testDate DESC LIMIT 1")
    suspend fun getLastResultByUser(userId: Int): Results?
}
