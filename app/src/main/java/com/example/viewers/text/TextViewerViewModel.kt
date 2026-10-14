package com.example.viewers.text

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface TextUiState {
  object Loading : TextUiState
  data class Content(
    val fileName: String,
    val text: String,
    val encoding: String = "UTF-8",
    val fontSizeSp: Int = 14,
    val searchQuery: String = "",
  ) : TextUiState
  data class Error(val message: String) : TextUiState
}

class TextViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<TextUiState>(TextUiState.Loading)
  val uiState: StateFlow<TextUiState> = _uiState.asStateFlow()

  fun loadFile(fileName: String, content: String = "") {
    _uiState.value = TextUiState.Content(fileName = fileName, text = content)
  }

  fun setError(message: String) {
    _uiState.value = TextUiState.Error(message)
  }
}
