package com.example.driveremote.models

import androidx.room.*

@Entity
data class Driver(
    @PrimaryKey val id: Int,
    val isCompleted: Boolean,
    val testingTime: List<String>?,
    val quantity: Int // Может быть только 1 или 2
) {
    init {
        require(quantity in listOf(1, 2)) { "Quantity must be either 1 or 2" }
    }

    constructor(user: User, isCompleted: Boolean, testingTime: List<String>, quantity: Int) : this(
        id = user.id,
        isCompleted = isCompleted,
        testingTime = testingTime,
        quantity = quantity
    )
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
}