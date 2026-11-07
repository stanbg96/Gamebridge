package com.gamebridge.app

/**
 * Client-side model for the GameBridge cloud build workflow.
 *
 * This module intentionally does not contain credentials or pretend to submit
 * a remote build. A secure backend endpoint must be configured before network
 * submission and artifact retrieval are implemented.
 */
enum class CloudBuildStage {
    READY,
    ANALYZING,
    QUEUED,
    BUILDING,
    APK_READY,
    FAILED
}

data class CloudBuildRequest(
    val gameName: String,
    val executableName: String,
    val architecture: String,
    val target: String = "Android ARM64"
)

data class CloudBuildState(
    val stage: CloudBuildStage,
    val message: String,
    val taskId: String? = null,
    val artifactUrl: String? = null
)

object CloudBuildManager {
    fun prepareRequest(
        gameName: String,
        executableName: String,
        architecture: String
    ): CloudBuildRequest {
        require(executableName.endsWith(".exe", true) ||
                executableName.endsWith(".dll", true)) {
            "Choose a Windows PE executable or library first."
        }
        return CloudBuildRequest(
            gameName = gameName.trim().ifBlank { executableName },
            executableName = executableName,
            architecture = architecture.ifBlank { "Unknown" }
        )
    }
}
