package it.curzel.tama.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import it.curzel.tama.ContentEditorScreen
import it.curzel.tama.FeedScreen
import it.curzel.tama.screens.SettingsScreen

enum class Tab(val title: String) {
    Feed("Feed"),
    Create("Create"),
    Settings("Settings")
}

@Composable
fun TabNavigationScreen() {
    var selectedTab by remember { mutableStateOf(Tab.Feed) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Text(tab.title.first().toString()) },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                Tab.Feed -> FeedScreen()
                Tab.Create -> ContentEditorScreen()
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}
