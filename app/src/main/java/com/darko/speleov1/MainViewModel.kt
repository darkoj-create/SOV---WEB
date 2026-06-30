package com.darko.speleov1

import android.app.Application
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darko.speleov1.data.SpeleoRepository
import com.darko.speleov1.model.DatasetEnvelope
import com.darko.speleov1.model.BoundingBoxFilter
import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.model.SourceFilter
import com.darko.speleov1.util.AppSessionStore
import com.darko.speleov1.util.UserContentStore
import com.darko.speleov1.util.MyBaseRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val WHITESPACE_REGEX = Regex("\\s+")
private val SEARCH_SEPARATOR_REGEX = Regex("[^a-z0-9]+")
private val SEARCH_COMPACT_SEPARATOR_REGEX = Regex("[^a-z0-9]+")

private data class SearchIndexRecord(
    val record: SpeleoRecord,
    val lat: Double?,
    val lon: Double?,
    val name: String,
    val source: String,
    val county: String,
    val municipality: String,
    val nearestPlace: String,
    val locality: String,
    val cadastral: String,
    val plate: String,
    val objectType: String,
    val cadastreStatus: String,
    val descriptionPresent: Boolean,
    val tasks: Set<String>,
    val areaTokens: Set<String>,
    val searchBlob: String
)

private data class FilterSnapshot(
    val queryNormalized: String,
    val filtersWithoutQuery: FilterState,
    val locationKey: String,
    val results: List<SearchIndexRecord>
)

private data class QueryDirectives(
    val queryNormalized: String,
    val favoritesOnly: Boolean,
    val recentsOnly: Boolean,
    val nearOnly: Boolean,
    val needsDescription: Boolean,
    val needsTaskHint: Boolean
)

private data class CachedSearchIndexRecord(
    val recordIndex: Int,
    val lat: Double?,
    val lon: Double?,
    val name: String,
    val source: String,
    val county: String,
    val municipality: String,
    val nearestPlace: String,
    val locality: String,
    val cadastral: String,
    val plate: String,
    val objectType: String,
    val cadastreStatus: String,
    val descriptionPresent: Boolean,
    val tasks: List<String>,
    val areaTokens: List<String>,
    val searchBlob: String
)

private data class SearchCachePayload(
    val datasetKey: String,
    val recordCount: Int,
    val locationOptions: List<SearchLocationOption>,
    val indexRecords: List<CachedSearchIndexRecord>
)

data class SearchLocationOption(
    val value: String,
    val label: String
)

data class MainUiState(
    val isLoading: Boolean = true,
    val isFiltering: Boolean = false,
    val isSearchIndexReady: Boolean = false,
    val loadingMessage: String = "Učitavam bazu…",
    val loadingProgress: Float = 0f,
    val allRecords: List<SpeleoRecord> = emptyList(),
    val filteredRecords: List<SpeleoRecord> = emptyList(),
    val locationOptions: List<SearchLocationOption> = emptyList(),
    val selectedRecord: SpeleoRecord? = null,
    val filters: FilterState = FilterState(),
    val myBaseCount: Int = 0,
    val myBaseSummary: String = "Nema učitanih KML/CSV točaka",
    val myBaseMessage: String? = null,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SpeleoRepository(application.applicationContext)
    private val appContext = application.applicationContext
    private val gson: Gson by lazy(LazyThreadSafetyMode.PUBLICATION) { Gson() }
    private val initialSession = AppSessionStore.load(appContext)
    private var currentUserLocation: GeoPoint? = null
    private var indexedRecords: List<SearchIndexRecord> = emptyList()
    private var filterJob: Job? = null
    private var filterGeneration: Long = 0L
    private var pendingReapplyAfterWarmup = false
    private var lastFilterSnapshot: FilterSnapshot? = null

    companion object {
        private const val SEARCH_CACHE_VERSION = 9001
        private const val SEARCH_CACHE_FILE_NAME = "search_index_cache_admin_v9001.json"
        @Volatile private var cachedRecordsKey: String? = null
        @Volatile private var cachedIndexedRecords: List<SearchIndexRecord>? = null
        @Volatile private var cachedLocationOptions: List<SearchLocationOption>? = null
    }

    private val initialFilters = initialSession.filters.copy(
        sourceFilter = if (initialSession.filters.sourceFilter == SourceFilter.KATASTAR) SourceFilter.ALL else initialSession.filters.sourceFilter,
        cadastreFilter = CadastreFilter.ALL
    )
    private val _uiState = MutableStateFlow(MainUiState(filters = initialFilters))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private fun saveFiltersToSession(filters: FilterState) {
        val current = AppSessionStore.load(appContext)
        AppSessionStore.save(appContext, current.copy(filters = filters))
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isFiltering = false, error = null, isSearchIndexReady = false, loadingMessage = "Učitavam bazu…", loadingProgress = 0.08f) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.loadDataset()
                }.also {
                    _uiState.update { state -> state.copy(loadingMessage = "Pripremam objekte za search…", loadingProgress = 0.42f) }
                }
            }.onSuccess { envelope ->
                val myBaseRecords = withContext(Dispatchers.IO) { MyBaseRepository.loadRecords(appContext) }
                val records = envelope.records + myBaseRecords
                val cacheKey = buildRecordsCacheKey(envelope, MyBaseRepository.cacheFingerprint(appContext))
                val cachedIndex = cachedIndexedRecords.takeIf { cachedRecordsKey == cacheKey }
                val cachedOptions = cachedLocationOptions.takeIf { cachedRecordsKey == cacheKey }
                val hasMemoryCache = !cachedIndex.isNullOrEmpty() && !cachedOptions.isNullOrEmpty()
                val diskCache = if (hasMemoryCache) null else withContext(Dispatchers.IO) { loadSearchCache(records, cacheKey) }
                val diskIndex = diskCache?.first
                val diskOptions = diskCache?.second
                val hasDiskCache = !diskIndex.isNullOrEmpty() && !diskOptions.isNullOrEmpty()
                val hasWarmCache = hasMemoryCache || hasDiskCache
                if (hasMemoryCache) {
                    indexedRecords = cachedIndex.orEmpty()
                } else if (hasDiskCache) {
                    indexedRecords = diskIndex.orEmpty()
                    cachedRecordsKey = cacheKey
                    cachedIndexedRecords = diskIndex
                    cachedLocationOptions = diskOptions
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFiltering = false,
                        isSearchIndexReady = hasWarmCache,
                        loadingMessage = if (hasWarmCache) "Search spreman" else "Gradim search index…",
                        loadingProgress = if (hasWarmCache) 1f else 0.5f,
                        allRecords = records,
                        filteredRecords = emptyList(),
                        myBaseCount = myBaseRecords.size,
                        myBaseSummary = MyBaseRepository.summary(appContext),
                        locationOptions = when {
                            hasMemoryCache -> cachedOptions.orEmpty()
                            hasDiskCache -> diskOptions.orEmpty()
                            else -> emptyList()
                        }
                    )
                }
                if (hasWarmCache) {
                    if (_uiState.value.filters.hasAnyActiveCriteriaFast()) {
                        scheduleFiltering(_uiState.value.filters, currentUserLocation)
                    }
                } else {
                    warmUpSearch(records, cacheKey)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFiltering = false,
                        isSearchIndexReady = false,
                        loadingMessage = "Greška pri učitavanju",
                        loadingProgress = 0f,
                        error = throwable.message ?: "Greška pri učitavanju podataka"
                    )
                }
            }
        }
    }

    private fun warmUpSearch(records: List<SpeleoRecord>, cacheKey: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Gradim search index…", loadingProgress = 0.58f) }
            val total = records.size.coerceAtLeast(1)
            val index = ArrayList<SearchIndexRecord>(records.size)
            val cacheEntries = ArrayList<CachedSearchIndexRecord>(records.size)
            val locationOptionsMap = linkedMapOf<String, SearchLocationOption>()
            records.forEachIndexed { recordIndex, record ->
                val indexed = buildIndex(record)
                index.add(indexed)
                cacheEntries.add(indexed.toCachedRecord(recordIndex))
                addLocationOptionsForRecord(locationOptionsMap, record)
                val processed = recordIndex + 1
                if (processed == total || processed % 300 == 0) {
                    val progress = 0.58f + ((processed.toFloat() / total.toFloat()) * 0.32f)
                    _uiState.update { it.copy(loadingMessage = "Gradim search index…", loadingProgress = progress.coerceIn(0.58f, 0.9f)) }
                }
            }
            val locationOptions = locationOptionsMap.values.sortedBy { normalizeSearchText(it.label) }
            withContext(Dispatchers.IO) {
                saveSearchCache(cacheKey, cacheEntries, locationOptions)
            }
            withContext(Dispatchers.Main) {
                indexedRecords = index
                cachedRecordsKey = cacheKey
                cachedIndexedRecords = index
                cachedLocationOptions = locationOptions
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSearchIndexReady = true,
                        loadingMessage = "Search spreman",
                        loadingProgress = 1f,
                        locationOptions = locationOptions
                    )
                }
                if (pendingReapplyAfterWarmup || _uiState.value.filters.hasAnyActiveCriteriaFast()) {
                    pendingReapplyAfterWarmup = false
                    scheduleFiltering(_uiState.value.filters, currentUserLocation)
                }
            }
        }
    }

    fun updateFilters(newFilters: FilterState) {
        _uiState.update { it.copy(filters = newFilters) }
        saveFiltersToSession(newFilters)
        if (!_uiState.value.isSearchIndexReady) {
            pendingReapplyAfterWarmup = newFilters.hasAnyActiveCriteriaFast()
            _uiState.update { it.copy(isFiltering = false, filteredRecords = emptyList()) }
            return
        }
        scheduleFiltering(newFilters, currentUserLocation)
    }

    fun updateCurrentUserLocation(location: GeoPoint?) {
        currentUserLocation = location
        if (_uiState.value.filters.distanceFilterKm != null && _uiState.value.isSearchIndexReady) {
            scheduleFiltering(_uiState.value.filters, currentUserLocation)
        }
    }

    fun selectRecord(record: SpeleoRecord?) {
        if (record != null) {
            UserContentStore.pushRecentRecord(appContext, record.id)
        }
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun importMyBaseKml(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(myBaseMessage = "Uvozim KML u Moju bazu…") }
            runCatching {
                withContext(Dispatchers.IO) { MyBaseRepository.importKml(appContext, uri) }
            }.onSuccess { result ->
                searchCacheFile().delete()
                _uiState.update { it.copy(myBaseMessage = "Moja baza: uvezeno ${result.importedPoints} točaka iz ${result.fileName}") }
                loadData()
            }.onFailure { throwable ->
                _uiState.update { it.copy(myBaseMessage = throwable.message ?: "Uvoz KML-a nije uspio") }
            }
        }
    }

    fun clearMyBase() {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) { MyBaseRepository.clear(appContext) }
            searchCacheFile().delete()
            _uiState.update { it.copy(myBaseMessage = "Moja baza očišćena ($removed datoteka)") }
            loadData()
        }
    }

    fun consumeMyBaseMessage() {
        _uiState.update { it.copy(myBaseMessage = null) }
    }

    private fun scheduleFiltering(filters: FilterState, currentLocation: GeoPoint?) {
        filterJob?.cancel()
        val requestId = ++filterGeneration

        if (!filters.hasAnyActiveCriteriaFast()) {
            lastFilterSnapshot = null
            _uiState.update { it.copy(isFiltering = false, filteredRecords = emptyList()) }
            return
        }

        if (indexedRecords.isEmpty()) {
            pendingReapplyAfterWarmup = true
            _uiState.update { it.copy(isFiltering = false, filteredRecords = emptyList()) }
            return
        }

        _uiState.update { it.copy(isFiltering = true) }
        filterJob = viewModelScope.launch {
            val normalizedQuery = normalizeSearchText(filters.query)
            val locationKey = buildLocationKey(currentLocation)
            val baseRecords = selectBaseRecords(filters, normalizedQuery, locationKey)
            val filteredIndexed = withContext(Dispatchers.Default) {
                applyFilters(baseRecords, filters, currentLocation)
            }
            if (requestId == filterGeneration) {
                lastFilterSnapshot = FilterSnapshot(
                    queryNormalized = normalizedQuery,
                    filtersWithoutQuery = filters.withoutQuery(),
                    locationKey = locationKey,
                    results = filteredIndexed
                )
                _uiState.update { it.copy(isFiltering = false, filteredRecords = filteredIndexed.map(SearchIndexRecord::record)) }
            }
        }
    }

    private fun selectBaseRecords(
        filters: FilterState,
        normalizedQuery: String,
        locationKey: String
    ): List<SearchIndexRecord> {
        val snapshot = lastFilterSnapshot ?: return indexedRecords
        if (snapshot.filtersWithoutQuery != filters.withoutQuery()) return indexedRecords
        if (snapshot.locationKey != locationKey) return indexedRecords
        if (snapshot.queryNormalized.isBlank()) return indexedRecords
        if (normalizedQuery.length < snapshot.queryNormalized.length) return indexedRecords
        return if (normalizedQuery.startsWith(snapshot.queryNormalized)) snapshot.results else indexedRecords
    }

    private fun buildIndex(record: SpeleoRecord): SearchIndexRecord {
        val county = normalizeSearchText(record.location.county)
        val municipality = normalizeSearchText(record.location.municipality)
        val locality = normalizeSearchText(record.location.locality)
        val nearestPlace = normalizeSearchText(record.location.nearest_place)
        val cadastral = normalizeSearchText(record.cadastre.cadastral_number)
        val plate = normalizeSearchText(record.condition.plate_number)
        val objectType = normalizeSearchText(record.classification.object_type)
        val cadastreStatus = normalizeSearchText(record.cadastre.status)
        val name = normalizeSearchText(record.name)
        val source = normalizeSearchText(record.source)
        val freeText = normalizeSearchText(listOfNotNull(
            record.search_text,
            record.content.access_description,
            record.content.technical_description,
            record.content.note,
            record.content.synonyms,
            record.content.other_synonyms
        ).joinToString(" "))
        val tasks = record.classification.field_tasks.orEmpty()
            .map(::normalizeSearchText)
            .filter { it.isNotBlank() }
            .toSet()
        val searchBlob = buildSearchBlob(
            name,
            municipality,
            locality,
            nearestPlace,
            county,
            cadastral,
            plate,
            objectType,
            cadastreStatus,
            freeText
        )

        return SearchIndexRecord(
            record = record,
            lat = record.location.lat,
            lon = record.location.lon,
            name = name,
            source = source,
            county = county,
            municipality = municipality,
            nearestPlace = nearestPlace,
            locality = locality,
            cadastral = cadastral,
            plate = plate,
            objectType = objectType,
            cadastreStatus = cadastreStatus,
            descriptionPresent = !record.content.technical_description.isNullOrBlank() ||
                !record.content.access_description.isNullOrBlank() ||
                !record.content.note.isNullOrBlank(),
            tasks = tasks,
            areaTokens = listOf(county, municipality, locality, nearestPlace)
                .filter { it.isNotBlank() }
                .toSet(),
            searchBlob = searchBlob
        )
    }

    private fun buildRecordsCacheKey(envelope: DatasetEnvelope, myBaseFingerprint: String): String {
        val schema = envelope.schema_version.ifBlank { "schema" }
        val generatedAt = envelope.generated_at_utc.ifBlank { "generated" }
        val source = envelope.source_file.ifBlank { "source" }
        return listOf(
            "v$SEARCH_CACHE_VERSION",
            schema,
            generatedAt,
            source,
            envelope.stats.records_total.toString(),
            envelope.records.firstOrNull()?.id.orEmpty(),
            envelope.records.lastOrNull()?.id.orEmpty(),
            myBaseFingerprint
        ).joinToString("|")
    }

    private fun addLocationOptionsForRecord(
        options: MutableMap<String, SearchLocationOption>,
        record: SpeleoRecord
    ) {
        fun add(type: String, rawValue: String?) {
            val clean = rawValue?.trim().orEmpty()
            if (clean.isBlank()) return
            val normalized = normalizeSearchText(clean)
            if (normalized.isBlank()) return
            val dedupeKey = "$type|$normalized"
            options.putIfAbsent(
                dedupeKey,
                SearchLocationOption(
                    value = "$type|$clean",
                    label = clean
                )
            )
        }

        add("municipality", record.location.municipality)
        add("place", record.location.locality)
        add("place", record.location.nearest_place)
        add("county", record.location.county)
    }

    private fun loadSearchCache(
        records: List<SpeleoRecord>,
        cacheKey: String
    ): Pair<List<SearchIndexRecord>, List<SearchLocationOption>>? {
        val file = searchCacheFile()
        if (!file.exists()) return null
        return runCatching {
            val payload = gson.fromJson(file.readText(), SearchCachePayload::class.java)
            if (payload.datasetKey != cacheKey || payload.recordCount != records.size) {
                file.delete()
                null
            } else {
                val indexed = payload.indexRecords.mapNotNull { cached ->
                    val record = records.getOrNull(cached.recordIndex) ?: return@mapNotNull null
                    SearchIndexRecord(
                        record = record,
                        lat = cached.lat,
                        lon = cached.lon,
                        name = cached.name,
                        source = cached.source,
                        county = cached.county,
                        municipality = cached.municipality,
                        nearestPlace = cached.nearestPlace,
                        locality = cached.locality,
                        cadastral = cached.cadastral,
                        plate = cached.plate,
                        objectType = cached.objectType,
                        cadastreStatus = cached.cadastreStatus,
                        descriptionPresent = cached.descriptionPresent,
                        tasks = cached.tasks.toSet(),
                        areaTokens = cached.areaTokens.toSet(),
                        searchBlob = cached.searchBlob
                    )
                }
                if (indexed.size != payload.indexRecords.size) {
                    file.delete()
                    null
                } else {
                    indexed to payload.locationOptions
                }
            }
        }.getOrElse {
            file.delete()
            null
        }
    }

    private fun saveSearchCache(
        cacheKey: String,
        indexRecords: List<CachedSearchIndexRecord>,
        locationOptions: List<SearchLocationOption>
    ) {
        runCatching {
            val payload = SearchCachePayload(
                datasetKey = cacheKey,
                recordCount = indexRecords.size,
                locationOptions = locationOptions,
                indexRecords = indexRecords
            )
            searchCacheFile().writeText(gson.toJson(payload))
        }
    }

    private fun searchCacheFile(): File {
        val dir = File(appContext.cacheDir, "search")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, SEARCH_CACHE_FILE_NAME)
    }

    private fun SearchIndexRecord.toCachedRecord(recordIndex: Int): CachedSearchIndexRecord = CachedSearchIndexRecord(
        recordIndex = recordIndex,
        lat = lat,
        lon = lon,
        name = name,
        source = source,
        county = county,
        municipality = municipality,
        nearestPlace = nearestPlace,
        locality = locality,
        cadastral = cadastral,
        plate = plate,
        objectType = objectType,
        cadastreStatus = cadastreStatus,
        descriptionPresent = descriptionPresent,
        tasks = tasks.toList(),
        areaTokens = areaTokens.toList(),
        searchBlob = searchBlob
    )

    private fun applyFilters(
        records: List<SearchIndexRecord>,
        filters: FilterState,
        currentLocation: GeoPoint?
    ): List<SearchIndexRecord> {
        if (records.isEmpty() || !filters.hasAnyActiveCriteriaFast()) return emptyList()

        val directives = parseQueryDirectives(filters.query)
        val queryNormalized = directives.queryNormalized
        val queryTerms = queryNormalized.split(' ').filter { it.isNotBlank() }
        val selectedTasks = filters.fieldTaskFilters.map(::normalizeSearchText).filter { it.isNotBlank() }.toMutableSet()
        if (directives.needsTaskHint) selectedTasks.addAll(setOf("treba_nacrt", "treba_koordinate", "treba_foto", "treba_opis"))
        val favoriteIds = if (directives.favoritesOnly) UserContentStore.loadFavoriteRecordIds(appContext) else emptySet()
        val recentIds = if (directives.recentsOnly) UserContentStore.loadRecentRecordIds(appContext).toSet() else emptySet()
        val effectiveDistanceKm = when {
            filters.distanceFilterKm != null -> filters.distanceFilterKm
            directives.nearOnly -> 25
            else -> null
        }
        val useAdvancedOr = filters.onlyWithDescription || directives.needsDescription || selectedTasks.isNotEmpty()
        val distanceCache = if (effectiveDistanceKm != null && currentLocation != null) HashMap<String, Double>(records.size) else null

        return records.asSequence()
            .filter { it.lat != null && it.lon != null }
            .filter { matchesBoundingBox(it, filters.boundingBoxFilter) }
            .filter { matchesQuery(it, queryNormalized, queryTerms) }
            .filter { !directives.favoritesOnly || favoriteIds.contains(it.record.id) }
            .filter { !directives.recentsOnly || recentIds.contains(it.record.id) }
            .filter {
                when (filters.sourceFilter) {
                    SourceFilter.ALL -> true
                    SourceFilter.SOV -> recordIsSovSource(it.record)
                    SourceFilter.KATASTAR -> recordIsKatastarSource(it.record)
                    SourceFilter.MY_BASE -> recordIsMyBaseSource(it.record)
                }
            }
            .filter {
                when (filters.cadastreFilter) {
                    CadastreFilter.ALL -> true
                    CadastreFilter.IN_CADASTRE -> it.record.cadastre.in_cadastre == true || it.cadastreStatus == "u_katastru"
                    CadastreFilter.NOT_IN_CADASTRE -> it.record.cadastre.not_in_cadastre_candidate == true || it.cadastreStatus.contains("nije")
                }
            }
            .filter {
                when (filters.caveTypeFilter) {
                    CaveTypeFilter.ALL -> true
                    CaveTypeFilter.JAMA -> it.objectType.startsWith("jama")
                    CaveTypeFilter.SPILJA -> it.objectType.startsWith("spilja")
                }
            }
            .filter { matchesAreaFilter(it, filters.areaFilter) }
            .filter { matchesDepthFilter(it, filters.depthMinM) }
            .filter { matchesDistanceFilter(it, effectiveDistanceKm, currentLocation, distanceCache) }
            .filter { indexed ->
                if (!useAdvancedOr) {
                    true
                } else {
                    val matchesDescription = filters.onlyWithDescription && indexed.descriptionPresent
                    val matchesTask = selectedTasks.isNotEmpty() && selectedTasks.any(indexed.tasks::contains)
                    matchesDescription || matchesTask
                }
            }
            .sortedWith(
                compareByDescending<SearchIndexRecord> { scoreRecord(it, queryNormalized, queryTerms) }
                    .thenBy { it.county }
                    .thenBy { it.municipality }
                    .thenBy { it.name }
            )
            .toList()
    }

    private fun matchesBoundingBox(record: SearchIndexRecord, bbox: BoundingBoxFilter?): Boolean {
        if (bbox == null) return true
        val lat = record.lat ?: return false
        val lon = record.lon ?: return false
        return lat >= bbox.minLat && lat <= bbox.maxLat && lon >= bbox.minLon && lon <= bbox.maxLon
    }

    private fun matchesQuery(record: SearchIndexRecord, queryNormalized: String, queryTerms: List<String>): Boolean {
        if (queryNormalized.isBlank()) return true
        if (queryTerms.isEmpty()) return true
        val blob = record.searchBlob
        return queryTerms.all(blob::contains)
    }

    private fun scoreRecord(record: SearchIndexRecord, queryNormalized: String, queryTerms: List<String>): Int {
        if (queryNormalized.isBlank()) return 0

        var score = 0
        val compactQuery = compactSearchToken(queryNormalized)

        fun addMatch(text: String, exact: Int, starts: Int, contains: Int) {
            if (text.isBlank()) return
            val compactText = compactSearchToken(text)
            score += when {
                text == queryNormalized -> exact
                compactQuery.isNotBlank() && compactText == compactQuery -> exact - 5
                text.startsWith(queryNormalized) -> starts
                compactQuery.isNotBlank() && compactText.startsWith(compactQuery) -> starts - 5
                text.contains(queryNormalized) -> contains
                compactQuery.isNotBlank() && compactText.contains(compactQuery) -> contains - 5
                else -> 0
            }
            if (queryTerms.size > 1) {
                val matchedTerms = queryTerms.count { term -> text.contains(term) || compactText.contains(compactSearchToken(term)) }
                score += matchedTerms * 12
            }
        }

        addMatch(record.name, exact = 220, starts = 170, contains = 130)
        addMatch(record.municipality, exact = 210, starts = 165, contains = 120)
        addMatch(record.locality, exact = 190, starts = 150, contains = 110)
        addMatch(record.nearestPlace, exact = 185, starts = 145, contains = 105)
        addMatch(record.county, exact = 175, starts = 135, contains = 95)
        addMatch(record.cadastral, exact = 90, starts = 70, contains = 45)
        addMatch(record.plate, exact = 80, starts = 60, contains = 40)

        return score
    }

    private fun matchesAreaFilter(record: SearchIndexRecord, areaFilter: String): Boolean {
        if (areaFilter.isBlank()) return true
        val parts = areaFilter.split("|", limit = 2)
        val type = parts.getOrNull(0).orEmpty()
        val value = normalizeSearchText(parts.getOrNull(1))
        if (value.isBlank()) return true
        return when (type) {
            "county" -> record.county == value
            "municipality" -> record.municipality == value
            "place" -> record.locality == value || record.nearestPlace == value
            else -> record.areaTokens.contains(value)
        }
    }

    private fun matchesDepthFilter(record: SearchIndexRecord, depthMinM: Int?): Boolean {
        if (depthMinM == null) return true
        val depth = record.record.metrics.depth_m ?: return false
        return depth >= depthMinM
    }

    private fun matchesDistanceFilter(
        record: SearchIndexRecord,
        distanceFilterKm: Int?,
        currentLocation: GeoPoint?,
        distanceCache: MutableMap<String, Double>?
    ): Boolean {
        if (distanceFilterKm == null) return true
        val lat = record.lat ?: return false
        val lon = record.lon ?: return false
        val user = currentLocation ?: return false
        val distanceM = if (distanceCache != null) {
            distanceCache.getOrPut(record.record.id) {
                val result = FloatArray(1)
                Location.distanceBetween(user.latitude, user.longitude, lat, lon, result)
                result.firstOrNull()?.toDouble() ?: Double.MAX_VALUE
            }
        } else {
            val result = FloatArray(1)
            Location.distanceBetween(user.latitude, user.longitude, lat, lon, result)
            result.firstOrNull()?.toDouble() ?: Double.MAX_VALUE
        }
        return distanceM <= distanceFilterKm * 1000.0
    }

    private fun parseQueryDirectives(rawQuery: String): QueryDirectives {
        val normalizedRaw = normalizeSearchText(rawQuery)
        val rawTerms = normalizedRaw.split(' ').filter { it.isNotBlank() }
        val controlTerms = setOf("fav", "favorite", "favorites", "recent", "recenti", "nedavno", "near", "opis", "fix", "task", "tasks")
        return QueryDirectives(
            queryNormalized = rawTerms.filterNot(controlTerms::contains).joinToString(" "),
            favoritesOnly = rawTerms.any { it == "fav" || it == "favorite" || it == "favorites" },
            recentsOnly = rawTerms.any { it == "recent" || it == "recenti" || it == "nedavno" },
            nearOnly = rawTerms.any { it == "near" },
            needsDescription = rawTerms.any { it == "opis" },
            needsTaskHint = rawTerms.any { it == "fix" || it == "task" || it == "tasks" }
        )
    }

    private fun buildLocationKey(currentLocation: GeoPoint?): String = currentLocation?.let {
        val lat = String.format(Locale.US, "%.4f", it.latitude)
        val lon = String.format(Locale.US, "%.4f", it.longitude)
        "$lat:$lon"
    } ?: "none"

    private fun buildSearchBlob(vararg parts: String): String {
        val values = linkedSetOf<String>()
        parts.forEach { part ->
            if (part.isBlank()) return@forEach
            values.add(part)
            val compact = compactSearchToken(part)
            if (compact.length >= 2) values.add(compact)
        }
        return values.joinToString(" ").trim()
    }

    private fun compactSearchToken(value: String): String =
        value.replace(SEARCH_COMPACT_SEPARATOR_REGEX, "").trim()

    private fun normalizeSearchText(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val replaced = value.trim().replace('đ', 'd').replace('Đ', 'D')
        return Normalizer.normalize(replaced, Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(SEARCH_SEPARATOR_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }
}

private fun FilterState.withoutQuery(): FilterState = copy(query = "")

private fun FilterState.hasAnyActiveCriteriaFast(): Boolean =
    query.isNotBlank() ||
        sourceFilter != SourceFilter.ALL ||
        cadastreFilter != CadastreFilter.ALL ||
        caveTypeFilter != CaveTypeFilter.ALL ||
        areaFilter.isNotBlank() ||
        distanceFilterKm != null ||
        depthMinM != null ||
        onlyWithDescription ||
        fieldTaskFilters.isNotEmpty() ||
        boundingBoxFilter != null
