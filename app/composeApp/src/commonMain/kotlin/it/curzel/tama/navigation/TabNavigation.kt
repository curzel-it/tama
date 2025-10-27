package it.curzel.tama.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import it.curzel.tama.screens.ContentEditorScreen
import it.curzel.tama.feed.FeedScreen
import it.curzel.tama.screens.SettingsScreen
import it.curzel.tama.utils.isLandscape
import org.jetbrains.compose.resources.painterResource
import tama.composeapp.generated.resources.*

enum class Tab(val title: String) {
    Feed("Feed"),
    Create("Create"),
    Settings("Settings")
}

@Composable
fun TabNavigationScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Feed) }
    var isInSubScreen by rememberSaveable { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val isLandscapeMode = isLandscape()

    val selectedColor = Color(0xFF88C070)
    val unselectedColor = Color(0xFF4A6A40)

    val shouldHideTabBar = (isLandscapeMode && selectedTab == Tab.Feed) ||
                           (selectedTab == Tab.Create && isInSubScreen)

    Scaffold(
        bottomBar = {
          if (!shouldHideTabBar) {
            NavigationBar(
                containerColor = colorScheme.background,
                contentColor = colorScheme.onBackground
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val iconTint = if (isSelected) selectedColor else unselectedColor

                    NavigationBarItem(
                        icon = {
                            val iconRes = when (tab) {
                                Tab.Feed -> Res.drawable.icon_feed
                                Tab.Create -> Res.drawable.icon_add
                                Tab.Settings -> Res.drawable.icon_settings
                            }
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = tab.title,
                                colorFilter = ColorFilter.tint(iconTint),
                                modifier = Modifier.size(32.dp),
                            )
                        },
                        label = { Text(tab.title, color = iconTint) },
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedColor,
                            unselectedIconColor = unselectedColor,
                            selectedTextColor = selectedColor,
                            unselectedTextColor = unselectedColor,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
          }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(
            if (shouldHideTabBar) {
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateRightPadding(LayoutDirection.Ltr),
                    bottom = 0.dp
                )
            } else {
                paddingValues
            }
        )) {
            when (selectedTab) {
                Tab.Feed -> FeedScreen(isLandscape = isLandscapeMode)
                Tab.Create -> ContentEditorScreen(
                    onSubScreenChange = { inSubScreen -> isInSubScreen = inSubScreen }
                )
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}
