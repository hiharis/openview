package com.example.viewers.pdf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PdfUiState {
  object Loading : PdfUiState
  data class Content(
    val fileName: String,
    val totalPages: Int,
    val currentPage: Int = 0,
  ) : PdfUiState
  data class Error(val message: String) : PdfUiState
}

class PdfViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<PdfUiState>(PdfUiState.Loading)
  val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

  fun loadPdf(fileName: String, totalPages: Int) {
    _uiState.value = PdfUiState.Content(fileName = fileName, totalPages = totalPages)
  }

  fun setError(message: String) {
    _uiState.value = PdfUiState.Error(message)
  }
}
