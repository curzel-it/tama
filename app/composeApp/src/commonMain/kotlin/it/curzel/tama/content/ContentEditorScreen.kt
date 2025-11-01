package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.content.PublishContentDialog
import it.curzel.tama.content.PublishContentViewModel
import it.curzel.tama.content.PublishState
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerScreen
import it.curzel.tama.pixeleditor.PixelEditorScreen
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class EditorScreen {
    Main,
    MidiComposer,
    PixelEditor
}

@Composable
fun ContentEditorScreen(
    onSubScreenChange: (Boolean) -> Unit = {},
    onNavigateToContent: (Long) -> Unit = {}
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

    DisposableEffect(Unit) {
        onDispose {
            try {
                MidiComposer.stop()
            } catch (e: Exception) {
                println("Failed to stop audio on screen dispose: ${e.message}")
            }
        }
    }

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = {
            checkLoginTrigger++
        })
        return
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentScreen) {
        // Stop audio when navigating away from main screen
        if (currentScreen != EditorScreen.Main) {
            try {
                MidiComposer.stop()
            } catch (e: Exception) {
                println("Failed to stop audio when navigating away: ${e.message}")
            }
        }
    }

    when (currentScreen) {
        EditorScreen.Main -> MainEditorScreen(
            onNavigateToMidi = { currentScreen = EditorScreen.MidiComposer },
            onNavigateToPixel = { currentScreen = EditorScreen.PixelEditor },
            refreshTrigger = refreshTrigger,
            onNavigateToContent = onNavigateToContent
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
    refreshTrigger: Int = 0,
    onNavigateToContent: (Long) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var hasContent by remember { mutableStateOf(false) }
    var contentTitle by remember { mutableStateOf("") }
    val publishViewModel = remember { PublishContentViewModel() }
    var localRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        localRefreshTrigger = refreshTrigger
        // Load title from WIP content
        val wipContent = ContentWipUseCase.loadWipContent()
        contentTitle = wipContent?.title ?: ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                OutlinedTextField(
                    value = contentTitle,
                    onValueChange = { newTitle ->
                        if (newTitle.length <= 50) {
                            contentTitle = newTitle
                            CoroutineScope(Dispatchers.Default).launch {
                                ContentWipUseCase.saveTitle(newTitle)
                            }
                        }
                    },
                    label = { Text("Title") },
                    placeholder = { Text("Enter content title (max 50 chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("${contentTitle.length}/50") }
                )

                key(localRefreshTrigger) {
                    ContentPreviewSection(
                        refreshTrigger = localRefreshTrigger,
                        onContentStateChange = { hasContent = it }
                    )
                }

                EditorToolsSection(
                    onNavigateToMidi = onNavigateToMidi,
                    onNavigateToPixel = onNavigateToPixel,
                    hasContent = hasContent,
                    onPublish = { publishViewModel.showConfirmation() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        PublishContentDialog(
            publishState = publishViewModel.publishState,
            onDismiss = { publishViewModel.dismissDialog() },
            onConfirm = { publishViewModel.publish() },
            onNavigateToContent = { contentId ->
                publishViewModel.dismissDialog()
                onNavigateToContent(contentId)
            },
            onShowToast = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    LaunchedEffect(publishViewModel.publishState) {
        if (publishViewModel.publishState is PublishState.Success) {
            localRefreshTrigger++
        }
    }
}

@Composable
fun EditorToolsSection(
    onNavigateToMidi: () -> Unit,
    onNavigateToPixel: () -> Unit,
    hasContent: Boolean,
    onPublish: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedCard(
            onClick = onNavigateToMidi,
            modifier = Modifier.fillMaxWidth().testTag("edit-audio-button")
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
            modifier = Modifier.fillMaxWidth().testTag("edit-animation-button")
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

        if (hasContent) {
            OutlinedCard(
                onClick = onPublish,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
}
