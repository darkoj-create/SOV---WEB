package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

internal data class TripCloudAsset(
    val id: String = "",
    val tripId: String = "",
    val assetType: String = "sovpkg",
    val title: String = "",
    val description: String = "",
    val storagePath: String = "",
    val originalFilename: String = "",
    val contentType: String = "application/zip",
    val sizeBytes: Long = 0L,
    val checksumSha256: String = "",
    val packageVersion: Int = 1,
    val downloadCount: Int = 0,
    val createdAt: String = "",
    val expiresAt: String = ""
) {
    val sizeLabel: String
        get() = when {
            sizeBytes <= 0L -> "—"
            sizeBytes < 1024L * 1024L -> "${(sizeBytes / 1024L).coerceAtLeast(1L)} KB"
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / 1024.0 / 1024.0)
        }
}

internal object TripAssetCloudRepository {
    private const val BUCKET = "sov-trip-assets"

    fun listAssets(context: Context, tripId: String): List<TripCloudAsset> {
        val cleanTripId = tripId.trim()
        if (cleanTripId.isBlank()) return emptyList()
        val session = ensureSession(context)
        runCatching { cleanupExpired(context) }
        val payload = JSONObject().put("p_trip_id", cleanTripId).toString()
        val text = SovHttpClient.post(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_trip_assets_for_trip",
            body = payload
        )
        val arr = JSONArray(text)
        return buildList {
            for (i in 0 until arr.length()) add(arr.getJSONObject(i).toAsset())
        }
    }

    fun uploadPackage(context: Context, tripId: String, file: File, title: String, description: String = ""): TripCloudAsset {
        val cleanTripId = tripId.trim()
        if (cleanTripId.isBlank()) error("Izlet nema paket.")
        if (!file.exists() || !file.isFile) error("Paket nije pronađen na uređaju.")
        val session = ensureSession(context)
        val safeFile = sanitizeFileName(file.name.ifBlank { "SOV_teren.sovpkg" })
        val path = listOf(
            cleanTripId,
            (SovPermissionsStore.loadPermissions(context).email.ifBlank { session.email }.ifBlank { "user" }).safePathPart(),
            UUID.randomUUID().toString() + "_" + safeFile
        ).joinToString("/")

        uploadToStorage(
            context = context,
            path = path,
            file = file,
            contentType = "application/zip"
        )

        val checksum = runCatching { sha256(file) }.getOrDefault("")
        val metadata = JSONObject()
            .put("source", "android_sovpkg")
            .put("note", "Paket: karta, trackovi, točke, privitci.")
            .put("checksum_sha256", checksum)
            .put("offline_ready", true)
        val payload = JSONObject()
            .put("p_trip_id", cleanTripId)
            .put("p_asset_type", "sovpkg")
            .put("p_title", title.trim().ifBlank { file.nameWithoutExtension.ifBlank { "Paket izleta" } })
            .put("p_description", description.trim())
            .put("p_storage_path", path)
            .put("p_original_filename", file.name)
            .put("p_content_type", "application/zip")
            .put("p_size_bytes", file.length())
            .put("p_checksum_sha256", checksum)
            .put("p_metadata", metadata)
            .toString()
        val response = SovHttpClient.post(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_trip_asset_register_v2",
            body = payload
        )
        return JSONObject(response).toAsset()
    }

    fun localFileFor(context: Context, asset: TripCloudAsset): File {
        val outDir = File(context.filesDir, "sov_trip_assets").apply { mkdirs() }
        val filename = sanitizeFileName(asset.originalFilename.ifBlank { asset.title.ifBlank { asset.id } + ".sovpkg" })
        return File(outDir, filename)
    }

    fun isAssetDownloaded(context: Context, asset: TripCloudAsset): Boolean {
        val file = localFileFor(context, asset)
        if (!file.exists() || !file.isFile) return false
        if (asset.sizeBytes > 0L && file.length() != asset.sizeBytes) return false
        val checksum = asset.checksumSha256.trim()
        if (checksum.isNotBlank()) return runCatching { sha256(file).equals(checksum, ignoreCase = true) }.getOrDefault(false)
        return true
    }

    fun downloadAsset(context: Context, asset: TripCloudAsset): File {
        if (asset.storagePath.isBlank()) error("Asset nema storage path.")
        val outFile = localFileFor(context, asset)
        if (isAssetDownloaded(context, asset)) return outFile
        val session = ensureSession(context)
        val url = "$SOV_SUPABASE_URL/storage/v1/object/$BUCKET/${asset.storagePath.storagePathEncoded()}"
        val conn = SovNetworkSecurity.openHttpConnection(url, "Trip assets").apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 120000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${SovHttpClient.activeToken(context) ?: SOV_SUPABASE_ANON_KEY}")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            val text = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            error("Preuzimanje nije uspjelo: HTTP $code ${text.take(160)}")
        }
        conn.inputStream.use { input -> FileOutputStream(outFile).use { output -> input.copyTo(output) } }
        conn.disconnect()
        if (!isAssetDownloaded(context, asset)) error("Paket je preuzet, ali ne odgovara očekivanoj veličini/checksumu.")
        runCatching { markDownloaded(context, asset.id) }
        return outFile
    }

    private fun markDownloaded(context: Context, assetId: String) {
        if (assetId.isBlank()) return
        ensureSession(context)
        SovHttpClient.post(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_trip_asset_mark_downloaded",
            body = JSONObject().put("p_asset_id", assetId).toString()
        )
    }

    private fun cleanupExpired(context: Context) {
        ensureSession(context)
        SovHttpClient.post(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_trip_assets_cleanup_expired",
            body = "{}"
        )
    }

    private fun ensureSession(context: Context): SovAuthSession {
        val sync = SovRoleSyncManager.syncNow(context, forceNetwork = false)
        val session = sync.session.takeIf { it.isLoggedIn } ?: SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Prijavi se u SOV Cloud za zajedničke pakete izleta.")
        return session
    }

    private fun uploadToStorage(context: Context, path: String, file: File, contentType: String) {
        val url = "$SOV_SUPABASE_URL/storage/v1/object/$BUCKET/${path.storagePathEncoded()}"
        val conn = SovNetworkSecurity.openHttpConnection(url, "Trip assets").apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20000
            readTimeout = 180000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${SovHttpClient.activeToken(context) ?: SOV_SUPABASE_ANON_KEY}")
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("x-upsert", "true")
        }
        file.inputStream().use { input -> conn.outputStream.use { output -> input.copyTo(output) } }
        val code = conn.responseCode
        if (code !in 200..299) {
            val text = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()
            error("Upload nije uspio: HTTP $code ${text.take(180)}")
        }
        conn.disconnect()
    }

    private fun JSONObject.toAsset(): TripCloudAsset = TripCloudAsset(
        id = optString("id", ""),
        tripId = optString("trip_id", ""),
        assetType = optString("asset_type", "sovpkg"),
        title = optString("title", ""),
        description = optString("description", ""),
        storagePath = optString("storage_path", ""),
        originalFilename = optString("original_filename", ""),
        contentType = optString("content_type", "application/zip"),
        sizeBytes = optLong("size_bytes", 0L),
        checksumSha256 = optString("checksum_sha256", optJSONObject("metadata")?.optString("checksum_sha256", "") ?: ""),
        packageVersion = optInt("package_version", 1),
        downloadCount = optInt("download_count", 0),
        createdAt = optString("created_at", ""),
        expiresAt = optString("expires_at", "")
    )

    private fun String.safePathPart(): String = lowercase(Locale.US)
        .replace(Regex("[^a-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "user" }

    private fun String.storagePathEncoded(): String = split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun sanitizeFileName(raw: String): String = raw
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "SOV_trip_asset.sovpkg" }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
