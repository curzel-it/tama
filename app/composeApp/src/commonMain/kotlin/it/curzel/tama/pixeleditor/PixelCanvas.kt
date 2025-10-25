package it.curzel.tama.pixeleditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
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
    availableWidth: Dp,
    availableHeight: Dp,
    zoomLevel: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onPanOffsetChange: (Offset) -> Unit = {},
    isPanMode: Boolean = false
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
    val availableHeightPx = with(density) { availableHeight.toPx() }

    val baseCellSize = minOf(
        availableWidthPx / frame.width,
        availableHeightPx / frame.height
    )
    val cellSize = baseCellSize * zoomLevel
    val canvasWidthPx = frame.width * cellSize
    val canvasHeightPx = frame.height * cellSize

    val centerOffsetX = (availableWidthPx - canvasWidthPx) / 2f
    val centerOffsetY = (availableHeightPx - canvasHeightPx) / 2f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(frame, isPanMode, zoomLevel, panOffset) {
                if (isPanMode) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPanOffsetChange(panOffset + dragAmount)
                    }
                } else {
                    detectTapGestures { offset ->
                        val adjustedX = offset.x - centerOffsetX - panOffset.x
                        val adjustedY = offset.y - centerOffsetY - panOffset.y

                        if (adjustedX >= 0 && adjustedY >= 0 &&
                            adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                            val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                            val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                            val currentValue = frame.pixels[y][x]
                            onPixelChange(x, y, !currentValue)
                        }
                    }
                }
            }
            .pointerInput(frame, isPanMode, zoomLevel, panOffset) {
                if (!isPanMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val adjustedX = offset.x - centerOffsetX - panOffset.x
                            val adjustedY = offset.y - centerOffsetY - panOffset.y

                            if (adjustedX >= 0 && adjustedY >= 0 &&
                                adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                                isDrawing = true
                                val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                                val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                                drawMode = !frame.pixels[y][x]
                                onPixelChange(x, y, drawMode)
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDrawing) {
                                val adjustedX = change.position.x - centerOffsetX - panOffset.x
                                val adjustedY = change.position.y - centerOffsetY - panOffset.y

                                if (adjustedX >= 0 && adjustedY >= 0 &&
                                    adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                                    val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                                    val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                                    onPixelChange(x, y, drawMode)
                                }
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
            }
    ) {
        drawRect(
            color = backgroundColor,
            topLeft = Offset(centerOffsetX + panOffset.x, centerOffsetY + panOffset.y),
            size = Size(canvasWidthPx, canvasHeightPx)
        )

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (frame.pixels[y][x]) {
                    drawRect(
                        color = pixelColor,
                        topLeft = Offset(
                            centerOffsetX + panOffset.x + x * cellSize,
                            centerOffsetY + panOffset.y + y * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }

        for (x in 0..frame.width) {
            drawLine(
                color = gridColor,
                start = Offset(centerOffsetX + panOffset.x + x * cellSize, centerOffsetY + panOffset.y),
                end = Offset(centerOffsetX + panOffset.x + x * cellSize, centerOffsetY + panOffset.y + canvasHeightPx),
                strokeWidth = 1f
            )
        }
        for (y in 0..frame.height) {
            drawLine(
                color = gridColor,
                start = Offset(centerOffsetX + panOffset.x, centerOffsetY + panOffset.y + y * cellSize),
                end = Offset(centerOffsetX + panOffset.x + canvasWidthPx, centerOffsetY + panOffset.y + y * cellSize),
                strokeWidth = 1f
            )
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
