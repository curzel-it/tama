package it.curzel.tama.pixeleditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

private const val TAG = "PixelCanvas"

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

    // Center offset changes with zoom, so recalculate it
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
            .pointerInput(viewModel.currentTool, cellSize, centerOffsetX, centerOffsetY) {
                awaitEachGesture {
                    // State for drawing
                    var lastDrawnCell: Pair<Int, Int>? = null

                    // State for multi-touch
                    var previousDistance = 0f
                    var previousCentroid = Offset.Zero
                    var wasMultiTouch = false

                    // State for move tool
                    var lastMovePosition = Offset.Zero

                    println("[$TAG] Gesture started, tool=${viewModel.currentTool}")

                    // Wait for first touch down
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = firstDown.position
                    lastMovePosition = downPosition

                    // Process the down event for drawing
                    if (viewModel.currentTool == ToolType.PENCIL || viewModel.currentTool == ToolType.ERASER) {
                        val canvasX = (downPosition.x - centerOffsetX - viewModel.panOffset.x) / cellSize
                        val canvasY = (downPosition.y - centerOffsetY - viewModel.panOffset.y) / cellSize

                        println("[$TAG] First down: pos=$downPosition, canvas=($canvasX,$canvasY)")

                        if (canvasX >= 0 && canvasY >= 0 && canvasX < frame.width && canvasY < frame.height) {
                            val x = canvasX.toInt().coerceIn(0, frame.width - 1)
                            val y = canvasY.toInt().coerceIn(0, frame.height - 1)
                            val fillValue = viewModel.currentTool == ToolType.PENCIL

                            println("[$TAG] TAP DRAW: cell=($x,$y), fillValue=$fillValue")
                            viewModel.setPixel(x, y, fillValue)
                            lastDrawnCell = Pair(x, y)
                        }
                    }

                    // Track all events until all fingers are up
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            val activePointers = pointers.filter { it.pressed }

                            println("[$TAG] Pointers: total=${pointers.size}, active=${activePointers.size}")

                            when {
                                // Multi-touch (zoom/pan)
                                activePointers.size >= 2 -> {
                                    wasMultiTouch = true
                                    lastDrawnCell = null // Stop drawing

                                    val p1 = activePointers[0].position
                                    val p2 = activePointers[1].position
                                    val centroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                                    val distance = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2)).toFloat()

                                    if (previousDistance > 0f) {
                                        val zoomChange = distance / previousDistance
                                        if (zoomChange != 1f) {
                                            println("[$TAG] Zoom: $zoomChange")
                                            viewModel.onZoom(zoomChange, centroid.x, centroid.y)
                                        }

                                        val panDelta = centroid - previousCentroid
                                        val panDistance = sqrt(panDelta.x * panDelta.x + panDelta.y * panDelta.y)
                                        if (viewModel.zoomLevel > 1f && panDistance > 1f) {
                                            println("[$TAG] Pan: $panDelta")
                                            viewModel.onPan(panDelta)
                                        }
                                    }

                                    previousDistance = distance
                                    previousCentroid = centroid
                                    pointers.forEach { it.consume() }
                                }

                                // Single touch
                                activePointers.size == 1 && !wasMultiTouch -> {
                                    val pointer = activePointers[0]
                                    val position = pointer.position

                                    when (viewModel.currentTool) {
                                        ToolType.MOVE -> {
                                            val delta = position - lastMovePosition
                                            val distanceSquared = delta.x * delta.x + delta.y * delta.y
                                            if (distanceSquared > 25f) { // 5px threshold
                                                println("[$TAG] Move tool pan: $delta")
                                                viewModel.onPan(delta)
                                                lastMovePosition = position
                                            }
                                            pointer.consume()
                                        }

                                        ToolType.PENCIL, ToolType.ERASER -> {
                                            val canvasX = (position.x - centerOffsetX - viewModel.panOffset.x) / cellSize
                                            val canvasY = (position.y - centerOffsetY - viewModel.panOffset.y) / cellSize

                                            if (canvasX >= 0 && canvasY >= 0 && canvasX < frame.width && canvasY < frame.height) {
                                                val x = canvasX.toInt().coerceIn(0, frame.width - 1)
                                                val y = canvasY.toInt().coerceIn(0, frame.height - 1)
                                                val cell = Pair(x, y)

                                                if (cell != lastDrawnCell) {
                                                    val fillValue = viewModel.currentTool == ToolType.PENCIL
                                                    println("[$TAG] DRAG DRAW: cell=($x,$y)")
                                                    viewModel.setPixel(x, y, fillValue)
                                                    lastDrawnCell = cell
                                                }
                                            }
                                            pointer.consume()
                                        }
                                    }
                                }

                                // After multi-touch, ignore single touch
                                activePointers.size == 1 && wasMultiTouch -> {
                                    println("[$TAG] Ignoring single touch after multi-touch")
                                    pointers.forEach { it.consume() }
                                }

                                // All fingers up
                                activePointers.isEmpty() -> {
                                    println("[$TAG] All fingers up")
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[$TAG] Gesture exception: $e")
                    }

                    println("[$TAG] Gesture ended")
                }
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
