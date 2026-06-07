package com.darko.speleov1.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val GITHUB_OWNER = "darkoj-create"
private const val GITHUB_REPO = "SOV-APP-ADMIN"
private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
private const val DEFAULT_APK_MIME = "application/vnd.android.package-archive"

sealed interface UpdateCheckResult {
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

data class AppUpdateInfo(
    val currentVersionName: String,
    val currentVersionCode: Long,
    val latestVersionName: String,
    val latestVersionCode: Long?,
    val releaseTag: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkName: String,
    val apkUrl: String,
    val publishedAt: String?
)

private data class GithubReleaseResponse(
    @SerializedName("tag_name") val tagName: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") val assets: List<GithubReleaseAsset> = emptyList()
)

private data class GithubReleaseAsset(
    @SerializedName("name") val name: String? = null,
    @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
    @SerializedName("content_type") val contentType: String? = null
)

private data class ReleaseUpdateJson(
    val versionCode: Long? = null,
    val versionName: String? = null,
    val apkFileName: String? = null,
    val notes: String? = null
)

object AppUpdateManager {
    private val gson = Gson()

    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val releaseJson = fetchText(GITHUB_LATEST_RELEASE_URL)
            val latestRelease = gson.fromJson(releaseJson, GithubReleaseResponse::class.java)
            val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
            val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val currentVersionName = packageInfo.versionName?.trim().orEmpty().ifBlank { "0" }

            val updateMetaAsset = latestRelease.assets.firstOrNull { it.name.equals("update.json", ignoreCase = true) && !it.browserDownloadUrl.isNullOrBlank() }
            val releaseUpdateJson = updateMetaAsset?.browserDownloadUrl?.let { url ->
                runCatching { gson.fromJson(fetchText(url), ReleaseUpdateJson::class.java) }.getOrNull()
            }

            val apkAsset = resolveApkAsset(latestRelease.assets, releaseUpdateJson)
                ?: return@runCatching UpdateCheckResult.Error("Na zadnjem GitHub releaseu nema APK asseta.")

            val latestVersionName = releaseUpdateJson?.versionName?.trim().orEmpty().ifBlank {
                normalizeTagToVersion(latestRelease.tagName)
            }
            val latestVersionCode = releaseUpdateJson?.versionCode
            val notes = releaseUpdateJson?.notes?.takeIf { it.isNotBlank() }
                ?: latestRelease.body.orEmpty().trim()

            val updateAvailable = when {
                latestVersionCode != null -> latestVersionCode > currentVersionCode
                latestVersionName.isNotBlank() -> compareVersionNames(latestVersionName, currentVersionName) > 0
                else -> false
            }

            if (!updateAvailable) {
                UpdateCheckResult.UpToDate(currentVersionName)
            } else {
                UpdateCheckResult.Available(
                    AppUpdateInfo(
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode,
                        latestVersionName = latestVersionName.ifBlank { latestRelease.tagName.orEmpty() },
                        latestVersionCode = latestVersionCode,
                        releaseTag = latestRelease.tagName.orEmpty(),
                        releaseNotes = notes,
                        releasePageUrl = latestRelease.htmlUrl.orEmpty(),
                        apkName = apkAsset.name.orEmpty().ifBlank { "sov-update.apk" },
                        apkUrl = apkAsset.browserDownloadUrl.orEmpty(),
                        publishedAt = latestRelease.publishedAt
                    )
                )
            }
        }.getOrElse { throwable ->
            UpdateCheckResult.Error(throwable.message ?: "Greška pri provjeri ažuriranja.")
        }
    }

    suspend fun downloadApk(context: Context, info: AppUpdateInfo): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val safeName = sanitizeFileName(info.apkName.ifBlank { "sov-update.apk" })
            val targetFile = File(targetDir, safeName)
            downloadBinary(info.apkUrl, targetFile)
            targetFile
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun launchApkInstaller(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DEFAULT_APK_MIME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun resolveApkAsset(
        assets: List<GithubReleaseAsset>,
        updateJson: ReleaseUpdateJson?
    ): GithubReleaseAsset? {
        val preferredName = updateJson?.apkFileName?.trim().orEmpty()
        if (preferredName.isNotBlank()) {
            assets.firstOrNull { it.name.equals(preferredName, ignoreCase = true) && !it.browserDownloadUrl.isNullOrBlank() }?.let { return it }
        }
        return assets.firstOrNull {
            val name = it.name.orEmpty().lowercase(Locale.US)
            !it.browserDownloadUrl.isNullOrBlank() && name.endsWith(".apk")
        }
    }

    private fun normalizeTagToVersion(tag: String?): String {
        return tag.orEmpty().trim().removePrefix("v").removePrefix("V")
    }

    private fun compareVersionNames(left: String, right: String): Int {
        val leftParts = extractVersionNumbers(left)
        val rightParts = extractVersionNumbers(right)
        val maxSize = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until maxSize) {
            val l = leftParts.getOrElse(index) { 0 }
            val r = rightParts.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun extractVersionNumbers(raw: String): List<Int> {
        val normalized = raw.trim().removePrefix("v").removePrefix("V")
        return normalized
            .split('.', '-', '_')
            .mapNotNull { token -> token.filter(Char::isDigit).takeIf { it.isNotBlank() }?.toIntOrNull() }
            .ifEmpty { listOf(0) }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SOV-APP-ADMIN")
        }
        return connection.useAndDisconnect {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            if (connection.responseCode !in 200..299) {
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val friendlyMessage = when (connection.responseCode) {
                    404 -> "Nema objavljenog GitHub releasea za darkoj-create/SOV-APP ili repo nije javan."
                    403 -> "GitHub trenutno blokira provjeru ažuriranja ili je dosegnut limit zahtjeva."
                    else -> "GitHub update check nije uspio."
                }
                error(friendlyMessage)
            }
            stream.bufferedReader().use { it.readText() }
        }
    }

    private fun downloadBinary(url: String, targetFile: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 60000
            setRequestProperty("User-Agent", "SOV-APP-ADMIN")
        }
        connection.useAndDisconnect {
            if (connection.responseCode !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("APK download nije uspio (${connection.responseCode}). ${body.take(180)}")
            }
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

private inline fun <T> HttpURLConnection.useAndDisconnect(block: () -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
} else {
    @Suppress("DEPRECATION")
    getPackageInfo(packageName, 0)
}
