package com.darko.speleov1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.util.Locale

/**
 * User-selected durable folder bridge.
 *
 * Android scoped storage does not reliably allow direct reads from Download/SOV/Offline.
 * The stable solution is: user selects the SOV folder once, we keep the tree URI,
 * then copy supported files into the app working folders without parsing them on startup.
 */
object SovLinkedOfflineFolder {
    private const val PREFS = "sov_linked_offline_folder"
    private const val KEY_TREE_URI = "tree_uri"

    data class ScanResult(
        val gpx: Int = 0,
        val kml: Int = 0,
        val layers: Int = 0,
        val maps: Int = 0,
        val skipped: Int = 0
    ) {
        val total: Int get() = gpx + kml + layers + maps
        fun summary(): String = "GPX $gpx · KML $kml · karte $maps"
    }

    fun linkedUri(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun isLinked(context: Context): Boolean = linkedUri(context) != null

    fun saveLinkedFolder(context: Context, treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(treeUri, flags) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .apply()
    }

    fun forgetLinkedFolder(context: Context) {
        linkedUri(context)?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TREE_URI).apply()
    }

    fun scanAndRestore(context: Context): ScanResult {
        val treeUri = linkedUri(context) ?: return ScanResult()
        OfflineTileManager.ensureOfflineFolderStructure(context)
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return ScanResult(skipped = 1)
        return scanChildren(context, treeUri, rootId, path = "")
    }

    private fun scanChildren(context: Context, treeUri: Uri, documentId: String, path: String): ScanResult {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        var result = ScanResult()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val cursor = runCatching { context.contentResolver.query(childrenUri, projection, null, null, null) }.getOrNull()
            ?: return result.copy(skipped = result.skipped + 1)
        cursor.use { c ->
            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext()) {
                val childId = c.getString(idCol) ?: continue
                val name = c.getString(nameCol).orEmpty()
                val mime = c.getString(mimeCol).orEmpty()
                val size = if (sizeCol >= 0) runCatching { c.getLong(sizeCol) }.getOrDefault(0L) else 0L
                val modified = if (modCol >= 0) runCatching { c.getLong(modCol) }.getOrDefault(0L) else 0L
                val childPath = listOf(path, name).filter { it.isNotBlank() }.joinToString("/")
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val nested = scanChildren(context, treeUri, childId, childPath)
                    result = result.plus(nested)
                } else {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    val copied = copySupportedFile(context, uri, name, childPath, size, modified)
                    result = result.plus(copied)
                }
            }
        }
        return result
    }

    private fun copySupportedFile(context: Context, uri: Uri, name: String, path: String, size: Long, modified: Long): ScanResult {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.ROOT)
        if (ext.isBlank()) return ScanResult(skipped = 1)
        return when (ext) {
            "gpx" -> {
                val ok = copyUriToFile(context, uri, OfflineTileManager.gpxRoot(context), name, size, modified)
                ScanResult(gpx = if (ok) 1 else 0, layers = if (ok) 1 else 0, skipped = if (ok) 0 else 1)
            }
            "kml", "kmz" -> {
                val ok = copyUriToFile(context, uri, OfflineTileManager.kmlRoot(context), name, size, modified)
                ScanResult(kml = if (ok) 1 else 0, layers = if (ok) 1 else 0, skipped = if (ok) 0 else 1)
            }
            "geojson", "json", "csv", "gpkg", "geopackage" -> {
                val ok = copyUriToFile(context, uri, OfflineTileManager.kmlRoot(context), name, size, modified)
                ScanResult(layers = if (ok) 1 else 0, skipped = if (ok) 0 else 1)
            }
            "mbtiles", "pmtiles" -> {
                val mapName = OfflineTileManager.sanitizeMapName(name.substringBeforeLast('.').ifBlank { "offline_karta" })
                val mapDir = File(OfflineTileManager.mbtilesRoot(context), mapName).apply { mkdirs() }
                val targetName = if (ext == "pmtiles") "tiles.pmtiles" else "tiles.mbtiles"
                val ok = copyUriToFile(context, uri, mapDir, targetName, size, modified)
                ScanResult(maps = if (ok) 1 else 0, skipped = if (ok) 0 else 1)
            }
            else -> ScanResult(skipped = 1)
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, targetDir: File, targetName: String, size: Long, modified: Long): Boolean {
        targetDir.mkdirs()
        val safeName = safeFileName(targetName)
        val target = uniqueTarget(targetDir, safeName, size)
        if (target.exists()) return false
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            if (modified > 0L) runCatching { target.setLastModified(modified) }
            true
        }.getOrElse {
            runCatching { target.delete() }
            false
        }
    }

    private fun uniqueTarget(dir: File, name: String, size: Long): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        val existingSame = dir.listFiles()?.firstOrNull { it.name.equals(name, true) && (size <= 0L || it.length() == size) }
        if (existingSame != null) return existingSame
        var candidate = File(dir, name)
        var i = 2
        while (candidate.exists()) {
            val nextName = if (ext.isBlank()) "${base}_$i" else "${base}_$i.$ext"
            candidate = File(dir, nextName)
            i++
        }
        return candidate
    }

    private fun safeFileName(raw: String): String =
        raw.trim().ifBlank { "sov_file" }
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .take(120)

    private fun ScanResult.plus(other: ScanResult): ScanResult = ScanResult(
        gpx = gpx + other.gpx,
        kml = kml + other.kml,
        layers = layers + other.layers,
        maps = maps + other.maps,
        skipped = skipped + other.skipped
    )
}
