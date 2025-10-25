package it.curzel.tama.pixeleditor

object BrailleConverter {
    private const val BRAILLE_BASE = 0x2800

    private val brailleOffsets = listOf(
        Triple(0, 0, 0x01),
        Triple(0, 1, 0x02),
        Triple(0, 2, 0x04),
        Triple(0, 3, 0x40),
        Triple(1, 0, 0x08),
        Triple(1, 1, 0x10),
        Triple(1, 2, 0x20),
        Triple(1, 3, 0x80)
    )

    fun pixelsToBraille(pixels: Array<BooleanArray>): String {
        if (pixels.isEmpty() || pixels[0].isEmpty()) {
            return ""
        }

        val lines = mutableListOf<String>()
        val charHeight = (pixels.size + 3) / 4
        val charWidth = (pixels[0].size + 1) / 2

        for (charY in 0 until charHeight) {
            val line = buildString {
                for (charX in 0 until charWidth) {
                    val baseX = charX * 2
                    val baseY = charY * 4

                    var brailleValue = 0

                    for ((dx, dy, bit) in brailleOffsets) {
                        val x = baseX + dx
                        val y = baseY + dy
                        if (y < pixels.size && x < pixels[0].size && pixels[y][x]) {
                            brailleValue = brailleValue or bit
                        }
                    }

                    append(Char(BRAILLE_BASE + brailleValue))
                }
            }
            lines.add(line.trimEnd())
        }

        return lines.joinToString("\n")
    }

    fun brailleToPixels(brailleText: String, charWidth: Int, charHeight: Int): List<Array<BooleanArray>> {
        val lines = brailleText.trim().split('\n')
        val framesData = mutableListOf<Array<BooleanArray>>()

        var frameStart = 0
        while (frameStart < lines.size) {
            val frameLines = lines.subList(frameStart, minOf(frameStart + charHeight, lines.size))
            val pixelHeight = charHeight * 4
            val pixelWidth = charWidth * 2
            val pixels = Array(pixelHeight) { BooleanArray(pixelWidth) { false } }

            for ((charY, line) in frameLines.withIndex()) {
                for (charX in 0 until minOf(line.length, charWidth)) {
                    val char = line[charX]
                    val brailleValue = char.code - BRAILLE_BASE

                    val baseX = charX * 2
                    val baseY = charY * 4

                    for ((dx, dy, bit) in brailleOffsets) {
                        if (brailleValue and bit != 0) {
                            val x = baseX + dx
                            val y = baseY + dy
                            if (x < pixelWidth && y < pixelHeight) {
                                pixels[y][x] = true
                            }
                        }
                    }
                }
            }

            framesData.add(pixels)
            frameStart += charHeight
        }

        return framesData
    }
}
