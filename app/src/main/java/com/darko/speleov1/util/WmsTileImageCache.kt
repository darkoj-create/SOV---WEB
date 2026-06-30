package com.darko.speleov1.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream

/**
 * Shared WMS tile image validation.
 *
 * Some WMS services return HTTP 200 with XML/HTML service exceptions or fully black image tiles.
 * Caching those responses makes the map keep flashing/drawing bad tiles even after the network
 * recovers, so every WMS renderer validates before writing to disk or memory.
 */
internal object WmsTileImageCache {
    fun decodeCachedBitmap(file: File, preferredConfig: Bitmap.Config, rejectMostlyBlack: Boolean): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val bitmap = decodeBytes(bytes, preferredConfig, rejectMostlyBlack)
        if (bitmap == null) {
            runCatching { file.delete() }
        }
        return bitmap
    }

    fun decodeBytes(bytes: ByteArray, preferredConfig: Bitmap.Config, rejectMostlyBlack: Boolean): Bitmap? {
        if (!looksLikeImagePayload(bytes)) return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = preferredConfig
        }
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull() ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        if (rejectMostlyBlack && bitmap.isMostlySolidBlack()) return null
        return bitmap
    }

    fun writeCacheFile(file: File, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp).use { it.write(bytes) }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }

    private fun looksLikeImagePayload(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        val first = bytes.firstOrNull { !it.toInt().toChar().isWhitespace() } ?: return false
        if (first == '<'.code.toByte()) return false
        val png = bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val jpg = bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        val gif = bytes.size >= 6 &&
            bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
        val webp = bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        return png || jpg || gif || webp
    }

    private fun Bitmap.isMostlySolidBlack(): Boolean {
        val stepX = (width / 16).coerceAtLeast(1)
        val stepY = (height / 16).coerceAtLeast(1)
        var samples = 0
        var opaqueBlack = 0
        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val color = getPixel(x, y)
                val alpha = Color.alpha(color)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                samples++
                if (alpha > 245 && red < 8 && green < 8 && blue < 8) {
                    opaqueBlack++
                }
            }
        }
        return samples >= 64 && opaqueBlack >= (samples * 0.98f).toInt()
    }
}
