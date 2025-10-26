package it.curzel.tama.theme

import androidx.compose.runtime.*
import it.curzel.tama.model.ThemePreference
import it.curzel.tama.storage.ConfigStorage
import kotlinx.coroutines.launch

object ThemeManager {
    private val _themePreference = mutableStateOf(ThemePreference.SYSTEM)
    val themePreference: State<ThemePreference> = _themePreference

    suspend fun loadThemePreference() {
        val config = ConfigStorage.loadConfig()
        _themePreference.value = config?.getThemePreference() ?: ThemePreference.SYSTEM
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
        val currentConfig = ConfigStorage.loadConfig()
        if (currentConfig != null) {
            val updatedConfig = currentConfig.copy(theme = preference.name)
            ConfigStorage.saveConfig(updatedConfig)
        }
    }
}

@Composable
fun rememberThemePreference(): State<ThemePreference> {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ThemeManager.loadThemePreference()
    }

    return ThemeManager.themePreference
}
