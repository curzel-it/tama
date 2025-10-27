package it.curzel.tama.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tama.pixeleditor.BrailleConverter
import it.curzel.tama.theme.firaCodeFontFamily
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.max

const val TV_WIDTH = 32
const val TV_HEIGHT = 10

data class AnimationData(
    val frames: List<Array<BooleanArray>>,
    val charWidth: Int,
    val charHeight: Int,
    val fps: Float
)

/**
 * Parse animation content from braille string
 * Format: "Ascii Art Animation, {width}x{height}, {fps}fps\n" followed by braille frames
 */
fun parseAnimationContent(content: String): AnimationData {
    val lines = content.split('\n')

    if (lines.isEmpty()) {
        return AnimationData(emptyList(), TV_WIDTH, TV_HEIGHT, 12f)
    }

    // Parse header
    val header = lines[0].trim()
    val dimensionMatch = Regex("""(\d+)x(\d+)""", RegexOption.IGNORE_CASE).find(header)
    val fpsMatch = Regex("""(\d+(?:\.\d+)?)fps""", RegexOption.IGNORE_CASE).find(header)

    val looksLikeHeader = dimensionMatch != null && (
        header.contains("ascii", ignoreCase = true) ||
        header.contains("animation", ignoreCase = true) ||
        header.matches(Regex("""\s*\d+x\d+.*""", RegexOption.IGNORE_CASE))
    )

    if (!looksLikeHeader) {
        // No valid header, treat as single frame with default dimensions
        val frames = BrailleConverter.brailleToPixels(content, TV_WIDTH, TV_HEIGHT)
        return AnimationData(frames, TV_WIDTH, TV_HEIGHT, 12f)
    }

    val charWidth = dimensionMatch?.groupValues?.get(1)?.toIntOrNull() ?: TV_WIDTH
    val charHeight = dimensionMatch?.groupValues?.get(2)?.toIntOrNull() ?: TV_HEIGHT
    val fps = fpsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 12f

    // Extract frame content (skip header)
    val brailleContent = lines.drop(1).joinToString("\n")
    val frames = BrailleConverter.brailleToPixels(brailleContent, charWidth, charHeight)

    return AnimationData(frames, charWidth, charHeight, fps)
}

/**
 * Main pixel-based content renderer with optional TV frame
 */
@Composable
fun PixelContentView(
    content: String,
    showTvFrame: Boolean = true,
    showGrid: Boolean = false,
    modifier: Modifier = Modifier,
    lightModeTextColor: Color = Color(0xFF081820),
    lightModeBackgroundColor: Color = Color(0xFFF0FAF0),
    darkModeTextColor: Color = Color(0xFF88C070),
    darkModeBackgroundColor: Color = Color(0xFF081820)
) {
    val animationData = remember(content) { parseAnimationContent(content) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }

    // Animation controller
    LaunchedEffect(content, animationData.fps) {
        if (animationData.frames.size > 1) {
            while (true) {
                delay((1000 / animationData.fps).toLong())
                currentFrameIndex = (currentFrameIndex + 1) % animationData.frames.size
            }
        }
    }

    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pixelColor = if (isLightMode) lightModeTextColor else darkModeTextColor
    val backgroundColor = if (isLightMode) lightModeBackgroundColor else darkModeBackgroundColor

    val currentFrame = animationData.frames.getOrNull(currentFrameIndex)

    if (currentFrame == null || currentFrame.isEmpty()) {
        return
    }

    if (showTvFrame) {
        PixelCanvasWithTv(
            pixels = currentFrame,
            pixelColor = pixelColor,
            backgroundColor = backgroundColor,
            showGrid = showGrid,
            modifier = modifier
        )
    } else {
        PixelCanvasWithoutTv(
            pixels = currentFrame,
            pixelColor = pixelColor,
            backgroundColor = backgroundColor,
            showGrid = showGrid,
            modifier = modifier
        )
    }
}

/**
 * Render pixel content with TV frame overlay
 */
@Composable
fun PixelCanvasWithTv(
    pixels: Array<BooleanArray>,
    pixelColor: Color,
    backgroundColor: Color,
    showGrid: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val firaCode = firaCodeFontFamily()

    Canvas(modifier = modifier) {
        val pixelSize = size.width / 76f

        // Calculate content offset (centered in TV screen)
        val contentWidth = pixels.firstOrNull()?.size ?: 0
        val contentHeight = pixels.size
        val offsetX = 6f + (64f - contentWidth) / 2f
        val offsetY = 18f + (40f - contentHeight) / 2f

        // Clear background
        drawRect(
            color = backgroundColor,
            size = size
        )

        // Render pixels
        renderPixelsToCanvas(
            pixels = pixels,
            offsetX = offsetX,
            offsetY = offsetY,
            pixelSize = pixelSize,
            color = pixelColor
        )

        // Render grid overlay
        if (showGrid) {
            renderPixelGrid(
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                offsetX = offsetX,
                offsetY = offsetY,
                pixelSize = pixelSize,
                backgroundColor = backgroundColor
            )
        }

        // Render TV frame
        renderTvFrame(
            pixelSize = pixelSize,
            color = pixelColor,
            textMeasurer = textMeasurer,
            firaCode = firaCode
        )
    }
}

/**
 * Render pixel content without TV frame
 */
@Composable
fun PixelCanvasWithoutTv(
    pixels: Array<BooleanArray>,
    pixelColor: Color,
    backgroundColor: Color,
    showGrid: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentWidth = pixels.firstOrNull()?.size ?: 0
    val contentHeight = pixels.size

    Canvas(
        modifier = modifier
            .width((contentWidth).dp)
            .height((contentHeight).dp)
    ) {
        val pixelSize = if (contentWidth > 0) size.width / contentWidth else 1f

        // Clear background
        drawRect(
            color = backgroundColor,
            size = size
        )

        // Render pixels
        renderPixelsToCanvas(
            pixels = pixels,
            offsetX = 0f,
            offsetY = 0f,
            pixelSize = pixelSize,
            color = pixelColor
        )

        // Render grid overlay
        if (showGrid) {
            renderPixelGrid(
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                offsetX = 0f,
                offsetY = 0f,
                pixelSize = pixelSize,
                backgroundColor = backgroundColor
            )
        }
    }
}

/**
 * Draw individual pixels to canvas
 */
private fun DrawScope.renderPixelsToCanvas(
    pixels: Array<BooleanArray>,
    offsetX: Float,
    offsetY: Float,
    pixelSize: Float,
    color: Color
) {
    for (y in pixels.indices) {
        val row = pixels[y]
        for (x in row.indices) {
            if (row[x]) {
                drawRect(
                    color = color,
                    topLeft = Offset(
                        (offsetX + x) * pixelSize,
                        (offsetY + y) * pixelSize
                    ),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}

/**
 * Draw grid overlay
 */
private fun DrawScope.renderPixelGrid(
    contentWidth: Int,
    contentHeight: Int,
    offsetX: Float,
    offsetY: Float,
    pixelSize: Float,
    backgroundColor: Color
) {
    if (pixelSize <= 1f) return

    val strokeWidth = 1f

    for (x in 0..contentWidth) {
        drawLine(
            color = backgroundColor,
            start = Offset((offsetX + x) * pixelSize, offsetY * pixelSize),
            end = Offset((offsetX + x) * pixelSize, (offsetY + contentHeight) * pixelSize),
            strokeWidth = strokeWidth
        )
    }

    for (y in 0..contentHeight) {
        drawLine(
            color = backgroundColor,
            start = Offset(offsetX * pixelSize, (offsetY + y) * pixelSize),
            end = Offset((offsetX + contentWidth) * pixelSize, (offsetY + y) * pixelSize),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Draw TV frame overlay (rounded rectangles, antenna, label)
 */
private fun DrawScope.renderTvFrame(
    pixelSize: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    firaCode: androidx.compose.ui.text.font.FontFamily
) {
    val strokeWidth = max(floor(pixelSize / 2f), 1f)
    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    // Outer frame
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth, 12 * pixelSize + strokeWidth),
        size = Size(
            76 * pixelSize - pixelSize,
            56 * pixelSize - pixelSize
        ),
        cornerRadius = CornerRadius(pixelSize, pixelSize),
        style = stroke
    )

    // Inner frame
    drawRoundRect(
        color = color,
        topLeft = Offset(4 * pixelSize + strokeWidth, 16 * pixelSize + strokeWidth),
        size = Size(
            68 * pixelSize - pixelSize,
            44 * pixelSize - pixelSize
        ),
        cornerRadius = CornerRadius(pixelSize, pixelSize),
        style = stroke
    )

    // Antenna
    val antennaCenterX = 25 * pixelSize
    val antennaCenterY = 12 * pixelSize
    val antennaLeftEndX = 21 * pixelSize
    val antennaLeftEndY = 4 * pixelSize
    val antennaRightEndX = 31 * pixelSize
    val antennaRightEndY = 0 * pixelSize

    drawLine(
        color = color,
        start = Offset(antennaLeftEndX, antennaLeftEndY),
        end = Offset(antennaCenterX, antennaCenterY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(antennaCenterX, antennaCenterY),
        end = Offset(antennaRightEndX, antennaRightEndY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Label text
    val textStyle = TextStyle(
        fontFamily = firaCode,
        fontSize = (3 * pixelSize / density).sp,
        color = color
    )

    val textLayoutResult = textMeasurer.measure("Tama Tv", textStyle)
    val textWidth = textLayoutResult.size.width
    val textX = 38 * pixelSize - textWidth / 2
    val textY = 61 * pixelSize + strokeWidth

    drawText(
        textMeasurer = textMeasurer,
        text = "Tama Tv",
        topLeft = Offset(textX, textY),
        style = textStyle
    )
}

/**
 * Display content with TV frame (for feed)
 */
@Composable
fun AsciiContentWithTv(
    content: String,
    fps: Float = 12f,
    showGrid: Boolean = false,
    modifier: Modifier = Modifier,
    availableWidthDp: Dp? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = availableWidthDp ?: maxWidth

        Box(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasWidth) // Square aspect ratio for TV
        ) {
            PixelContentView(
                content = content,
                showTvFrame = true,
                showGrid = showGrid,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Display content without TV frame (for pixel editor)
 */
@Composable
fun AsciiContentWithoutTv(
    content: String,
    fps: Float = 12f,
    modifier: Modifier = Modifier
) {
    PixelContentView(
        content = content,
        showTvFrame = false,
        modifier = modifier
    )
}

/**
 * Extension function to calculate color luminance
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
