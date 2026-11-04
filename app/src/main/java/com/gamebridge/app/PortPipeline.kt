package com.gamebridge.app

/**
 * GameBridge v0.9 preflight pipeline.
 * This is an honest diagnostic/planning stage, not a game converter.
 */
object PortPipeline {
    fun plan(title: String, executable: String, architecture: String,
             graphicsApi: String, resolution: String, fps: Int): String {
        val name = title.trim().ifBlank { "Untitled game" }
        val exe = executable.trim()
        val arch = architecture.trim().ifBlank { "Unknown" }
        val isPe = exe.endsWith(".exe", ignoreCase = true) ||
                   exe.endsWith(".dll", ignoreCase = true)
        val lines = mutableListOf(
            "PREFLIGHT · $name",
            "1. Input: ${if (isPe) exe else "Executable not identified"}",
            "2. Architecture: $arch",
            "3. Target: Android / $graphicsApi / $resolution / $fps FPS"
        )
        if (!isPe) {
            lines += "4. Result: NEEDS INPUT — choose a game executable or a supported source project."
        } else {
            lines += "4. Result: ANALYSIS ONLY — PE metadata does not establish port compatibility."
        }
        lines += "5. Required next: identify engine, available source/porting rights, platform APIs, and renderer."
        lines += "6. Backend: NONE CONNECTED — no APK conversion was performed."
        return lines.joinToString("\\n")
    }
}
