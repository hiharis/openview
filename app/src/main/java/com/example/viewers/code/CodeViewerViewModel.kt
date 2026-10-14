package com.example.viewers.code

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CodeUiState {
  object Loading : CodeUiState
  data class Content(
    val fileName: String,
    val lines: List<String>,
    val language: String,
    val fontSizeSp: Int = 13,
  ) : CodeUiState
  data class Error(val message: String) : CodeUiState
}

class CodeViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<CodeUiState>(CodeUiState.Loading)
  val uiState: StateFlow<CodeUiState> = _uiState.asStateFlow()

  fun loadCode(fileName: String, lines: List<String>, language: String) {
    _uiState.value = CodeUiState.Content(fileName = fileName, lines = lines, language = language)
  }

  fun setError(message: String) {
    _uiState.value = CodeUiState.Error(message)
  }
}
