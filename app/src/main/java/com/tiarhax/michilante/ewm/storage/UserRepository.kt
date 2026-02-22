package com.tiarhax.michilante.ewm.storage

import Auth0Manager
import android.content.Context
import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.coroutines.suspendCoroutine

private data class UserRepositoryError(
    override val message: String,
    val details: HashMap<String, List<String>>?
): Throwable()

class UserRepository(private val context: Context, private val authManager: Auth0Manager): IUserRepository {
    private val client = httpClient

    private fun getUnknownError(): UserRepositoryError {
        return UserRepositoryError(message = "An unknown error has occurred", details = null)
    }

    private suspend fun handleHttpResponse(i: HttpResponse): Result<HttpResponse> {
        if (i.status.isSuccess()) {
            return Result.success(i)
        }

        try {
            val err = i.body<UserRepositoryError>()
            return Result.failure(err)
        } catch(err: Throwable) {
            val e = UserRepositoryError(message = "An unknown error has occurred", details = null)
            return Result.failure(e)
        }
    }

    private suspend fun getToken(): String = suspendCoroutine { continuation ->
        authManager.getAccessToken { token, err ->
            if (err != null) {
                Log.e("UserRepository.getToken", err)
                continuation.resumeWith(Result.failure(Throwable("Authentication error")))
            } else {
                continuation.resumeWith(Result.success(token!!))
            }
        }
    }

    override suspend fun listUsers(): Result<List<UserResultItem>> {
        return try {
            val token = getToken()
            val response = client.get("$baseUrl/users") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            val handledResponse = handleHttpResponse(response).getOrThrow()
            val users = handledResponse.body<List<UserResultItem>>()
            Result.success(users)
        } catch (c: UserRepositoryError) {
            Result.failure(c)
        } catch (e: Throwable) {
            Log.e("UserRepository", e.stackTraceToString())
            return Result.failure(getUnknownError())
        }
    }
}

class UserRepositoryForPreview : IUserRepository {
    override suspend fun listUsers(): Result<List<UserResultItem>> {
        val dummyUsers = listOf(
            UserResultItem(userId = "1", email = "user1@example.com", name = "User One"),
            UserResultItem(userId = "2", email = "user2@example.com", name = "User Two"),
            UserResultItem(userId = "3", email = "user3@example.com", name = "User Three")
        )
        return Result.success(dummyUsers)
    }
}
