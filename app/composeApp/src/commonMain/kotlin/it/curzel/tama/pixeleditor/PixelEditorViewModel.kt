package it.curzel.tama.pixeleditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class PixelEditorViewModel {
    var canvasWidth by mutableIntStateOf(64)
        private set
    var canvasHeight by mutableIntStateOf(40)
        private set
    var fps by mutableFloatStateOf(10f)
        private set

    var frames by mutableStateOf<List<PixelFrame>>(emptyList())
        private set
    var currentFrameIndex by mutableIntStateOf(-1)
        private set

    var showPreview by mutableStateOf(false)
        private set

    var validationError by mutableStateOf<String?>(null)
        private set

    var zoomLevel by mutableFloatStateOf(1f)
        private set
    var panOffset by mutableStateOf(Offset.Zero)
        private set
    var isPanMode by mutableStateOf(false)
        private set

    var showToolsMenu by mutableStateOf(false)
        private set
    var showSettingsMenu by mutableStateOf(false)
        private set
    var showFramesMenu by mutableStateOf(false)
        private set

    val currentFrame: PixelFrame?
        get() = frames.getOrNull(currentFrameIndex)

    val charWidth: Int
        get() = canvasWidth / 2

    val charHeight: Int
        get() = canvasHeight / 4

    init {
        addFrame()
    }

    fun updateCanvasWidth(width: Int) {
        canvasWidth = width.coerceIn(2, 128)
        validateCurrentDimensions()
    }

    fun updateCanvasHeight(height: Int) {
        canvasHeight = height.coerceIn(4, 128)
        validateCurrentDimensions()
    }

    fun updateFps(newFps: Float) {
        fps = newFps.coerceIn(1f, 30f)
        when (val result = PixelEditorUseCase.validateFps(fps)) {
            is ValidationResult.Valid -> validationError = null
            is ValidationResult.Invalid -> validationError = result.message
        }
    }

    fun resizeCanvas() {
        when (val result = PixelEditorUseCase.validateDimensions(canvasWidth, canvasHeight)) {
            is ValidationResult.Valid -> {
                validationError = null
                val resizedFrames = frames.map { frame ->
                    PixelEditorUseCase.resizeFrame(frame, canvasWidth, canvasHeight)
                }
                frames = resizedFrames
            }
            is ValidationResult.Invalid -> {
                validationError = result.message
            }
        }
    }

    fun addFrame() {
        val newFrame = PixelEditorUseCase.createEmptyFrame(canvasWidth, canvasHeight)
        frames = frames + newFrame
        currentFrameIndex = frames.size - 1
    }

    fun deleteFrame(index: Int) {
        if (frames.size <= 1 || index < 0 || index >= frames.size) {
            return
        }

        frames = frames.filterIndexed { i, _ -> i != index }

        if (currentFrameIndex >= frames.size) {
            currentFrameIndex = frames.size - 1
        } else if (currentFrameIndex > index) {
            currentFrameIndex--
        }
    }

    fun selectFrame(index: Int) {
        if (index in frames.indices) {
            currentFrameIndex = index
        }
    }

    fun setPixel(x: Int, y: Int, value: Boolean) {
        currentFrame?.let { frame ->
            val updatedFrame = PixelEditorUseCase.setPixel(frame, x, y, value)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) updatedFrame else f
            }
        }
    }

    fun clearCurrentFrame() {
        currentFrame?.let { frame ->
            val clearedFrame = PixelEditorUseCase.clearFrame(frame)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) clearedFrame else f
            }
        }
    }

    fun fillCurrentFrame() {
        currentFrame?.let { frame ->
            val filledFrame = PixelEditorUseCase.fillFrame(frame)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) filledFrame else f
            }
        }
    }

    fun togglePreview() {
        showPreview = !showPreview
    }

    fun exportToText(): String {
        return PixelEditorUseCase.framesToBrailleArt(frames, charWidth, charHeight, fps)
    }

    fun loadFromText(content: String) {
        // TODO: Implement loading from text file
    }

    fun zoomIn() {
        val levels = listOf(0.5f, 1f, 2f, 4f, 8f)
        val currentIndex = levels.indexOfFirst { it >= zoomLevel }
        if (currentIndex < levels.size - 1) {
            zoomLevel = levels[currentIndex + 1]
        }
    }

    fun zoomOut() {
        val levels = listOf(0.5f, 1f, 2f, 4f, 8f)
        val currentIndex = levels.indexOfLast { it <= zoomLevel }
        if (currentIndex > 0) {
            zoomLevel = levels[currentIndex - 1]
        }
    }

    fun resetZoom() {
        zoomLevel = 1f
        panOffset = Offset.Zero
    }

    fun updatePanOffset(offset: Offset) {
        panOffset = offset
    }

    fun togglePanMode() {
        isPanMode = !isPanMode
    }

    fun openToolsMenu() {
        showToolsMenu = true
    }

    fun closeToolsMenu() {
        showToolsMenu = false
    }

    fun openSettingsMenu() {
        showSettingsMenu = true
    }

    fun closeSettingsMenu() {
        showSettingsMenu = false
    }

    fun openFramesMenu() {
        showFramesMenu = true
    }

    fun closeFramesMenu() {
        showFramesMenu = false
    }

    private fun validateCurrentDimensions() {
        when (val result = PixelEditorUseCase.validateDimensions(canvasWidth, canvasHeight)) {
            is ValidationResult.Valid -> validationError = null
            is ValidationResult.Invalid -> validationError = result.message
        }
    }
}
