package it.curzel.tama.pixeleditor

object FrameListUseCase {
    fun calculateThumbnailSize(
        isLandscape: Boolean,
        canvasWidth: Int,
        canvasHeight: Int,
        maxSize: Float = 80f
    ): Pair<Float, Float> {
        val aspectRatio = canvasWidth.toFloat() / canvasHeight.toFloat()

        return if (isLandscape) {
            val width = maxSize
            val height = width / aspectRatio
            Pair(width, height)
        } else {
            val height = maxSize
            val width = height * aspectRatio
            Pair(width, height)
        }
    }

    fun getFrameAspectRatio(canvasWidth: Int, canvasHeight: Int): Float {
        return canvasWidth.toFloat() / canvasHeight.toFloat()
    }
}
