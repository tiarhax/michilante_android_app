package com.tiarhax.michilante.ewm.storage

import Auth0Manager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import androidx.datastore.preferences.preferencesDataStore
import com.tiarhax.michilante.BuildConfig
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeParseException



import kotlinx.serialization.json.Json
import java.time.ZoneId
import kotlin.coroutines.suspendCoroutine
import kotlin.math.exp
private data class AuthenticationError(val code: Int?) : Throwable()
private data class CameraRepositoryError(
    override val message: String,
    val details: HashMap<String, List<String>>?
): Throwable()
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings");
val baseUrl = BuildConfig.BASE_URL;
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


class CameraRepository (private val context: Context, private val authManager: Auth0Manager): ICameraRepository {
    private val client = httpClient

    private fun getUnknownError(): CameraRepositoryError {
        return CameraRepositoryError(message = "An unknown error has occurred", details = null)
    }
    private suspend fun handleHttpResponse(i: HttpResponse): Result<HttpResponse> {
        if (i.status.isSuccess()) {
            return Result.success(i);
        }

        try {
            val err = i.body<CameraRepositoryError>()
            return Result.failure(err)
        } catch(err: Throwable) {
            val e = CameraRepositoryError(message = "An unknown error has occurred", details = null)
            return Result.failure(e)
        }
    }


    private suspend fun getToken() : String = suspendCoroutine { continuation ->
        authManager.getAccessToken { token, err ->
            if (err != null) {
                Log.e("CameraRepository.getToken", err);
                continuation.resumeWith(Result.failure(AuthenticationError(401)))
            } else {
                continuation.resumeWith(Result.success(token!!))
            }
        }
    }

    override suspend fun listCameras(): Result<List<CameraListItem>> {
        return try {
            val token = getToken()
            val response = client.get("$baseUrl/cameras") {
                headers{
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
           };

            val handledResponse = handleHttpResponse(response).getOrThrow()
            val cameras = handledResponse.body<List<CameraListItem>>()
            Result.success(cameras)
        } catch (e: PutCameraInputError) {
            Log.e("CameraRepository", e.toString());
            Result.failure(e)
        } catch(c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput> {
        return try {
            val token = getToken()
            val response = client.post("$baseUrl/cameras") {
                headers{
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            val handledResponse = handleHttpResponse(response).getOrThrow()
            val camera = handledResponse.body<CreateCameraOutput>()
            Result.success(camera)
        } catch(c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun putCamera(id: String, cameraData: PutCameraInput): Result<PutCameraOutput> {
        return try {
            val token = getToken()
            val url = "$baseUrl/cameras/$id";
            Log.d("PutCameraURL", url);
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
        } catch (c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun deleteCamera(id: String): Result<Unit> {
        return try {
            val url = "$baseUrl/cameras/$id";
            val token = getToken()
            client.delete(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            Result.success(Unit)
        } catch (c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }

    override suspend fun getCameraStream(id: String): Result<CameraStream> {
        return try {

            val cameraStreamFromDataStore = getCameraStreamFromPreferences(id).getOrThrow();
            if (cameraStreamFromDataStore != null) {
                Result.success(cameraStreamFromDataStore)
            } else {
                getNewCameraStream(id)
            }
        } catch (e: PutCameraInputError) {
            Log.e("CameraRepository.getCameraStream", e.toString());
            Result.failure(e)
        } catch (c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }



    private suspend fun getCameraStreamFromPreferences(id: String): Result<CameraStream?> {
        val preferences = context.dataStore.data.first()
        return try {
            val key = "cameraStream/$id";
            val streamFromPreferences: String? = preferences[stringPreferencesKey(key)];
            if (streamFromPreferences != null) {

                val cameraStream : CameraStream = Json.decodeFromString(streamFromPreferences);
                val expirationDate = OffsetDateTime.parse(cameraStream.expirationDate);
                val now = OffsetDateTime.now(org.threeten.bp.ZoneId.of("UTC"))
                val eStr = expirationDate.toString();
                val nowStr = now.toString();
                if (expirationDate.isAfter(now)) {
                    Log.d("CameraRepository.getCameraStreamFromPreferences", "expiration date is before now $eStr $nowStr")
                    return Result.success(cameraStream)
                }

                return Result.success(null)
            } else {
                return Result.success(null)
            }

        } catch (e: PutCameraInputError) {
            Log.e("CameraRepository.getCameraStreamFromPreferences", e.toString());
            Result.failure(e)
        } catch (c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
        }
    }


    private suspend fun getNewCameraStream(id: String): Result<CameraStream> {
        return try {
            val url = "$baseUrl/cameras/$id/temp-stream";
            val token = getToken()
            Log.d("CameraRepository.getNewCameraStream", "url $url");

            val response = client.get(url) {
                headers{
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            val handledResponse = handleHttpResponse(response).getOrThrow();
            val cameraStreamUnparsed = handledResponse.body<String>()
            Log.d("CameraRepository.getNewCameraStream", "rawBody $cameraStreamUnparsed");
            val cameraStream = Json.decodeFromString<CameraStream>(cameraStreamUnparsed);
            Log.d("CameraRepository.getNewCameraStream", "it does passes request")
            val key = stringPreferencesKey("cameraStream/$id")
            context.dataStore.edit { preferences ->
                preferences[key] = Json.encodeToString(cameraStream)
            }
            Result.success(cameraStream)
        } catch (e: PutCameraInputError) {
            Log.e("CameraRepository.getNewCameraStream", e.toString());
            Result.failure(e)
        } catch(c: CameraRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("CameraRepository", e.stackTraceToString());
            return Result.failure(getUnknownError())
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

    override suspend fun createCamera(input: CreateCameraInput): Result<CreateCameraOutput> {
        TODO("Not yet implemented")
    }

    override suspend fun putCamera(
        id: String,
        cameraData: PutCameraInput
    ): Result<PutCameraOutput> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCamera(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getCameraStream(id: String): Result<CameraStream> {
        TODO("Not yet implemented")
    }

}