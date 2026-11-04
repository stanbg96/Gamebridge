package com.gamebridge.app

import android.content.ContentResolver
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PeInfo(
    val fileName: String,
    val isPe: Boolean,
    val architecture: String = "Unknown",
    val machine: String = "Unknown",
    val subsystem: String = "Unknown",
    val note: String = ""
)

object PeInspector {

    fun inspect(
        resolver: ContentResolver,
        uri: Uri,
        name: String
    ): PeInfo {
        return try {
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val sample = ByteArray(4096)
                val n = input.read(sample)

                if (n > 0) {
                    sample.copyOf(n)
                } else {
                    byteArrayOf()
                }
            } ?: byteArrayOf()

            if (
                bytes.size < 64 ||
                bytes[0] != 0x4d.toByte() ||
                bytes[1] != 0x5a.toByte()
            ) {
                return PeInfo(
                    name,
                    false,
                    note = "No MZ header"
                )
            }

            val buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)

            val offset = buffer.getInt(0x3c).toLong() and 0xffffffffL

            if (
                offset + 26 >= bytes.size ||
                buffer.getInt(offset.toInt()) != 0x00004550
            ) {
                return PeInfo(
                    name,
                    false,
                    note = "MZ found; PE signature outside header sample"
                )
            }

            val code = buffer
                .getShort(offset.toInt() + 4)
                .toInt() and 0xffff

            val machine = when (code) {
                0x014c -> "Intel x86"
                0x8664 -> "AMD64"
                0x01c0 -> "ARM"
                0x01c4 -> "ARMv7"
                0xaa64 -> "ARM64"
                else -> "Unknown 0x${code.toString(16)}"
            }

            val architecture = when (code) {
                0x014c -> "x86 32-bit"
                0x8664 -> "x86-64"
                0x01c0,
                0x01c4 -> "ARM 32-bit"
                0xaa64 -> "ARM64"
                else -> "Unknown"
            }

            val optionalHeaderSize = buffer
                .getShort(offset.toInt() + 20)
                .toInt() and 0xffff

            val subsystemOffset = offset.toInt() + 24 + 68

            val subsystem = if (
                optionalHeaderSize >= 70 &&
                subsystemOffset + 2 <= bytes.size
            ) {
                when (
                    buffer.getShort(subsystemOffset).toInt() and 0xffff
                ) {
                    2 -> "Windows GUI"
                    3 -> "Windows Console"
                    1 -> "Native"
                    else -> "Other"
                }
            } else {
                "Unavailable"
            }

            PeInfo(
                fileName = name,
                isPe = true,
                architecture = architecture,
                machine = machine,
                subsystem = subsystem,
                note = "PE metadata only; no decompilation performed"
            )

        } catch (e: Exception) {
            PeInfo(
                fileName = name,
                isPe = false,
                note = e.message ?: "Inspection error"
            )
        }
    }
}
