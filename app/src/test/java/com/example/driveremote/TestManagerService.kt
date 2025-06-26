package com.example.driveremote
import com.example.driveremote.api.ApiService
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Manager
import com.example.driveremote.models.Request
import com.example.driveremote.models.Results
import com.example.driveremote.models.User
class TestManagerService : ApiService {
    private val managers = mutableMapOf<Int, Manager>()
    override suspend fun getAllUsers(): List<User> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserById(id: Int): User {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(user: User): User {
        TODO("Not yet implemented")
    }

    override suspend fun loginUser(email: String, password: String): User? {
        TODO("Not yet implemented")
    }

    override suspend fun getResults(): List<Results> {
        TODO("Not yet implemented")
    }

    override suspend fun getResultsByUser(userId: Int): List<Results> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastResultByUser(userId: Int): Results? {
        TODO("Not yet implemented")
    }

    override suspend fun addResult(result: Results): Results {
        TODO("Not yet implemented")
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
        return managers[id]
    }

    override suspend fun saveManager(manager: Manager): Manager {
        managers[manager.id] = manager
        return manager
    }

    override suspend fun updateEmployeesList(id: Int, employeesList: List<Int>) {
        val existing = managers[id]
        if (existing != null) {
            managers[id] = existing.copy(employeesList = employeesList)
        }
    }
}