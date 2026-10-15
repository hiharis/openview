// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hiharis.openview.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImageUiState {
  object Loading : ImageUiState()
  data class Content(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val fileSize: Long,
  ) : ImageUiState()
  data class Error(val message: String) : ImageUiState()
}

class ImageViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<ImageUiState>(ImageUiState.Loading)
  val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

  companion object {
    private const val TAG = "FileOpen"
    private const val MAX_DIMENSION = 4096
  }

  fun loadImage(context: Context, uri: Uri, fileName: String) {
    Log.d(TAG, "ImageViewerViewModel: start load image uri=$uri, name=$fileName")
    viewModelScope.launch {
      _uiState.value = ImageUiState.Loading
      withContext(Dispatchers.IO) {
        try {
          val fileSize = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
          } catch (_: Exception) {
            -1L
          }
          Log.d(TAG, "FileOpen: starting load image uri=$uri, name=$fileName, size=$fileSize bytes")

          // 1. Decode bounds first
          val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
          }
          val boundsStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for Uri: $uri")
          boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
          }

          val origWidth = boundsOptions.outWidth
          val origHeight = boundsOptions.outHeight
          if (origWidth <= 0 || origHeight <= 0) {
            throw IllegalStateException(context.getString(R.string.viewer_error_image_corrupt))
          }

          // 2. Compute inSampleSize
          var sampleSize = 1
          while ((origWidth / sampleSize) > MAX_DIMENSION || (origHeight / sampleSize) > MAX_DIMENSION) {
            sampleSize *= 2
          }

          // 3. Decode bitmap with inSampleSize
          val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
          val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
          } ?: throw IllegalArgumentException("Failed to decode image bitmap")

          Log.d(TAG, "FileOpen: image decoded successfully (${origWidth}x${origHeight}, sampleSize=$sampleSize)")
          _uiState.value = ImageUiState.Content(
            bitmap = bitmap,
            width = origWidth,
            height = origHeight,
            fileSize = fileSize,
          )
        } catch (oom: OutOfMemoryError) {
          Log.e(TAG, "FileOpen: OutOfMemoryError loading image: ${oom.message}", oom)
          _uiState.value = ImageUiState.Error(context.getString(R.string.viewer_error_image_oom))
        } catch (e: Exception) {
          Log.e(TAG, "FileOpen: error loading image: ${e.message}", e)
          _uiState.value = ImageUiState.Error(e.message ?: "Failed to load image")
        }
      }
    }
  }
}
