package it.curzel.tama.storage

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import platform.Foundation.NSUserDefaults

class ReportedContentStorageIos : ReportedContentStorageProvider {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val reportedKey = "tama_reported_content"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun addReportedContent(server: String, contentId: Long) {
        val reported = getReportedContent().toMutableSet()
        reported.add(ReportedContent(server, contentId))
        saveReportedContent(reported)
    }

    override suspend fun getReportedContent(): Set<ReportedContent> {
        return try {
            val jsonStr = userDefaults.stringForKey(reportedKey)
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
        userDefaults.removeObjectForKey(reportedKey)
        userDefaults.synchronize()
    }

    private suspend fun saveReportedContent(reported: Set<ReportedContent>) {
        val jsonStr = json.encodeToString(reported)
        userDefaults.setObject(jsonStr, reportedKey)
        userDefaults.synchronize()
    }
}
