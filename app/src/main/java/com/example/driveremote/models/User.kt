package com.example.driveremote.models

import android.content.Context
import androidx.room.*
import kotlinx.serialization.Serializable
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

    suspend fun insertTestUsers(context: Context, userDao: UserDao, driverDao: DriverDao, managerDao: ManagerDao) {
        if (userDao.getAllUsers().isEmpty()) {
            users.forEach { userDao.insertUser(it) }
        }

        // Добавляем только пользователей с ролью ВОДИТЕЛЬ в таблицу Driver
        users.filter { it.post == Post.ВОДИТЕЛЬ }.forEach { user ->
            val existingDriver = driverDao.getDriverById(user.id)
            if (existingDriver == null) {
                val testDriver = Driver(
                    id = user.id,
                    isCompleted = false,
                    testingTime = listOf("08:00"), // Пример расписания тестирования
                    quantity = 1
                )
                driverDao.insertDriver(testDriver)
            }
        }

        // Добавляем менеджера с пустым списком сотрудников (employeesList)
        val managerUser = users.find { it.post == Post.РУКОВОДИТЕЛЬ }
        managerUser?.let {
            val existingManager = managerDao.getManagerById(it.id)
            if (existingManager == null) {
                val manager = Manager(
                    user = it,
                    employeesList = emptyList() // Пустой список сотрудников
                )
                managerDao.insertManager(manager)
            }
        }

        val resultsDao = AppDatabase.getDatabase(context).resultsDao()

        if (resultsDao.getResultsByUser(1).isEmpty()) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy — HH:mm", Locale.getDefault())

            val result1 = Results(
                userId = 1,
                testDate = dateFormat.format(Date()),
                emotionalExhaustionScore = 30,
                depersonalizationScore = 20,
                personalAchievementScore = 15,
                totalScore = 65
            )
            resultsDao.insertResult(result1)

            val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val result2 = Results(
                userId = 1,
                testDate = dateFormat.format(yesterday),
                emotionalExhaustionScore = 35,
                depersonalizationScore = 22,
                personalAchievementScore = 18,
                totalScore = 75
            )
            resultsDao.insertResult(result2)

            // После добавления результатов обновляем статус водителя
            val driverDao = AppDatabase.getDatabase(context).driverDao()
            updateDriverStatus(context, driverDao, resultsDao, driverId = 1)
        }
    }

    suspend fun updateDriverStatus(context: Context, driverDao: DriverDao, resultsDao: ResultsDao, driverId: Int) {
        val results = resultsDao.getResultsByUser(driverId)
        val newStatus = Driver.calculateStatus(results)
        driverDao.updateStatus(driverId, newStatus)
    }
}