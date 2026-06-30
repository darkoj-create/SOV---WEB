package com.darko.speleov1

import androidx.lifecycle.ViewModel
import com.darko.speleov1.model.FilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(SearchScreenState())
    val state: StateFlow<SearchScreenState> = _state.asStateFlow()

    fun rememberFilters(filters: FilterState) {
        _state.value = _state.value.copy(lastFilters = filters)
    }
}

data class SearchScreenState(
    val lastFilters: FilterState? = null
)
