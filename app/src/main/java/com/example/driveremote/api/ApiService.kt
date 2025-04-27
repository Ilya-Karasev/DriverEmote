package com.example.driveremote.api

import com.example.driveremote.models.User
import com.example.driveremote.models.UserDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("users/login")
    suspend fun loginUser(@Query("email") email: String, @Query("password") password: String): UserDto?

    @POST("users")
    suspend fun createUser(@Body user: User): User?

    companion object {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
