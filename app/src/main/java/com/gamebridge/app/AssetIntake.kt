package com.gamebridge.app

import android.net.Uri
import android.content.ContentResolver
import android.database.Cursor
import android.provider.OpenableColumns
import java.util.Locale

/**
 * GameBridge v0.13: metadata-only asset intake.
 * This does not unpack, execute, or convert game binaries.
 */
data class IntakeAsset(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val extension: String,
    val kind: AssetKind
)

enum class AssetKind {
    WINDOWS_EXE, WINDOWS_DLL, ARCHIVE, GAME_DATA, UNKNOWN
}

object AssetIntake {
    fun inspect(resolver: ContentResolver, uri: Uri): IntakeAsset {
        var name = uri.lastPathSegment ?: "selected_file"
        var size: Long? = null
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0) name = cursor.getString(ni) ?: name
                if (si >= 0 && !cursor.isNull(si)) size = cursor.getLong(si)
            }
        } finally {
            cursor?.close()
        }
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val kind = when (ext) {
            "exe" -> AssetKind.WINDOWS_EXE
            "dll" -> AssetKind.WINDOWS_DLL
            "zip", "7z", "rar" -> AssetKind.ARCHIVE
            "pak", "dat", "bin", "rpf", "wad", "obb" -> AssetKind.GAME_DATA
            else -> AssetKind.UNKNOWN
        }
        return IntakeAsset(uri, name, size, ext, kind)
    }

    fun report(asset: IntakeAsset): String = buildString {
        appendLine("GameBridge Asset Intake")
        appendLine("Name: ${asset.displayName}")
        appendLine("Type: ${asset.kind}")
        appendLine("Extension: ${asset.extension.ifBlank { "(none)" }}")
        appendLine("Size: ${asset.sizeBytes?.let { "$it bytes" } ?: "unknown"}")
        appendLine("Status: metadata inspected; no conversion performed")
    }
}
