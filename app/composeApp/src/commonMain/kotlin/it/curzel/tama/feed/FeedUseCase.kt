package it.curzel.tama.feed

import it.curzel.tama.api.ApiManager
import it.curzel.tama.api.FeedItem
import it.curzel.tama.model.TamaConfig
import it.curzel.tama.storage.ConfigStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object FeedUseCase {
    suspend fun loadFeedFromServers(
        onItemsLoaded: (List<FeedItem>) -> Unit,
        onServerLoading: (String) -> Unit,
        onServerCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val config = ConfigStorage.loadConfig() ?: TamaConfig()

            if (config.server_url.isEmpty()) {
                onError("Server URL not configured")
                return
            }

            val servers = if (config.server_override) {
                (listOf(config.server_url) + config.servers).distinct()
            } else {
                val client = ApiManager.getClient(config.server_url)
                val result = client.fetchServers()

                if (result.isFailure) {
                    onError("Failed to fetch server list: ${result.exceptionOrNull()?.message}")
                    return
                }

                val fetchedServers = result.getOrNull() ?: emptyList()

                val updatedConfig = config.copy(servers = fetchedServers)
                ConfigStorage.saveConfig(updatedConfig)

                (listOf(config.server_url) + fetchedServers).distinct()
            }

            coroutineScope {
                servers.forEach { serverUrl ->
                    launch {
                        onServerLoading(serverUrl)
                        try {
                            val client = ApiManager.getClient(serverUrl)
                            val feedResult = client.fetchFeed()

                            if (feedResult.isSuccess) {
                                val items = feedResult.getOrNull() ?: emptyList()
                                if (items.isNotEmpty()) {
                                    onItemsLoaded(items)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error fetching from $serverUrl: ${e.message}")
                        } finally {
                            onServerCompleted(serverUrl)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onError("Unexpected error: ${e.message}")
        }
    }
}
