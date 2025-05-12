package com.example.driveremote.models

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["sender"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["receiver"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sender"),
        Index("receiver")
    ]
)
data class Request(
    @PrimaryKey(autoGenerate = true) @SerializedName("id") val id: Int = 0,
    @SerializedName("sender") val sender: Int,
    @SerializedName("receiver") val receiver: Int
)

@Dao
interface RequestDao {
    @Insert
    suspend fun insertRequest(request: Request)

    @Delete
    suspend fun deleteRequest(request: Request)

    @Query("SELECT * FROM Request")
    suspend fun getAllRequests(): List<Request>

    @Query("SELECT * FROM Request WHERE receiver = :receiver")
    suspend fun getRequestsForReceiver(receiver: Int): List<Request>

    @Query("SELECT * FROM Request WHERE sender = :sender")
    suspend fun getRequestsForSender(sender: Int): List<Request>
}
