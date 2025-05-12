package com.example.driveremote.api

import com.example.driveremote.models.Driver
import com.example.driveremote.models.Manager
import com.example.driveremote.models.Request
import com.example.driveremote.models.Results
import com.example.driveremote.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // ---------- USERS ----------
    @GET("/api/users")
    suspend fun getAllUsers(): List<User>

    @GET("/api/users/{id}")
    suspend fun getUserById(@Path("id") id: Int): User

    @POST("/api/users")
    suspend fun createUser(@Body user: User): User

    @GET("/api/users/login")
    suspend fun loginUser(
        @Query("email") email: String,
        @Query("password") password: String
    ): User?

    // ---------- RESULTS ----------
    @GET("/api/results")
    suspend fun getResults(): List<Results>

    @GET("/api/results/user/{userId}")
    suspend fun getResultsByUser(@Path("userId") userId: Int): List<Results>

    @GET("/api/results/user/{userId}/last")
    suspend fun getLastResultByUser(@Path("userId") userId: Int): Results?

    @POST("/api/results")
    suspend fun addResult(@Body result: Results): Results

    // ---------- REQUESTS ----------
    @GET("/api/requests")
    suspend fun getAllRequests(): List<Request>

    @GET("/api/requests/sender/{sender}")
    suspend fun getRequestsBySender(@Path("sender") sender: Int): List<Request>

    @GET("/api/requests/receiver/{receiver}")
    suspend fun getRequestsByReceiver(@Path("receiver") receiver: Int): List<Request>

    @POST("/api/requests")
    suspend fun createRequest(@Body request: Request): Request

    @DELETE("/api/requests/{id}")
    suspend fun deleteRequest(@Path("id") id: Int)

    // ---------- DRIVERS ----------
    @GET("/api/drivers/{id}")
    suspend fun getDriverById(@Path("id") id: Int): Driver

    @GET("/api/drivers/user/{userId}")
    suspend fun getDriverByUserId(@Path("userId") userId: Int): Driver

    @POST("/api/drivers")
    suspend fun saveDriver(@Body driver: Driver): Driver

    @PUT("/api/drivers/{id}")
    suspend fun updateDriver(@Path("id") id: Int, @Body driver: Driver): Driver

    // ---------- MANAGERS ----------
    @GET("/api/managers/{id}")
    suspend fun getManagerById(@Path("id") id: Int): Manager?

    @POST("/api/managers")
    suspend fun saveManager(@Body manager: Manager): Manager

    @PATCH("/api/managers/{id}/employees")
    suspend fun updateEmployeesList(
        @Path("id") id: Int,
        @Body employeesList: List<Int>
    )
}