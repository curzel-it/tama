package it.curzel.tama.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tama.theme.firaCodeFontFamily
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor

const val TV_WIDTH = 32
const val TV_HEIGHT = 10
const val SPACE = ' '

const val CHAR_WIDTH = 9f
const val CHAR_HEIGHT = 18f
const val CHARACTER_SPACING = 1.5f
const val LINE_SPACING = 3f

/**
 * Parse ASCII art sprite-sheet format.
 * Format: "Ascii Art Animation, {width}x{height}\n" followed by frames
 * or "ascii art animation, {width}x{height}\n" (case insensitive)
 */
fun parseFrames(artString: String): List<String> {
    val lines = artString.split('\n')

    if (lines.isEmpty()) {
        return listOf(artString)
    }

    // Parse header: Check if first line looks like a header with dimensions
    val header = lines[0].trim()
    val dimensionMatch = Regex("""(\d+)x(\d+)""", RegexOption.IGNORE_CASE).find(header)

    // Check if this line is actually a header (contains "ascii" or "animation" or just dimensions)
    val looksLikeHeader = dimensionMatch != null && (
        header.contains("ascii", ignoreCase = true) ||
        header.contains("animation", ignoreCase = true) ||
        header.matches(Regex("""\s*\d+x\d+\s*""", RegexOption.IGNORE_CASE))
    )

    if (!looksLikeHeader) {
        // No valid header, treat as single frame
        return listOf(artString)
    }

    val height = dimensionMatch!!.groupValues[2].toInt()

    // Extract frame lines (skip header)
    val frameLines = lines.drop(1)

    // Split into individual frames
    val frames = mutableListOf<String>()
    var i = 0
    while (i < frameLines.size) {
        val frameContent = frameLines.subList(i, (i + height).coerceAtMost(frameLines.size))
            .joinToString("\n")
        if (frameContent.isNotBlank()) {
            frames.add(frameContent)
        }
        i += height
    }

    return frames.ifEmpty { listOf(artString) }
}

/**
 * Pad text to the right with spaces
 */
fun paddedRight(text: String, count: Int): String {
    if (count <= text.length) return text
    val padding = count - text.length
    return text + SPACE.toString().repeat(padding)
}

/**
 * Pad text on both sides to center it
 */
fun padded(text: String, count: Int): String {
    if (count <= text.length) return text
    val totalPadding = count - text.length
    val left = floor(totalPadding / 2.0).toInt()
    val right = ceil(totalPadding / 2.0).toInt()
    return SPACE.toString().repeat(left) + text + SPACE.toString().repeat(right)
}

/**
 * Format content to TV dimensions (TV_WIDTH x TV_HEIGHT)
 */
fun formatContentToLines(content: String): List<String> {
    // Replace various space characters with regular space
    val normalizedContent = content.replace('⠀', SPACE).replace(' ', SPACE)
    val lines = normalizedContent.split('\n')
    val contentWidth = lines.maxOfOrNull { it.length } ?: 0

    val formattedLines = lines
        .map { paddedRight(it, contentWidth) }
        .map { padded(it, TV_WIDTH) }
        .toMutableList()

    // Ensure we always have TV_HEIGHT lines
    while (formattedLines.size < TV_HEIGHT) {
        formattedLines.add(SPACE.toString().repeat(TV_WIDTH))
    }

    return formattedLines.take(TV_HEIGHT)
}

/**
 * Render content wrapped in TV frame
 * The TV border is dynamically generated using TV_WIDTH and TV_HEIGHT constants
 */
fun renderContentToString(content: String): String {
    val formattedLines = formatContentToLines(content)

    // Build horizontal borders dynamically based on TV_WIDTH
    val outerHorizontal = "─".repeat(TV_WIDTH + 2)
    val innerHorizontal = "─".repeat(TV_WIDTH)

    // Build the TV label, centered
    val label = "Tama Tv"
    val labelLine = "│" + padded(label, TV_WIDTH + 2) + "│"

    // Build antenna (scaled to TV width)
    val antennaOffset = TV_WIDTH / 3
    val antenna = buildString {
        appendLine(" ".repeat(antennaOffset) + "╱")
        appendLine(" ".repeat(antennaOffset - 4) + "╲  ╱")
        appendLine(" ".repeat(antennaOffset - 3) + "╲╱")
    }.trimEnd()

    // Build content lines dynamically based on TV_HEIGHT
    val contentLines = buildString {
        for (i in 0 until TV_HEIGHT) {
            val line = formattedLines.getOrNull(i) ?: SPACE.toString().repeat(TV_WIDTH)
            appendLine("││$line││")
        }
    }.trimEnd()

    return """$antenna
╭$outerHorizontal╮
│╭$innerHorizontal╮│
$contentLines
│╰$innerHorizontal╯│
$labelLine
╰$outerHorizontal╯"""
}

/**
 * Main composable for displaying ASCII art content with optional animation
 *
 * @param content The ASCII art string (can be single frame or multi-frame format)
 * @param fps Frames per second for animation (default 12)
 * @param showTvFrame Whether to wrap content in TV border (default true)
 * @param modifier Modifier for styling
 * @param charWidth Width of each character in pixels
 * @param charHeight Height of each character in pixels
 * @param characterSpacing Spacing between characters
 * @param lineSpacing Spacing between lines
 * @param lightModeTextColor Text color in light mode
 * @param lightModeBackgroundColor Background color in light mode
 * @param darkModeTextColor Text color in dark mode
 * @param darkModeBackgroundColor Background color in dark mode
 */
@Composable
fun AsciiContentView(
    content: String,
    fps: Float = 12f,
    showTvFrame: Boolean = true,
    modifier: Modifier = Modifier,
    charWidth: Float = CHAR_WIDTH,
    charHeight: Float = CHAR_HEIGHT,
    characterSpacing: Float = CHARACTER_SPACING,
    lineSpacing: Float = LINE_SPACING,
    lightModeTextColor: Color = Color(0xFF081820),
    lightModeBackgroundColor: Color = Color(0xFFF0FAF0),
    darkModeTextColor: Color = Color(0xFF88C070),
    darkModeBackgroundColor: Color = Color(0xFF081820)
) {
    val frames = remember(content) { parseFrames(content) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }

    // Animation controller
    LaunchedEffect(content, fps) {
        if (frames.size > 1) {
            while (true) {
                delay((1000 / fps).toLong())
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }
    }

    val currentFrame = frames.getOrNull(currentFrameIndex) ?: content
    val displayText = if (showTvFrame) {
        renderContentToString(currentFrame)
    } else {
        currentFrame
    }

    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val textColor = if (isLightMode) lightModeTextColor else darkModeTextColor
    val backgroundColor = if (isLightMode) lightModeBackgroundColor else darkModeBackgroundColor

    AsciiCanvas(
        content = displayText,
        charWidth = charWidth,
        charHeight = charHeight,
        characterSpacing = characterSpacing,
        lineSpacing = lineSpacing,
        textColor = textColor,
        backgroundColor = backgroundColor,
        modifier = modifier
    )
}

/**
 * Canvas-based ASCII art renderer with precise character positioning
 * Renders each character individually at calculated pixel positions,
 * matching the JavaScript canvas implementation
 */
@Composable
fun AsciiCanvas(
    content: String,
    charWidth: Float,
    charHeight: Float,
    characterSpacing: Float,
    lineSpacing: Float,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    // Validate and sanitize input parameters to avoid constraint errors
    val safeCharWidth = charWidth.coerceAtLeast(1f)
    val safeCharHeight = charHeight.coerceAtLeast(1f)
    val safeCharSpacing = characterSpacing.coerceAtLeast(0f)
    val safeLineSpacing = lineSpacing.coerceAtLeast(0f)

    val textMeasurer = rememberTextMeasurer()
    val firaCode = firaCodeFontFamily()
    val lines = content.split('\n').filter { it.isNotEmpty() }
    val maxLineLength = lines.maxOfOrNull { it.length } ?: 0

    // Calculate canvas dimensions in dp with minimum size
    val canvasWidthDp = (maxLineLength * (safeCharWidth + safeCharSpacing)).coerceAtLeast(10f).dp
    val canvasHeightDp = (lines.size * (safeCharHeight + safeLineSpacing)).coerceAtLeast(10f).dp

    // Text style for rendering characters with validated font size
    val textStyle = TextStyle(
        fontFamily = firaCode,
        fontSize = safeCharHeight.coerceAtLeast(8f).sp,
        color = textColor
    )

    Canvas(
        modifier = modifier
            .width(canvasWidthDp)
            .height(canvasHeightDp)
    ) {
        // Fill background
        drawRect(
            color = backgroundColor,
            size = size
        )

        // Only draw if we have valid content and dimensions
        if (maxLineLength > 0 && lines.isNotEmpty() &&
            size.width > 0f && size.height > 0f &&
            density > 0f) {
            // Draw each character at precise position
            lines.forEachIndexed { lineIndex, line ->
                line.forEachIndexed { charIndex, char ->
                    if (char.isWhitespace() && char != ' ') return@forEachIndexed

                    val x = charIndex * (safeCharWidth + safeCharSpacing) * density
                    val y = lineIndex * (safeCharHeight + safeLineSpacing) * density

                    // Only draw if position is within canvas bounds
                    if (x >= 0f && y >= 0f && x < size.width && y < size.height) {
                        try {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = char.toString(),
                                topLeft = Offset(x, y),
                                style = textStyle
                            )
                        } catch (e: IllegalArgumentException) {
                            // Skip this character if drawing fails
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display ASCII content with TV frame
 */
@Composable
fun AsciiContentWithTv(
    content: String,
    fps: Float = 12f,
    modifier: Modifier = Modifier
) {
    AsciiContentView(
        content = content,
        fps = fps,
        showTvFrame = true,
        modifier = modifier
    )
}

/**
 * Display ASCII content without TV frame
 */
@Composable
fun AsciiContentWithoutTv(
    content: String,
    fps: Float = 12f,
    modifier: Modifier = Modifier
) {
    AsciiContentView(
        content = content,
        fps = fps,
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
