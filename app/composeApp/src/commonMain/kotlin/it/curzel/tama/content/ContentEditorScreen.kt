package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposerScreen
import it.curzel.tama.pixeleditor.PixelEditorScreen
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton

enum class EditorScreen {
    Main,
    MidiComposer,
    PixelEditor
}

@Composable
fun ContentEditorScreen(
    onSubScreenChange: (Boolean) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(EditorScreen.Main) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var checkLoginTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(currentScreen) {
        onSubScreenChange(currentScreen != EditorScreen.Main)
    }

    LaunchedEffect(checkLoginTrigger) {
        val channel = ConfigStorage.loadChannelInfo()
        val token = ConfigStorage.loadToken()
        isLoggedIn = channel != null && token != null
    }

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = {
            checkLoginTrigger++
        })
        return
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    when (currentScreen) {
        EditorScreen.Main -> MainEditorScreen(
            onNavigateToMidi = { currentScreen = EditorScreen.MidiComposer },
            onNavigateToPixel = { currentScreen = EditorScreen.PixelEditor },
            refreshTrigger = refreshTrigger
        )

        EditorScreen.MidiComposer -> MidiComposerScreen(
            onBack = {
                currentScreen = EditorScreen.Main
                refreshTrigger++
            }
        )

        EditorScreen.PixelEditor -> PixelEditorScreen(
            onBack = {
                currentScreen = EditorScreen.Main
                refreshTrigger++
            }
        )
    }
}

@Composable
fun MainEditorScreen(
    onNavigateToMidi: () -> Unit,
    onNavigateToPixel: () -> Unit,
    refreshTrigger: Int = 0
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MyNavigationBar(
            title = "Content Editor"
        )

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContentPreviewSection(refreshTrigger = refreshTrigger)

            EditorToolsSection(
                onNavigateToMidi = onNavigateToMidi,
                onNavigateToPixel = onNavigateToPixel
            )
        }
    }
}

@Composable
fun EditorToolsSection(
    onNavigateToMidi: () -> Unit,
    onNavigateToPixel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedCard(
            onClick = onNavigateToMidi,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Edit Audio",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create music for your content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(
            onClick = onNavigateToPixel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Edit Animation",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Draw and animate pixel art",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(
            onClick = { /* TODO: Handle publish */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Publish",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share your content with the world",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
