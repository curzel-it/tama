package it.curzel.tama.pixeleditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PixelCanvas(
    viewModel: PixelEditorViewModel,
    availableWidth: Dp,
    availableHeight: Dp,
    modifier: Modifier = Modifier
) {
    val frame = viewModel.currentFrame
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
    val cellSize = baseCellSize * viewModel.zoomLevel
    val canvasWidthPx = frame.width * cellSize
    val canvasHeightPx = frame.height * cellSize

    val centerOffsetX = (availableWidthPx - canvasWidthPx) / 2f
    val centerOffsetY = (availableHeightPx - canvasHeightPx) / 2f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (scrollDelta != 0f) {
                    val zoomFactor = if (scrollDelta > 0) 0.9f else 1.1f
                    val center = event.changes.firstOrNull()?.position
                    if (center != null) {
                        viewModel.onZoom(zoomFactor, center.x, center.y)
                    }
                }
            }
            .pointerInput(frame) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1f) {
                        viewModel.onZoom(zoom, centroid.x, centroid.y)
                    }
                    if (pan != Offset.Zero) {
                        viewModel.onPan(pan)
                    }
                }
            }
            .pointerInput(frame) {
                detectTapGestures { offset ->
                    val adjustedX = offset.x - centerOffsetX - viewModel.panOffset.x
                    val adjustedY = offset.y - centerOffsetY - viewModel.panOffset.y

                    if (adjustedX >= 0 && adjustedY >= 0 &&
                        adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                        val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                        val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                        val currentValue = frame.pixels[y][x]
                        viewModel.setPixel(x, y, !currentValue)
                    }
                }
            }
            .pointerInput(frame) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val adjustedX = offset.x - centerOffsetX - viewModel.panOffset.x
                        val adjustedY = offset.y - centerOffsetY - viewModel.panOffset.y

                        if (adjustedX >= 0 && adjustedY >= 0 &&
                            adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                            isDrawing = true
                            val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                            val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                            drawMode = !frame.pixels[y][x]
                            viewModel.setPixel(x, y, drawMode)
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDrawing) {
                            val adjustedX = change.position.x - centerOffsetX - viewModel.panOffset.x
                            val adjustedY = change.position.y - centerOffsetY - viewModel.panOffset.y

                            if (adjustedX >= 0 && adjustedY >= 0 &&
                                adjustedX < canvasWidthPx && adjustedY < canvasHeightPx) {
                                val x = (adjustedX / cellSize).toInt().coerceIn(0, frame.width - 1)
                                val y = (adjustedY / cellSize).toInt().coerceIn(0, frame.height - 1)
                                viewModel.setPixel(x, y, drawMode)
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
    ) {
        clipRect {
            val canvasOffsetX = centerOffsetX + viewModel.panOffset.x
            val canvasOffsetY = centerOffsetY + viewModel.panOffset.y

            val visibleStartX = maxOf(0, ((-canvasOffsetX) / cellSize).toInt())
            val visibleEndX = minOf(frame.width, ((-canvasOffsetX + size.width) / cellSize).toInt() + 1)
            val visibleStartY = maxOf(0, ((-canvasOffsetY) / cellSize).toInt())
            val visibleEndY = minOf(frame.height, ((-canvasOffsetY + size.height) / cellSize).toInt() + 1)

            drawRect(
                color = backgroundColor,
                topLeft = Offset(canvasOffsetX, canvasOffsetY),
                size = Size(canvasWidthPx, canvasHeightPx)
            )

            for (y in visibleStartY until visibleEndY) {
                for (x in visibleStartX until visibleEndX) {
                    if (frame.pixels[y][x]) {
                        drawRect(
                            color = pixelColor,
                            topLeft = Offset(
                                canvasOffsetX + x * cellSize,
                                canvasOffsetY + y * cellSize
                            ),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }

            for (x in visibleStartX..minOf(frame.width, visibleEndX)) {
                drawLine(
                    color = gridColor,
                    start = Offset(canvasOffsetX + x * cellSize, canvasOffsetY),
                    end = Offset(canvasOffsetX + x * cellSize, canvasOffsetY + canvasHeightPx),
                    strokeWidth = 1f
                )
            }
            for (y in visibleStartY..minOf(frame.height, visibleEndY)) {
                drawLine(
                    color = gridColor,
                    start = Offset(canvasOffsetX, canvasOffsetY + y * cellSize),
                    end = Offset(canvasOffsetX + canvasWidthPx, canvasOffsetY + y * cellSize),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
