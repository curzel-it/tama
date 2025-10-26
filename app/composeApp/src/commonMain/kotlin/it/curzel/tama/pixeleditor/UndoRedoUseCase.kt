package it.curzel.tama.pixeleditor

data class EditorState(
    val frames: List<PixelFrame>,
    val currentFrameIndex: Int,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val fps: Float
) {
    fun deepCopy(): EditorState {
        val copiedFrames = frames.map { frame ->
            val copiedPixels = frame.pixels.map { row -> row.copyOf() }.toTypedArray()
            PixelFrame(copiedPixels, frame.width, frame.height)
        }
        return EditorState(
            frames = copiedFrames,
            currentFrameIndex = currentFrameIndex,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            fps = fps
        )
    }
}

class UndoRedoManager(private val maxHistorySize: Int = 20) {
    private val history = mutableListOf<EditorState>()
    private var currentPosition = -1

    fun canUndo(): Boolean = currentPosition > 0

    fun canRedo(): Boolean = currentPosition < history.size - 1

    fun saveState(state: EditorState) {
        val stateCopy = state.deepCopy()

        if (currentPosition < history.size - 1) {
            history.subList(currentPosition + 1, history.size).clear()
        }

        history.add(stateCopy)

        if (history.size > maxHistorySize) {
            history.removeAt(0)
        } else {
            currentPosition++
        }

        if (currentPosition >= maxHistorySize) {
            currentPosition = maxHistorySize - 1
        }
    }

    fun undo(): EditorState? {
        if (!canUndo()) return null
        currentPosition--
        return history[currentPosition].deepCopy()
    }

    fun redo(): EditorState? {
        if (!canRedo()) return null
        currentPosition++
        return history[currentPosition].deepCopy()
    }

    fun clear() {
        history.clear()
        currentPosition = -1
    }
}
