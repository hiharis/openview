// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.code

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hiharis.openview.data.settings.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

data class CodeSearchMatch(
  val lineIndex: Int,
  val charIndex: Int,
  val length: Int,
)

sealed interface CodeUiState {
  object Loading : CodeUiState

  data class Content(
    val fileName: String,
    val lines: List<String>,
    val totalLines: Int,
    val language: String,
    val encoding: String,
    val sizeBytes: Long,
    val isLargeFile: Boolean,
    val isFullLoadComplete: Boolean = true,
    val wordWrap: Boolean = false,
    val fontSizeSp: Int = 14,
    val zoomScale: Float = 1.0f,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<CodeSearchMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val isEmpty: Boolean = false,
  ) : CodeUiState

  data class Error(val message: String) : CodeUiState
}

class CodeViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<CodeUiState>(CodeUiState.Loading)
  val uiState: StateFlow<CodeUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null
  private var loadJob: Job? = null

  companion object {
    private const val TAG = "FileOpen"
    private const val LARGE_FILE_THRESHOLD_BYTES = 2L * 1024L * 1024L // 2MB
    private const val INITIAL_CHUNK_LINES = 1000
    private const val BATCH_CHUNK_LINES = 2000
  }

  fun loadFile(context: Context, uri: Uri, fileName: String, sizeBytes: Long) {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      _uiState.value = CodeUiState.Loading
      Log.d(TAG, "CodeViewerViewModel: start load file uri=$uri, name=$fileName, size=$sizeBytes")

      try {
        val language = detectLanguage(fileName)
        val settingsFontSize = try {
          SettingsManager(context).fontSizeFlow.first()
        } catch (_: Exception) {
          14
        }

        withContext(Dispatchers.IO) {
          context.contentResolver.openInputStream(uri)?.use { rawStream ->
            val buffered = BufferedInputStream(rawStream, 65536)
            val (charset, _) = detectCharset(buffered)
            val isLargeFile = sizeBytes > LARGE_FILE_THRESHOLD_BYTES
            val reader = BufferedReader(InputStreamReader(buffered, charset), 32768)

            if (!isLargeFile) {
              val lines = mutableListOf<String>()
              var line = reader.readLine()
              while (line != null) {
                lines.add(line)
                line = reader.readLine()
              }
              val isEmpty = lines.isEmpty() || (lines.size == 1 && lines[0].isEmpty() && sizeBytes <= 0)
              Log.d(TAG, "CodeViewerViewModel: fully read ${lines.size} lines in $language, isEmpty=$isEmpty")

              _uiState.value = CodeUiState.Content(
                fileName = fileName,
                lines = lines,
                totalLines = lines.size,
                language = language,
                encoding = charset.displayName(),
                sizeBytes = sizeBytes,
                isLargeFile = false,
                isFullLoadComplete = true,
                fontSizeSp = settingsFontSize,
                isEmpty = isEmpty,
              )
            } else {
              Log.d(TAG, "CodeViewerViewModel: large code file ($sizeBytes bytes). Beginning streaming.")
              val allLines = ArrayList<String>(10000)
              var lineCount = 0

              while (lineCount < INITIAL_CHUNK_LINES) {
                val line = reader.readLine() ?: break
                allLines.add(line)
                lineCount++
              }

              val initialEmpty = allLines.isEmpty()
              _uiState.value = CodeUiState.Content(
                fileName = fileName,
                lines = allLines.toList(),
                totalLines = lineCount,
                language = language,
                encoding = charset.displayName(),
                sizeBytes = sizeBytes,
                isLargeFile = true,
                isFullLoadComplete = false,
                fontSizeSp = settingsFontSize,
                isEmpty = initialEmpty,
              )

              var batchCounter = 0
              var moreLinesAvailable = true
              while (moreLinesAvailable) {
                val nextLine = reader.readLine()
                if (nextLine == null) {
                  moreLinesAvailable = false
                  break
                }
                allLines.add(nextLine)
                lineCount++
                batchCounter++

                if (batchCounter >= BATCH_CHUNK_LINES) {
                  batchCounter = 0
                  val currentSnapshot = allLines.toList()
                  val currentState = _uiState.value
                  if (currentState is CodeUiState.Content) {
                    _uiState.value = currentState.copy(
                      lines = currentSnapshot,
                      totalLines = lineCount,
                    )
                  }
                }
              }

              val finalSnapshot = allLines.toList()
              val currentState = _uiState.value
              if (currentState is CodeUiState.Content) {
                _uiState.value = currentState.copy(
                  lines = finalSnapshot,
                  totalLines = lineCount,
                  isFullLoadComplete = true,
                  isEmpty = finalSnapshot.isEmpty(),
                )
              }
              Log.d(TAG, "CodeViewerViewModel: finished streaming large code file: $lineCount lines")
            }
          } ?: run {
            Log.e(TAG, "CodeViewerViewModel: openInputStream returned null for $uri")
            _uiState.value = CodeUiState.Error("Could not access code file from device storage.")
          }
        }
      } catch (ce: CancellationException) {
        throw ce
      } catch (e: Exception) {
        Log.e(TAG, "CodeViewerViewModel: exception reading code file", e)
        _uiState.value = CodeUiState.Error(e.localizedMessage ?: "Failed to read code file.")
      }
    }
  }

  fun toggleWordWrap() {
    val current = _uiState.value
    if (current is CodeUiState.Content) {
      _uiState.value = current.copy(wordWrap = !current.wordWrap)
    }
  }

  fun setZoomScale(scale: Float) {
    val current = _uiState.value
    if (current is CodeUiState.Content) {
      _uiState.value = current.copy(zoomScale = scale.coerceIn(0.8f, 3.0f))
    }
  }

  fun toggleSearch() {
    val current = _uiState.value
    if (current is CodeUiState.Content) {
      val newOpen = !current.isSearchOpen
      if (!newOpen) {
        searchJob?.cancel()
        _uiState.value = current.copy(
          isSearchOpen = false,
          searchQuery = "",
          searchMatches = emptyList(),
          currentMatchIndex = -1,
        )
      } else {
        _uiState.value = current.copy(isSearchOpen = true)
      }
    }
  }

  fun updateSearchQuery(query: String) {
    val current = _uiState.value
    if (current !is CodeUiState.Content) return

    searchJob?.cancel()
    if (query.isBlank()) {
      _uiState.value = current.copy(
        searchQuery = query,
        searchMatches = emptyList(),
        currentMatchIndex = -1,
      )
      return
    }

    _uiState.value = current.copy(searchQuery = query)

    searchJob = viewModelScope.launch(Dispatchers.Default) {
      val matches = mutableListOf<CodeSearchMatch>()
      val lines = current.lines
      val lowerQuery = query.lowercase()

      for (lineIdx in lines.indices) {
        val line = lines[lineIdx].lowercase()
        var startIndex = 0
        while (startIndex < line.length) {
          val found = line.indexOf(lowerQuery, startIndex)
          if (found == -1) break
          matches.add(CodeSearchMatch(lineIdx, found, query.length))
          startIndex = found + lowerQuery.length
        }
      }

      val stateAfterSearch = _uiState.value
      if (stateAfterSearch is CodeUiState.Content && stateAfterSearch.searchQuery == query) {
        _uiState.value = stateAfterSearch.copy(
          searchMatches = matches,
          currentMatchIndex = if (matches.isNotEmpty()) 0 else -1,
        )
      }
    }
  }

  fun nextMatch() {
    val current = _uiState.value
    if (current is CodeUiState.Content && current.searchMatches.isNotEmpty()) {
      val next = (current.currentMatchIndex + 1) % current.searchMatches.size
      _uiState.value = current.copy(currentMatchIndex = next)
    }
  }

  fun prevMatch() {
    val current = _uiState.value
    if (current is CodeUiState.Content && current.searchMatches.isNotEmpty()) {
      val prev = if (current.currentMatchIndex - 1 < 0) current.searchMatches.size - 1 else current.currentMatchIndex - 1
      _uiState.value = current.copy(currentMatchIndex = prev)
    }
  }

  fun setError(message: String) {
    _uiState.value = CodeUiState.Error(message)
  }

  private fun detectLanguage(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (ext) {
      "kt", "kts" -> "Kotlin"
      "java" -> "Java"
      "py" -> "Python"
      "cpp", "cc", "cxx", "hpp" -> "C++"
      "c", "h" -> "C"
      "js", "jsx", "mjs" -> "JavaScript"
      "ts", "tsx" -> "TypeScript"
      "html", "htm" -> "HTML"
      "css", "scss", "sass" -> "CSS"
      "json" -> "JSON"
      "xml" -> "XML"
      "md", "markdown" -> "Markdown"
      "sh", "bash", "zsh" -> "Shell"
      "yml", "yaml" -> "YAML"
      "gradle" -> "Gradle"
      "sql" -> "SQL"
      "rs" -> "Rust"
      "go" -> "Go"
      "php" -> "PHP"
      "swift" -> "Swift"
      else -> "Code"
    }
  }

  private fun detectCharset(stream: BufferedInputStream): Pair<Charset, Int> {
    stream.mark(4096)
    val bom = ByteArray(4)
    val read = stream.read(bom, 0, 4)

    if (read >= 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
      stream.reset()
      stream.skip(3)
      return Pair(StandardCharsets.UTF_8, 3)
    }
    if (read >= 2 && bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()) {
      stream.reset()
      stream.skip(2)
      return Pair(StandardCharsets.UTF_16LE, 2)
    }
    if (read >= 2 && bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte()) {
      stream.reset()
      stream.skip(2)
      return Pair(StandardCharsets.UTF_16BE, 2)
    }

    stream.reset()
    val sample = ByteArray(4096)
    val sampleRead = stream.read(sample)
    stream.reset()

    if (sampleRead > 0) {
      try {
        val decoder = StandardCharsets.UTF_8.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(sample, 0, sampleRead))
        return Pair(StandardCharsets.UTF_8, 0)
      } catch (_: Exception) {
        return Pair(Charset.forName("windows-1252"), 0)
      }
    }

    return Pair(StandardCharsets.UTF_8, 0)
  }
}
