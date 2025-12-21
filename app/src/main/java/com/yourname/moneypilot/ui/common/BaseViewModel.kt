package com.yourname.moneypilot.ui.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<T> : ViewModel() {
    protected val _uiState = MutableStateFlow<ScreenState<T>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<T>> = _uiState.asStateFlow()
}
