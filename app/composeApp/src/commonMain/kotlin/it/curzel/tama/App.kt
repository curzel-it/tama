package it.curzel.tama

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import it.curzel.tama.model.ThemePreference
import it.curzel.tama.navigation.TabNavigationScreen
import it.curzel.tama.screens.UpdateRequiredScreen
import it.curzel.tama.theme.TamaTheme
import it.curzel.tama.theme.rememberThemePreference
import it.curzel.tama.version.VersionCheckResult
import it.curzel.tama.version.VersionUseCase
import kotlinx.coroutines.launch

@Composable
fun App(deepLinkContentId: Long? = null) {
    val themePreference by rememberThemePreference()
    val systemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> systemInDarkTheme
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    var versionCheckResult by remember { mutableStateOf<VersionCheckResult?>(null) }
    var versionCheckComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            val result = VersionUseCase.checkVersion()
            if (result.isSuccess) {
                versionCheckResult = result.getOrNull()
            }
            versionCheckComplete = true
        }
    }

    TamaTheme(darkTheme = darkTheme) {
        if (!versionCheckComplete) {
            return@TamaTheme
        }

        if (versionCheckResult?.updateRequired == true) {
            UpdateRequiredScreen(versionCheckResult!!)
        } else {
            TabNavigationScreen(deepLinkContentId = deepLinkContentId)
        }
    }
}