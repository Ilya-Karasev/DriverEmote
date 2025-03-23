package com.example.driveremote.models

data class UserDto(
    val id: Long,
    val surName: String,
    val firstName: String,
    val fatherName: String?,
    val age: Int,
    val post: String,
    val email: String,
    val password: String
)

enum class Post {
    ВОДИТЕЛЬ,
    РУКОВОДИТЕЛЬ
}