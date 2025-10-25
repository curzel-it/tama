package it.curzel.tama.pixeleditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    private fun validateCurrentDimensions() {
        when (val result = PixelEditorUseCase.validateDimensions(canvasWidth, canvasHeight)) {
            is ValidationResult.Valid -> validationError = null
            is ValidationResult.Invalid -> validationError = result.message
        }
    }
}
