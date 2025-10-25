package it.curzel.tama.pixeleditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PixelCanvas(
    frame: PixelFrame?,
    onPixelChange: (x: Int, y: Int, value: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    availableWidth: Dp
) {
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pixelColor = if (isLightMode) Color(0xFF081820) else Color(0xFF88C070)
    val backgroundColor = if (isLightMode) Color(0xFFF0FAF0) else Color(0xFF081820)
    val gridColor = pixelColor.copy(alpha = 0.2f)

    var isDrawing by remember { mutableStateOf(false) }
    var drawMode by remember { mutableStateOf(true) }

    if (frame == null) {
        return
    }

    val density = LocalDensity.current
    val availableWidthPx = with(density) { availableWidth.toPx() }
    val cellSize = availableWidthPx / frame.width
    val canvasHeightPx = cellSize * frame.height
    val canvasHeight = with(density) { canvasHeightPx.toDp() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .pointerInput(frame) {
                detectTapGestures { offset ->
                    val x = (offset.x / cellSize).toInt().coerceIn(0, frame.width - 1)
                    val y = (offset.y / cellSize).toInt().coerceIn(0, frame.height - 1)
                    val currentValue = frame.pixels[y][x]
                    onPixelChange(x, y, !currentValue)
                }
            }
            .pointerInput(frame) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDrawing = true
                        val x = (offset.x / cellSize).toInt().coerceIn(0, frame.width - 1)
                        val y = (offset.y / cellSize).toInt().coerceIn(0, frame.height - 1)
                        drawMode = !frame.pixels[y][x]
                        onPixelChange(x, y, drawMode)
                    },
                    onDrag = { change, _ ->
                        if (isDrawing) {
                            val x = (change.position.x / cellSize).toInt().coerceIn(0, frame.width - 1)
                            val y = (change.position.y / cellSize).toInt().coerceIn(0, frame.height - 1)
                            onPixelChange(x, y, drawMode)
                        }
                    },
                    onDragEnd = {
                        isDrawing = false
                    },
                    onDragCancel = {
                        isDrawing = false
                    }
                )
            }
    ) {
        val canvasWidthPx = frame.width * cellSize
        val canvasHeightPx = frame.height * cellSize

        drawRect(
            color = backgroundColor,
            size = Size(canvasWidthPx, canvasHeightPx)
        )

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (frame.pixels[y][x]) {
                    drawRect(
                        color = pixelColor,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }

        for (x in 0..frame.width) {
            drawLine(
                color = gridColor,
                start = Offset(x * cellSize, 0f),
                end = Offset(x * cellSize, canvasHeightPx),
                strokeWidth = 1f
            )
        }
        for (y in 0..frame.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * cellSize),
                end = Offset(canvasWidthPx, y * cellSize),
                strokeWidth = 1f
            )
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
