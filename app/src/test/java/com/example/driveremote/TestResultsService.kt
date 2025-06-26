package com.example.driveremote

import com.example.driveremote.api.ApiService
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Manager
import com.example.driveremote.models.Request
import com.example.driveremote.models.Results
import com.example.driveremote.models.User

class TestResultsService : ApiService {
    private val results = mutableListOf<Results>()

    override suspend fun getResults(): List<Results> = results

    override suspend fun getResultsByUser(userId: Int): List<Results> =
        results.filter { it.userId == userId }

    override suspend fun getLastResultByUser(userId: Int): Results? =
        results.filter { it.userId == userId }.maxByOrNull { it.testDate }

    override suspend fun addResult(result: Results): Results {
        results.add(result)
        return result
    }

    override suspend fun getAllRequests(): List<Request> {
        TODO("Not yet implemented")
    }

    override suspend fun getRequestsBySender(sender: Int): List<Request> {
        TODO("Not yet implemented")
    }

    override suspend fun getRequestsByReceiver(receiver: Int): List<Request> {
        TODO("Not yet implemented")
    }

    override suspend fun createRequest(request: Request): Request {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRequest(id: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun getDriverById(id: Int): Driver {
        TODO("Not yet implemented")
    }

    override suspend fun getDriverByUserId(userId: Int): Driver {
        TODO("Not yet implemented")
    }

    override suspend fun saveDriver(driver: Driver): Driver {
        TODO("Not yet implemented")
    }

    override suspend fun updateDriver(id: Int, driver: Driver): Driver {
        TODO("Not yet implemented")
    }

    override suspend fun getManagerById(id: Int): Manager? {
        TODO("Not yet implemented")
    }

    override suspend fun saveManager(manager: Manager): Manager {
        TODO("Not yet implemented")
    }

    override suspend fun updateEmployeesList(id: Int, employeesList: List<Int>) {
        TODO("Not yet implemented")
    }

    // Stub implementations to satisfy interface
    override suspend fun getAllUsers(): List<User> = emptyList()
    override suspend fun getUserById(id: Int): User = throw NotImplementedError()
    override suspend fun createUser(user: User): User = throw NotImplementedError()
    override suspend fun loginUser(email: String, password: String): User? = null
}