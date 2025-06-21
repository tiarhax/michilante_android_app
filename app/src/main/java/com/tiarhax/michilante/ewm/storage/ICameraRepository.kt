package com.tiarhax.michilante.ewm.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CameraListItem (
    val id: String,
    val name: String,
    @SerialName("source_url")
    val sourceUrl: String
)
@Serializable
data class PutCameraInput (

    val id: String?,
    val name: String,
    @SerialName("source_url")
    val sourceUrl: String
)
@Serializable
data class PutCameraOutput (
    val id: String,
    val name: String,
    @SerialName("source_url")
    val sourceUrl: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)
@Serializable
data class PutCameraInputErrorDetails (
    val name: List<String>?,
    @SerialName("source_url")
    val sourceUrl: List<String>?
) : Exception()
data class PutCameraInputError (
    override val message: String?,
    val details: PutCameraInputErrorDetails?
) : Exception()

interface ICameraRepository {
    suspend fun listCameras(): Result<List<CameraListItem>>;
    suspend fun putCamera(input: PutCameraInput): Result<PutCameraOutput>;
}