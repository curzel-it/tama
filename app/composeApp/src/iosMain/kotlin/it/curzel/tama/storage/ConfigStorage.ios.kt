package it.curzel.tama.storage

import it.curzel.tama.api.ChannelInfo
import it.curzel.tama.model.TamaConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import platform.Foundation.NSUserDefaults

class ConfigStorageIos : ConfigStorageProvider {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val configKey = "tama_config"
    private val tokenKey = "tama_auth_token"
    private val channelKey = "tama_channel_info"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveConfig(config: TamaConfig) {
        val jsonStr = json.encodeToString(config)
        userDefaults.setObject(jsonStr, configKey)
        userDefaults.synchronize()
    }

    override suspend fun loadConfig(): TamaConfig? {
        return try {
            val jsonStr = userDefaults.stringForKey(configKey)
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
        userDefaults.removeObjectForKey(configKey)
        userDefaults.synchronize()
    }

    override suspend fun saveToken(token: String) {
        userDefaults.setObject(token, tokenKey)
        userDefaults.synchronize()
    }

    override suspend fun loadToken(): String? {
        return try {
            userDefaults.stringForKey(tokenKey)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearToken() {
        userDefaults.removeObjectForKey(tokenKey)
        userDefaults.synchronize()
    }

    override suspend fun saveChannelInfo(channel: ChannelInfo) {
        val jsonStr = json.encodeToString(channel)
        userDefaults.setObject(jsonStr, channelKey)
        userDefaults.synchronize()
    }

    override suspend fun loadChannelInfo(): ChannelInfo? {
        return try {
            val jsonStr = userDefaults.stringForKey(channelKey)
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
        userDefaults.removeObjectForKey(channelKey)
        userDefaults.synchronize()
    }
}
