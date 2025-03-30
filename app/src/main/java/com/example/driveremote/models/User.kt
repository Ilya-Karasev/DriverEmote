package com.example.driveremote.models

import android.content.Context
import androidx.room.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surName: String,
    val firstName: String,
    val fatherName: String,
    val age: Int,
    val post: Post,
    val email: String,
    val password: String
)

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM User")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM User WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): User?

    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?
}

object TestUsers {
    val users = listOf(
        User(1, "Иванов", "Иван", "Иванович", 30, Post.ВОДИТЕЛЬ, "driver@example.com", "driver123"),
        User(2, "Петров", "Петр", "Петрович", 40, Post.РУКОВОДИТЕЛЬ, "manager@example.com", "manager123")
    )

    suspend fun insertTestUsers(context: Context, userDao: UserDao) {
        if (userDao.getAllUsers().isEmpty()) {
            users.forEach { userDao.insertUser(it) }
        }

        val resultsDao = AppDatabase.getDatabase(context).resultsDao()

        if (resultsDao.getResultsByUser(1).isEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val result1 = Results(
                userId = 1,
                testDate = dateFormat.format(Date()), // Текущая дата и время
                emotionalExhaustionScore = 30,
                depersonalizationScore = 20,
                personalAchievementScore = 15,
                totalScore = 65
            )
            resultsDao.insertResult(result1)

            // Добавляем ещё один тестовый результат с разницей в один день
            val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000) // Вчерашняя дата
            val result2 = Results(
                userId = 1,
                testDate = dateFormat.format(yesterday),
                emotionalExhaustionScore = 35,
                depersonalizationScore = 22,
                personalAchievementScore = 18,
                totalScore = 75
            )
            resultsDao.insertResult(result2)
        }
    }
}