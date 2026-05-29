package com.darko.speleov1.util

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight WMS GetCapabilities reader used by the Custom WMS Manager.
 * It intentionally avoids heavy GIS dependencies: the map renderer still uses WmsConfig,
 * while this helper only discovers layer names, supported CRS/SRS values and styles.
 */
data class WmsLayerOption(
    val name: String,
    val title: String,
    val crsOptions: List<String>,
    val styleOptions: List<String>
)

data class WmsCapabilitiesResult(
    val serviceUrl: String,
    val version: String,
    val layers: List<WmsLayerOption>
)

object WmsCapabilitiesClient {
    suspend fun fetch(rawUrl: String): Result<WmsCapabilitiesResult> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = MapLayerPrefs.cleanWmsBaseUrl(rawUrl)
            require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) { "WMS URL mora početi s http:// ili https://." }
            val separator = if (baseUrl.contains('?')) "&" else "?"
            val capabilitiesUrl = baseUrl + separator + "SERVICE=WMS&REQUEST=GetCapabilities"
            val connection = (URL(capabilitiesUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SOV-Android-WMS-Manager/1.2.6")
            }
            connection.inputStream.use { input ->
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(input, null)
                }
                parseCapabilities(parser, baseUrl)
            }
        }
    }

    private data class LayerAccumulator(
        val inheritedCrs: List<String>,
        var name: String = "",
        var title: String = "",
        val localCrs: MutableList<String> = mutableListOf(),
        val styles: MutableList<String> = mutableListOf()
    ) {
        fun allCrs(): List<String> = (inheritedCrs + localCrs).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun parseCapabilities(parser: XmlPullParser, serviceUrl: String): WmsCapabilitiesResult {
        val layers = mutableListOf<WmsLayerOption>()
        val stack = mutableListOf<LayerAccumulator>()
        var version = "1.3.0"
        var currentTag = ""
        var insideStyle = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.orEmpty()
                    currentTag = tag
                    when (tag.lowercase()) {
                        "wms_capabilities", "wmt_ms_capabilities" -> {
                            version = parser.getAttributeValue(null, "version")?.trim().orEmpty().ifBlank { version }
                        }
                        "layer" -> {
                            stack.add(LayerAccumulator(inheritedCrs = stack.lastOrNull()?.allCrs().orEmpty()))
                        }
                        "style" -> insideStyle = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotBlank() && stack.isNotEmpty()) {
                        val current = stack.last()
                        when (currentTag.lowercase()) {
                            "name" -> if (insideStyle) current.styles.add(text) else if (current.name.isBlank()) current.name = text
                            "title" -> if (!insideStyle && current.title.isBlank()) current.title = text
                            "crs", "srs" -> current.localCrs.add(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.orEmpty()
                    when (tag.lowercase()) {
                        "style" -> insideStyle = false
                        "layer" -> {
                            val finished = stack.removeAt(stack.lastIndex)
                            if (finished.name.isNotBlank()) {
                                layers.add(
                                    WmsLayerOption(
                                        name = finished.name,
                                        title = finished.title.ifBlank { finished.name },
                                        crsOptions = finished.allCrs(),
                                        styleOptions = finished.styles.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                    )
                                )
                            }
                        }
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }

        return WmsCapabilitiesResult(
            serviceUrl = serviceUrl,
            version = version,
            layers = layers.distinctBy { it.name }.take(250)
        )
    }
}
