package com.tiarhax.michilante.ewm.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CameraListItemV2(
    val id: String,
    val name: String,
    @SerialName("source_url")
    val sourceUrl: String,
    @SerialName("is_available")
    val isAvailable: Boolean,
    @SerialName("available_at")
    val availableAt: String? = null
)

interface ICameraRepositoryV2 {
    suspend fun listCameras(): Result<List<CameraListItemV2>>
    suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput>
    suspend fun putCamera(id: String, cameraData: PutCameraInput): Result<PutCameraOutput>
    suspend fun deleteCamera(id: String): Result<Unit>
    suspend fun getCameraStream(id: String): Result<CameraStream>
    suspend fun createCameraTempBlocking(input: CreateCameraTempBlockingInput): Result<Unit>
    suspend fun listCameraTempBlockings(cameraId: String): Result<List<CameraTempBlockingItem>>
    suspend fun deleteCameraTempBlocking(cameraId: String, userId: String): Result<Unit>
}

@Serializable
data class BlockedUser(
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String
)

@Serializable
data class CameraTempBlockingItem(
    val id: String,
    @SerialName("camera_id")
    val cameraId: String,
    @SerialName("end_date")
    val endDate: String,
    @SerialName("blocked_user")
    val blockedUser: BlockedUser
)

@Serializable
data class CreateCameraTempBlockingInput(
    @SerialName("camera_id")
    val cameraId: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("user_ids")
    val userIds: List<String>
)
