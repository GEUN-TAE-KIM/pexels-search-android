package com.gtkim.pexelssearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVIの配管をここに集める。
 */
abstract class BaseViewModel<UiState, Intent, Effect>(initialState: UiState) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = effectChannel.receiveAsFlow()

    /** 画面からの入力はすべてここを通る。個別の公開関数は増やさない。 */
    abstract fun onIntent(intent: Intent)

    protected val currentState: UiState get() = _uiState.value

    protected fun updateState(reduce: (UiState) -> UiState) {
        _uiState.update(reduce)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
