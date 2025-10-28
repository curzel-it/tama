package it.curzel.tama.version

import it.curzel.tama.Constants
import it.curzel.tama.api.ApiClient
import it.curzel.tama.api.VersionResponse

enum class Platform {
    ANDROID, IOS, JVM
}

data class VersionCheckResult(
    val updateRequired: Boolean,
    val currentVersion: String,
    val minimumVersion: String,
    val latestVersion: String
)

object VersionUseCase {
    lateinit var apiClient: ApiClient
    lateinit var currentPlatform: Platform

    suspend fun checkVersion(): Result<VersionCheckResult> {
        return try {
            val response = apiClient.checkVersion()
            if (response.isFailure) {
                return Result.failure(response.exceptionOrNull()!!)
            }

            val versionResponse = response.getOrThrow()
            val currentVersion = Constants.API_VERSION
            val minimumVersion = getMinimumVersionForPlatform(versionResponse)
            val latestVersion = getLatestVersionForPlatform(versionResponse)

            val updateRequired = compareVersions(currentVersion, minimumVersion) < 0

            Result.success(
                VersionCheckResult(
                    updateRequired = updateRequired,
                    currentVersion = currentVersion,
                    minimumVersion = minimumVersion,
                    latestVersion = latestVersion
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMinimumVersionForPlatform(versionResponse: VersionResponse): String {
        return when (currentPlatform) {
            Platform.ANDROID -> versionResponse.minimum.android
            Platform.IOS -> versionResponse.minimum.ios
            Platform.JVM -> versionResponse.minimum.android // JVM uses android version for now
        }
    }

    private fun getLatestVersionForPlatform(versionResponse: VersionResponse): String {
        return when (currentPlatform) {
            Platform.ANDROID -> versionResponse.latest.android
            Platform.IOS -> versionResponse.latest.ios
            Platform.JVM -> versionResponse.latest.android // JVM uses android version for now
        }
    }

    private fun compareVersions(current: String, minimum: String): Int {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val minimumParts = minimum.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentParts.size, minimumParts.size)

        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val minimumPart = minimumParts.getOrElse(i) { 0 }

            when {
                currentPart < minimumPart -> return -1
                currentPart > minimumPart -> return 1
            }
        }

        return 0 // versions are equal
    }
}
