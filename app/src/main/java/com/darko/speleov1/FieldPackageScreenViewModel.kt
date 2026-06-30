package com.darko.speleov1

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FieldPackageScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(FieldPackageScreenState())
    val state: StateFlow<FieldPackageScreenState> = _state.asStateFlow()

    fun setActivePackage(packageId: String?) {
        _state.value = _state.value.copy(activePackageId = packageId)
    }
}

data class FieldPackageScreenState(
    val activePackageId: String? = null
)
