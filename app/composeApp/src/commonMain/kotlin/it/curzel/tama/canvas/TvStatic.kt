package it.curzel.tama.canvas

import kotlin.random.Random

/**
 * Generate TV static effect - random ASCII characters
 */
fun generateTvStatic(width: Int = TV_WIDTH, height: Int = TV_HEIGHT): String {
    val staticChars = listOf('▓', '▒', '░', '█', '▀', '▄', '■', '□', '▪', '▫')

    return buildString {
        repeat(height) { lineIndex ->
            repeat(width) {
                append(staticChars.random())
            }
            if (lineIndex < height - 1) {
                append('\n')
            }
        }
    }
}

/**
 * Generate multiple static frames for animation
 */
fun generateStaticFrames(frameCount: Int = 5, width: Int = TV_WIDTH, height: Int = TV_HEIGHT): List<String> {
    return List(frameCount) { generateTvStatic(width, height) }
}
