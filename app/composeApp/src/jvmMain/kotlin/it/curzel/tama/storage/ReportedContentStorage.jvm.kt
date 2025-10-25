package it.curzel.tama.storage

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

class ReportedContentStorageJvm : ReportedContentStorageProvider {
    private val configDir = File(System.getProperty("user.home"), ".tama")
    private val reportedFile = File(configDir, "reported_content.json")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun addReportedContent(server: String, contentId: Long) {
        val reported = getReportedContent().toMutableSet()
        reported.add(ReportedContent(server, contentId))
        saveReportedContent(reported)
    }

    override suspend fun getReportedContent(): Set<ReportedContent> {
        return try {
            if (reportedFile.exists()) {
                val jsonStr = reportedFile.readText()
                json.decodeFromString<Set<ReportedContent>>(jsonStr)
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    override suspend fun isContentReported(server: String, contentId: Long): Boolean {
        val reported = getReportedContent()
        return reported.contains(ReportedContent(server, contentId))
    }

    override suspend fun clearReportedContent() {
        if (reportedFile.exists()) {
            reportedFile.delete()
        }
    }

    private suspend fun saveReportedContent(reported: Set<ReportedContent>) {
        configDir.mkdirs()
        val jsonStr = json.encodeToString(reported)
        reportedFile.writeText(jsonStr)
    }
}
