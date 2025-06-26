package com.example.driveremote

import com.example.driveremote.models.Request

class TestRequestService {
    private val requests = mutableListOf<Request>()
    private var nextId = 1

    suspend fun createRequest(request: Request): Request {
        val saved = request.copy(id = nextId++)
        requests.add(saved)
        return saved
    }

    suspend fun deleteRequest(id: Int) {
        requests.removeIf { it.id == id }
    }

    suspend fun getAllRequests(): List<Request> = requests

    suspend fun getRequestsBySender(sender: Int): List<Request> =
        requests.filter { it.sender == sender }

    suspend fun getRequestsByReceiver(receiver: Int): List<Request> =
        requests.filter { it.receiver == receiver }
}