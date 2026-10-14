// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class PdfFitMode {
  FIT_WIDTH,
  FIT_PAGE,
}

sealed interface PdfUiState {
  object Loading : PdfUiState

  data class Content(
    val fileName: String,
    val totalPages: Int,
    val currentPage: Int = 1,
    val zoomScale: Float = 1.0f,
    val isNightMode: Boolean = false,
    val fitMode: PdfFitMode = PdfFitMode.FIT_WIDTH,
  ) : PdfUiState

  data class Error(val message: String) : PdfUiState
}

class PdfViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<PdfUiState>(PdfUiState.Loading)
  val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

  private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
  val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

  private var pdfRenderer: PdfRenderer? = null
  private var parcelFileDescriptor: ParcelFileDescriptor? = null
  private var cachedTempFile: File? = null
  private val renderLock = Any()
  private var isClosed = false

  private val bitmapCache = object : LruCache<Int, Bitmap>(16) {
    override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
      super.entryRemoved(evicted, key, oldValue, newValue)
    }
  }

  companion object {
    private const val TAG = "FileOpen"
  }

  fun loadPdf(context: Context, uri: Uri, fileName: String) {
    viewModelScope.launch {
      _uiState.value = PdfUiState.Loading
      Log.d(TAG, "PdfViewerViewModel: start load PDF uri=$uri, name=$fileName")

      withContext(Dispatchers.IO) {
        synchronized(renderLock) {
          closeResourcesInternal()
          isClosed = false
        }

        try {
          var pfd: ParcelFileDescriptor? = null
          var renderer: PdfRenderer? = null

          // 1. Try opening descriptor directly from ContentResolver
          try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
              renderer = PdfRenderer(pfd)
              Log.d(TAG, "PdfViewerViewModel: PdfRenderer initialized directly from ContentResolver PFD")
            }
          } catch (ioe: IOException) {
            // Non-seekable descriptor or pipe, copy to cacheDir
            Log.d(TAG, "PdfViewerViewModel: direct PFD not seekable (${ioe.message}), copying stream to cache")
            pfd?.close()
            pfd = null
          } catch (se: SecurityException) {
            val msg = se.message ?: ""
            if (msg.contains("password", ignoreCase = true) || msg.contains("encrypt", ignoreCase = true)) {
              Log.e(TAG, "PdfViewerViewModel: password protected PDF detected", se)
              _uiState.value = PdfUiState.Error("Password-protected PDFs are not supported yet.")
              return@withContext
            }
            throw se
          }

          // 2. Fallback to copying to cacheDir if direct open was non-seekable
          if (renderer == null) {
            val tempFile = File(context.cacheDir, "openview_pdf_${System.currentTimeMillis()}.pdf")
            cachedTempFile = tempFile

            context.contentResolver.openInputStream(uri)?.use { inStream ->
              FileOutputStream(tempFile).use { outStream ->
                inStream.copyTo(outStream)
              }
            } ?: run {
              _uiState.value = PdfUiState.Error("Could not access PDF stream from device storage.")
              return@withContext
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            Log.d(TAG, "PdfViewerViewModel: PdfRenderer initialized from temporary cached file")
          }

          synchronized(renderLock) {
            pdfRenderer = renderer
            parcelFileDescriptor = pfd
          }

          val pages = renderer.pageCount
          Log.d(TAG, "PdfViewerViewModel: PDF opened successfully. Total pages: $pages")

          _uiState.value = PdfUiState.Content(
            fileName = fileName,
            totalPages = pages,
            currentPage = 1,
            zoomScale = 1.0f,
            isNightMode = false,
            fitMode = PdfFitMode.FIT_WIDTH,
          )
        } catch (se: SecurityException) {
          Log.e(TAG, "PdfViewerViewModel: SecurityException opening PDF", se)
          _uiState.value = PdfUiState.Error("Password-protected PDFs are not supported yet.")
        } catch (ce: CancellationException) {
          throw ce
        } catch (e: Exception) {
          Log.e(TAG, "PdfViewerViewModel: error initializing PdfRenderer", e)
          val errorText = if (e.message?.contains("password", ignoreCase = true) == true) {
            "Password-protected PDFs are not supported yet."
          } else {
            "The PDF file is corrupted or cannot be rendered."
          }
          _uiState.value = PdfUiState.Error(errorText)
        }
      }
    }
  }

  fun requestPageBitmap(pageIndex: Int, targetWidthPx: Int = 1080) {
    val cached = bitmapCache.get(pageIndex)
    if (cached != null) {
      if (_pageBitmaps.value[pageIndex] != cached) {
        _pageBitmaps.value = _pageBitmaps.value + (pageIndex to cached)
      }
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      val bitmap = synchronized(renderLock) {
        if (isClosed || pdfRenderer == null) return@synchronized null
        try {
          val renderer = pdfRenderer ?: return@synchronized null
          if (pageIndex !in 0 until renderer.pageCount) return@synchronized null

          val page = renderer.openPage(pageIndex)
          try {
            val pageWidth = page.width
            val pageHeight = page.height
            val scale = (targetWidthPx.toFloat() / pageWidth).coerceIn(1.0f, 3.5f)
            val w = (pageWidth * scale).toInt().coerceAtLeast(1)
            val h = (pageHeight * scale).toInt().coerceAtLeast(1)

            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
          } finally {
            page.close()
          }
        } catch (e: Exception) {
          Log.e(TAG, "PdfViewerViewModel: failed to render page $pageIndex", e)
          null
        }
      }

      if (bitmap != null) {
        bitmapCache.put(pageIndex, bitmap)
        _pageBitmaps.value = _pageBitmaps.value + (pageIndex to bitmap)
      }
    }
  }

  fun updateCurrentPage(page: Int) {
    val current = _uiState.value
    if (current is PdfUiState.Content && current.currentPage != page) {
      _uiState.value = current.copy(currentPage = page)
    }
  }

  fun setZoomScale(scale: Float) {
    val current = _uiState.value
    if (current is PdfUiState.Content) {
      _uiState.value = current.copy(zoomScale = scale.coerceIn(1.0f, 4.0f))
    }
  }

  fun toggleDoubleTapZoom() {
    val current = _uiState.value as? PdfUiState.Content ?: return
    val nextScale = if (current.zoomScale < 1.5f) 2.0f else 1.0f
    _uiState.value = current.copy(zoomScale = nextScale)
  }

  fun toggleNightMode() {
    val current = _uiState.value as? PdfUiState.Content ?: return
    _uiState.value = current.copy(isNightMode = !current.isNightMode)
  }

  fun toggleFitMode() {
    val current = _uiState.value as? PdfUiState.Content ?: return
    val nextMode = if (current.fitMode == PdfFitMode.FIT_WIDTH) PdfFitMode.FIT_PAGE else PdfFitMode.FIT_WIDTH
    _uiState.value = current.copy(fitMode = nextMode)
  }

  fun setError(message: String) {
    _uiState.value = PdfUiState.Error(message)
  }

  override fun onCleared() {
    super.onCleared()
    synchronized(renderLock) {
      closeResourcesInternal()
    }
  }

  private fun closeResourcesInternal() {
    isClosed = true
    try {
      pdfRenderer?.close()
    } catch (e: Exception) {
      Log.d(TAG, "PdfViewerViewModel: error closing renderer: ${e.message}")
    }
    pdfRenderer = null

    try {
      parcelFileDescriptor?.close()
    } catch (e: Exception) {
      Log.d(TAG, "PdfViewerViewModel: error closing PFD: ${e.message}")
    }
    parcelFileDescriptor = null

    try {
      cachedTempFile?.delete()
    } catch (_: Exception) {}
    cachedTempFile = null

    bitmapCache.evictAll()
    _pageBitmaps.value = emptyMap()
  }
}
