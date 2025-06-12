package com.tiarhax.michilante.ewm.storage
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.*

import kotlinx.serialization.json.Json
val baseUrl = "http://192.168.100.9:9096";
val httpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}


class CameraRepository : ICameraRepository {
    private val client = httpClient
    override suspend fun listCameras(): Result<List<CameraListItem>> {
        return try {
           val cameras = client.get("$baseUrl/cameras").body<List<CameraListItem>>()
            Result.success(cameras)
        } catch (e: Exception) {
            Log.e("CameraRepository", e.toString());
            Result.failure(e)
        }
    }
        
}

class CameraRepositoryForPreview: ICameraRepository {
    override suspend fun listCameras(): Result<List<CameraListItem>> {
        val dummyCameras = listOf(
            CameraListItem(id = "1", name = "Front Door"),
            CameraListItem(id = "2", name = "Backyard"),
            CameraListItem(id = "3", name = "Garage")
        )
        return Result.success(dummyCameras)
    }
}