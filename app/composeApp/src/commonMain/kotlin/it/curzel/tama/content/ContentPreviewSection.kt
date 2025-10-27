package it.curzel.tama.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import it.curzel.tama.canvas.PixelCanvasWithTv
import it.curzel.tama.canvas.parseAnimationContent
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.content.WipContent
import it.curzel.tama.midi.MidiComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import tama.composeapp.generated.resources.*

enum class PlaybackState {
    Stopped,
    Playing,
    Paused
}

@Composable
fun ContentPreviewSection(
    refreshTrigger: Int = 0,
    onContentStateChange: (Boolean) -> Unit = {}
) {
    var wipContent by remember { mutableStateOf<WipContent?>(null) }
    var playbackState by remember { mutableStateOf(PlaybackState.Stopped) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        wipContent = ContentWipUseCase.loadWipContent()
        playbackState = PlaybackState.Stopped
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                MidiComposer.stop()
            } catch (e: Exception) {
                println("Failed to stop audio: ${e.message}")
            }
        }
    }

    val content = wipContent
    val hasContent = content != null && (content.art.isNotBlank() || content.midi.isNotBlank())

    LaunchedEffect(hasContent) {
        onContentStateChange(hasContent)
    }

    ContentPreviewContent(
        content = content,
        hasContent = hasContent,
        playbackState = playbackState,
        onPlaybackStateChange = { playbackState = it },
        scope = scope
    )
}

@Composable
private fun ContentPreviewContent(
    content: WipContent?,
    hasContent: Boolean,
    playbackState: PlaybackState,
    onPlaybackStateChange: (PlaybackState) -> Unit,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (hasContent) "Here's what you were working on:" else "Let's create something!",
            style = MaterialTheme.typography.titleMedium
        )

        content?.let { contentData ->
            if (contentData.art.isNotBlank()) {
                PixelArtPreview(
                    art = contentData.art,
                    fps = contentData.fps,
                    midi = contentData.midi,
                    playbackState = playbackState,
                    onPlay = {
                        if (contentData.midi.isNotBlank()) {
                            scope.launch {
                                try {
                                    MidiComposer.stop()
                                    MidiComposer.play(contentData.midi, loop = true)
                                } catch (e: Exception) {
                                    println("Failed to play audio: ${e.message}")
                                }
                            }
                        }
                        onPlaybackStateChange(PlaybackState.Playing)
                    },
                    onPause = {
                        MidiComposer.stop()
                        onPlaybackStateChange(PlaybackState.Paused)
                    },
                    onStop = {
                        MidiComposer.stop()
                        onPlaybackStateChange(PlaybackState.Stopped)
                    }
                )
            }

            if (contentData.midi.isNotBlank() && contentData.art.isBlank()) {
                MidiPreview(
                    playbackState = playbackState,
                    onPlay = {
                        scope.launch {
                            try {
                                MidiComposer.stop()
                                MidiComposer.play(contentData.midi, loop = true)
                                onPlaybackStateChange(PlaybackState.Playing)
                            } catch (e: Exception) {
                                println("Failed to play MIDI: ${e.message}")
                            }
                        }
                    },
                    onPause = {
                        MidiComposer.stop()
                        onPlaybackStateChange(PlaybackState.Paused)
                    },
                    onStop = {
                        MidiComposer.stop()
                        onPlaybackStateChange(PlaybackState.Stopped)
                    }
                )
            }
        }
    }
}

@Composable
fun PixelArtPreview(
    art: String,
    fps: Float,
    midi: String,
    playbackState: PlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight)

            ControlledPixelContentView(
                content = art,
                fps = fps,
                playbackState = playbackState,
                modifier = Modifier.size(size)
            )
        }

        PlaybackControls(
            playbackState = playbackState,
            onPlay = onPlay,
            onPause = onPause,
            onStop = onStop
        )
    }
}

@Composable
fun ControlledPixelContentView(
    content: String,
    fps: Float,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    val animationData = remember(content) { parseAnimationContent(content) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(playbackState) {
        if (playbackState == PlaybackState.Stopped) {
            currentFrameIndex = 0
        }
    }

    LaunchedEffect(content, fps, playbackState) {
        if (playbackState == PlaybackState.Playing && animationData.frames.size > 1) {
            while (playbackState == PlaybackState.Playing) {
                delay((1000 / fps).toLong())
                currentFrameIndex = (currentFrameIndex + 1) % animationData.frames.size
            }
        }
    }

    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pixelColor = if (isLightMode) Color(0xFF081820) else Color(0xFF88C070)
    val backgroundColor = if (isLightMode) Color(0xFFF0FAF0) else Color(0xFF081820)

    val currentFrame = animationData.frames.getOrNull(currentFrameIndex)

    if (currentFrame != null && currentFrame.isNotEmpty()) {
        PixelCanvasWithTv(
            pixels = currentFrame,
            pixelColor = pixelColor,
            backgroundColor = backgroundColor,
            showGrid = false,
            modifier = modifier
        )
    }
}

@Composable
fun MidiPreview(
    playbackState: PlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MIDI Composition",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        PlaybackControls(
            playbackState = playbackState,
            onPlay = onPlay,
            onPause = onPause,
            onStop = onStop
        )
    }
}

@Composable
fun PlaybackControls(
    playbackState: PlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (playbackState) {
            PlaybackState.Stopped -> {
                PlaybackButton(
                    icon = Res.drawable.icon_start,
                    contentDescription = "Play",
                    onClick = onPlay
                )
            }
            PlaybackState.Playing -> {
                PlaybackButton(
                    icon = Res.drawable.icon_pause,
                    contentDescription = "Pause",
                    onClick = onPause
                )
                PlaybackButton(
                    icon = Res.drawable.icon_stop,
                    contentDescription = "Stop",
                    onClick = onStop
                )
            }
            PlaybackState.Paused -> {
                PlaybackButton(
                    icon = Res.drawable.icon_start,
                    contentDescription = "Resume",
                    onClick = onPlay
                )
                PlaybackButton(
                    icon = Res.drawable.icon_stop,
                    contentDescription = "Stop",
                    onClick = onStop
                )
            }
        }
    }
}

@Composable
private fun PlaybackButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp)
        )
    }
}
