package it.curzel.tama.model

import kotlinx.serialization.Serializable

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class TamaConfig(
    val server_url: String = "https://tama.curzel.it",
    val servers: List<String> = emptyList(),
    val server_override: Boolean = false,
    val theme: String = "SYSTEM"
) {
    fun validate(): Result<Unit> {
        if (server_url.isEmpty()) {
            return Result.failure(Exception("Server URL is empty"))
        }
        return Result.success(Unit)
    }

    fun getThemePreference(): ThemePreference {
        return try {
            ThemePreference.valueOf(theme)
        } catch (e: IllegalArgumentException) {
            ThemePreference.SYSTEM
        }
    }
}
