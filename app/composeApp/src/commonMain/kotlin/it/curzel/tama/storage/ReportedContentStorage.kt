package it.curzel.tama.storage

import kotlinx.serialization.Serializable

@Serializable
data class ReportedContent(
    val server: String,
    val contentId: Long
)

interface ReportedContentStorageProvider {
    suspend fun addReportedContent(server: String, contentId: Long)
    suspend fun getReportedContent(): Set<ReportedContent>
    suspend fun isContentReported(server: String, contentId: Long): Boolean
    suspend fun clearReportedContent()
}

object ReportedContentStorage {
    lateinit var provider: ReportedContentStorageProvider

    suspend fun addReportedContent(server: String, contentId: Long) =
        provider.addReportedContent(server, contentId)

    suspend fun getReportedContent(): Set<ReportedContent> =
        provider.getReportedContent()

    suspend fun isContentReported(server: String, contentId: Long): Boolean =
        provider.isContentReported(server, contentId)

    suspend fun clearReportedContent() =
        provider.clearReportedContent()
}
