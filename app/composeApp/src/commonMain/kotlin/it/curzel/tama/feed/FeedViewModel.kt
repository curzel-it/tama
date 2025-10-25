package it.curzel.tama.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.curzel.tama.api.FeedItem
import it.curzel.tama.midi.MidiComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedViewModel {
    companion object {
        private var sessionFeedLoaded = false
        private var sessionFeedItems = mutableListOf<FeedItem>()
        private var sessionConfigHash: Int? = null

        // Clear cached feed data (useful when settings change)
        fun clearSessionCache() {
            sessionFeedLoaded = false
            sessionFeedItems.clear()
            sessionConfigHash = null
        }
    }

    var feedItems by mutableStateOf<List<FeedItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var loadingServers by mutableStateOf<Set<String>>(emptySet())
        private set

    var currentIndex by mutableIntStateOf(0)
        private set

    var isShowingStatic by mutableStateOf(false)
        private set

    val currentItem: FeedItem?
        get() = feedItems.getOrNull(currentIndex)

    val hasPrevious: Boolean
        get() = feedItems.isNotEmpty()

    val hasNext: Boolean
        get() = feedItems.isNotEmpty()

    fun loadFeed() {
        CoroutineScope(Dispatchers.Default).launch {
            // Load config to check if it has changed
            val config = it.curzel.tama.storage.ConfigStorage.loadConfig()
                ?: it.curzel.tama.model.TamaConfig()
            val currentConfigHash = config.hashCode()

            // If config changed, clear cache
            if (sessionConfigHash != null && sessionConfigHash != currentConfigHash) {
                clearSessionCache()
            }

            // If feed was already loaded with same config, use cached data
            if (sessionFeedLoaded && sessionConfigHash == currentConfigHash) {
                feedItems = sessionFeedItems.toList()
                isLoading = false
                return@launch
            }

            sessionConfigHash = currentConfigHash

            FeedUseCase.loadFeedFromServers(
                onItemsLoaded = { items ->
                    feedItems = feedItems + items
                    sessionFeedItems.addAll(items)
                },
                onServerLoading = { server ->
                    loadingServers = loadingServers + server
                },
                onServerCompleted = { server ->
                    loadingServers = loadingServers - server
                    if (loadingServers.isEmpty()) {
                        isLoading = false
                        sessionFeedLoaded = true
                    }
                },
                onError = { message ->
                    errorMessage = message
                    isLoading = false
                }
            )
        }
    }

    fun showStatic() {
        isShowingStatic = true
    }

    fun hideStatic() {
        isShowingStatic = false
    }

    fun next() {
        if (feedItems.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % feedItems.size
        }
    }

    fun previous() {
        if (feedItems.isNotEmpty()) {
            currentIndex = if (currentIndex == 0) feedItems.size - 1 else currentIndex - 1
        }
    }

    fun navigateNext() {
        if (feedItems.isNotEmpty() && !isShowingStatic) {
            CoroutineScope(Dispatchers.Default).launch {
                showStatic()
                next()
                kotlinx.coroutines.delay(200)
                hideStatic()
            }
        }
    }

    fun navigatePrevious() {
        if (feedItems.isNotEmpty() && !isShowingStatic) {
            CoroutineScope(Dispatchers.Default).launch {
                showStatic()
                previous()
                kotlinx.coroutines.delay(200)
                hideStatic()
            }
        }
    }

    fun shareCurrentContent() {
        currentItem?.let { item ->
            // TODO: Implement platform-specific sharing
            // This will be implemented using expect/actual pattern
            println("Sharing content: ${item.channel.name} - ID: ${item.content.id}")
        }
    }

    fun reportCurrentContent(reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        currentItem?.let { item ->
            CoroutineScope(Dispatchers.Default).launch {
                val config = it.curzel.tama.storage.ConfigStorage.loadConfig()
                    ?: it.curzel.tama.model.TamaConfig()

                val client = it.curzel.tama.api.ApiManager.getClient(config.server_url)
                val result = client.reportContent(item.content.id, reason)

                if (result.isSuccess) {
                    it.curzel.tama.storage.ReportedContentStorage.addReportedContent(
                        item.serverUrl,
                        item.content.id
                    )

                    feedItems = feedItems.filterIndexed { index, _ -> index != currentIndex }
                    sessionFeedItems.removeAt(currentIndex)

                    if (currentIndex >= feedItems.size && feedItems.isNotEmpty()) {
                        currentIndex = feedItems.size - 1
                    }

                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Failed to report content")
                }
            }
        }
    }

    fun playCurrentAudio() {
        stopAudio()
        currentItem?.let { item ->
            if (!isShowingStatic && item.content.midiComposition.isNotBlank()) {
                try {
                    MidiComposer.play(item.content.midiComposition, loop = true)
                } catch (e: Exception) {
                    println("Failed to play audio: ${e.message}")
                }
            }
        }
    }

    fun stopAudio() {
        try {
            MidiComposer.stop()
        } catch (e: Exception) {
            println("Failed to stop audio: ${e.message}")
        }
    }

    fun onDispose() {
        stopAudio()
    }
}
