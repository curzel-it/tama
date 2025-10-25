package it.curzel.tama.pixeleditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton

@Composable
fun PixelEditorScreen(
    viewModel: PixelEditorViewModel = remember { PixelEditorViewModel() },
    onBack: () -> Unit
) {
    var showKebabMenu by remember { mutableStateOf(false) }

    AnimatedPreviewPanel(
        frames = viewModel.frames,
        fps = viewModel.fps,
        charWidth = viewModel.charWidth,
        charHeight = viewModel.charHeight,
        visible = viewModel.showPreview,
        onClose = { viewModel.togglePreview() }
    )

    if (viewModel.showToolsMenu) {
        ToolsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeToolsMenu() }
        )
    }

    if (viewModel.showSettingsMenu) {
        CanvasSettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeSettingsMenu() }
        )
    }

    if (viewModel.showFramesMenu) {
        FramesDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeFramesMenu() }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box {
            MyNavigationBar(
                title = "Pixel Art Editor",
                onBackClick = onBack
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { showKebabMenu = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }

                DropdownMenu(
                    expanded = showKebabMenu,
                    onDismissRequest = { showKebabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tools") },
                        onClick = {
                            showKebabMenu = false
                            viewModel.openToolsMenu()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Canvas Settings") },
                        onClick = {
                            showKebabMenu = false
                            viewModel.openSettingsMenu()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Frames") },
                        onClick = {
                            showKebabMenu = false
                            viewModel.openFramesMenu()
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            FullScreenCanvasWithControls(viewModel)
        }
    }
}

@Composable
private fun FullScreenCanvasWithControls(viewModel: PixelEditorViewModel) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            PixelCanvas(
                frame = viewModel.currentFrame,
                onPixelChange = { x, y, value ->
                    viewModel.setPixel(x, y, value)
                },
                availableWidth = maxWidth,
                availableHeight = maxHeight,
                zoomLevel = viewModel.zoomLevel,
                panOffset = viewModel.panOffset,
                onPanOffsetChange = { viewModel.updatePanOffset(it) },
                isPanMode = viewModel.isPanMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.togglePanMode() },
                containerColor = if (viewModel.isPanMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(if (viewModel.isPanMode) "Draw" else "Pan")
            }

            FloatingActionButton(
                onClick = { viewModel.zoomIn() }
            ) {
                Text("+")
            }

            FloatingActionButton(
                onClick = { viewModel.zoomOut() }
            ) {
                Text("-")
            }

            FloatingActionButton(
                onClick = { viewModel.resetZoom() }
            ) {
                Text("Reset")
            }
        }

        Text(
            text = "Zoom: ${kotlin.math.round(viewModel.zoomLevel * 10) / 10.0}x",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ToolsDialog(
    viewModel: PixelEditorViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tools") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TamaButton(
                    onClick = {
                        viewModel.clearCurrentFrame()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.currentFrame != null
                ) {
                    Text("Clear Canvas")
                }

                TamaButton(
                    onClick = {
                        viewModel.fillCurrentFrame()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.currentFrame != null
                ) {
                    Text("Fill Canvas")
                }

                TamaButton(
                    onClick = {
                        viewModel.togglePreview()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.frames.isNotEmpty()
                ) {
                    Text(if (viewModel.showPreview) "Hide Preview" else "Show Preview")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CanvasSettingsDialog(
    viewModel: PixelEditorViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canvas Settings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.fps.toString(),
                    onValueChange = { value ->
                        value.toFloatOrNull()?.let { viewModel.updateFps(it) }
                    },
                    label = { Text("FPS (1-30)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.canvasWidth.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { viewModel.updateCanvasWidth(it) }
                    },
                    label = { Text("Width (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.canvasHeight.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { viewModel.updateCanvasHeight(it) }
                    },
                    label = { Text("Height (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TamaButton(
                    onClick = { viewModel.resizeCanvas() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resize Canvas")
                }

                if (viewModel.validationError != null) {
                    Text(
                        text = viewModel.validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Canvas: ${viewModel.canvasWidth}×${viewModel.canvasHeight} pixels (${viewModel.charWidth}×${viewModel.charHeight} chars)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FramesDialog(
    viewModel: PixelEditorViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frames") },
        text = {
            FrameListView(
                frames = viewModel.frames,
                currentFrameIndex = viewModel.currentFrameIndex,
                onFrameSelect = { index -> viewModel.selectFrame(index) },
                onFrameDelete = { index -> viewModel.deleteFrame(index) },
                onAddFrame = { viewModel.addFrame() }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
