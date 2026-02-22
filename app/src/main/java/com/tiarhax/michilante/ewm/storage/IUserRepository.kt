package com.tiarhax.michilante.ewm.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResultItem(
    @SerialName("user_id")
    val userId: String,
    val email: String,
    val name: String
)

interface IUserRepository {
    suspend fun listUsers(): Result<List<UserResultItem>>
}
