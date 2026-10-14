package com.example.viewers.router

import android.content.Context
import android.net.Uri
import com.example.core.FileType
import com.example.core.FileUtils
import java.io.InputStream
import java.util.Locale

/**
 * FileRouter: Detects file type from MIME type, then file extension, then magic bytes,
 * with graceful fallback to Unsupported.
 */
object FileRouter {

  fun detectTypeWithMethod(
    context: Context,
    uri: Uri,
    fileName: String,
    mimeType: String?,
  ): Pair<FileType, String> {
    // 1. MIME type inspection
    if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream" && mimeType != "*/*") {
      val normalizedMime = mimeType.lowercase(Locale.ROOT)
      when {
        normalizedMime == "application/pdf" -> return Pair(FileType.PDF, "MIME ($mimeType)")
        normalizedMime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
          return Pair(FileType.DOCX, "MIME ($mimeType)")
        normalizedMime == "application/msword" ->
          return Pair(FileType.LEGACY_DOC, "MIME ($mimeType)")
        isCodeMime(normalizedMime) ->
          return Pair(FileType.CODE, "MIME ($mimeType)")
        normalizedMime.startsWith("text/") ->
          return Pair(FileType.TEXT, "MIME ($mimeType)")
      }
    }

    // 2. Extension inspection
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (ext.isNotEmpty()) {
      val typeFromExt = FileUtils.detectFileType(fileName)
      if (typeFromExt != FileType.UNSUPPORTED) {
        return Pair(typeFromExt, "extension (.$ext)")
      }
    }

    // 3. Magic bytes inspection
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val header = ByteArray(512)
        val bytesRead = readFully(stream, header)
        if (bytesRead >= 4) {
          // PDF magic bytes: %PDF (0x25, 0x50, 0x44, 0x46)
          if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() &&
            header[2] == 0x44.toByte() && header[3] == 0x46.toByte()
          ) {
            return Pair(FileType.PDF, "magic_bytes (%PDF)")
          }

          // DOCX / ZIP magic bytes: PK\x03\x04
          if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
          ) {
            return Pair(FileType.DOCX, "magic_bytes (PK / DOCX)")
          }

          // Plain text sniffing: check for null bytes or binary garbage in the first chunk
          var hasNullByte = false
          for (i in 0 until bytesRead) {
            if (header[i] == 0.toByte()) {
              hasNullByte = true
              break
            }
          }
          if (!hasNullByte && bytesRead > 0) {
            return Pair(FileType.TEXT, "magic_bytes (text sniffing)")
          }
        }
      }
    } catch (_: Exception) {
      // Stream reading error will be caught during viewer rendering
    }

    // 4. Fallback
    return Pair(FileType.UNSUPPORTED, "fallback")
  }

  private fun isCodeMime(mime: String): Boolean {
    return when (mime) {
      "application/json", "application/xml", "text/xml", "text/html",
      "text/css", "text/javascript", "application/javascript",
      "text/x-python", "text/x-java-source", "text/x-c", "text/x-c++",
      "text/markdown", "text/x-markdown",
      -> true
      else -> false
    }
  }

  private fun readFully(stream: InputStream, buffer: ByteArray): Int {
    var totalRead = 0
    while (totalRead < buffer.size) {
      val read = stream.read(buffer, totalRead, buffer.size - totalRead)
      if (read == -1) break
      totalRead += read
    }
    return totalRead
  }
}
