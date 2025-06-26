package com.example.driveremote

import com.example.driveremote.models.User
import com.example.driveremote.models.UserDao

class TestUser : UserDao {
    private val users = mutableListOf<User>()
    private var idCounter = 1

    override suspend fun insertUser(user: User) {
        users.add(user.copy(id = idCounter++))
    }

    override suspend fun getAllUsers(): List<User> {
        return users
    }

    override suspend fun getUserByEmailAndPassword(email: String, password: String): User? {
        return users.find { it.email == email && it.password == password }
    }

    override suspend fun getUserById(id: Int): User? {
        return users.find { it.id == id }
    }
}