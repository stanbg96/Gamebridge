
package com.gamebridge.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
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

    private const val HEADER_SAMPLE_SIZE = 8192

    fun inspect(
        resolver: ContentResolver,
        uri: Uri,
        name: String
    ): PeInfo {
        return try {
            val fileSize = getFileSize(resolver, uri)

            val bytes = resolver.openInputStream(uri)?.use { input ->
                val sample = ByteArray(HEADER_SAMPLE_SIZE)
                val n = input.read(sample)

                if (n > 0) sample.copyOf(n) else byteArrayOf()
            } ?: byteArrayOf()

            if (
                bytes.size < 64 ||
                bytes[0] != 0x4d.toByte() ||
                bytes[1] != 0x5a.toByte()
            ) {
                return PeInfo(
                    fileName = name,
                    isPe = false,
                    fileSize = fileSize,
                    note = "No MZ header"
                )
            }

            val buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)

            val peOffset = buffer.getInt(0x3c).toLong() and 0xffffffffL

            if (
                peOffset > bytes.size - 24 ||
                peOffset < 64 ||
                buffer.getInt(peOffset.toInt()) != 0x00004550
            ) {
                return PeInfo(
                    fileName = name,
                    isPe = false,
                    fileSize = fileSize,
                    note = "MZ found; PE signature not available"
                )
            }

            val coffOffset = peOffset.toInt() + 4

            val machineCode = buffer
                .getShort(coffOffset)
                .toInt() and 0xffff

            val sectionCount = buffer
                .getShort(coffOffset + 2)
                .toInt() and 0xffff

            val optionalHeaderSize = buffer
                .getShort(coffOffset + 16)
                .toInt() and 0xffff

            val characteristics = buffer
                .getShort(coffOffset + 18)
                .toInt() and 0xffff

            val optionalOffset = coffOffset + 20

            if (
                optionalHeaderSize < 2 ||
                optionalOffset.toLong() + optionalHeaderSize > bytes.size
            ) {
                return PeInfo(
                    fileName = name,
                    isPe = false,
                    fileSize = fileSize,
                    note = "Optional header is incomplete"
                )
            }

            val magic = buffer
                .getShort(optionalOffset)
                .toInt() and 0xffff

            val peFormat = when (magic) {
                0x10b -> "PE32"
                0x20b -> "PE32+"
                else -> "Unknown"
            }

            val entryPoint = if (optionalHeaderSize >= 20) {
                "0x" + buffer
                    .getInt(optionalOffset + 16)
                    .toUInt()
                    .toString(16)
                    .uppercase()
            } else {
                "Unavailable"
            }

            val subsystem = if (optionalHeaderSize >= 70) {
                when (
                    buffer.getShort(optionalOffset + 68).toInt() and 0xffff
                ) {
                    1 -> "Native"
                    2 -> "Windows GUI"
                    3 -> "Windows Console"
                    9 -> "Windows CE GUI"
                    10 -> "EFI Application"
                    else -> "Other"
                }
            } else {
                "Unavailable"
            }

            val machine = when (machineCode) {
                0x014c -> "Intel x86"
                0x8664 -> "AMD64"
                0x01c0 -> "ARM"
                0x01c4 -> "ARMv7"
                0xaa64 -> "ARM64"
                else -> "Unknown 0x${machineCode.toString(16)}"
            }

            val architecture = when (machineCode) {
                0x014c -> "x86 32-bit"
                0x8664 -> "x86-64"
                0x01c0,
                0x01c4 -> "ARM 32-bit"
                0xaa64 -> "ARM64"
                else -> "Unknown"
            }

            val fileType = if ((characteristics and 0x2000) != 0) {
                "DLL"
            } else {
                "EXE"
            }

            val sectionTableOffset =
                optionalOffset + optionalHeaderSize

            val sections = mutableListOf<String>()

            for (i in 0 until sectionCount.coerceAtMost(96)) {
                val sectionOffset = sectionTableOffset + i * 40

                if (sectionOffset + 40 > bytes.size) {
                    break
                }

                val nameBytes = ByteArray(8)

                for (j in 0 until 8) {
                    nameBytes[j] = bytes[sectionOffset + j]
                }

                val sectionName = nameBytes
                    .toString(Charsets.US_ASCII)
                    .trimEnd('\u0000')
                    .ifEmpty { "Unnamed" }

                sections.add(sectionName)
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
            PeInfo(
                fileName = name,
                isPe = false,
                note = e.message ?: "Inspection error"
            )
        }
    }

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

