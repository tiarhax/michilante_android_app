package com.tiarhax.michilante.ewm.storage

import kotlinx.serialization.Serializable

@Serializable
data class CameraListItem (
    val id: String,
    val name: String
)

interface ICameraRepository {
    suspend fun listCameras(): Result<List<CameraListItem>>;
}