package it.curzel.tama.content

import it.curzel.tama.api.ApiManager
import it.curzel.tama.api.CreateContentResponse
import it.curzel.tama.storage.ConfigStorage

object PublishContentUseCase {
    suspend fun publishContent(): Result<CreateContentResponse> {
        val wipContent = ContentWipUseCase.loadWipContent() ?: return Result.failure(
            Exception("No content to publish")
        )

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

        val result = apiClient.uploadContent(
            channelId = channelInfo.id,
            name = "",
            art = wipContent.art,
            midi = wipContent.midi,
            fps = wipContent.fps
        )

        if (result.isSuccess) {
            ContentWipUseCase.storageProvider.clearContent()
        }

        return result
    }
}
