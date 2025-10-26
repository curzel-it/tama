package it.curzel.tama.pixeleditor

data class PixelFrame(
    val pixels: Array<BooleanArray>,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PixelFrame

        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentDeepEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pixels.contentDeepHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

object PixelEditorUseCase {
    fun createEmptyFrame(width: Int, height: Int): PixelFrame {
        val pixels = Array(height) { BooleanArray(width) { false } }
        return PixelFrame(pixels, width, height)
    }

    fun clearFrame(frame: PixelFrame): PixelFrame {
        val pixels = Array(frame.height) { BooleanArray(frame.width) { false } }
        return PixelFrame(pixels, frame.width, frame.height)
    }

    fun fillFrame(frame: PixelFrame): PixelFrame {
        val pixels = Array(frame.height) { BooleanArray(frame.width) { true } }
        return PixelFrame(pixels, frame.width, frame.height)
    }

    fun setPixel(frame: PixelFrame, x: Int, y: Int, value: Boolean): PixelFrame {
        if (x < 0 || x >= frame.width || y < 0 || y >= frame.height) {
            return frame
        }

        val newPixels = frame.pixels.map { it.copyOf() }.toTypedArray()
        newPixels[y][x] = value
        return PixelFrame(newPixels, frame.width, frame.height)
    }

    fun validateDimensions(width: Int, height: Int): ValidationResult {
        return when {
            width <= 0 || height <= 0 ->
                ValidationResult.Invalid("Width and height must be greater than 0")
            width % 2 != 0 ->
                ValidationResult.Invalid("Width must be a multiple of 2")
            height % 4 != 0 ->
                ValidationResult.Invalid("Height must be a multiple of 4")
            width > 128 ->
                ValidationResult.Invalid("Width must be 128 or less")
            height > 128 ->
                ValidationResult.Invalid("Height must be 128 or less")
            else -> ValidationResult.Valid
        }
    }

    fun validateFps(fps: Float): ValidationResult {
        return when {
            fps < 1f -> ValidationResult.Invalid("FPS must be at least 1")
            fps > 30f -> ValidationResult.Invalid("FPS must be 30 or less")
            else -> ValidationResult.Valid
        }
    }

    fun framesToBrailleArt(
        frames: List<PixelFrame>,
        charWidth: Int,
        charHeight: Int,
        fps: Float
    ): String {
        if (frames.isEmpty()) {
            return ""
        }

        val result = StringBuilder()
        result.append("Ascii Art Animation, ${charWidth}x${charHeight}, ${fps}fps\n")

        frames.forEach { frame ->
            val brailleArt = BrailleConverter.pixelsToBraille(frame.pixels)
            result.append(brailleArt)
            result.append("\n")
        }

        return result.toString()
    }

    fun resizeFrame(frame: PixelFrame, newWidth: Int, newHeight: Int): PixelFrame {
        val newPixels = Array(newHeight) { BooleanArray(newWidth) { false } }

        val offsetX = (newWidth - frame.width) / 2
        val offsetY = (newHeight - frame.height) / 2

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val targetY = y + offsetY
                val targetX = x + offsetX
                if (targetY >= 0 && targetY < newHeight && targetX >= 0 && targetX < newWidth) {
                    newPixels[targetY][targetX] = frame.pixels[y][x]
                }
            }
        }

        return PixelFrame(newPixels, newWidth, newHeight)
    }
}
