package com.darko.speleov1.data

import android.content.Context
import com.darko.speleov1.model.DatasetEnvelope
import com.google.gson.Gson
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class SpeleoRepository(private val context: Context) {
    private val gson: Gson by lazy(LazyThreadSafetyMode.PUBLICATION) { Gson() }

    fun loadDataset(assetName: String = DEFAULT_DATASET_ASSET_GZ): DatasetEnvelope {
        cachedDatasets[assetName]?.let { return it }

        val candidates = datasetAssetCandidates(assetName)
        var lastError: Throwable? = null

        for (candidate in candidates) {
            val result = runCatching {
                context.assets.open(candidate).use { raw ->
                    readDatasetFromAssetStream(raw, candidate)
                }
            }
            if (result.isSuccess) {
                return result.getOrThrow().also { cachedDatasets[assetName] = it }
            }
            lastError = result.exceptionOrNull()
        }

        val availableAssets = runCatching {
            context.assets.list("")?.joinToString().orEmpty()
        }.getOrDefault("unknown")

        throw IllegalStateException(
            "Ne mogu učitati bazu. Pokušano: ${candidates.joinToString()}. Assets: $availableAssets",
            lastError
        )
    }

    fun loadKatastarDataset(): DatasetEnvelope = loadDataset(KATASTAR_DATASET_ASSET_GZ)

    private fun readDatasetFromAssetStream(
        raw: java.io.InputStream,
        assetName: String
    ): DatasetEnvelope {
        val buffered = BufferedInputStream(raw, 64 * 1024)
        buffered.mark(2)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()

        val input = if (first == GZIP_MAGIC_1 && second == GZIP_MAGIC_2) {
            GZIPInputStream(buffered, 64 * 1024)
        } else {
            buffered
        }

        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8), 64 * 1024).use { reader ->
            return gson.fromJson(reader, DatasetEnvelope::class.java)
                ?: throw IllegalStateException("Prazna ili neispravna SOV baza: $assetName")
        }
    }

    private fun datasetAssetCandidates(assetName: String): List<String> {
        return buildList {
            add(assetName)
            if (assetName.endsWith(".gz", ignoreCase = true)) {
                add(assetName.removeSuffix(".gz"))
            } else {
                add("$assetName.gz")
            }
            if (assetName == DEFAULT_DATASET_ASSET_GZ || assetName == DEFAULT_DATASET_ASSET_JSON) {
                add(DEFAULT_DATASET_ASSET_GZ)
                add(DEFAULT_DATASET_ASSET_JSON)
            }
        }.distinct()
    }

    companion object {
        private const val DEFAULT_DATASET_ASSET_JSON = "baza_velebit_2026_android_v2.json"
        private const val DEFAULT_DATASET_ASSET_GZ = "baza_velebit_2026_android_v2.json.gz"
        private const val KATASTAR_DATASET_ASSET_GZ = "katastar_crospeleo_2026_android_v1.json.gz"
        private const val GZIP_MAGIC_1 = 0x1f
        private const val GZIP_MAGIC_2 = 0x8b

        private val cachedDatasets = java.util.concurrent.ConcurrentHashMap<String, DatasetEnvelope>()
    }
}
