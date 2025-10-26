package it.curzel.tama.pixeleditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

enum class ToolType {
    PENCIL,
    ERASER,
    MOVE
}

class PixelEditorViewModel {
    var canvasWidth by mutableIntStateOf(48)
        private set
    var canvasHeight by mutableIntStateOf(32)
        private set
    var fps by mutableFloatStateOf(10f)
        private set

    var frames by mutableStateOf<List<PixelFrame>>(emptyList())
        private set
    var currentFrameIndex by mutableIntStateOf(-1)
        private set

    var validationError by mutableStateOf<String?>(null)
        private set

    var zoomLevel by mutableFloatStateOf(1f)
        private set
    var panOffset by mutableStateOf(Offset.Zero)
        private set

    var showSettingsMenu by mutableStateOf(false)
        private set

    var currentTool by mutableStateOf(ToolType.PENCIL)
        private set

    private val undoRedoManager = UndoRedoManager(maxHistorySize = 20)
    private var isDrawing = false

    val currentFrame: PixelFrame?
        get() = frames.getOrNull(currentFrameIndex)

    val charWidth: Int
        get() = canvasWidth / 2

    val charHeight: Int
        get() = canvasHeight / 4

    val canUndo: Boolean
        get() = undoRedoManager.canUndo()

    val canRedo: Boolean
        get() = undoRedoManager.canRedo()

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
                saveStateToHistory()
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
        saveStateToHistory()
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

        saveStateToHistory()
    }

    fun selectFrame(index: Int) {
        if (index in frames.indices) {
            currentFrameIndex = index
            saveStateToHistory()
        }
    }

    fun setPixel(x: Int, y: Int, value: Boolean) {
        println("[PixelEditorViewModel] setPixel called: x=$x, y=$y, value=$value, hasFrame=${currentFrame != null}")
        currentFrame?.let { frame ->
            println("[PixelEditorViewModel] Current pixel value at ($x,$y) = ${frame.pixels[y][x]}")
            val updatedFrame = PixelEditorUseCase.setPixel(frame, x, y, value)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) updatedFrame else f
            }
            println("[PixelEditorViewModel] Pixel set successfully, new value = ${updatedFrame.pixels[y][x]}")
        } ?: println("[PixelEditorViewModel] ERROR: currentFrame is null!")
    }

    fun clearCurrentFrame() {
        currentFrame?.let { frame ->
            val clearedFrame = PixelEditorUseCase.clearFrame(frame)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) clearedFrame else f
            }
            saveStateToHistory()
        }
    }

    fun fillCurrentFrame() {
        currentFrame?.let { frame ->
            val filledFrame = PixelEditorUseCase.fillFrame(frame)
            frames = frames.mapIndexed { index, f ->
                if (index == currentFrameIndex) filledFrame else f
            }
            saveStateToHistory()
        }
    }

    fun exportToText(): String {
        return PixelEditorUseCase.framesToBrailleArt(frames, charWidth, charHeight, fps)
    }

    fun loadFromText(content: String) {
        // TODO: Implement loading from text file
    }

    fun onZoom(zoomFactor: Float, centerX: Float, centerY: Float) {
        val newZoom = (zoomLevel * zoomFactor).coerceIn(1f, 8f)

        val zoomChange = newZoom / zoomLevel
        val adjustedPanX = centerX - (centerX - panOffset.x) * zoomChange
        val adjustedPanY = centerY - (centerY - panOffset.y) * zoomChange

        zoomLevel = newZoom
        panOffset = Offset(adjustedPanX, adjustedPanY)
    }

    fun onPan(delta: Offset) {
        panOffset += delta
    }

    fun updatePanOffset(offset: Offset) {
        panOffset = offset
    }

    fun openSettingsMenu() {
        showSettingsMenu = true
    }

    fun closeSettingsMenu() {
        showSettingsMenu = false
    }

    fun selectTool(tool: ToolType) {
        println("[PixelEditorViewModel] Tool changed: $currentTool -> $tool")
        currentTool = tool
    }

    fun undo() {
        undoRedoManager.undo()?.let { state ->
            restoreState(state)
        }
    }

    fun redo() {
        undoRedoManager.redo()?.let { state ->
            restoreState(state)
        }
    }

    fun startDrawing() {
        isDrawing = true
    }

    fun commitDrawingAction() {
        if (isDrawing) {
            isDrawing = false
            saveStateToHistory()
        }
    }

    private fun saveStateToHistory() {
        val state = EditorState(
            frames = frames,
            currentFrameIndex = currentFrameIndex,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            fps = fps
        )
        undoRedoManager.saveState(state)
    }

    private fun restoreState(state: EditorState) {
        frames = state.frames
        currentFrameIndex = state.currentFrameIndex
        canvasWidth = state.canvasWidth
        canvasHeight = state.canvasHeight
        fps = state.fps
    }

    private fun validateCurrentDimensions() {
        when (val result = PixelEditorUseCase.validateDimensions(canvasWidth, canvasHeight)) {
            is ValidationResult.Valid -> validationError = null
            is ValidationResult.Invalid -> validationError = result.message
        }
    }
}
