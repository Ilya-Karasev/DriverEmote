package com.example.driveremote.models

import androidx.room.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["receiverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("senderId"),
        Index("receiverId")
    ]
)
data class Request(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int,
    val receiverId: Int
)

@Dao
interface RequestDao {
    @Insert
    suspend fun insertRequest(request: Request)

    @Delete
    suspend fun deleteRequest(request: Request)

    @Query("SELECT * FROM Request")
    suspend fun getAllRequests(): List<Request>

    @Query("SELECT * FROM Request WHERE receiverId = :receiverId")
    suspend fun getRequestsForReceiver(receiverId: Int): List<Request>

    @Query("SELECT * FROM Request WHERE senderId = :senderId")
    suspend fun getRequestsForSender(senderId: Int): List<Request>
}
