package com.tiarhax.michilante.ewm.storage
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*

import kotlinx.serialization.json.Json
val baseUrl = "http://192.168.100.10:9096";
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
        } catch (e: PutCameraInputError) {
            Log.e("CameraRepository", e.toString());
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("CameraRepository", e.toString());
            Result.failure(e)
        }
    }

    override suspend fun putCamera(input: PutCameraInput): Result<PutCameraOutput> {
        return try {
            val camera = client.put("$baseUrl/cameras") {
                contentType(ContentType.Application.Json)
                setBody(input)
            }.body<PutCameraOutput>()
            Result.success(camera)
        } catch (e: Exception) {
            Log.e("CameraRepository.putCamera", e.toString());
            Result.failure(e)
        }
    }

}

class CameraRepositoryForPreview: ICameraRepository {
    override suspend fun listCameras(): Result<List<CameraListItem>> {
        val dummyCameras = listOf(
            CameraListItem(id = "1", name = "Front Door", sourceUrl = ""),
            CameraListItem(id = "2", name = "Backyard", sourceUrl = ""),
            CameraListItem(id = "3", name = "Garage", sourceUrl = "")
        )
        return Result.success(dummyCameras)
    }

    override suspend fun putCamera(input: PutCameraInput): Result<PutCameraOutput> {
        TODO("Not yet implemented")
    }
}