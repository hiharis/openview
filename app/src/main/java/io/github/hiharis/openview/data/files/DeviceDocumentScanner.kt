// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.data.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import io.github.hiharis.openview.core.FileUtils
import io.github.hiharis.openview.ui.home.HomeTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class DeviceDocument(
  val id: String,
  val name: String,
  val uri: Uri,
  val extension: String,
  val sizeBytes: Long,
  val formattedSize: String,
  val folderName: String,
  val lastModified: Long,
  val lastModifiedFormatted: String,
  val tab: HomeTab,
)

object DeviceDocumentScanner {
  private const val TAG = "DeviceDocumentScanner"

  // Supported document and code extensions for OpenView
  private val SUPPORTED_DOCUMENT_EXTENSIONS = setOf(
    // PDF
    "pdf",
    // Word / Office
    "doc", "docx", "rtf", "odt",
    // Text
    "txt", "md", "csv", "log", "tsv",
    // Source Code
    "py", "kt", "kts", "java", "cpp", "c", "h", "hpp", "js", "ts", "html", "css", "json", "xml", "sh", "sql",
  )

  fun mapExtensionToTab(ext: String): HomeTab {
    return when (ext.lowercase(Locale.ROOT)) {
      "pdf" -> HomeTab.PDF
      "doc", "docx", "rtf", "odt" -> HomeTab.WORD
      "txt", "md", "csv", "log", "tsv" -> HomeTab.TEXT
      "py", "kt", "kts", "java", "cpp", "c", "h", "hpp", "js", "ts", "html", "css", "json", "xml", "sh", "sql" -> HomeTab.CODE
      else -> HomeTab.TEXT
    }
  }

  suspend fun scanDocuments(context: Context? = null): List<DeviceDocument> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DeviceDocument>()
    val seenPaths = mutableSetOf<String>()

    val targetDirectories = listOf(
      Pair(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Download"),
      Pair(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Documents"),
    )

    for ((dir, folderName) in targetDirectories) {
      if (dir == null || !dir.exists() || !dir.isDirectory || !dir.canRead()) {
        continue
      }

      val files = dir.listFiles() ?: continue
      for (file in files) {
        if (file.name.startsWith(".")) continue

        if (file.isFile) {
          val ext = file.extension.lowercase(Locale.ROOT)
          if (ext in SUPPORTED_DOCUMENT_EXTENSIONS) {
            val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
            if (seenPaths.add(canonical)) {
              results.add(createDeviceDocument(file, folderName, ext))
            }
          }
        } else if (file.isDirectory && file.canRead()) {
          val subFiles = file.listFiles() ?: continue
          for (subFile in subFiles) {
            if (!subFile.isFile || subFile.name.startsWith(".")) continue
            val ext = subFile.extension.lowercase(Locale.ROOT)
            if (ext in SUPPORTED_DOCUMENT_EXTENSIONS) {
              val canonical = try { subFile.canonicalPath } catch (_: Exception) { subFile.absolutePath }
              if (seenPaths.add(canonical)) {
                results.add(createDeviceDocument(subFile, folderName, ext))
              }
            }
          }
        }
      }
    }

    results.sortByDescending { it.lastModified }
    Log.d(TAG, "Scanned ${results.size} documents from Download and Documents folders")
    results
  }

  private fun createDeviceDocument(file: File, folderName: String, ext: String): DeviceDocument {
    val sizeBytes = file.length()
    val lastModifiedMillis = file.lastModified()
    val relativeTime = DateUtils.getRelativeTimeSpanString(
      lastModifiedMillis,
      System.currentTimeMillis(),
      DateUtils.MINUTE_IN_MILLIS,
      DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    return DeviceDocument(
      id = "doc_${file.absolutePath.hashCode()}",
      name = file.name,
      uri = Uri.fromFile(file),
      extension = ext,
      sizeBytes = sizeBytes,
      formattedSize = FileUtils.formatFileSize(sizeBytes),
      folderName = folderName,
      lastModified = lastModifiedMillis,
      lastModifiedFormatted = relativeTime,
      tab = mapExtensionToTab(ext),
    )
  }
}
