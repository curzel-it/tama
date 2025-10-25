package it.curzel.tama

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import it.curzel.tama.navigation.TabNavigationScreen

@Composable
fun App() {
    MaterialTheme {
        TabNavigationScreen()
    }
}