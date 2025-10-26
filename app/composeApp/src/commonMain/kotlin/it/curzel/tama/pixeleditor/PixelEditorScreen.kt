package it.curzel.tama.pixeleditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
    if (viewModel.showSettingsMenu) {
        CanvasSettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeSettingsMenu() }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            PixelEditorLandscapeLayout(
                viewModel = viewModel,
                onBack = onBack
            )
        } else {
            PixelEditorPortraitLayout(
                viewModel = viewModel,
                onBack = onBack
            )
        }
    }
}

@Composable
fun PixelEditorLandscapeLayout(
    viewModel: PixelEditorViewModel,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EditorNavigationBar(onBack = onBack)

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.width(100.dp)
            ) {
                FrameListView(
                    frames = viewModel.frames,
                    currentFrameIndex = viewModel.currentFrameIndex,
                    onFrameSelect = { viewModel.selectFrame(it) },
                    onFrameDelete = { viewModel.deleteFrame(it) },
                    onAddFrame = { viewModel.addFrame() },
                    isLandscape = true,
                    canvasWidth = viewModel.canvasWidth,
                    canvasHeight = viewModel.canvasHeight,
                    fps = viewModel.fps
                )
            }

            CanvasWithZoomDisplay(
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            ToolsPanelView(
                isLandscape = true,
                currentTool = viewModel.currentTool,
                onSelectTool = { viewModel.selectTool(it) },
                onClearCanvas = { viewModel.clearCurrentFrame() },
                onFillCanvas = { viewModel.fillCurrentFrame() },
                onOpenCanvasSettings = { viewModel.openSettingsMenu() },
            )
        }
    }
}

@Composable
fun PixelEditorPortraitLayout(
    viewModel: PixelEditorViewModel,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EditorNavigationBar(onBack = onBack)

        ToolsPanelView(
            isLandscape = false,
            currentTool = viewModel.currentTool,
            onSelectTool = { viewModel.selectTool(it) },
            onClearCanvas = { viewModel.clearCurrentFrame() },
            onFillCanvas = { viewModel.fillCurrentFrame() },
            onOpenCanvasSettings = { viewModel.openSettingsMenu() },
        )

        CanvasWithZoomDisplay(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        FrameListView(
            frames = viewModel.frames,
            currentFrameIndex = viewModel.currentFrameIndex,
            onFrameSelect = { viewModel.selectFrame(it) },
            onFrameDelete = { viewModel.deleteFrame(it) },
            onAddFrame = { viewModel.addFrame() },
            isLandscape = false,
            canvasWidth = viewModel.canvasWidth,
            canvasHeight = viewModel.canvasHeight,
            fps = viewModel.fps
        )
    }
}

@Composable
fun EditorNavigationBar(onBack: () -> Unit) {
    MyNavigationBar(
        title = "Pixel Art Editor",
        onBackClick = onBack
    )
}

@Composable
fun CanvasWithZoomDisplay(
    viewModel: PixelEditorViewModel,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        PixelCanvas(
            viewModel = viewModel,
            availableWidth = maxWidth,
            availableHeight = maxHeight,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "Zoom: ${kotlin.math.round(viewModel.zoomLevel * 10) / 10.0}x",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
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

