package com.darko.speleov1.util

import android.content.Context
import com.darko.speleov1.MarkedPoint
import com.darko.speleov1.model.SpeleoRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

const val DEFAULT_ZAPISNIK_EMAIL = "darko.jeras@gmail.com"

data class OfflineZapisnikDraft(
    val email: String = DEFAULT_ZAPISNIK_EMAIL,
    val objectName: String = "",
    val plateNumber: String = "",
    val objectType: String = "",
    val hydrology: List<String> = emptyList(),
    val hydrogeology: List<String> = emptyList(),
    val nearestPlace: String = "",
    val coordinateSystem: String = "HTRS96/TM",
    val xCoordinate: String = "",
    val yCoordinate: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val activityPurpose: String = "Speleološka istraživanja",
    val activityPurposeOther: String = "",
    val activityDescription: String = "",
    val executionMethod: String = "",
    val threats: List<String> = emptyList(),
    val batColony: String = "nepoznato",
    val caveBivalve: String = "nepoznato",
    val caveSponge: String = "nepoznato",
    val olm: String = "nepoznato",
    val fossilFinds: String = "nepoznato",
    val teamMembers: String = ""
)

data class OfflineZapisnikTarget(
    val storageKey: String,
    val title: String,
    val draft: OfflineZapisnikDraft,
    val sourceHint: String
)

object OfflineZapisnikStore {
    private const val PREFS = "offline_zapisnik_store"
    private const val KEY_DRAFTS = "drafts"
    private val gson = Gson()

    fun loadDraft(context: Context, key: String): OfflineZapisnikDraft? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DRAFTS, null) ?: return null
        return runCatching {
            val type = object : TypeToken<Map<String, OfflineZapisnikDraft>>() {}.type
            gson.fromJson<Map<String, OfflineZapisnikDraft>>(raw, type).orEmpty()[key]
        }.getOrNull()
    }

    fun saveDraft(context: Context, key: String, draft: OfflineZapisnikDraft) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = loadAll(prefs).toMutableMap()
        existing[key] = draft
        prefs.edit().putString(KEY_DRAFTS, gson.toJson(existing)).apply()
    }

    private fun loadAll(prefs: android.content.SharedPreferences): Map<String, OfflineZapisnikDraft> {
        val raw = prefs.getString(KEY_DRAFTS, null) ?: return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, OfflineZapisnikDraft>>() {}.type
            gson.fromJson<Map<String, OfflineZapisnikDraft>>(raw, type).orEmpty()
        }.getOrDefault(emptyMap())
    }
}

private fun toHtrsStrings(lat: Double?, lon: Double?): Pair<String, String> {
    if (lat == null || lon == null) return "" to ""
    val converted = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
    return String.format(Locale.US, "%.2f", converted.x) to String.format(Locale.US, "%.2f", converted.y)
}

private fun mapObjectType(raw: String?): String {
    val normalized = raw.orEmpty().trim().lowercase(Locale.ROOT)
    return when {
        normalized.contains("špiljski sustav") -> "špiljski sustav"
        normalized.contains("jamski sustav") -> "jamski sustav"
        normalized.contains("kompleks") -> "kompleksni speleološki objekt"
        normalized.contains("špilja s jamskim") -> "špilja s jamskim ulazom"
        normalized.contains("jama sa špilj") -> "jama sa špiljskim ulazom"
        normalized == "špilja" || normalized == "spilja" -> "špilja"
        normalized == "jama" -> "jama"
        normalized == "sustav" -> "sustav"
        normalized == "kaverna" -> "kaverna"
        else -> ""
    }
}

fun createOfflineZapisnikTarget(record: SpeleoRecord, context: Context): OfflineZapisnikTarget {
    val key = "record:${record.id}"
    val saved = OfflineZapisnikStore.loadDraft(context, key)
    val (x, y) = toHtrsStrings(record.location.lat, record.location.lon)
    val prefilled = OfflineZapisnikDraft(
        objectName = record.name,
        plateNumber = (record.condition.plate_number?.takeIf { it.isNotBlank() } ?: record.cadastre.cadastral_number).orEmpty(),
        objectType = mapObjectType(record.classification.object_type),
        nearestPlace = listOfNotNull(record.location.nearest_place, record.location.locality, record.location.municipality).firstOrNull().orEmpty(),
        xCoordinate = x,
        yCoordinate = y
    )
    return OfflineZapisnikTarget(
        storageKey = key,
        title = record.name,
        draft = saved ?: prefilled,
        sourceHint = "SoV"
    )
}

fun createOfflineZapisnikTarget(point: MarkedPoint, context: Context): OfflineZapisnikTarget {
    val key = "point:${point.id}"
    val saved = OfflineZapisnikStore.loadDraft(context, key)
    val prefilled = OfflineZapisnikDraft(
        objectName = point.name,
        objectType = mapObjectType(point.type),
        nearestPlace = point.description,
        xCoordinate = String.format(Locale.US, "%.2f", point.htrsX),
        yCoordinate = String.format(Locale.US, "%.2f", point.htrsY)
    )
    return OfflineZapisnikTarget(
        storageKey = key,
        title = point.name,
        draft = saved ?: prefilled,
        sourceHint = "Custom waypoint"
    )
}

fun buildOfflineZapisnikShareText(draft: OfflineZapisnikDraft): String {
    fun line(label: String, value: String?): String? = value?.takeIf { it.isNotBlank() }?.let { "$label: $it" }
    fun lineList(label: String, values: List<String>): String? = values.takeIf { it.isNotEmpty() }?.let { "$label: ${it.joinToString(", ")}" }

    val purpose = if (draft.activityPurpose == "Other" && draft.activityPurposeOther.isNotBlank()) {
        draft.activityPurposeOther
    } else {
        draft.activityPurpose
    }

    return listOfNotNull(
        line("Email", draft.email),
        line("Ime speleološkog objekta", draft.objectName),
        line("Broj pločice na objektu", draft.plateNumber),
        line("Vrsta objekta", draft.objectType),
        lineList("Hidrološke karakteristike", draft.hydrology),
        lineList("Hidrogeološke pojave", draft.hydrogeology),
        line("Najbliže mjesto", draft.nearestPlace),
        line("Koordinatni sustav", draft.coordinateSystem),
        line("X koordinata", draft.xCoordinate),
        line("Y koordinata", draft.yCoordinate),
        line("Datum početka istraživanja", draft.startDate),
        line("Datum završetka istraživanja", draft.endDate),
        line("Svrha aktivnosti", purpose),
        line("Opis aktivnosti", draft.activityDescription),
        line("Način izvođenja aktivnosti", draft.executionMethod),
        lineList("Primjećene ugroze objekta", draft.threats),
        line("Primjećena fauna - kolonija šišmiša", draft.batColony),
        line("Primjećena fauna - špiljski školjkaš", draft.caveBivalve),
        line("Primjećena fauna - špiljska spužva", draft.caveSponge),
        line("Primjećena fauna - čovječja ribica", draft.olm),
        line("Primjećeni fosilni nalazi", draft.fossilFinds),
        line("Članovi istraživanja", draft.teamMembers)
    ).joinToString("\n")
}
