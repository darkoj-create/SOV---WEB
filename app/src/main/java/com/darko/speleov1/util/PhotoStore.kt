package com.darko.speleov1.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoStore {
    private const val PREFS_NAME = "speleo_photo_store"
    private const val KEY_PREFIX = "photos_"

    fun getPhotos(context: Context, recordId: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PREFIX + recordId, null) ?: return emptyList()
        return runCatching {
            Gson().fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    }

    fun addPhotoCopyFromUri(context: Context, recordId: String, sourceUri: Uri, displayName: String? = null): String? {
        val destination = createPhotoFile(context, recordId, displayName, extension = guessExtension(sourceUri))
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            syncPhotoToPublicFolder(context, recordId, displayName, destination)
            addStoredPath(context, recordId, destination.absolutePath, displayName)
            destination.absolutePath
        }.getOrNull()
    }

    fun createCameraPhoto(context: Context, recordId: String, displayName: String? = null): PendingCameraPhoto {
        val destination = createPhotoFile(context, recordId, displayName, extension = "jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destination
        )
        return PendingCameraPhoto(destination.absolutePath, uri)
    }

    fun addStoredPath(context: Context, recordId: String, path: String, displayName: String? = null) {
        syncPhotoToPublicFolder(context, recordId, displayName, File(path))
        val current = getPhotos(context, recordId).toMutableList()
        if (!current.contains(path)) current.add(path)
        save(context, recordId, current)
    }

    fun removePhoto(context: Context, recordId: String, storedValue: String) {
        val current = getPhotos(context, recordId).toMutableList()
        current.remove(storedValue)
        if (!storedValue.startsWith("content://") && !storedValue.startsWith("file://")) {
            runCatching { File(storedValue).delete() }
        }
        save(context, recordId, current)
    }

    fun resolvePhotoUri(context: Context, storedValue: String): Uri {
        return when {
            storedValue.startsWith("content://") || storedValue.startsWith("file://") -> Uri.parse(storedValue)
            else -> FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(storedValue)
            )
        }
    }

    private fun createPhotoFile(context: Context, recordId: String, displayName: String?, extension: String): File {
        val root = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "speleo_photos")
        val recordDir = File(root, folderNameForRecord(recordId, displayName)).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(recordDir, "${folderNameForRecord(recordId, displayName)}_$stamp.$extension")
    }

    private fun syncPhotoToPublicFolder(context: Context, recordId: String, displayName: String?, source: File) {
        if (!source.exists()) return
        runCatching {
            val publicDir = File(OfflineTileManager.publicPhotosRoot(), folderNameForRecord(recordId, displayName)).apply { mkdirs() }
            source.copyTo(File(publicDir, source.name), overwrite = true)
        }
    }

    private fun guessExtension(uri: Uri): String {
        val path = uri.lastPathSegment?.lowercase(Locale.US).orEmpty()
        return when {
            path.endsWith(".png") -> "png"
            path.endsWith(".webp") -> "webp"
            else -> "jpg"
        }
    }

    private fun folderNameForRecord(recordId: String, displayName: String?): String {
        val base = displayName?.takeIf { it.isNotBlank() } ?: recordId
        val safe = sanitize(base).trim('_')
        return safe.ifBlank { sanitize(recordId).ifBlank { "point" } }
    }

    private fun sanitize(input: String): String = input.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun save(context: Context, recordId: String, items: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFIX + recordId, Gson().toJson(items)).apply()
    }
}

data class PendingCameraPhoto(
    val absolutePath: String,
    val contentUri: Uri
)
