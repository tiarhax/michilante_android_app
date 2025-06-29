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
    val name: String,
    @SerialName("source_url")
    val sourceUrl: String
)

@Serializable
data class CreateCameraInput (
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
data class CreateCameraOutput (
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
@Serializable
data class CameraStream(
    @SerialName("camera_id")
    val cameraId: String,
    @SerialName("temp_rtsp_url")
    val tempRtspUrl: String,
    @SerialName("expiration_date")
    val expirationDate: String
)

interface ICameraRepository {
    suspend fun listCameras(): Result<List<CameraListItem>>;
    suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput>;
    suspend fun putCamera(id: String, cameraData: PutCameraInput): Result<PutCameraOutput>;
    suspend fun deleteCamera(id: String): Result<Unit>;
    suspend fun getCameraStream(id: String): Result<CameraStream>;
}