package com.tiarhax.michilante.ewm.storage

import Auth0Manager
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlin.coroutines.suspendCoroutine

private data class CameraRepositoryV2Error(
    override val message: String,
    val details: HashMap<String, List<String>>?
): Throwable()

class CameraRepositoryV2(private val context: Context, private val authManager: Auth0Manager): ICameraRepositoryV2 {
    private val client = httpClient

    private fun getUnknownError(): CameraRepositoryV2Error {
        return CameraRepositoryV2Error(message = "An unknown error has occurred", details = null)
    }

    private suspend fun handleHttpResponse(i: HttpResponse): Result<HttpResponse> {
        if (i.status.isSuccess()) {
            return Result.success(i)
        }

        try {
            val err = i.body<CameraRepositoryV2Error>()
            return Result.failure(err)
        } catch(err: Throwable) {
            val e = CameraRepositoryV2Error(message = "An unknown error has occurred", details = null)
            return Result.failure(e)
        }
    }

    private suspend fun getToken(): String = suspendCoroutine { continuation ->
        authManager.getAccessToken { token, err ->
            if (err != null) {
                Log.e("CameraRepositoryV2.getToken", err)
                continuation.resumeWith(Result.failure(Throwable("Authentication error")))
            } else {
                continuation.resumeWith(Result.success(token!!))
            }
        }
    }

    override suspend fun listCameras(): Result<List<CameraListItemV2>> {
        return try {
            val token = getToken()
            val response = client.get("$baseUrl/v2/cameras") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            val handledResponse = handleHttpResponse(response).getOrThrow()
            val cameras = handledResponse.body<List<CameraListItemV2>>()
            Result.success(cameras)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput> {
        return try {
            val token = getToken()
            val response = client.post("$baseUrl/cameras") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            val handledResponse = handleHttpResponse(response).getOrThrow()
            val camera = handledResponse.body<CreateCameraOutput>()
            Result.success(camera)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun putCamera(id: String, cameraData: PutCameraInput): Result<PutCameraOutput> {
        return try {
            val token = getToken()
            val url = "$baseUrl/cameras/$id"
            Log.d("PutCameraURL", url)
            val response = client.put(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(cameraData)
            }

            val handledResponse = handleHttpResponse(response).getOrThrow()
            val camera = handledResponse.body<PutCameraOutput>()
            Result.success(camera)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun deleteCamera(id: String): Result<Unit> {
        return try {
            val url = "$baseUrl/cameras/$id"
            val token = getToken()
            client.delete(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            Result.success(Unit)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun getCameraStream(id: String): Result<CameraStream> {
        return try {
            val cameraStreamFromDataStore = getCameraStreamFromPreferences(id).getOrThrow()
            if (cameraStreamFromDataStore != null) {
                Result.success(cameraStreamFromDataStore)
            } else {
                getNewCameraStream(id)
            }
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun createCameraTempBlocking(input: CreateCameraTempBlockingInput): Result<Unit> {
        return try {
            val token = getToken()
            val response = client.post("$baseUrl/cameras/temp-blocking") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            handleHttpResponse(response).getOrThrow()
            Result.success(Unit)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun listCameraTempBlockings(cameraId: String): Result<List<CameraTempBlockingItem>> {
        return try {
            val token = getToken()
            val url = "$baseUrl/cameras/$cameraId/temp-blockings"
            val response = client.get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            val handledResponse = handleHttpResponse(response).getOrThrow()
            val blockings = handledResponse.body<List<CameraTempBlockingItem>>()
            Result.success(blockings)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun deleteCameraTempBlocking(cameraId: String, userId: String): Result<Unit> {
        return try {
            val token = getToken()
            val url = "$baseUrl/cameras/$cameraId/temp-blockings/$userId"
            val response = client.delete(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            handleHttpResponse(response).getOrThrow()
            Result.success(Unit)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    private suspend fun getCameraStreamFromPreferences(id: String): Result<CameraStream?> {
        val preferences = context.dataStore.data.first()
        return try {
            val key = "cameraStream/$id"
            val streamFromPreferences: String? = preferences[stringPreferencesKey(key)]
            if (streamFromPreferences != null) {
                val cameraStream: CameraStream = Json.decodeFromString(streamFromPreferences)
                val expirationDate = OffsetDateTime.parse(cameraStream.expirationDate)
                val now = OffsetDateTime.now(org.threeten.bp.ZoneId.of("UTC"))
                if (expirationDate.isAfter(now)) {
                    return Result.success(cameraStream)
                }
                return Result.success(null)
            } else {
                return Result.success(null)
            }
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }

    private suspend fun getNewCameraStream(id: String): Result<CameraStream> {
        return try {
            val url = "$baseUrl/cameras/$id/temp-stream"
            val token = getToken()
            Log.d("CameraRepositoryV2.getNewCameraStream", "url $url")

            val response = client.get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            val handledResponse = handleHttpResponse(response).getOrThrow()
            val cameraStreamUnparsed = handledResponse.body<String>()
            Log.d("CameraRepositoryV2.getNewCameraStream", "rawBody $cameraStreamUnparsed")
            val cameraStream = Json.decodeFromString<CameraStream>(cameraStreamUnparsed)
            val key = stringPreferencesKey("cameraStream/$id")
            context.dataStore.edit { preferences ->
                preferences[key] = Json.encodeToString(cameraStream)
            }
            Result.success(cameraStream)
        } catch (c: CameraRepositoryV2Error) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepositoryV2", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }
}

class CameraRepositoryV2ForPreview : ICameraRepositoryV2 {
    override suspend fun listCameras(): Result<List<CameraListItemV2>> {
        val dummyCameras = listOf(
            CameraListItemV2(id = "1", name = "Front Door", sourceUrl = "", isAvailable = true, availableAt = null),
            CameraListItemV2(id = "2", name = "Backyard", sourceUrl = "", isAvailable = false, availableAt = "2026-02-22T10:00:00Z"),
            CameraListItemV2(id = "3", name = "Garage", sourceUrl = "", isAvailable = true, availableAt = null)
        )
        return Result.success(dummyCameras)
    }

    override suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput> {
        TODO("Not yet implemented")
    }

    override suspend fun putCamera(id: String, cameraData: PutCameraInput): Result<PutCameraOutput> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCamera(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getCameraStream(id: String): Result<CameraStream> {
        TODO("Not yet implemented")
    }

    override suspend fun createCameraTempBlocking(input: CreateCameraTempBlockingInput): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun listCameraTempBlockings(cameraId: String): Result<List<CameraTempBlockingItem>> {
        val dummyBlockings = listOf(
            CameraTempBlockingItem(
                id = "1",
                cameraId = cameraId,
                endDate = "2026-02-22T10:00:00Z",
                blockedUser = BlockedUser(userId = "user1", userName = "John Doe")
            )
        )
        return Result.success(dummyBlockings)
    }

    override suspend fun deleteCameraTempBlocking(cameraId: String, userId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
