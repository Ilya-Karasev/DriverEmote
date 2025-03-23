package com.example.driveremote.models

data class User(
    val surName: String,
    val firstName: String,
    val fatherName: String?,
    val age: Int,
    val post: String,
    val email: String,
    val password: String
)