package it.curzel.tama.content

import it.curzel.tama.pixeleditor.BrailleConverter
import it.curzel.tama.pixeleditor.PixelFrame

data class WipContent(
    val title: String,
    val midi: String,
    val art: String,
    val width: Int,
    val height: Int,
    val fps: Float
)

interface ContentWipStorageProvider {
    suspend fun saveContent(content: String)
    suspend fun loadContent(): String?
    suspend fun clearContent()
}

object ContentWipUseCase {
    lateinit var storageProvider: ContentWipStorageProvider

    private var cachedTitle: String = ""
    private var cachedMidi: String = ""
    private var cachedArt: String = ""
    private var cachedWidth: Int = 24
    private var cachedHeight: Int = 8
    private var cachedFps: Float = 10f

    suspend fun savePixelArt(frames: List<PixelFrame>, fps: Float) {
        if (frames.isEmpty()) return

        val charWidth = frames[0].width / 2
        val charHeight = frames[0].height / 4

        val artBuilder = StringBuilder()
        artBuilder.append("Ascii Art Animation, ${charWidth}x${charHeight}, ${fps}fps\n")

        frames.forEach { frame ->
            val brailleArt = BrailleConverter.pixelsToBraille(frame.pixels)
            artBuilder.append(brailleArt)
            artBuilder.append("\n")
        }

        cachedArt = artBuilder.toString()
        cachedWidth = charWidth
        cachedHeight = charHeight
        cachedFps = fps

        saveToFile()
    }

    suspend fun saveMidi(composition: String) {
        println("[WIP_STORAGE] saveMidi called with: '${composition}'")
        cachedMidi = composition
        saveToFile()
    }

    suspend fun saveTitle(title: String) {
        println("[WIP_STORAGE] saveTitle called with: '${title}'")
        cachedTitle = title
        saveToFile()
    }

    suspend fun loadWipContent(): WipContent? {
        val content = storageProvider.loadContent()
        println("[WIP_STORAGE] loadWipContent: content ${if (content == null) "is null" else "loaded (${content.length} chars)"}")
        if (content == null) return null
        val parsed = parseContent(content)
        println("[WIP_STORAGE] Parsed WIP: title='${parsed?.title}', midi='${parsed?.midi}', art_len=${parsed?.art?.length}")
        return parsed
    }

    suspend fun loadPixelArtFrames(): Triple<List<PixelFrame>, Int, Float>? {
        val wipContent = loadWipContent() ?: return null

        val lines = wipContent.art.lines()
        if (lines.isEmpty()) return null

        val header = lines[0]
        val charWidth = wipContent.width
        val charHeight = wipContent.height
        val fps = wipContent.fps

        val brailleText = lines.drop(1).joinToString("\n")
        val pixelArrays = BrailleConverter.brailleToPixels(brailleText, charWidth, charHeight)

        val frames = pixelArrays.map { pixels ->
            PixelFrame(pixels, charWidth * 2, charHeight * 4)
        }

        return Triple(frames, charWidth * 2, fps)
    }

    suspend fun loadMidiComposition(): String? {
        val wipContent = loadWipContent() ?: return null
        return wipContent.midi.ifEmpty { null }
    }

    private suspend fun saveToFile() {
        println("[WIP_STORAGE] saveToFile: title='${cachedTitle}', midi='${cachedMidi}', art_len=${cachedArt.length}")

        val contentBuilder = StringBuilder()

        contentBuilder.append("--- TITLE ---\n")
        contentBuilder.append(cachedTitle)
        contentBuilder.append("\n")

        contentBuilder.append("--- MIDI ---\n")
        contentBuilder.append(cachedMidi)
        contentBuilder.append("\n")

        contentBuilder.append("--- ART ---\n")
        contentBuilder.append(cachedArt)

        val finalContent = contentBuilder.toString()
        println("[WIP_STORAGE] Saving ${finalContent.length} chars to storage")
        storageProvider.saveContent(finalContent)
    }

    private fun parseContent(content: String): WipContent? {
        val lines = content.lines()

        var titleSection = ""
        var midiSection = ""
        var artSection = ""
        var width = 24
        var height = 8
        var fps = 10f

        var currentSection = ""
        val sectionLines = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("--- TITLE ---") -> {
                    currentSection = "TITLE"
                    sectionLines.clear()
                }
                line.startsWith("--- MIDI ---") -> {
                    if (currentSection == "TITLE") {
                        titleSection = sectionLines.joinToString("\n").trim()
                    }
                    currentSection = "MIDI"
                    sectionLines.clear()
                }
                line.startsWith("--- ART ---") -> {
                    if (currentSection == "MIDI") {
                        midiSection = sectionLines.joinToString("\n").trim()
                    }
                    currentSection = "ART"
                    sectionLines.clear()
                }
                else -> {
                    sectionLines.add(line)
                }
            }
        }

        if (currentSection == "ART") {
            artSection = sectionLines.joinToString("\n").trim()
        }

        val headerRegex = """Ascii Art Animation, (\d+)x(\d+), ([\d.]+)fps""".toRegex()
        val artLines = artSection.lines()
        if (artLines.isNotEmpty()) {
            val headerMatch = headerRegex.find(artLines[0])
            if (headerMatch != null) {
                width = headerMatch.groupValues[1].toIntOrNull() ?: 24
                height = headerMatch.groupValues[2].toIntOrNull() ?: 8
                fps = headerMatch.groupValues[3].toFloatOrNull() ?: 10f
            }
        }

        cachedTitle = titleSection
        cachedMidi = midiSection
        cachedArt = artSection
        cachedWidth = width
        cachedHeight = height
        cachedFps = fps

        return WipContent(
            title = titleSection,
            midi = midiSection,
            art = artSection,
            width = width,
            height = height,
            fps = fps
        )
    }
}
