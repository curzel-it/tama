package it.curzel.tama.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.curzel.tama.api.FeedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedViewModel {
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
            FeedUseCase.loadFeedFromServers(
                onItemsLoaded = { items ->
                    feedItems = feedItems + items
                },
                onServerLoading = { server ->
                    loadingServers = loadingServers + server
                },
                onServerCompleted = { server ->
                    loadingServers = loadingServers - server
                    if (loadingServers.isEmpty()) {
                        isLoading = false
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
}
