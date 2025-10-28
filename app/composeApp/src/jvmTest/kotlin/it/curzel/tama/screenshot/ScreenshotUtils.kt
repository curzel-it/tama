package it.curzel.tama.screenshot

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.unit.IntSize
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun SemanticsNodeInteraction.captureScreenshot(
    label: String,
    originalSize: IntSize,
    theme: String
) {
    val image = captureToImage()
    val bufferedImage = image.toAwtImage()

    val resizedImage = resizeImage(bufferedImage, originalSize)

    val filename = "${label}-${originalSize.width}x${originalSize.height}-${theme}.png"

    println("Captured image size: ${image.width}x${image.height}, resized to: ${resizedImage.width}x${resizedImage.height}")

    saveScreenshotFromBufferedImage(resizedImage, filename)
}

/**
 * Resizes a BufferedImage by the given scale factor using high-quality interpolation.
 */
private fun resizeImage(image: BufferedImage, newSize: IntSize): BufferedImage {
    val resized = BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_INT_ARGB)
    val graphics = resized.createGraphics()

    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

    graphics.drawImage(image, 0, 0, newSize.width, newSize.height, null)
    graphics.dispose()

    return resized
}

/**
 * Saves a BufferedImage to the screenshots directory as a PNG file.
 */
private fun saveScreenshotFromBufferedImage(bufferedImage: BufferedImage, filename: String) {
    val projectRoot = File(System.getProperty("user.dir")).parentFile.parentFile
    val screenshotsDir = File(projectRoot, "screenshots")
    if (!screenshotsDir.exists()) {
        screenshotsDir.mkdirs()
    }

    val outputFile = File(screenshotsDir, filename)
    ImageIO.write(bufferedImage, "png", outputFile)

    println("Screenshot saved: ${outputFile.absolutePath}")
}
