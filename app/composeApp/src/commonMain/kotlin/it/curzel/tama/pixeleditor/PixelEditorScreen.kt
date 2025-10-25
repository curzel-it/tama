package it.curzel.tama.pixeleditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val scrollState = rememberScrollState()

    AnimatedPreviewPanel(
        frames = viewModel.frames,
        fps = viewModel.fps,
        charWidth = viewModel.charWidth,
        charHeight = viewModel.charHeight,
        visible = viewModel.showPreview,
        onClose = { viewModel.togglePreview() }
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "Pixel Art Editor",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CanvasSettingsSection(viewModel)

            DrawingCanvasSection(viewModel)

            ToolsSection(viewModel)

            FramesSection(viewModel)
        }
    }
}

@Composable
private fun CanvasSettingsSection(viewModel: PixelEditorViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Canvas Settings",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.canvasWidth.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { viewModel.updateCanvasWidth(it) }
                },
                label = { Text("Width (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.canvasHeight.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { viewModel.updateCanvasHeight(it) }
                },
                label = { Text("Height (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.fps.toString(),
                onValueChange = { value ->
                    value.toFloatOrNull()?.let { viewModel.updateFps(it) }
                },
                label = { Text("FPS (1-30)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            TamaButton(
                onClick = { viewModel.resizeCanvas() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Resize")
            }
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
}

@Composable
private fun DrawingCanvasSection(viewModel: PixelEditorViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Drawing Canvas",
            style = MaterialTheme.typography.titleLarge
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val availableWidth = maxWidth - 32.dp

            PixelCanvas(
                frame = viewModel.currentFrame,
                onPixelChange = { x, y, value ->
                    viewModel.setPixel(x, y, value)
                },
                availableWidth = availableWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ToolsSection(viewModel: PixelEditorViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TamaButton(
                onClick = { viewModel.clearCurrentFrame() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.currentFrame != null
            ) {
                Text("Clear")
            }

            TamaButton(
                onClick = { viewModel.fillCurrentFrame() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.currentFrame != null
            ) {
                Text("Fill")
            }

            TamaButton(
                onClick = { viewModel.togglePreview() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.frames.isNotEmpty()
            ) {
                Text(if (viewModel.showPreview) "Hide Preview" else "Preview")
            }
        }
    }
}

@Composable
private fun FramesSection(viewModel: PixelEditorViewModel) {
    FrameListView(
        frames = viewModel.frames,
        currentFrameIndex = viewModel.currentFrameIndex,
        onFrameSelect = { index -> viewModel.selectFrame(index) },
        onFrameDelete = { index -> viewModel.deleteFrame(index) },
        onAddFrame = { viewModel.addFrame() }
    )
}
