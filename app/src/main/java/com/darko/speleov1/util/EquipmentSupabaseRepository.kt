package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class EquipmentCloudItem(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val location: String,
    val total: Int,
    val available: Int,
    val status: String,
    val note: String,
    val unit: String = "kom",
    val subcategory: String = "Ostalo",
    val searchText: String = "",
    val groupKey: String = "",
    val displayName: String = "",
    val variantCount: Int = 1,
    val detailSummary: String = "",
    val bundleName: String = ""
)

internal data class EquipmentCloudRequestLine(
    val id: String,
    val equipmentItemId: String,
    val legacyId: String,
    val name: String,
    val quantity: Int,
    val unit: String = "kom",
    val note: String = ""
)

internal data class EquipmentCloudRequest(
    val id: String,
    val itemId: String,
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val dateFrom: String,
    val dateTo: String,
    val note: String,
    val status: String,
    val requesterName: String = "",
    val requesterEmail: String = "",
    val lines: List<EquipmentCloudRequestLine> = emptyList()
)

internal data class EquipmentCatalogManifest(
    val catalogVersion: String = "",
    val rawRowCount: Int = 0,
    val groupedRowCount: Int = 0,
    val lastChangedAt: String = ""
)

internal data class EquipmentCloudSnapshot(
    val items: List<EquipmentCloudItem>,
    val myRequests: List<EquipmentCloudRequest>,
    val armoryQueue: List<EquipmentCloudRequest>,
    val syncedAtMillis: Long = System.currentTimeMillis(),
    val fromCache: Boolean = false,
    val message: String = "",
    val rawItems: List<EquipmentCloudItem> = items,
    val catalogVersion: String = "",
    val rawRowCount: Int = rawItems.size,
    val groupedRowCount: Int = items.size,
    val lastChangedAt: String = ""
)

internal data class EquipmentInventoryCountPayload(
    val appId: String,
    val sourceTable: String,
    val sourceId: String,
    val code: String,
    val name: String,
    val category: String,
    val subcategory: String,
    val location: String,
    val expectedQty: Int,
    val countedQty: Int,
    val unit: String,
    val note: String = ""
)

internal data class EquipmentInventorySubmitResult(
    val synced: Boolean,
    val message: String,
    val sessionId: String = ""
)

internal data class EquipmentReturnLinePayload(
    val requestItemId: String,
    val name: String,
    val borrowedQty: Int,
    val returnedQty: Int,
    val destination: String,
    val note: String = ""
)

private fun EquipmentCloudItem.toCacheJson(): JSONObject = JSONObject()
    .put("id", id).put("code", code).put("name", name).put("category", category)
    .put("location", location).put("total", total).put("available", available)
    .put("status", status).put("note", note).put("unit", unit).put("subcategory", subcategory).put("search_text", searchText)
    .put("group_key", groupKey).put("display_name", displayName).put("variant_count", variantCount).put("detail_summary", detailSummary).put("bundle_name", bundleName)

private fun JSONObject.toEquipmentCacheItem(): EquipmentCloudItem = EquipmentCloudItem(
    id = optString("id"), code = optString("code"), name = optString("name"), category = optString("category"),
    location = optString("location"), total = optInt("total"), available = optInt("available"),
    status = optString("status"), note = optString("note"), unit = optString("unit", "kom"), subcategory = optString("subcategory", "Ostalo"), searchText = optString("search_text"),
    groupKey = optString("group_key"), displayName = optString("display_name"), variantCount = optInt("variant_count", 1), detailSummary = optString("detail_summary"), bundleName = optString("bundle_name")
)

private fun firstNonBlankCache(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

private fun EquipmentCloudRequestLine.toCacheJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("equipment_item_id", equipmentItemId)
    .put("equipment_legacy_id", legacyId)
    .put("name", name)
    .put("quantity", quantity)
    .put("unit", unit)
    .put("note", note)

private fun JSONObject.toEquipmentCacheRequestLine(): EquipmentCloudRequestLine = EquipmentCloudRequestLine(
    id = optString("id"),
    equipmentItemId = optString("equipment_item_id"),
    legacyId = optString("equipment_legacy_id"),
    name = firstNonBlankCache(optString("name"), optString("item_name"), optString("equipment_legacy_id"), "Oprema"),
    quantity = optInt("quantity", 1).coerceAtLeast(1),
    unit = optString("unit", "kom"),
    note = optString("note")
)

private fun EquipmentCloudRequest.toCacheJson(): JSONObject = JSONObject()
    .put("id", id).put("item_id", itemId).put("item_code", itemCode).put("item_name", itemName)
    .put("quantity", quantity).put("date_from", dateFrom).put("date_to", dateTo).put("note", note)
    .put("status", status).put("requester_name", requesterName).put("requester_email", requesterEmail)
    .put("lines", JSONArray().also { arr -> lines.forEach { arr.put(it.toCacheJson()) } })

private fun JSONObject.toEquipmentCacheRequest(): EquipmentCloudRequest = EquipmentCloudRequest(
    id = optString("id"), itemId = optString("item_id"), itemCode = optString("item_code"), itemName = optString("item_name"),
    quantity = optInt("quantity", 1), dateFrom = optString("date_from"), dateTo = optString("date_to"), note = optString("note"),
    status = optString("status"), requesterName = optString("requester_name"), requesterEmail = optString("requester_email"),
    lines = optJSONArray("lines")?.mapObjects { it.toEquipmentCacheRequestLine() }.orEmpty()
)

private inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> = buildList {
    for (i in 0 until length()) add(mapper(getJSONObject(i)))
}


internal object EquipmentCloudCache {
    private const val PREFS = "sov_equipment_cloud_cache_v2"
    private const val KEY_ITEMS = "items"
    private const val KEY_RAW_ITEMS = "raw_items"
    private const val KEY_MY_REQUESTS = "my_requests"
    private const val KEY_QUEUE = "queue"
    private const val KEY_SYNCED_AT = "synced_at"
    private const val KEY_CATALOG_VERSION = "catalog_version"
    private const val KEY_RAW_ROW_COUNT = "raw_row_count"
    private const val KEY_GROUPED_ROW_COUNT = "grouped_row_count"
    private const val KEY_LAST_CHANGED_AT = "last_changed_at"

    fun save(context: Context, snapshot: EquipmentCloudSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, JSONArray().also { arr -> snapshot.items.forEach { arr.put(it.toCacheJson()) } }.toString())
            .putString(KEY_RAW_ITEMS, JSONArray().also { arr -> snapshot.rawItems.forEach { arr.put(it.toCacheJson()) } }.toString())
            .putString(KEY_MY_REQUESTS, JSONArray().also { arr -> snapshot.myRequests.forEach { arr.put(it.toCacheJson()) } }.toString())
            .putString(KEY_QUEUE, JSONArray().also { arr -> snapshot.armoryQueue.forEach { arr.put(it.toCacheJson()) } }.toString())
            .putLong(KEY_SYNCED_AT, snapshot.syncedAtMillis)
            .putString(KEY_CATALOG_VERSION, snapshot.catalogVersion)
            .putInt(KEY_RAW_ROW_COUNT, snapshot.rawRowCount)
            .putInt(KEY_GROUPED_ROW_COUNT, snapshot.groupedRowCount)
            .putString(KEY_LAST_CHANGED_AT, snapshot.lastChangedAt)
            .apply()
    }

    fun load(context: Context): EquipmentCloudSnapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val itemsRaw = prefs.getString(KEY_ITEMS, null) ?: return null
        return runCatching {
            val cachedItems = JSONArray(itemsRaw).mapObjects { it.toEquipmentCacheItem() }
            val rawRaw = prefs.getString(KEY_RAW_ITEMS, null)
            EquipmentCloudSnapshot(
                items = cachedItems,
                myRequests = JSONArray(prefs.getString(KEY_MY_REQUESTS, "[]") ?: "[]").mapObjects { it.toEquipmentCacheRequest() },
                armoryQueue = JSONArray(prefs.getString(KEY_QUEUE, "[]") ?: "[]").mapObjects { it.toEquipmentCacheRequest() },
                syncedAtMillis = prefs.getLong(KEY_SYNCED_AT, 0L),
                fromCache = true,
                message = "Prikazujem zadnji spremljeni katalog.",
                rawItems = rawRaw?.let { JSONArray(it).mapObjects { obj -> obj.toEquipmentCacheItem() } } ?: cachedItems,
                catalogVersion = prefs.getString(KEY_CATALOG_VERSION, "") ?: "",
                rawRowCount = prefs.getInt(KEY_RAW_ROW_COUNT, rawRaw?.let { JSONArray(it).length() } ?: cachedItems.size),
                groupedRowCount = prefs.getInt(KEY_GROUPED_ROW_COUNT, cachedItems.size),
                lastChangedAt = prefs.getString(KEY_LAST_CHANGED_AT, "") ?: ""
            )
        }.getOrNull()
    }
}

internal object EquipmentSupabaseRepository {
    fun loadCachedSnapshot(context: Context): EquipmentCloudSnapshot? = EquipmentCloudCache.load(context)

    fun loadSnapshot(context: Context): EquipmentCloudSnapshot {
        val permissions = SovPermissionsStore.loadPermissions(context)
        val session = SovPermissionsStore.loadSession(context)
        val cached = EquipmentCloudCache.load(context)
        if (!session.isLoggedIn) {
            return cached ?: fallbackSnapshot("Nisi prijavljen u SOV Cloud.")
        }
        return runCatching {
            val manifest = fetchCatalogManifest(session.accessToken)
            val requests = runCatching { fetchRequests(session.accessToken, permissions) }.getOrDefault(cached?.let { it.myRequests + it.armoryQueue } ?: emptyList())
            val isArmoryManager = permissions.canManageEquipment || permissions.role.equals("admin", ignoreCase = true)
            val my = requests.filter { it.requesterEmail.equals(session.email, ignoreCase = true) }
            val queue = if (isArmoryManager) requests else emptyList()

            val cacheStillFresh = cached != null && cached.items.isNotEmpty() && manifest.catalogVersion.isNotBlank() && manifest.catalogVersion == cached.catalogVersion
            if (cacheStillFresh) {
                return@runCatching cached!!.copy(
                    myRequests = my,
                    armoryQueue = queue,
                    syncedAtMillis = System.currentTimeMillis(),
                    fromCache = false,
                    message = "Katalog je već ažuran (${manifest.groupedRowCount} grupa). Osvježeni su zahtjevi.",
                    rawRowCount = manifest.rawRowCount,
                    groupedRowCount = manifest.groupedRowCount,
                    lastChangedAt = manifest.lastChangedAt
                ).also { EquipmentCloudCache.save(context, it) }
            }

            val items = fetchItems(session.accessToken)
            val rawItems = fetchInventoryItems(session.accessToken).ifEmpty { items }
            EquipmentCloudSnapshot(
                items = items,
                myRequests = my,
                armoryQueue = queue,
                fromCache = false,
                message = if (cached == null) "Preuzet katalog iz SOV Clouda." else "Katalog se promijenio, preuzet je novi katalog.",
                rawItems = rawItems,
                catalogVersion = manifest.catalogVersion,
                rawRowCount = manifest.rawRowCount.takeIf { it > 0 } ?: rawItems.size,
                groupedRowCount = manifest.groupedRowCount.takeIf { it > 0 } ?: items.size,
                lastChangedAt = manifest.lastChangedAt
            ).also { EquipmentCloudCache.save(context, it) }
        }.getOrElse { err ->
            cached?.copy(message = "Cloud nije dostupan: prikazujem zadnji katalog. ${err.message.orEmpty().take(120)}")
                ?: fallbackSnapshot("Cloud nije dostupan i nema cachea: ${err.message.orEmpty().take(120)}")
        }
    }

    fun createRequest(context: Context, item: EquipmentCloudItem, quantity: Int, dateFrom: String, dateTo: String, note: String): EquipmentCloudRequest =
        createRequest(context, listOf(item to quantity), dateFrom, dateTo, note)

    fun createRequest(context: Context, items: List<Pair<EquipmentCloudItem, Int>>, dateFrom: String, dateTo: String, note: String): EquipmentCloudRequest {
        val cleanItems = items
            .map { (item, qty) -> item to qty.coerceAtLeast(1) }
            .filter { (item, _) -> item.name.isNotBlank() || item.displayName.isNotBlank() }
        if (cleanItems.isEmpty()) error("Košarica je prazna.")
        val permissions = SovPermissionsStore.loadPermissions(context)
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val normalizedFrom = normalizeDateOrNull(dateFrom)
        val normalizedTo = normalizeDateOrNull(dateTo)
        val requestTitle = if (cleanItems.size == 1) {
            cleanItems.first().first.displayName.ifBlank { cleanItems.first().first.name }
        } else {
            "${cleanItems.size} artikla: " + cleanItems.take(3).joinToString(", ") { (item, qty) -> "${item.displayName.ifBlank { item.name }} ×$qty" } + if (cleanItems.size > 3) "…" else ""
        }
        val requestBody = JSONObject()
            .put("requester_email", session.email)
            .put("requester_name", permissions.fullName.ifBlank { permissions.email.ifBlank { session.email } })
            .put("date_from", normalizedFrom)
            .put("date_to", normalizedTo)
            .put("note", note.ifBlank { "Zahtjev iz Android appa" })
            .put("status", "pending")
        val createdReq = JSONArray(requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_requests?select=*",
            method = "POST",
            accessToken = session.accessToken,
            body = requestBody.toString(),
            prefer = "return=representation"
        )).getJSONObject(0)
        val requestId = createdReq.optString("id")
        val rows = JSONArray()
        cleanItems.forEach { (item, qty) ->
            val requestDisplayName = item.displayName.ifBlank { item.name }
            val safeEquipmentItemId = item.id.takeIf { looksLikeUuid(it) }
            val itemBody = JSONObject()
                .put("request_id", requestId)
                .put("equipment_legacy_id", item.code.ifBlank { item.id })
                .put("name", requestDisplayName)
                .put("quantity", qty)
                .put("unit", item.unit.ifBlank { "kom" })
                .put("note", listOf(note, item.category, item.subcategory, item.location).filter { it.isNotBlank() }.joinToString(" · "))
            if (safeEquipmentItemId != null) itemBody.put("equipment_item_id", safeEquipmentItemId)
            rows.put(itemBody)
        }
        requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_request_items",
            method = "POST",
            accessToken = session.accessToken,
            body = rows.toString(),
            prefer = "return=minimal"
        )
        val first = cleanItems.first().first
        val requestLines = cleanItems.map { (item, qty) ->
            EquipmentCloudRequestLine(
                id = "local:${item.id}",
                equipmentItemId = item.id.takeIf { looksLikeUuid(it) }.orEmpty(),
                legacyId = item.code.ifBlank { item.id },
                name = item.displayName.ifBlank { item.name },
                quantity = qty,
                unit = item.unit.ifBlank { "kom" },
                note = listOf(note, item.category, item.subcategory, item.location).filter { it.isNotBlank() }.joinToString(" · ")
            )
        }
        return EquipmentCloudRequest(
            id = requestId,
            itemId = first.id,
            itemCode = cleanItems.joinToString(", ") { it.first.code }.take(80),
            itemName = requestTitle,
            quantity = cleanItems.sumOf { it.second },
            dateFrom = normalizedFrom.orEmpty(),
            dateTo = normalizedTo.orEmpty(),
            note = note.ifBlank { "Bez napomene" },
            status = "Zatraženo",
            requesterName = permissions.fullName,
            requesterEmail = session.email,
            lines = requestLines
        )
    }

    fun updateRequestStatus(context: Context, requestId: String, statusHr: String) {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val status = when (statusHr) {
            "Izdano" -> "issued"
            "Vraćeno" -> "returned"
            "Djelomično vraćeno" -> "partial_return"
            "Otkazano", "Odbijeno" -> "cancelled"
            else -> statusHr.lowercase(Locale.getDefault())
        }
        requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_requests?id=eq.${urlEncode(requestId)}",
            method = "PATCH",
            accessToken = session.accessToken,
            body = JSONObject().put("status", status).toString(),
            prefer = "return=minimal"
        )
    }

    fun returnRequestItems(context: Context, requestId: String, lines: List<EquipmentReturnLinePayload>, note: String): String {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        if (lines.isEmpty()) error("Nema stavki za povrat.")
        val hasPartial = lines.any { it.returnedQty.coerceAtLeast(0) < it.borrowedQty.coerceAtLeast(1) }
        val status = if (hasPartial) "partial_return" else "returned"
        val statusHr = if (hasPartial) "Djelomično vraćeno" else "Vraćeno"
        val summary = lines.joinToString(" | ") { line ->
            "${line.name}: ${line.returnedQty.coerceAtLeast(0)}/${line.borrowedQty.coerceAtLeast(1)} → ${line.destination}" +
                if (line.note.isNotBlank()) " (${line.note})" else ""
        }
        val requestNote = listOf(note, "APK povrat: $summary").filter { it.isNotBlank() }.joinToString("\n")
        requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_requests?id=eq.${urlEncode(requestId)}",
            method = "PATCH",
            accessToken = session.accessToken,
            body = JSONObject().put("status", status).put("note", requestNote).toString(),
            prefer = "return=minimal"
        )
        lines.filter { it.requestItemId.isNotBlank() && looksLikeUuid(it.requestItemId) }.forEach { line ->
            runCatching {
                requestText(
                    url = "$SOV_SUPABASE_URL/rest/v1/equipment_request_items?id=eq.${urlEncode(line.requestItemId)}",
                    method = "PATCH",
                    accessToken = session.accessToken,
                    body = JSONObject()
                        .put("note", "APK povrat: ${line.returnedQty.coerceAtLeast(0)}/${line.borrowedQty.coerceAtLeast(1)} → ${line.destination}" + if (line.note.isNotBlank()) " · ${line.note}" else "")
                        .toString(),
                    prefer = "return=minimal"
                )
            }
        }
        return statusHr
    }

    fun pendingInventoryCount(context: Context): Int = EquipmentInventoryPendingStore.count(context)

    fun syncPendingInventories(context: Context): Int {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) return 0
        val pending = EquipmentInventoryPendingStore.load(context)
        if (pending.isEmpty()) return 0
        val stillPending = mutableListOf<JSONObject>()
        var synced = 0
        pending.forEach { payload ->
            runCatching {
                submitInventoryRemote(
                    accessToken = session.accessToken,
                    locationName = payload.optString("location_name"),
                    categoryName = payload.optString("category_name"),
                    note = payload.optString("note"),
                    counts = payload.optJSONArray("counts")?.mapObjects { it.toInventoryCountPayload() }.orEmpty()
                )
            }.onSuccess { synced++ }.onFailure { stillPending.add(payload) }
        }
        EquipmentInventoryPendingStore.saveAll(context, stillPending)
        return synced
    }

    fun submitInventoryOrQueue(
        context: Context,
        locationName: String,
        categoryName: String,
        note: String,
        counts: List<EquipmentInventoryCountPayload>
    ): EquipmentInventorySubmitResult {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) {
            EquipmentInventoryPendingStore.enqueue(context, locationName, categoryName, note, counts)
            return EquipmentInventorySubmitResult(false, "Inventura je spremljena offline. Sinkronizirat će se kad se prijaviš u SOV Cloud.")
        }
        return runCatching {
            val sessionId = submitInventoryRemote(session.accessToken, locationName, categoryName, note, counts)
            EquipmentInventorySubmitResult(true, "Inventura zaključena i sinkronizirana.", sessionId)
        }.getOrElse { err ->
            EquipmentInventoryPendingStore.enqueue(context, locationName, categoryName, "Offline queue: ${err.message.orEmpty().take(120)} · $note", counts)
            EquipmentInventorySubmitResult(false, "Nema stabilne veze. Inventura je spremljena offline i čeka sync.")
        }
    }

    private fun submitInventoryRemote(
        accessToken: String,
        locationName: String,
        categoryName: String,
        note: String,
        counts: List<EquipmentInventoryCountPayload>
    ): String {
        val itemCount = counts.size
        val mismatchCount = counts.count { it.countedQty != it.expectedQty }
        val shortageCount = counts.count { it.countedQty < it.expectedQty }
        val surplusCount = counts.count { it.countedQty > it.expectedQty }
        val sessionBody = JSONObject()
            .put("location_name", locationName.ifBlank { "Sve lokacije" })
            .put("category_name", categoryName.ifBlank { "Sve kategorije" })
            .put("status", "closed")
            .put("item_count", itemCount)
            .put("mismatch_count", mismatchCount)
            .put("shortage_count", shortageCount)
            .put("surplus_count", surplusCount)
            .put("note", note.ifBlank { "Inventura iz Android appa" })
            .put("synced_from", "android")
        val created = JSONArray(requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_inventory_sessions?select=id",
            method = "POST",
            accessToken = accessToken,
            body = sessionBody.toString(),
            prefer = "return=representation"
        )).getJSONObject(0)
        val sessionId = created.optString("id")
        if (counts.isNotEmpty()) {
            val body = JSONArray().also { arr ->
                counts.forEach { c ->
                    arr.put(c.toInventoryJson().put("session_id", sessionId))
                }
            }.toString()
            requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/equipment_inventory_counts",
                method = "POST",
                accessToken = accessToken,
                body = body,
                prefer = "return=minimal"
            )
        }
        counts
            .filter { it.sourceTable == "equipment_items" && looksLikeUuid(it.sourceId) }
            .forEach { c ->
                runCatching {
                    requestText(
                        url = "$SOV_SUPABASE_URL/rest/v1/equipment_items?id=eq.${urlEncode(c.sourceId)}",
                        method = "PATCH",
                        accessToken = accessToken,
                        body = JSONObject()
                            .put("quantity", c.countedQty.coerceAtLeast(0))
                            .put("available", c.countedQty.coerceAtLeast(0))
                            .toString(),
                        prefer = "return=minimal"
                    )
                }
            }
        return sessionId
    }

    private fun fetchCatalogManifest(accessToken: String): EquipmentCatalogManifest {
        return runCatching {
            val arr = JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_equipment_catalog_manifest?select=*&limit=1",
                method = "GET",
                accessToken = accessToken,
                body = null
            ))
            val obj = arr.optJSONObject(0) ?: JSONObject()
            EquipmentCatalogManifest(
                catalogVersion = firstNonBlank(obj.optString("catalog_version"), obj.optString("version")),
                rawRowCount = obj.optIntFlexible("raw_row_count", obj.optIntFlexible("row_count", 0)),
                groupedRowCount = obj.optIntFlexible("grouped_row_count", 0),
                lastChangedAt = firstNonBlank(obj.optString("last_changed_at"), obj.optString("updated_at"))
            )
        }.getOrElse {
            // If the SQL manifest has not been installed yet, fall back to full catalog sync.
            EquipmentCatalogManifest()
        }
    }

    private fun fetchItems(accessToken: String): List<EquipmentCloudItem> {
        // 1.4.1: browsing/search uses the grouped catalog view.
        // Inventory uses fetchInventoryItems(), so grouped rows never pollute counting.
        val grouped = runCatching {
            JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_equipment_app_catalog_grouped?select=*&order=priority.asc,name.asc&limit=7000",
                method = "GET",
                accessToken = accessToken,
                body = null
            )).toItems { obj -> obj.toEquipmentAppCatalogItem() }
        }.getOrElse { emptyList() }
        if (grouped.isNotEmpty()) return grouped.sortedCatalog()

        val rawAppCatalog = fetchInventoryItems(accessToken)
        if (rawAppCatalog.isNotEmpty()) return rawAppCatalog.sortedCatalog()

        return fetchLegacyItems(accessToken).sortedCatalog()
    }

    private fun fetchInventoryItems(accessToken: String): List<EquipmentCloudItem> {
        // Raw normalized rows: required for Inventura mode and admin detail.
        return runCatching {
            JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_equipment_app_catalog?select=*&order=priority.asc,name.asc&limit=9000",
                method = "GET",
                accessToken = accessToken,
                body = null
            )).toItems { obj -> obj.toEquipmentAppCatalogItem() }
        }.getOrElse { emptyList() }.sortedCatalog()
    }

    private fun fetchLegacyItems(accessToken: String): List<EquipmentCloudItem> {
        // Legacy fallback if the v5.40+ SQL views have not been installed yet.
        val normalItems = runCatching {
            JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/equipment_items?select=*&order=category_name.asc,name.asc&limit=7000",
                method = "GET",
                accessToken = accessToken,
                body = null
            )).toItems { obj -> obj.toEquipmentItemFromSupabase() }
        }.getOrElse { emptyList() }

        val ropeItems = runCatching {
            JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/equipment_ropes?select=*&limit=3000",
                method = "GET",
                accessToken = accessToken,
                body = null
            )).toItems { obj -> obj.toEquipmentRopeFromSupabase() }
        }.getOrElse { emptyList() }

        val pieceItems = runCatching {
            JSONArray(requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/equipment_pieces?select=*&limit=5000",
                method = "GET",
                accessToken = accessToken,
                body = null
            )).toItems { obj -> obj.toEquipmentPieceFromSupabase() }
        }.getOrElse { emptyList() }

        return (normalItems + ropeItems + pieceItems)
            .filter { it.name.isNotBlank() || it.code.isNotBlank() }
            .distinctBy { it.id.ifBlank { it.code } }
    }

    private fun List<EquipmentCloudItem>.sortedCatalog(): List<EquipmentCloudItem> =
        sortedWith(compareBy<EquipmentCloudItem> { normalizeCategoryForSort(it.category) }.thenBy { it.displayName.ifBlank { it.name }.lowercase(Locale.getDefault()) })

    private fun fetchRequests(accessToken: String, permissions: SovAppPermissions): List<EquipmentCloudRequest> {
        val requests = JSONArray(requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_requests?select=*&order=created_at.desc&limit=150",
            method = "GET",
            accessToken = accessToken,
            body = null
        )).toItems { it }
        val requestItems = JSONArray(requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/equipment_request_items?select=*&order=created_at.desc&limit=300",
            method = "GET",
            accessToken = accessToken,
            body = null
        )).toItems { it }
        val byRequest = requestItems.groupBy { it.optString("request_id") }
        return requests.map { req ->
            val lines = byRequest[req.optString("id")].orEmpty()
            val requestLines = lines.map { line -> line.toEquipmentCacheRequestLine() }
            val firstItem = lines.firstOrNull()
            val itemNames = requestLines.map { line ->
                val name = firstNonBlank(line.name, line.legacyId, "Oprema")
                val qty = line.quantity.coerceAtLeast(1)
                "$name ×$qty"
            }
            val title = when {
                itemNames.isEmpty() -> "Oprema"
                itemNames.size == 1 -> itemNames.first()
                else -> "${itemNames.size} artikla: " + itemNames.take(4).joinToString(", ") + if (itemNames.size > 4) "…" else ""
            }
            EquipmentCloudRequest(
                id = req.optString("id"),
                itemId = firstItem?.optString("equipment_item_id").orEmpty(),
                itemCode = lines.joinToString(", ") { it.optString("equipment_legacy_id") }.take(90),
                itemName = title,
                quantity = lines.sumOf { it.optInt("quantity", 1).coerceAtLeast(1) }.coerceAtLeast(1),
                dateFrom = req.optString("date_from"),
                dateTo = req.optString("date_to"),
                note = req.optString("note"),
                status = statusToHr(req.optString("status")),
                requesterName = req.optString("requester_name"),
                requesterEmail = req.optString("requester_email"),
                lines = requestLines
            )
        }
    }

    private fun fallbackSnapshot(message: String): EquipmentCloudSnapshot = EquipmentCloudSnapshot(
        items = listOf(
            EquipmentCloudItem("local-uz-001", "UZ-001", "Statičko uže 10.5 mm / 50 m", "Užad", "Zagreb", 4, 3, "Dostupno", "Lokalni demo cache"),
            EquipmentCloudItem("local-op-014", "OP-014", "Komplet za postavljanje", "Postavljanje", "Zagreb", 2, 1, "Dostupno", "Lokalni demo cache"),
            EquipmentCloudItem("local-me-003", "ME-003", "Prva pomoć speleo komplet", "Medicinska", "Krasno", 2, 2, "Dostupno", "Lokalni demo cache")
        ),
        myRequests = emptyList(),
        armoryQueue = emptyList(),
        fromCache = true,
        message = message,
        catalogVersion = "fallback",
        rawRowCount = 3,
        groupedRowCount = 3
    )

    private fun statusToHr(status: String): String = when (status.lowercase(Locale.getDefault())) {
        "pending", "requested", "zatrazeno", "zatraženo", "approved", "reserved", "prepared", "odobreno" -> "Zatraženo"
        "issued", "izdano" -> "Izdano"
        "partial_return", "partial", "djelomicno", "djelomično" -> "Djelomično vraćeno"
        "returned", "closed", "vraćeno", "vraceno" -> "Vraćeno"
        "cancelled", "canceled", "rejected", "odbijeno", "otkazano" -> "Otkazano"
        "service", "servis" -> "Servis"
        else -> status.ifBlank { "Zatraženo" }
    }

    private fun JSONObject.toEquipmentAppCatalogItem(): EquipmentCloudItem {
        val total = optIntFlexible("total_qty", optIntFlexible("total", optIntFlexible("quantity", 0)))
        val available = optIntFlexible("available_qty", optIntFlexible("available", total))
        val category = firstNonBlank(optString("main_category"), optString("category"), optString("category_name"), "Ostalo")
        val subcategory = firstNonBlank(optString("subcategory"), optString("raw_subcategory"), "Ostalo")
        val rawName = firstNonBlank(optString("name"), optString("item_name"), optString("code"), "Oprema")
        val displayName = firstNonBlank(optString("display_name"), rawName)
        val groupKey = firstNonBlank(optString("catalog_group_key"), optString("group_key"), optString("app_id"), optString("id"))
        val detailSummary = firstNonBlank(optString("detail_summary"), optString("detail_label"))
        val statusRaw = firstNonBlank(optString("status"), optString("availability"))
        return EquipmentCloudItem(
            id = firstNonBlank(optString("app_id"), optString("id"), optString("source_id")),
            code = firstNonBlank(optString("code"), optString("sku"), optString("legacy_id"), optString("source_id")),
            name = rawName,
            category = category,
            location = firstNonBlank(optString("location_name"), optString("location"), "Nije upisano"),
            total = total,
            available = available,
            status = equipmentStatusToHr(statusRaw, optString("availability"), available),
            note = firstNonBlank(optString("note"), detailSummary, optString("source_table"), statusRaw),
            unit = firstNonBlank(optString("unit"), "kom"),
            subcategory = subcategory,
            searchText = firstNonBlank(optString("search_text"), listOf(rawName, displayName, category, subcategory, optString("note"), optString("code")).joinToString(" ")),
            groupKey = groupKey,
            displayName = displayName,
            variantCount = optInt("variant_count", 1).coerceAtLeast(1),
            detailSummary = detailSummary,
            bundleName = optString("bundle_name")
        )
    }

    private fun JSONObject.toEquipmentItemFromSupabase(): EquipmentCloudItem {
        val total = optIntFlexible("quantity", optIntFlexible("qty", 0))
        val available = optIntFlexible("available", total)
        val availability = optString("availability")
        val statusRaw = optString("status")
        val status = equipmentStatusToHr(statusRaw, availability, available)
        val rawCategory = firstNonBlank(optString("category_name"), optString("category"), optString("type"), "Ostalo")
        val rawSubcategory = firstNonBlank(optString("subcategory"), optString("sub_category"), optString("group_name"), "")
        val name = firstNonBlank(optString("name"), optString("item_name"), optString("model"), optString("sku"), "Oprema")
        val detailText = listOf(name, optString("model"), rawSubcategory, optString("internal_note"), optString("note"), optString("manufacturer")).joinToString(" ")
        val category = canonicalArmoryCategory(rawCategory, detailText)
        val subcategory = displayArmorySubcategory(name, optString("model"), rawSubcategory, optString("internal_note"), optString("note"))
        return EquipmentCloudItem(
            id = optString("id"),
            code = firstNonBlank(optString("sku"), optString("legacy_id"), optString("catalog_id"), optString("id")),
            name = name,
            category = category,
            location = firstNonBlank(optString("location_name"), optString("location"), "Nije upisano"),
            total = total,
            available = available,
            status = status,
            note = firstNonBlank(optString("internal_note"), optString("note"), statusRaw, availability),
            unit = optString("unit", "kom"),
            subcategory = subcategory
        )
    }

    private fun JSONObject.toEquipmentRopeFromSupabase(): EquipmentCloudItem {
        val code = firstNonBlank(optString("sku"), optString("legacy_id"), optString("id"))
        val length = optString("length_m")
        val diameter = optString("diameter_mm")
        val baseName = firstNonBlank(optString("name"), optString("model"), "Uže")
        val name = buildString {
            append(baseName)
            if (diameter.isNotBlank() && !baseName.contains(diameter, ignoreCase = true)) append(" · ").append(diameter).append(" mm")
            if (length.isNotBlank() && !baseName.contains(length, ignoreCase = true)) append(" · ").append(length).append(" m")
        }
        val availability = optString("availability")
        val statusRaw = optString("status")
        val unavailable = statusRaw.contains("posu", true) || statusRaw.contains("vani", true) || statusRaw.contains("izgubl", true) || statusRaw.contains("otpis", true) || availability.contains("izd", true)
        return EquipmentCloudItem(
            id = "rope:${optString("id")}",
            code = code,
            name = name,
            category = "Užad",
            location = firstNonBlank(optString("location_name"), optString("location"), "Nije upisano"),
            total = 1,
            available = if (unavailable) 0 else 1,
            status = equipmentStatusToHr(statusRaw, availability, if (unavailable) 0 else 1),
            note = listOf(optString("manufacturer"), optString("model"), optString("note")).filter { it.isNotBlank() }.joinToString(" · "),
            unit = "kom",
            subcategory = firstNonBlank(optString("subcategory"), "Užad")
        )
    }

    private fun JSONObject.toEquipmentPieceFromSupabase(): EquipmentCloudItem {
        val code = firstNonBlank(optString("sku"), optString("legacy_id"), optString("serial_number"), optString("id"))
        val availability = optString("availability")
        val statusRaw = optString("status")
        val unavailable = statusRaw.contains("posu", true) || statusRaw.contains("vani", true) || statusRaw.contains("izgubl", true) || statusRaw.contains("otpis", true) || availability.contains("izd", true)
        val available = if (unavailable) 0 else 1
        val name = firstNonBlank(optString("name"), optString("model"), optString("serial_number"), "Komad opreme")
        val rawCategory = firstNonBlank(optString("category_name"), optString("category"), "Ostalo")
        val rawSubcategory = firstNonBlank(optString("subcategory"), "")
        val detailText = listOf(name, optString("model"), rawSubcategory, optString("note"), optString("manufacturer")).joinToString(" ")
        return EquipmentCloudItem(
            id = "piece:${optString("id")}",
            code = code,
            name = name,
            category = canonicalArmoryCategory(rawCategory, detailText),
            location = firstNonBlank(optString("location_name"), optString("location"), "Nije upisano"),
            total = 1,
            available = available,
            status = equipmentStatusToHr(statusRaw, availability, available),
            note = listOf(optString("serial_number"), optString("note")).filter { it.isNotBlank() }.joinToString(" · "),
            unit = "kom",
            subcategory = displayArmorySubcategory(name, optString("model"), rawSubcategory, optString("note"), "")
        )
    }

    private fun firstNonBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun JSONObject.optIntFlexible(name: String, fallback: Int): Int {
        val raw = opt(name) ?: return fallback
        return when (raw) {
            is Number -> raw.toInt()
            else -> raw.toString().replace(",", ".").toDoubleOrNull()?.toInt() ?: fallback
        }
    }

    private fun stripArmoryText(value: String): String = value
        .replace("Č", "C").replace("č", "c")
        .replace("Ć", "C").replace("ć", "c")
        .replace("Š", "S").replace("š", "s")
        .replace("Ž", "Z").replace("ž", "z")
        .replace("Đ", "D").replace("đ", "d")
        .lowercase(Locale.getDefault())
        .trim()

    private fun canonicalArmoryCategory(raw: String, text: String): String {
        val r = stripArmoryText(raw)
        val t = stripArmoryText(listOf(raw, text).filter { it.isNotBlank() }.joinToString(" "))
        return when {
            Regex("uzad|uzetna|\buze\b|rope|prusik|gurt|traka|kolotur|transportna vreca").containsMatchIn(t) && !Regex("busil|bater|punjac|svrd").containsMatchIn(t) -> "Užad"
            Regex("postavlj|spit|sidrist|ploc|ring|anker|bolt|karabiner|matica|hms").containsMatchIn(t) && !Regex("descender|croll|bloker|pojas").containsMatchIn(t) -> "Oprema za postavljanje"
            Regex("crtan|mjeren|disto|kompas|topodroid|dokumentac|nacrt|skic").containsMatchIn(t) -> "Oprema za crtanje"
            (Regex("descender|\bstop\b|rig|maestro|id['’]?s|croll|krol|crol|bloker|zumar|ascender|pojas|sjedal|pedal|stremen|prsni|pupak|pupcano").containsMatchIn(t) && !Regex("penjack|penjac|alpinist|climbing").containsMatchIn(t)) -> "Osobna oprema - komplet"
            Regex("busil|baterija bosch|bosch.*bater|punjac|svrd|gbh18|gbh180|boschhammer").containsMatchIn(t) -> "Bušilice i baterije"
            Regex("elektro|foto|kamera|video|rasvjet|svjetl|lampa|ceona|ceo").containsMatchIn(t) -> "Elektro i foto oprema"
            Regex("medicin|medicina|prva pomoc|prva pom").containsMatchIn(t) -> "Medicinska oprema"
            Regex("ronil|ronjenje|neopren|maska|peraj|boca").containsMatchIn(t) -> "Ronilačka oprema"
            Regex("alpinist|alpin|penjack|penjac").containsMatchIn(t) -> "Alpinistička oprema"
            Regex("cisto podzemlje|ciscenje|cistoc|otpad").containsMatchIn(t) -> "Čisto podzemlje"
            Regex("prosir|prosirivanje|klin|cekic|macol|dlijet|stem").containsMatchIn(t) -> "Oprema za proširivanje"
            Regex("logor|kamp|ekspedic|sator|kuhal|plin|podlog|vreca za spavanje").containsMatchIn(t) -> "Oprema za logor"
            Regex("alat|kljuc|odvijac|klijest|toolbox").containsMatchIn(t) -> "Ostali alat"
            r.contains("ostalo") || r.contains("razno") -> "Ostalo"
            raw.isNotBlank() && !raw.equals("Oprema", true) && !raw.equals("Equipment", true) -> raw.trim()
            else -> "Ostalo"
        }
    }

    private fun displayArmorySubcategory(name: String, model: String, subcategory: String, note: String, internalNote: String): String {
        val text = stripArmoryText(listOf(name, model, subcategory, note, internalNote).filter { it.isNotBlank() }.joinToString(" "))
        return when {
            Regex("descender|\bstop\b|rig|maestro|id['’]?s").containsMatchIn(text) -> "Descenderi"
            Regex("croll|krol|crol|prsni").containsMatchIn(text) -> "Croll / prsni blokeri"
            Regex("zumar|ascender|rucn|bloker").containsMatchIn(text) -> "Ručni blokeri"
            Regex("pedal|stremen").containsMatchIn(text) -> "Pedale / stremeni"
            Regex("pojas|sjedal").containsMatchIn(text) -> "Pojasevi i sjedalice"
            Regex("karabiner|matica|hms|ok triact|amd").containsMatchIn(text) -> "Karabineri"
            Regex("\buze\b|rope|uzad").containsMatchIn(text) -> "Užad"
            Regex("prusik").containsMatchIn(text) -> "Prusici"
            Regex("gurt|traka").containsMatchIn(text) -> "Gurtne i trake"
            Regex("spit|sidri|ploc|ring|anker|bolt").containsMatchIn(text) -> "Spitovi i sidrišta"
            Regex("busil").containsMatchIn(text) -> "Bušilice"
            Regex("bater|aku").containsMatchIn(text) -> "Baterije"
            Regex("punja").containsMatchIn(text) -> "Punjači"
            Regex("svrd").containsMatchIn(text) -> "Svrdla"
            Regex("kacig|helmet").containsMatchIn(text) -> "Kacige"
            Regex("lamp|rasvjet|svjetl|ceo").containsMatchIn(text) -> "Lampe i rasvjeta"
            Regex("kombinezon|odijel|rukavic|cizm|obuc").containsMatchIn(text) -> "Odjeća i obuća"
            subcategory.isNotBlank() -> subcategory.trim()
            else -> "Ostalo"
        }
    }

    private fun equipmentStatusToHr(statusRaw: String, availability: String, available: Int): String = when {
        availability.contains("servis", true) || statusRaw.contains("servis", true) -> "Servis"
        availability.contains("otpis", true) || statusRaw.contains("otpis", true) -> "Otpisano"
        availability.contains("rezerv", true) || statusRaw.contains("rezerv", true) -> "Rezervirano"
        availability.contains("izd", true) || statusRaw.contains("izd", true) || available <= 0 -> "Izdano"
        else -> "Dostupno"
    }

    private fun normalizeCategoryForSort(category: String): String {
        val priority = when (category.trim().lowercase(Locale.getDefault())) {
            "užad", "uzad", "užad i užetna oprema", "uzad i uzetna oprema" -> "00"
            "oprema za postavljanje", "postavljanje" -> "01"
            "oprema za crtanje", "crtanje" -> "02"
            "osobna oprema - komplet" -> "03"
            "osobna oprema" -> "04"
            else -> "99"
        }
        val clean = category
            .replace("Č", "C").replace("č", "c")
            .replace("Ć", "C").replace("ć", "c")
            .replace("Š", "S").replace("š", "s")
            .replace("Ž", "Z").replace("ž", "z")
            .replace("Đ", "D").replace("đ", "d")
            .lowercase(Locale.getDefault())
        return "$priority-$clean"
    }

    private fun looksLikeUuid(value: String): Boolean = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(value)

    private fun EquipmentCloudItem.toJson(): JSONObject = JSONObject()
        .put("id", id).put("code", code).put("name", name).put("category", category)
        .put("location", location).put("total", total).put("available", available)
        .put("status", status).put("note", note).put("unit", unit).put("subcategory", subcategory).put("search_text", searchText)
    .put("group_key", groupKey).put("display_name", displayName).put("variant_count", variantCount).put("detail_summary", detailSummary).put("bundle_name", bundleName)

    private fun JSONObject.toEquipmentItem(): EquipmentCloudItem = EquipmentCloudItem(
        id = optString("id"), code = optString("code"), name = optString("name"), category = optString("category"),
        location = optString("location"), total = optInt("total"), available = optInt("available"),
        status = optString("status"), note = optString("note"), unit = optString("unit", "kom"), subcategory = optString("subcategory", "Ostalo")
    )

    private fun EquipmentCloudRequest.toJson(): JSONObject = JSONObject()
        .put("id", id).put("item_id", itemId).put("item_code", itemCode).put("item_name", itemName)
        .put("quantity", quantity).put("date_from", dateFrom).put("date_to", dateTo).put("note", note)
        .put("status", status).put("requester_name", requesterName).put("requester_email", requesterEmail)

    private fun JSONObject.toEquipmentRequest(): EquipmentCloudRequest = EquipmentCloudRequest(
        id = optString("id"), itemId = optString("item_id"), itemCode = optString("item_code"), itemName = optString("item_name"),
        quantity = optInt("quantity", 1), dateFrom = optString("date_from"), dateTo = optString("date_to"), note = optString("note"),
        status = optString("status"), requesterName = optString("requester_name"), requesterEmail = optString("requester_email")
    )

    private fun EquipmentInventoryCountPayload.toInventoryJson(): JSONObject = JSONObject()
        .put("app_id", appId)
        .put("source_table", sourceTable)
        .put("source_id", sourceId)
        .put("item_code", code)
        .put("item_name", name)
        .put("category_name", category)
        .put("subcategory", subcategory)
        .put("location_name", location)
        .put("expected_qty", expectedQty)
        .put("counted_qty", countedQty)
        .put("unit", unit)
        .put("note", note)

    private fun JSONObject.toInventoryCountPayload(): EquipmentInventoryCountPayload = EquipmentInventoryCountPayload(
        appId = optString("app_id"),
        sourceTable = optString("source_table"),
        sourceId = optString("source_id"),
        code = optString("item_code"),
        name = optString("item_name"),
        category = optString("category_name"),
        subcategory = optString("subcategory"),
        location = optString("location_name"),
        expectedQty = optInt("expected_qty"),
        countedQty = optInt("counted_qty"),
        unit = optString("unit", "kom"),
        note = optString("note")
    )

    private fun normalizeDateOrNull(value: String): String? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(clean)) return clean
        val patterns = listOf("dd.MM.yyyy", "dd.MM.yy", "dd/MM/yyyy", "dd/MM/yy")
        patterns.forEach { pattern ->
            runCatching {
                val parsed = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }.parse(clean)
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed ?: Date())
            }
        }
        return null
    }

    private fun requestText(url: String, method: String, accessToken: String, body: String?, prefer: String? = null): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 25000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Supabase HTTP $code: ${text.ifBlank { "bez detalja" }}")
        return text
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private inline fun <T> JSONArray.toItems(mapper: (JSONObject) -> T): List<T> = buildList {
        for (i in 0 until length()) add(mapper(getJSONObject(i)))
    }
}


private object EquipmentInventoryPendingStore {
    private const val PREFS = "sov_equipment_inventory_pending_v1"
    private const val KEY_QUEUE = "queue"

    fun count(context: Context): Int = load(context).size

    fun load(context: Context): List<JSONObject> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_QUEUE, "[]") ?: "[]"
        return runCatching { JSONArray(raw).mapObjects { it } }.getOrDefault(emptyList())
    }

    fun saveAll(context: Context, queue: List<JSONObject>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_QUEUE, JSONArray().also { arr -> queue.forEach { arr.put(it) } }.toString())
            .apply()
    }

    fun enqueue(context: Context, locationName: String, categoryName: String, note: String, counts: List<EquipmentInventoryCountPayload>) {
        val queue = load(context).toMutableList()
        queue.add(JSONObject()
            .put("location_name", locationName)
            .put("category_name", categoryName)
            .put("note", note)
            .put("created_at", System.currentTimeMillis())
            .put("counts", JSONArray().also { arr ->
                counts.forEach { c ->
                    arr.put(JSONObject()
                        .put("app_id", c.appId)
                        .put("source_table", c.sourceTable)
                        .put("source_id", c.sourceId)
                        .put("item_code", c.code)
                        .put("item_name", c.name)
                        .put("category_name", c.category)
                        .put("subcategory", c.subcategory)
                        .put("location_name", c.location)
                        .put("expected_qty", c.expectedQty)
                        .put("counted_qty", c.countedQty)
                        .put("unit", c.unit)
                        .put("note", c.note))
                }
            }))
        saveAll(context, queue)
    }
}
