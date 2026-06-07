package com.darko.speleov1.model

data class DatasetEnvelope(
    val schema_version: String,
    val generated_at_utc: String,
    val source_file: String,
    val stats: DatasetStats,
    val records: List<SpeleoRecord>
)

data class DatasetStats(
    val records_total: Int,
    val records_valid_coordinates: Int,
    val records_invalid_coordinates: Int,
    val status_counts: Map<String, Int>,
    val object_type_counts: Map<String, Int>,
    val field_task_counts: Map<String, Int>
)

data class SpeleoRecord(
    val id: String,
    val source: String? = null,
    val source_labels: List<String>? = null,
    val name: String,
    val location: Location,
    val cadastre: Cadastre,
    val classification: Classification,
    val metrics: Metrics,
    val condition: Condition,
    val research: Research,
    val content: Content,
    val raw: Map<String, Any?>? = null,
    val search_text: String? = null
)

data class Location(
    val lat: Double?,
    val lon: Double?,
    val county: String?,
    val municipality: String?,
    val nearest_place: String?,
    val locality: String?,
    val island: String?,
    val altitude_m: Double?,
    val protected_area: String?
)

data class Cadastre(
    val status: String?,
    val cadastral_number: String?,
    val in_cadastre: Boolean?,
    val not_in_cadastre_candidate: Boolean?
)

data class Classification(
    val object_type: String?,
    val object_type_source: String?,
    val record_status: String?,
    val field_tasks: List<String>?,
    val priority: String?,
    val kml_export_candidate: Boolean?
)

data class Metrics(
    val depth_m: Double?,
    val length_m: Double?,
    val vertical_range_m: Double?,
    val entrance_count: Int?
)

data class Condition(
    val plate_number: String?,
    val main_entrance_status: String?,
    val hazards: String?,
    val pollution: String?
)

data class Research(
    val last_research_year: Int?,
    val last_research_date: String?,
    val clubs: String?,
    val team_members: String?,
    val survey_author: String?,
    val survey_in_digital_base: Boolean?,
    val bibliography_record: Boolean?,
    val georef_record: Boolean?,
    val further_research_possible: Boolean?,
    val further_research_note: String?
)

data class Content(
    val access_description: String?,
    val technical_description: String?,
    val note: String?,
    val literature: String?,
    val name_origin: String?,
    val synonyms: String?,
    val other_synonyms: String?,
    val clean_cave_report: String?,
    val geological_or_anthropogenic_activities: String?
)

enum class CadastreFilter { ALL, IN_CADASTRE, NOT_IN_CADASTRE }

enum class CaveTypeFilter { ALL, JAMA, SPILJA }

enum class SourceFilter { ALL, SOV, KATASTAR, MY_BASE }

data class BoundingBoxFilter(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val label: String = ""
)

data class FilterState(
    val query: String = "",
    val sourceFilter: SourceFilter = SourceFilter.ALL,
    val cadastreFilter: CadastreFilter = CadastreFilter.ALL,
    val caveTypeFilter: CaveTypeFilter = CaveTypeFilter.ALL,
    val areaFilter: String = "",
    val distanceFilterKm: Int? = null,
    val depthMinM: Int? = null,
    val onlyWithDescription: Boolean = false,
    val fieldTaskFilters: List<String> = emptyList(),
    val boundingBoxFilter: BoundingBoxFilter? = null
)

enum class SearchMode { BASIC, DEEP }
