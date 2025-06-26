package com.example.driveremote

import com.example.driveremote.models.Results
import com.example.driveremote.models.ResultsDao

class TestResults : ResultsDao {
    private val results = mutableListOf<Results>()
    private var idCounter = 1

    override suspend fun insertResult(result: Results) {
        results.add(result.copy(id = idCounter++))
    }

    override suspend fun getResultsByUser(userId: Int): List<Results> =
        results.filter { it.userId == userId }.sortedByDescending { it.testDate }

    override suspend fun getLastResultByUser(userId: Int): Results? =
        results.filter { it.userId == userId }.maxByOrNull { it.testDate }
}
