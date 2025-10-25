package it.curzel.tama.api

object ApiManager {
    private val clients = mutableMapOf<String, ApiClient>()

    fun getClient(serverUrl: String): ApiClient {
        return clients.getOrPut(serverUrl) {
            ApiClient(serverUrl)
        }
    }

    fun clearClients() {
        clients.clear()
    }
}
