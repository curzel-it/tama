package it.curzel.tama.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(
    private val baseUrl: String
) {
    private var sessionToken: String? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    fun setSessionToken(token: String) {
        sessionToken = token
    }

    fun getSessionToken(): String? = sessionToken

    fun clearSessionToken() {
        sessionToken = null
    }

    suspend fun fetchFeed(): Result<List<FeedItem>> = try {
        val response: List<FeedItem> = client.get("$baseUrl/feed").body()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun fetchContent(contentId: Long): Result<ContentData> = try {
        val response: ContentData = client.get("$baseUrl/content/$contentId").body()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun fetchChannel(channelIdentifier: String): Result<ChannelResponse> = try {
        val response: ChannelResponse = client.get("$baseUrl/channel/$channelIdentifier").body()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginOrSignup(channelName: String, password: String): Result<AuthResponse> = try {
        val request = LoginRequest(channelName = channelName, password = password)
        val response: AuthResponse = client.post("$baseUrl/auth/login-or-signup") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        sessionToken = response.token
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(channelName: String, password: String): Result<AuthResponse> = try {
        val request = RegisterRequest(channelName = channelName, password = password)
        val response: AuthResponse = client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        sessionToken = response.token
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun login(channelName: String, password: String): Result<AuthResponse> = try {
        val request = LoginRequest(channelName = channelName, password = password)
        val response: AuthResponse = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        sessionToken = response.token
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun uploadContent(
        channelId: Long,
        name: String,
        art: String,
        midi: String,
        fps: Float
    ): Result<CreateContentResponse> = try {
        val token = sessionToken
            ?: return Result.failure(Exception("No session token available. Please authenticate first."))

        val request = CreateContentRequest(
            channelId = channelId,
            name = name,
            art = art,
            midi = midi,
            fps = fps
        )

        val response: CreateContentResponse = client.post("$baseUrl/content") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(request)
        }.body()

        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun fetchServers(): Result<List<String>> = try {
        val response: List<String> = client.get("$baseUrl/servers").body()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun close() {
        client.close()
    }
}
