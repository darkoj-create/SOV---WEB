package com.darko.speleov1

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(OfflineScreenState())
    val state: StateFlow<OfflineScreenState> = _state.asStateFlow()

    fun markChanged() {
        _state.value = _state.value.copy(changeNonce = _state.value.changeNonce + 1)
    }
}

data class OfflineScreenState(
    val changeNonce: Int = 0
)
