package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.canvas.AsciiContentWithTv
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.content.WipContent
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.launch

@Composable
fun ContentPreviewSection(
    refreshTrigger: Int = 0
) {
    var wipContent by remember { mutableStateOf<WipContent?>(null) }
    var isPlayingPixelArt by remember { mutableStateOf(false) }
    var isPlayingMidi by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        wipContent = ContentWipUseCase.loadWipContent()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                MidiComposer.stop()
            } catch (e: Exception) {
                println("Failed to stop audio: ${e.message}")
            }
            isPlayingPixelArt = false
            isPlayingMidi = false
        }
    }

    val content = wipContent
    if (content == null || (content.art.isBlank() && content.midi.isBlank())) {
        return
    }

    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (content.art.isNotBlank()) {
            PixelArtPreview(
                art = content.art,
                fps = content.fps,
                midi = content.midi,
                isPlaying = isPlayingPixelArt,
                onPlayStop = {
                    if (isPlayingPixelArt) {
                        MidiComposer.stop()
                        isPlayingPixelArt = false
                        isPlayingMidi = false
                    } else {
                        if (content.midi.isNotBlank()) {
                            scope.launch {
                                try {
                                    MidiComposer.stop()
                                    MidiComposer.play(content.midi, loop = true)
                                    isPlayingPixelArt = true
                                    isPlayingMidi = false
                                } catch (e: Exception) {
                                    println("Failed to play pixel art audio: ${e.message}")
                                }
                            }
                        }
                    }
                }
            )
        }

        if (content.midi.isNotBlank() && content.art.isBlank()) {
            MidiPreview(
                isPlaying = isPlayingMidi,
                onPlayStop = {
                    if (isPlayingMidi) {
                        MidiComposer.stop()
                        isPlayingMidi = false
                    } else {
                        scope.launch {
                            try {
                                MidiComposer.stop()
                                MidiComposer.play(content.midi, loop = true)
                                isPlayingMidi = true
                                isPlayingPixelArt = false
                            } catch (e: Exception) {
                                println("Failed to play MIDI: ${e.message}")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PixelArtPreview(
    art: String,
    fps: Float,
    midi: String,
    isPlaying: Boolean,
    onPlayStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight)

            AsciiContentWithTv(
                content = art,
                fps = fps,
                availableWidthDp = size
            )
        }

        if (midi.isNotBlank()) {
            TamaButton(
                onClick = onPlayStop,
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                Text(if (isPlaying) "Stop Audio" else "Play Audio")
            }
        }
    }
}

@Composable
fun MidiPreview(
    isPlaying: Boolean,
    onPlayStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "MIDI Composition",
            style = MaterialTheme.typography.bodyMedium
        )

        TamaButton(
            onClick = onPlayStop,
            modifier = Modifier.widthIn(max = 200.dp)
        ) {
            Text(if (isPlaying) "Stop" else "Play")
        }
    }
}
