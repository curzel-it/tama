package it.curzel.tama.storage

import it.curzel.tama.api.ChannelInfo
import it.curzel.tama.model.TamaConfig

interface ConfigStorageProvider {
    suspend fun saveConfig(config: TamaConfig)
    suspend fun loadConfig(): TamaConfig?
    suspend fun clearConfig()

    suspend fun saveToken(token: String)
    suspend fun loadToken(): String?
    suspend fun clearToken()

    suspend fun saveChannelInfo(channel: ChannelInfo)
    suspend fun loadChannelInfo(): ChannelInfo?
    suspend fun clearChannelInfo()
}

object ConfigStorage {
    lateinit var provider: ConfigStorageProvider

    suspend fun saveConfig(config: TamaConfig) = provider.saveConfig(config)
    suspend fun loadConfig(): TamaConfig? = provider.loadConfig()
    suspend fun clearConfig() = provider.clearConfig()

    suspend fun saveToken(token: String) = provider.saveToken(token)
    suspend fun loadToken(): String? = provider.loadToken()
    suspend fun clearToken() = provider.clearToken()

    suspend fun saveChannelInfo(channel: ChannelInfo) = provider.saveChannelInfo(channel)
    suspend fun loadChannelInfo(): ChannelInfo? = provider.loadChannelInfo()
    suspend fun clearChannelInfo() = provider.clearChannelInfo()
}
