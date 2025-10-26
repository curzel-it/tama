package it.curzel.tama.storage

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ReportedContentStorageAndroid(context: Context) : ReportedContentStorageProvider {
    private val prefs = context.getSharedPreferences("tama_config", Context.MODE_PRIVATE)
    private val reportedKey = "reported_content"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun addReportedContent(server: String, contentId: Long) {
        val reported = getReportedContent().toMutableSet()
        reported.add(ReportedContent(server, contentId))
        saveReportedContent(reported)
    }

    override suspend fun getReportedContent(): Set<ReportedContent> {
        return try {
            val jsonStr = prefs.getString(reportedKey, null)
            if (jsonStr != null) {
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
        prefs.edit().remove(reportedKey).apply()
    }

    private suspend fun saveReportedContent(reported: Set<ReportedContent>) {
        val jsonStr = json.encodeToString(reported)
        prefs.edit().putString(reportedKey, jsonStr).apply()
    }
}
