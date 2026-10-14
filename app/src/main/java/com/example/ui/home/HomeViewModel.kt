package com.example.ui.home

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FileUtils
import com.example.data.recent.AppDatabase
import com.example.data.recent.RecentFile
import com.example.data.recent.RecentFileRepository
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
  val isLoading: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = RecentFileRepository(
    AppDatabase.getDatabase(application).recentFileDao(),
  )

  private val _selectedTab = MutableStateFlow(HomeTab.ALL)

  val uiState: StateFlow<HomeUiState> = combine(
    repository.allRecentFiles,
    _selectedTab,
  ) { files, tab ->
    val items = files.map { file -> mapToItem(file) }
    val filtered = when (tab) {
      HomeTab.ALL -> items
      HomeTab.PDF -> items.filter { it.tab == HomeTab.PDF }
      HomeTab.WORD -> items.filter { it.tab == HomeTab.WORD }
      HomeTab.TEXT -> items.filter { it.tab == HomeTab.TEXT }
      HomeTab.CODE -> items.filter { it.tab == HomeTab.CODE }
    }
    HomeUiState(
      selectedTab = tab,
      allFiles = items,
      filteredFiles = filtered,
      isLoading = false,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = HomeUiState(isLoading = true),
  )

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
