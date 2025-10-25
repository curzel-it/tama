package it.curzel.tama.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelInfo(
    val id: Long,
    val name: String
)

@Serializable
data class ContentData(
    val id: Long,
    val art: String,
    @SerialName("midi_composition") val midiComposition: String,
    val fps: Float
)

@Serializable
data class FeedItem(
    val channel: ChannelInfo,
    val content: ContentData
)

@Serializable
data class ChannelResponse(
    val id: Long,
    val name: String,
    val contents: List<ContentData>
)

@Serializable
data class LoginRequest(
    @SerialName("channel_name") val channelName: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    @SerialName("channel_name") val channelName: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: Long,
    val channel: ChannelInfo
)

@Serializable
data class CreateContentRequest(
    @SerialName("channel_id") val channelId: Long,
    val name: String,
    val art: String,
    val midi: String,
    val fps: Float
)

@Serializable
data class CreateContentResponse(
    val id: Long,
    @SerialName("channel_id") val channelId: Long,
    val message: String
)
