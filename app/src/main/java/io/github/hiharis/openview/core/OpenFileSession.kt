// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import io.github.hiharis.openview.viewers.router.FileRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OpenedFileInfo(
  val uri: Uri,
  val displayName: String,
  val sizeBytes: Long,
  val mimeType: String?,
  val fileType: FileType,
)

sealed interface OpenFileSessionState {
  object Idle : OpenFileSessionState
  object Loading : OpenFileSessionState
  data class Ready(val fileInfo: OpenedFileInfo) : OpenFileSessionState
  data class Error(val errorMessage: String) : OpenFileSessionState
}

object OpenFileSession {
  private const val TAG = "FileOpen"
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private val _sessionState = MutableStateFlow<OpenFileSessionState>(OpenFileSessionState.Idle)
  val sessionState: StateFlow<OpenFileSessionState> = _sessionState.asStateFlow()

  fun openUri(context: Context, uri: Uri) {
    scope.launch {
      _sessionState.value = OpenFileSessionState.Loading

      // Log scheme and authority only to avoid leaking private paths
      Log.d(TAG, "File picked / URI received: scheme=${uri.scheme}, authority=${uri.authority}")

      // 1. Take persistable URI permission if supported by the provider
      try {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        Log.d(TAG, "takePersistableUriPermission granted successfully")
      } catch (se: SecurityException) {
        // Transient intent providers (e.g. email or chat apps) may not allow persistable grants
        Log.d(TAG, "takePersistableUriPermission not supported by provider for this URI: ${se.message}")
      } catch (e: Exception) {
        Log.e(TAG, "takePersistableUriPermission failed", e)
      }

      // 2. Query file metadata off main thread
      val result = withContext(Dispatchers.IO) {
        try {
          var displayName = "unknown_file"
          var sizeBytes = -1L

          context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (nameIndex != -1) {
                val queriedName = cursor.getString(nameIndex)
                if (!queriedName.isNullOrBlank()) {
                  displayName = queriedName
                }
              }
              val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
              if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                sizeBytes = cursor.getLong(sizeIndex)
              }
            }
          }

          if (displayName == "unknown_file") {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
              displayName = lastSegment.substringAfterLast('/')
            }
          }

          val mimeType = context.contentResolver.getType(uri)
          Log.d(TAG, "Metadata read: name=$displayName, size=$sizeBytes, mime=$mimeType")

          val (fileType, detectionMethod) = FileRouter.detectTypeWithMethod(
            context = context,
            uri = uri,
            fileName = displayName,
            mimeType = mimeType,
          )
          Log.d(TAG, "Type detected: $fileType via method=$detectionMethod")

          // Save to Room recent files database
          try {
            val repo = io.github.hiharis.openview.data.recent.RecentFileRepository(
              io.github.hiharis.openview.data.recent.AppDatabase.getDatabase(context).recentFileDao()
            )
            val existing = repo.getFileByUri(uri.toString())
            val ext = displayName.substringAfterLast('.', "")
            val recentEntity = io.github.hiharis.openview.data.recent.RecentFile(
              id = existing?.id ?: 0,
              fileName = displayName,
              fileUri = uri.toString(),
              extension = ext,
              sizeBytes = sizeBytes,
              lastOpenedTimestamp = System.currentTimeMillis(),
              lastPageOrScrollPosition = existing?.lastPageOrScrollPosition ?: 0,
            )
            repo.saveRecentFile(recentEntity)
            Log.d(TAG, "Recent file saved to Room: $displayName")
          } catch (re: Exception) {
            Log.e(TAG, "Failed to save recent file to Room", re)
          }

          Result.success(
            OpenedFileInfo(
              uri = uri,
              displayName = displayName,
              sizeBytes = sizeBytes,
              mimeType = mimeType,
              fileType = fileType,
            ),
          )
        } catch (e: Exception) {
          Log.e(TAG, "Failure reading URI metadata: ${e.message}", e)
          Result.failure(e)
        }
      }

      result.onSuccess { info ->
        Log.d(TAG, "Viewer started for type: ${info.fileType}")
        _sessionState.value = OpenFileSessionState.Ready(info)
        Log.d(TAG, "File open pipeline success for: ${info.displayName}")
      }.onFailure { ex ->
        val friendlyMessage = ex.localizedMessage ?: "Failed to open file."
        Log.e(TAG, "File open pipeline failure: $friendlyMessage", ex)
        _sessionState.value = OpenFileSessionState.Error(friendlyMessage)
      }
    }
  }

  fun reset() {
    _sessionState.value = OpenFileSessionState.Idle
  }
}
