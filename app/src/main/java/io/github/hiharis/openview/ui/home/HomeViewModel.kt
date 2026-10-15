// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hiharis.openview.core.FileUtils
import io.github.hiharis.openview.data.files.DeviceDocument
import io.github.hiharis.openview.data.files.DeviceDocumentScanner
import io.github.hiharis.openview.data.recent.AppDatabase
import io.github.hiharis.openview.data.recent.RecentFile
import io.github.hiharis.openview.data.recent.RecentFileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class HomeTab {
  ALL,
  PDF,
  WORD,
  TEXT,
  CODE,
}

data class RecentFileItem(
  val id: Long,
  val fileName: String,
  val fileUri: String,
  val extension: String,
  val formattedSize: String,
  val lastOpenedFormatted: String,
  val tab: HomeTab,
)

data class HomeUiState(
  val selectedTab: HomeTab = HomeTab.ALL,
  val allFiles: List<RecentFileItem> = emptyList(),
  val filteredFiles: List<RecentFileItem> = emptyList(),
  val deviceDocuments: List<DeviceDocument> = emptyList(),
  val filteredDeviceDocuments: List<DeviceDocument> = emptyList(),
  val isScanningDocuments: Boolean = false,
  val hasStoragePermission: Boolean = false,
  val isLoading: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = RecentFileRepository(
    AppDatabase.getDatabase(application).recentFileDao(),
  )

  private val _selectedTab = MutableStateFlow(HomeTab.ALL)
  private val _deviceDocuments = MutableStateFlow<List<DeviceDocument>>(emptyList())
  private val _isScanning = MutableStateFlow(false)
  private val _hasStoragePermission = MutableStateFlow(checkStoragePermission())

  init {
    if (_hasStoragePermission.value) {
      scanDeviceDocuments()
    }
  }

  val uiState: StateFlow<HomeUiState> = combine(
    repository.allRecentFiles,
    _selectedTab,
    _deviceDocuments,
    _isScanning,
    _hasStoragePermission,
  ) { files, tab, docs, isScanning, hasPerm ->
    val items = files.map { file -> mapToItem(file) }
    val filteredRecent = when (tab) {
      HomeTab.ALL -> items
      HomeTab.PDF -> items.filter { it.tab == HomeTab.PDF }
      HomeTab.WORD -> items.filter { it.tab == HomeTab.WORD }
      HomeTab.TEXT -> items.filter { it.tab == HomeTab.TEXT }
      HomeTab.CODE -> items.filter { it.tab == HomeTab.CODE }
    }
    val filteredDocs = when (tab) {
      HomeTab.ALL -> docs
      HomeTab.PDF -> docs.filter { it.tab == HomeTab.PDF }
      HomeTab.WORD -> docs.filter { it.tab == HomeTab.WORD }
      HomeTab.TEXT -> docs.filter { it.tab == HomeTab.TEXT }
      HomeTab.CODE -> docs.filter { it.tab == HomeTab.CODE }
    }
    HomeUiState(
      selectedTab = tab,
      allFiles = items,
      filteredFiles = filteredRecent,
      deviceDocuments = docs,
      filteredDeviceDocuments = filteredDocs,
      isScanningDocuments = isScanning,
      hasStoragePermission = hasPerm,
      isLoading = false,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = HomeUiState(isLoading = true),
  )

  fun checkStoragePermission(): Boolean {
    val app = getApplication<Application>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Environment.isExternalStorageManager()
    } else {
      ContextCompat.checkSelfPermission(
        app,
        Manifest.permission.READ_EXTERNAL_STORAGE,
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun checkAndRefreshPermission() {
    val granted = checkStoragePermission()
    _hasStoragePermission.value = granted
    if (granted && _deviceDocuments.value.isEmpty() && !_isScanning.value) {
      scanDeviceDocuments()
    }
  }

  fun updatePermissionStatus(granted: Boolean) {
    _hasStoragePermission.value = granted
    if (granted) {
      scanDeviceDocuments()
    }
  }

  fun scanDeviceDocuments() {
    viewModelScope.launch {
      _isScanning.value = true
      try {
        val docs = DeviceDocumentScanner.scanDocuments(getApplication())
        _deviceDocuments.value = docs
      } catch (e: Exception) {
        Log.e("HomeViewModel", "Error scanning documents: ${e.message}", e)
      } finally {
        _isScanning.value = false
      }
    }
  }

  fun selectTab(tab: HomeTab) {
    _selectedTab.value = tab
  }

  fun removeRecent(id: Long) {
    viewModelScope.launch {
      repository.deleteById(id)
    }
  }

  private fun mapToItem(file: RecentFile): RecentFileItem {
    val ext = file.extension.lowercase(Locale.ROOT)
    val tab = when (ext) {
      "pdf" -> HomeTab.PDF
      "doc", "docx", "rtf", "odt" -> HomeTab.WORD
      "txt", "md", "csv", "log" -> HomeTab.TEXT
      "py", "kt", "kts", "java", "cpp", "c", "h", "hpp", "js", "ts", "html", "css", "json", "xml", "sh", "sql" -> HomeTab.CODE
      else -> HomeTab.TEXT
    }

    val typeLabel = when (tab) {
      HomeTab.PDF -> "PDF"
      HomeTab.WORD -> "Word"
      HomeTab.TEXT -> "Text"
      HomeTab.CODE -> "Code"
      HomeTab.ALL -> ext.uppercase(Locale.ROOT)
    }

    val timeStr = DateUtils.getRelativeTimeSpanString(
      file.lastOpenedTimestamp,
      System.currentTimeMillis(),
      DateUtils.MINUTE_IN_MILLIS,
      DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    val sizeStr = FileUtils.formatFileSize(file.sizeBytes)
    val details = "$typeLabel • $sizeStr • $timeStr"

    return RecentFileItem(
      id = file.id,
      fileName = file.fileName,
      fileUri = file.fileUri,
      extension = ext,
      formattedSize = sizeStr,
      lastOpenedFormatted = details,
      tab = tab,
    )
  }
}
