package com.example.driveremote.models

import androidx.room.*

@Entity
data class Manager(
    @PrimaryKey val id: Int, // Совпадает с ID пользователя в User
    val employeesList: List<Int> // Список ID подчинённых сотрудников
) {
    constructor(user: User, employeesList: List<Int>) : this(
        id = user.id,
        employeesList = employeesList
    )
}

@Dao
interface ManagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManager(manager: Manager)

    @Query("SELECT * FROM Manager WHERE id = :id LIMIT 1")
    suspend fun getManagerById(id: Int): Manager?

    @Query("UPDATE Manager SET employeesList = :employeesList WHERE id = :id")
    suspend fun updateEmployees(id: Int, employeesList: List<Int>)

    @Query("SELECT * FROM User WHERE id IN (:ids)")
    suspend fun getUsersByIds(ids: List<Int>): List<User>
}
