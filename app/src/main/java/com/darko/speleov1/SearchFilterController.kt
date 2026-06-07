package com.darko.speleov1

import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SourceFilter

/**
 * Small controller for Search filter actions.
 *
 * This is intentionally pure Kotlin and UI-free so SpeleoAppRoot/SearchFeature
 * can keep moving toward a ViewModel/controller based structure without changing
 * the current search behaviour in one risky step.
 */
class SearchFilterController(
    private val hasCurrentUserLocation: Boolean,
    private val onFiltersChanged: (FilterState) -> Unit,
    private val onRequestGpsLocation: () -> Unit
) {
    fun resetAll() {
        onFiltersChanged(FilterState())
    }

    fun applyFieldWorkPreset() {
        onFiltersChanged(
            FilterState(
                sourceFilter = SourceFilter.SOV,
                caveTypeFilter = CaveTypeFilter.ALL,
                cadastreFilter = CadastreFilter.ALL,
                distanceFilterKm = if (hasCurrentUserLocation) 25 else null
            )
        )
        if (!hasCurrentUserLocation) onRequestGpsLocation()
    }

    fun applyNearbyPreset() {
        if (!hasCurrentUserLocation) onRequestGpsLocation()
        onFiltersChanged(
            FilterState(
                sourceFilter = SourceFilter.ALL,
                caveTypeFilter = CaveTypeFilter.ALL,
                cadastreFilter = CadastreFilter.ALL,
                distanceFilterKm = 25
            )
        )
    }
}

internal fun buildSearchActiveFilterPills(
    filters: FilterState,
    selectedDistanceLabel: String
): List<String> = buildList {
    when (filters.sourceFilter) {
        SourceFilter.SOV -> add("SOV baza")
        SourceFilter.KATASTAR -> Unit
        SourceFilter.MY_BASE -> add("Moja baza")
        SourceFilter.ALL -> add("Sve baze")
    }
    if (filters.query.isNotBlank()) add("Traži: ${filters.query}")
    when (filters.caveTypeFilter) {
        CaveTypeFilter.JAMA -> add("Jame")
        CaveTypeFilter.SPILJA -> add("Špilje")
        else -> Unit
    }
    if (filters.distanceFilterKm != null) add(selectedDistanceLabel)
    if (filters.depthMinM != null) add("Dubina ${filters.depthMinM}+ m")
    if (filters.onlyWithDescription) add("Ima opis")
    filters.fieldTaskFilters.forEach { key ->
        FIELD_TASK_FILTERS.firstOrNull { it.first == key }?.second?.let { add(it) }
    }
}
