package it.curzel.tama.model

import kotlinx.serialization.Serializable

@Serializable
data class TamaConfig(
    val server_url: String = "",
    val servers: List<String> = emptyList(),
    val server_override: Boolean = false
) {
    fun validate(): Result<Unit> {
        if (server_url.isEmpty()) {
            return Result.failure(Exception("Server URL is empty"))
        }
        return Result.success(Unit)
    }
}
