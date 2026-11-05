package com.gamebridge.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PeInfo(
    val fileName: String,
    val isPe: Boolean,
    val architecture: String = "Unknown",
    val machine: String = "Unknown",
    val subsystem: String = "Unknown",
    val note: String = "",
    val peFormat: String = "Unknown",
    val fileSize: Long = -1L,
    val entryPoint: String = "Unknown",
    val sectionCount: Int = -1,
    val sections: List<String> = emptyList(),
    val fileType: String = "Unknown"
)

object PeInspector {

    private const val MAX_HEADER_BYTES = 1024 * 1024
    private const val SECTION_HEADER_SIZE = 40
    private const val MAX_SECTIONS = 96

    fun inspect(
        resolver: ContentResolver,
        uri: Uri,
        name: String
    ): PeInfo {
        val fileSize = getFileSize(resolver, uri)

        return try {
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                var count = 0

                while (
                    total < MAX_HEADER_BYTES &&
                    input.read(buffer, 0, minOf(buffer.size, MAX_HEADER_BYTES - total))
                        .also { count = it } > 0
                ) {
                    output.write(buffer, 0, count)
                    total += count
                }

                output.toByteArray()
            } ?: byteArrayOf()

            if (
                bytes.size < 64 ||
                bytes[0] != 0x4D.toByte() ||
                bytes[1] != 0x5A.toByte()
            ) {
                return invalid(name, fileSize, "No MZ header")
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val peOffset = buffer.getInt(0x3C).toLong() and 0xFFFFFFFFL

            if (
                peOffset < 64 ||
                peOffset + 24L > bytes.size ||
                buffer.getInt(peOffset.toInt()) != 0x00004550
            ) {
                return invalid(name, fileSize, "PE signature not found or incomplete")
            }

            val coffOffset = peOffset.toInt() + 4
            val machineCode = buffer.getShort(coffOffset).toInt() and 0xFFFF
            val sectionCount = buffer.getShort(coffOffset + 2).toInt() and 0xFFFF
            val optionalHeaderSize =
                buffer.getShort(coffOffset + 16).toInt() and 0xFFFF
            val characteristics =
                buffer.getShort(coffOffset + 18).toInt() and 0xFFFF

            val optionalOffset = coffOffset + 20
            val optionalEnd = optionalOffset.toLong() + optionalHeaderSize

            if (
                optionalHeaderSize < 2 ||
                optionalEnd > bytes.size
            ) {
                return invalid(name, fileSize, "Optional header is incomplete")
            }

            val magic = buffer.getShort(optionalOffset).toInt() and 0xFFFF

            val peFormat = when (magic) {
                0x10B -> "PE32"
                0x20B -> "PE32+"
                else -> "Unknown"
            }

            val entryPoint = if (optionalHeaderSize >= 20) {
                "0x" + buffer.getInt(optionalOffset + 16)
                    .toUInt()
                    .toString(16)
                    .uppercase()
            } else {
                "Unavailable"
            }

            val subsystem = if (optionalHeaderSize >= 70) {
                when (buffer.getShort(optionalOffset + 68).toInt() and 0xFFFF) {
                    1 -> "Native"
                    2 -> "Windows GUI"
                    3 -> "Windows Console"
                    9 -> "Windows CE GUI"
                    10 -> "EFI Application"
                    11 -> "EFI Boot Service Driver"
                    12 -> "EFI Runtime Driver"
                    13 -> "EFI ROM"
                    14 -> "Xbox"
                    else -> "Other"
                }
            } else {
                "Unavailable"
            }

            val machine = when (machineCode) {
                0x014C -> "Intel x86"
                0x8664 -> "AMD64"
                0x01C0 -> "ARM"
                0x01C4 -> "ARMv7"
                0xAA64 -> "ARM64"
                else -> "Unknown 0x${machineCode.toString(16).uppercase()}"
            }

            val architecture = when (machineCode) {
                0x014C -> "x86 32-bit"
                0x8664 -> "x86-64"
                0x01C0, 0x01C4 -> "ARM 32-bit"
                0xAA64 -> "ARM64"
                else -> "Unknown"
            }

            val fileType =
                if ((characteristics and 0x2000) != 0) "DLL" else "EXE"

            val sectionTableOffset = optionalEnd.toInt()
            val sections = mutableListOf<String>()
            val readableSectionCount = sectionCount.coerceAtMost(MAX_SECTIONS)

            for (i in 0 until readableSectionCount) {
                val sectionOffset =
                    sectionTableOffset + i * SECTION_HEADER_SIZE

                if (sectionOffset.toLong() + SECTION_HEADER_SIZE > bytes.size) {
                    break
                }

                val nameBytes = ByteArray(8)
                System.arraycopy(bytes, sectionOffset, nameBytes, 0, 8)

                val sectionName = nameBytes
                    .toString(Charsets.US_ASCII)
                    .trimEnd('\u0000')
                    .ifEmpty { "Unnamed" }

                val virtualSize =
                    buffer.getInt(sectionOffset + 8).toLong() and 0xFFFFFFFFL
                val rawSize =
                    buffer.getInt(sectionOffset + 16).toLong() and 0xFFFFFFFFL
                val rawOffset =
                    buffer.getInt(sectionOffset + 20).toLong() and 0xFFFFFFFFL

                sections.add(
                    "$sectionName | Virtual: $virtualSize | Raw: $rawSize | Offset: 0x${rawOffset.toString(16).uppercase()}"
                )
            }

            PeInfo(
                fileName = name,
                isPe = true,
                architecture = architecture,
                machine = machine,
                subsystem = subsystem,
                note = "PE metadata only; no decompilation performed",
                peFormat = peFormat,
                fileSize = fileSize,
                entryPoint = entryPoint,
                sectionCount = sectionCount,
                sections = sections,
                fileType = fileType
            )
        } catch (e: Exception) {
            invalid(name, fileSize, e.message ?: "Inspection error")
        }
    }

    private fun invalid(
        name: String,
        fileSize: Long,
        message: String
    ) = PeInfo(
        fileName = name,
        isPe = false,
        fileSize = fileSize,
        note = message
    )

    private fun getFileSize(
        resolver: ContentResolver,
        uri: Uri
    ): Long {
        return try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) cursor.getLong(index) else -1L
                } else {
                    -1L
                }
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}
