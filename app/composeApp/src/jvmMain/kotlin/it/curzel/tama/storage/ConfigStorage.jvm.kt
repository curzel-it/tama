package it.curzel.tama.storage

import it.curzel.tama.api.ChannelInfo
import it.curzel.tama.model.TamaConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

class ConfigStorageJvm : ConfigStorageProvider {
    private val configDir = File(System.getProperty("user.home"), ".tama")
    private val configFile = File(configDir, "config.json")
    private val tokenFile = File(configDir, "token.txt")
    private val channelFile = File(configDir, "channel.json")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveConfig(config: TamaConfig) {
        configDir.mkdirs()
        val jsonStr = json.encodeToString(config)
        configFile.writeText(jsonStr)
    }

    override suspend fun loadConfig(): TamaConfig? {
        return try {
            if (configFile.exists()) {
                val jsonStr = configFile.readText()
                json.decodeFromString<TamaConfig>(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearConfig() {
        if (configFile.exists()) {
            configFile.delete()
        }
    }

    override suspend fun saveToken(token: String) {
        configDir.mkdirs()
        tokenFile.writeText(token)
    }

    override suspend fun loadToken(): String? {
        return try {
            if (tokenFile.exists()) {
                tokenFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearToken() {
        if (tokenFile.exists()) {
            tokenFile.delete()
        }
    }

    override suspend fun saveChannelInfo(channel: ChannelInfo) {
        configDir.mkdirs()
        val jsonStr = json.encodeToString(channel)
        channelFile.writeText(jsonStr)
    }

    override suspend fun loadChannelInfo(): ChannelInfo? {
        return try {
            if (channelFile.exists()) {
                val jsonStr = channelFile.readText()
                json.decodeFromString<ChannelInfo>(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearChannelInfo() {
        if (channelFile.exists()) {
            channelFile.delete()
        }
    }
}
