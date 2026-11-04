package com.gamebridge.app

/**
 * A backend descriptor documents a possible integration route.
 * "Available" means the integration is present in this app, not merely that
 * an upstream project exists. All current entries are research-only.
 */
enum class BackendKind {
    SOURCE_PORT,
    STATIC_RECOMPILER,
    WINDOWS_COMPATIBILITY,
    GRAPHICS_TRANSLATION,
    ENGINE_REIMPLEMENTATION
}

enum class BackendIntegrationState {
    RESEARCH_ONLY,
    PROTOTYPE,
    INTEGRATED,
    DEVICE_VERIFIED
}

data class BackendDescriptor(
    val id: String,
    val name: String,
    val kind: BackendKind,
    val state: BackendIntegrationState,
    val target: String,
    val notes: String,
    val blockers: List<String>
)

object BackendCatalog {
    val entries = listOf(
        BackendDescriptor(
            id = "source-port",
            name = "Native source port",
            kind = BackendKind.SOURCE_PORT,
            state = BackendIntegrationState.RESEARCH_ONLY,
            target = "Android ARM64",
            notes = "Best path when a legitimate source/project and dependencies are available.",
            blockers = listOf("Need the actual game/engine project", "Need Android platform integration and licensing review")
        ),
        BackendDescriptor(
            id = "static-recomp",
            name = "Static recompilation",
            kind = BackendKind.STATIC_RECOMPILER,
            state = BackendIntegrationState.RESEARCH_ONLY,
            target = "ARM64",
            notes = "Can translate supported binaries with project-specific metadata; not a universal EXE converter.",
            blockers = listOf("Need a backend that supports this binary/ISA and metadata", "Need runtime and platform services")
        ),
        BackendDescriptor(
            id = "windows-runtime",
            name = "Windows compatibility runtime",
            kind = BackendKind.WINDOWS_COMPATIBILITY,
            state = BackendIntegrationState.RESEARCH_ONLY,
            target = "Android/Linux-derived runtime",
            notes = "Compatibility approach rather than native recompilation; may involve CPU translation and Windows API layers.",
            blockers = listOf("Android integration is complex", "Game-specific APIs, DRM, GPU and performance may block execution")
        ),
        BackendDescriptor(
            id = "graphics-translation",
            name = "Graphics API translation",
            kind = BackendKind.GRAPHICS_TRANSLATION,
            state = BackendIntegrationState.RESEARCH_ONLY,
            target = "Vulkan-capable Android GPU",
            notes = "Translates a graphics API; it does not supply Windows APIs, game logic, or a full runtime.",
            blockers = listOf("Requires Vulkan/GPU feature support", "Does not by itself make a Windows game an APK")
        ),
        BackendDescriptor(
            id = "engine-reimplementation",
            name = "Engine reimplementation",
            kind = BackendKind.ENGINE_REIMPLEMENTATION,
            state = BackendIntegrationState.RESEARCH_ONLY,
            target = "Android",
            notes = "A game-specific engine implementation may enable a native port when its compatibility is mature.",
            blockers = listOf("Requires title-specific reverse engineering or implementation", "Assets and legal rights remain separate")
        )
    )

    fun statusSummary(): String = entries.joinToString("\\n\\n") {
        "${it.name} — ${it.state}\\n${it.notes}\\nBlockers: ${it.blockers.joinToString("; ")}"
    }
}
