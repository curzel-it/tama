package it.curzel.tama.content

import it.curzel.tama.api.ApiManager
import it.curzel.tama.api.CreateContentResponse
import it.curzel.tama.storage.ConfigStorage

object PublishContentUseCase {
    suspend fun publishContent(): Result<CreateContentResponse> {
        val wipContent = ContentWipUseCase.loadWipContent() ?: return Result.failure(
            Exception("No content to publish")
        )

        println("[PUBLISH] Loaded WIP content:")
        println("[PUBLISH]   title: '${wipContent.title}'")
        println("[PUBLISH]   art length: ${wipContent.art.length}")
        println("[PUBLISH]   midi: '${wipContent.midi}'")
        println("[PUBLISH]   midi length: ${wipContent.midi.length}")
        println("[PUBLISH]   fps: ${wipContent.fps}")

        if (wipContent.title.trim().isEmpty()) {
            println("[PUBLISH] Error: Title is empty")
            return Result.failure(Exception("Please enter a title for your content"))
        }

        val config = ConfigStorage.loadConfig() ?: return Result.failure(
            Exception("Configuration not found")
        )

        val channelInfo = ConfigStorage.loadChannelInfo() ?: return Result.failure(
            Exception("Not logged in. Please log in first.")
        )

        val token = ConfigStorage.loadToken() ?: return Result.failure(
            Exception("No authentication token found")
        )

        val apiClient = ApiManager.getClient(config.server_url)
        apiClient.setSessionToken(token)

        println("[PUBLISH] About to upload:")
        println("[PUBLISH]   channelId: ${channelInfo.id}")
        println("[PUBLISH]   name: '${wipContent.title.trim()}'")
        println("[PUBLISH]   art length: ${wipContent.art.length}")
        println("[PUBLISH]   midi: '${wipContent.midi}'")
        println("[PUBLISH]   fps: ${wipContent.fps}")

        val result = apiClient.uploadContent(
            channelId = channelInfo.id,
            name = wipContent.title.trim(),
            art = wipContent.art,
            midi = wipContent.midi,
            fps = wipContent.fps
        )

        if (result.isSuccess) {
            println("[PUBLISH] Success! Clearing WIP content.")
            ContentWipUseCase.storageProvider.clearContent()
        } else {
            println("[PUBLISH] Failed: ${result.exceptionOrNull()?.message}")
        }

        return result
    }
}
