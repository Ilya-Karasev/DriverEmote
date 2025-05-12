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