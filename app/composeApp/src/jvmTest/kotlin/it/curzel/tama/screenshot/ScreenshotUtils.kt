package it.curzel.tama.screenshot

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Captures a screenshot of a composable and saves it to the screenshots directory.
 *
 * @param label A descriptive label for the screenshot (e.g., "homepage", "profile")
 * @param width Width in pixels
 * @param height Height in pixels
 * @param theme Theme variant ("light" or "dark")
 */
fun SemanticsNodeInteraction.captureScreenshot(
    label: String,
    width: Int,
    height: Int,
    theme: String
) {
    val image = captureToImage()
    val filename = "${label}-${width}x${height}-${theme}.png"

    println("Captured image size: ${image.width}x${image.height}, requested: ${width}x${height}")

    saveScreenshot(image, filename)
}

/**
 * Saves an ImageBitmap to the screenshots directory as a PNG file.
 */
private fun saveScreenshot(image: ImageBitmap, filename: String) {
    val projectRoot = File(System.getProperty("user.dir")).parentFile.parentFile
    val screenshotsDir = File(projectRoot, "screenshots")
    if (!screenshotsDir.exists()) {
        screenshotsDir.mkdirs()
    }

    val outputFile = File(screenshotsDir, filename)
    val bufferedImage = image.toAwtImage()
    ImageIO.write(bufferedImage, "png", outputFile)

    println("Screenshot saved: ${outputFile.absolutePath}")
}
