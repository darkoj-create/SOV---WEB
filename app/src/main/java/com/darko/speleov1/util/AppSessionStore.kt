package com.darko.speleov1.util

import android.content.Context
import com.darko.speleov1.AppTab
import com.darko.speleov1.AppLanguage
import com.darko.speleov1.MapOrientationMode
import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SourceFilter
import com.google.gson.Gson

private data class SessionFilterDto(
    val query: String = "",
    val sourceFilter: String = SourceFilter.ALL.name,
    val cadastreFilter: String = CadastreFilter.ALL.name,
    val caveTypeFilter: String = CaveTypeFilter.ALL.name,
    val areaFilter: String = "",
    val distanceFilterKm: Int? = null,
    val onlyWithDescription: Boolean = false,
    val fieldTaskFilters: List<String> = emptyList()
)

private data class SearchPresetDto(
    val name: String = "",
    val filters: SessionFilterDto = SessionFilterDto()
)

data class SearchPreset(
    val name: String,
    val filters: FilterState
)

data class AppSessionSnapshot(
    val currentTab: AppTab = AppTab.HOME,
    val appLanguage: AppLanguage = AppLanguage.HR,
    val hasSeenWelcome: Boolean = false,
    val simplePointViewEnabled: Boolean = false,
    val hideUserContentOnMap: Boolean = false,
    val autoCenterOnUserEnabled: Boolean = false,
    val positionEnabled: Boolean = false,
    val mapOrientationMode: MapOrientationMode = MapOrientationMode.NORTH_UP,
    val lastMapCenterLat: Double? = null,
    val lastMapCenterLon: Double? = null,
    val lastMapZoom: Double? = null,
    val filters: FilterState = FilterState(),
    val recentSearchQueries: List<String> = emptyList(),
    val savedSearchPresets: List<SearchPreset> = emptyList()
)

object AppSessionStore {
    private const val PREFS = "app_session_store"
    private const val KEY_SESSION = "session_json"
    private val gson = Gson()

    private data class SessionDto(
        val currentTab: String = AppTab.HOME.name,
        val appLanguage: String = AppLanguage.HR.name,
        val hasSeenWelcome: Boolean = false,
        val simplePointViewEnabled: Boolean = false,
        val hideUserContentOnMap: Boolean = false,
        val autoCenterOnUserEnabled: Boolean = false,
        val positionEnabled: Boolean = false,
        val mapOrientationMode: String = MapOrientationMode.NORTH_UP.name,
        val lastMapCenterLat: Double? = null,
        val lastMapCenterLon: Double? = null,
        val lastMapZoom: Double? = null,
        val filters: SessionFilterDto = SessionFilterDto(),
        val recentSearchQueries: List<String> = emptyList(),
        val savedSearchPresets: List<SearchPresetDto> = emptyList()
    )

    fun load(context: Context): AppSessionSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSION, null) ?: return AppSessionSnapshot()
        return runCatching {
            val dto = gson.fromJson(raw, SessionDto::class.java) ?: SessionDto()
            AppSessionSnapshot(
                currentTab = enumValueOrDefault(dto.currentTab, AppTab.HOME).let { if (it == AppTab.SPELEO_RUNNER) AppTab.HOME else it },
                appLanguage = enumValueOrDefault(dto.appLanguage, AppLanguage.HR),
                hasSeenWelcome = dto.hasSeenWelcome,
                simplePointViewEnabled = dto.simplePointViewEnabled,
                hideUserContentOnMap = dto.hideUserContentOnMap,
                autoCenterOnUserEnabled = dto.autoCenterOnUserEnabled,
                positionEnabled = dto.positionEnabled,
                mapOrientationMode = enumValueOrDefault(dto.mapOrientationMode, MapOrientationMode.NORTH_UP),
                lastMapCenterLat = dto.lastMapCenterLat,
                lastMapCenterLon = dto.lastMapCenterLon,
                lastMapZoom = dto.lastMapZoom,
                recentSearchQueries = dto.recentSearchQueries.take(5),
                savedSearchPresets = dto.savedSearchPresets.mapNotNull { p ->
                    if (p.name.isBlank()) null else SearchPreset(
                        name = p.name,
                        filters = FilterState(
                            query = p.filters.query,
                            sourceFilter = enumValueOrDefault(p.filters.sourceFilter, SourceFilter.ALL),
                            cadastreFilter = enumValueOrDefault(p.filters.cadastreFilter, CadastreFilter.ALL),
                            caveTypeFilter = enumValueOrDefault(p.filters.caveTypeFilter, CaveTypeFilter.ALL),
                            areaFilter = p.filters.areaFilter,
                            distanceFilterKm = p.filters.distanceFilterKm,
                            onlyWithDescription = p.filters.onlyWithDescription,
                            fieldTaskFilters = p.filters.fieldTaskFilters
                        )
                    )
                }.take(5),
                filters = FilterState(
                    query = dto.filters.query,
                    sourceFilter = enumValueOrDefault(dto.filters.sourceFilter, SourceFilter.ALL),
                    cadastreFilter = enumValueOrDefault(dto.filters.cadastreFilter, CadastreFilter.ALL),
                    caveTypeFilter = enumValueOrDefault(dto.filters.caveTypeFilter, CaveTypeFilter.ALL),
                    areaFilter = dto.filters.areaFilter,
                    distanceFilterKm = dto.filters.distanceFilterKm,
                    onlyWithDescription = dto.filters.onlyWithDescription,
                    fieldTaskFilters = dto.filters.fieldTaskFilters
                )
            )
        }.getOrDefault(AppSessionSnapshot())
    }

    fun save(context: Context, snapshot: AppSessionSnapshot) {
        val dto = SessionDto(
            currentTab = (if (snapshot.currentTab == AppTab.SPELEO_RUNNER) AppTab.HOME else snapshot.currentTab).name,
            appLanguage = snapshot.appLanguage.name,
            hasSeenWelcome = snapshot.hasSeenWelcome,
            simplePointViewEnabled = snapshot.simplePointViewEnabled,
            hideUserContentOnMap = snapshot.hideUserContentOnMap,
            autoCenterOnUserEnabled = snapshot.autoCenterOnUserEnabled,
            positionEnabled = snapshot.positionEnabled,
            mapOrientationMode = snapshot.mapOrientationMode.name,
            lastMapCenterLat = snapshot.lastMapCenterLat,
            lastMapCenterLon = snapshot.lastMapCenterLon,
            lastMapZoom = snapshot.lastMapZoom,
            recentSearchQueries = snapshot.recentSearchQueries.take(5),
            savedSearchPresets = snapshot.savedSearchPresets.take(5).map { p ->
                SearchPresetDto(
                    name = p.name,
                    filters = SessionFilterDto(
                        query = p.filters.query,
                        sourceFilter = p.filters.sourceFilter.name,
                        cadastreFilter = p.filters.cadastreFilter.name,
                        caveTypeFilter = p.filters.caveTypeFilter.name,
                        areaFilter = p.filters.areaFilter,
                        distanceFilterKm = p.filters.distanceFilterKm,
                        onlyWithDescription = p.filters.onlyWithDescription,
                        fieldTaskFilters = p.filters.fieldTaskFilters
                    )
                )
            },
            filters = SessionFilterDto(
                query = snapshot.filters.query,
                sourceFilter = snapshot.filters.sourceFilter.name,
                cadastreFilter = snapshot.filters.cadastreFilter.name,
                caveTypeFilter = snapshot.filters.caveTypeFilter.name,
                areaFilter = snapshot.filters.areaFilter,
                distanceFilterKm = snapshot.filters.distanceFilterKm,
                onlyWithDescription = snapshot.filters.onlyWithDescription,
                fieldTaskFilters = snapshot.filters.fieldTaskFilters
            )
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION, gson.toJson(dto))
            .apply()
    }

    fun addRecentSearchQuery(context: Context, query: String) {
        if (query.isBlank() || query.length < 2) return
        val current = load(context)
        val updated = (listOf(query.trim()) + current.recentSearchQueries.filterNot {
            it.equals(query.trim(), ignoreCase = true)
        }).take(5)
        save(context, current.copy(recentSearchQueries = updated))
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        if (value.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
    }
}
