package com.example.viewers.docx

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DocxUiState {
  object Loading : DocxUiState
  data class Content(
    val fileName: String,
    val paragraphs: List<String> = emptyList(),
  ) : DocxUiState
  data class Error(val message: String) : DocxUiState
}

class DocxViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<DocxUiState>(DocxUiState.Loading)
  val uiState: StateFlow<DocxUiState> = _uiState.asStateFlow()

  fun loadDocx(fileName: String, paragraphs: List<String>) {
    _uiState.value = DocxUiState.Content(fileName = fileName, paragraphs = paragraphs)
  }

  fun setError(message: String) {
    _uiState.value = DocxUiState.Error(message)
  }
}
