package com.example.driveremote

import com.example.driveremote.models.Driver
import com.example.driveremote.models.DriverDao

class TestDriver : DriverDao {
    private val drivers = mutableListOf<Driver>()

    override suspend fun insertDriver(driver: Driver) {
        drivers.removeAll { it.id == driver.id }
        drivers.add(driver)
    }

    override suspend fun getDriverById(id: Int): Driver? {
        return drivers.find { it.id == id }
    }

    override suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean) {
        drivers.find { it.id == id }?.let {
            val updated = it.copy(isCompleted = isCompleted)
            insertDriver(updated)
        }
    }

    override suspend fun updateQuantity(id: Int, quantity: Int) {
        drivers.find { it.id == id }?.let {
            val updated = it.copy(quantity = quantity)
            insertDriver(updated)
        }
    }

    override suspend fun updateStatus(id: Int, status: String) {
        drivers.find { it.id == id }?.let {
            val updated = it.copy(status = status)
            insertDriver(updated)
        }
    }
}