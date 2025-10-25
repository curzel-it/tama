package it.curzel.tama

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import it.curzel.tama.model.ThemePreference
import it.curzel.tama.navigation.TabNavigationScreen
import it.curzel.tama.theme.TamaTheme
import it.curzel.tama.theme.rememberThemePreference

@Composable
fun App() {
    val themePreference by rememberThemePreference()
    val systemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> systemInDarkTheme
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    TamaTheme(darkTheme = darkTheme) {
        TabNavigationScreen()
    }
}