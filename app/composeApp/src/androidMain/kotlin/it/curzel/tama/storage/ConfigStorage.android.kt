package it.curzel.tama.storage

import android.content.Context
import it.curzel.tama.api.ChannelInfo
import it.curzel.tama.model.TamaConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ConfigStorageAndroid(context: Context) : ConfigStorageProvider {
    private val prefs = context.getSharedPreferences("tama_config", Context.MODE_PRIVATE)
    private val configKey = "config_json"
    private val tokenKey = "auth_token"
    private val channelKey = "channel_info"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveConfig(config: TamaConfig) {
        val jsonStr = json.encodeToString(config)
        prefs.edit().putString(configKey, jsonStr).apply()
    }

    override suspend fun loadConfig(): TamaConfig? {
        return try {
            val jsonStr = prefs.getString(configKey, null)
            if (jsonStr != null) {
                json.decodeFromString<TamaConfig>(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearConfig() {
        prefs.edit().remove(configKey).apply()
    }

    override suspend fun saveToken(token: String) {
        prefs.edit().putString(tokenKey, token).apply()
    }

    override suspend fun loadToken(): String? {
        return try {
            prefs.getString(tokenKey, null)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearToken() {
        prefs.edit().remove(tokenKey).apply()
    }

    override suspend fun saveChannelInfo(channel: ChannelInfo) {
        val jsonStr = json.encodeToString(channel)
        prefs.edit().putString(channelKey, jsonStr).apply()
    }

    override suspend fun loadChannelInfo(): ChannelInfo? {
        return try {
            val jsonStr = prefs.getString(channelKey, null)
            if (jsonStr != null) {
                json.decodeFromString<ChannelInfo>(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearChannelInfo() {
        prefs.edit().remove(channelKey).apply()
    }
}
