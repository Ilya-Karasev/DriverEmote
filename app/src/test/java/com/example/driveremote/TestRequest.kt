package com.example.driveremote

import com.example.driveremote.models.Request
import com.example.driveremote.models.RequestDao

class TestRequest : RequestDao {
    private val requests = mutableListOf<Request>()

    override suspend fun insertRequest(request: Request) {
        requests.add(request)
    }

    override suspend fun deleteRequest(request: Request) {
        requests.removeIf { it.id == request.id }
    }

    override suspend fun getAllRequests(): List<Request> = requests

    override suspend fun getRequestsForReceiver(receiver: Int): List<Request> =
        requests.filter { it.receiver == receiver }

    override suspend fun getRequestsForSender(sender: Int): List<Request> =
        requests.filter { it.sender == sender }
}