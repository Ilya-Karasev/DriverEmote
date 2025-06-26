package com.example.driveremote

import com.example.driveremote.models.Manager
import com.example.driveremote.models.ManagerDao
import com.example.driveremote.models.Post
import com.example.driveremote.models.User

class TestManager : ManagerDao {
    private val managers = mutableListOf<Manager>()

    override suspend fun insertManager(manager: Manager) {
        managers.removeAll { it.id == manager.id }
        managers.add(manager)
    }

    override suspend fun getManagerById(id: Int): Manager? {
        return managers.find { it.id == id }
    }

    override suspend fun updateEmployees(id: Int, employeesList: List<Int>) {
        val existing = managers.find { it.id == id }
        if (existing != null) {
            val updated = existing.copy(employeesList = employeesList)
            insertManager(updated)
        }
    }

    override suspend fun getUsersByIds(userIds: List<Int>): List<User> {
        // Возвращает список заглушек пользователей с указанными id
        return userIds.map { id ->
            User(
                id = id,
                surName = "Фамилия$id",
                firstName = "Имя$id",
                fatherName = "Отчество$id",
                age = 30,
                post = Post.ВОДИТЕЛЬ,
                email = "user$id@example.com",
                password = "password$id"
            )
        }
    }
}