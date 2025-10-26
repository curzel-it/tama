package it.curzel.tama.content

import java.io.File

class ContentWipStorageJvm : ContentWipStorageProvider {
    private val configDir = File(System.getProperty("user.home"), ".tama")
    private val wipFile = File(configDir, "wip-content.txt")

    override suspend fun saveContent(content: String) {
        configDir.mkdirs()
        wipFile.writeText(content)
    }

    override suspend fun loadContent(): String? {
        return try {
            if (wipFile.exists()) {
                wipFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearContent() {
        if (wipFile.exists()) {
            wipFile.delete()
        }
    }
}
