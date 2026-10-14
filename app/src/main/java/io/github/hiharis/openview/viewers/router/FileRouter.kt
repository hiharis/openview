// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.router

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.hiharis.openview.core.FileType
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * FileRouter: Precision file-type routing evaluated in strict order:
 * 1. Extension (txt, code, pdf, docx, doc)
 * 2. Magic bytes (%PDF, PK + word/document.xml, OLE CFB D0CF11E0A1B11AE1)
 * 3. MIME type (text prefix)
 * 4. First 4KB UTF-8 validation (no null bytes, valid UTF-8 charset decoding)
 * 5. Fallback to Unsupported
 *
 * Extension and magic bytes override MIME because content:// URIs often report
 * application/octet-stream or wrong MIME types.
 */
object FileRouter {
  private const val TAG = "FileOpen"

  private val TEXT_EXTENSIONS = setOf("txt", "log", "csv", "text")

  private val CODE_EXTENSIONS = setOf(
    "py", "kt", "java", "cpp", "c", "h", "js", "html", "css", "json",
    "xml", "md", "kts", "gradle", "yml", "yaml", "sh",
  )

  private val OLE_DOC_MAGIC = byteArrayOf(
    0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
    0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte(),
  )

  fun detectTypeWithMethod(
    context: Context,
    uri: Uri,
    fileName: String,
    mimeType: String?,
  ): Pair<FileType, String> {
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    Log.d(TAG, "FileRouter evaluating: uri=$uri, name=$fileName, ext=$ext, mime=$mimeType")

    // 1. Extension inspection
    if (ext.isNotEmpty()) {
      when {
        TEXT_EXTENSIONS.contains(ext) -> {
          Log.d(TAG, "FileRouter: matched TEXT by extension .$ext")
          return Pair(FileType.TEXT, "extension (.$ext)")
        }
        CODE_EXTENSIONS.contains(ext) -> {
          Log.d(TAG, "FileRouter: matched CODE by extension .$ext")
          return Pair(FileType.CODE, "extension (.$ext)")
        }
        ext == "pdf" -> {
          Log.d(TAG, "FileRouter: matched PDF by extension .pdf")
          return Pair(FileType.PDF, "extension (.pdf)")
        }
        ext == "docx" -> {
          Log.d(TAG, "FileRouter: matched DOCX by extension .docx")
          return Pair(FileType.DOCX, "extension (.docx)")
        }
        ext == "doc" -> {
          Log.d(TAG, "FileRouter: matched LEGACY_DOC by extension .doc")
          return Pair(FileType.LEGACY_DOC, "extension (.doc)")
        }
      }
    }

    // 2. Magic bytes inspection (first 4KB)
    var header = ByteArray(4096)
    var bytesRead = 0
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        bytesRead = readFully(stream, header)
      }
      Log.d(TAG, "FileRouter: read $bytesRead header bytes for magic bytes sniffing")
    } catch (e: Exception) {
      Log.e(TAG, "FileRouter: failed reading magic bytes: ${e.message}", e)
    }

    if (bytesRead >= 4) {
      // PDF magic bytes: %PDF (0x25, 0x50, 0x44, 0x46)
      if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() &&
        header[2] == 0x44.toByte() && header[3] == 0x46.toByte()
      ) {
        Log.d(TAG, "FileRouter: matched PDF via magic bytes (%PDF)")
        return Pair(FileType.PDF, "magic_bytes (%PDF)")
      }

      // DOCX: magic bytes PK (0x50, 0x4B) AND zip contains word/document.xml
      if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
        if (zipContainsWordDocument(context, uri)) {
          Log.d(TAG, "FileRouter: matched DOCX via magic bytes (PK + word/document.xml)")
          return Pair(FileType.DOCX, "magic_bytes (PK / word/document.xml)")
        }
      }

      // Legacy .doc magic bytes: D0 CF 11 E0 A1 B1 1A E1 (OLE Compound File)
      if (bytesRead >= 8 && startsWithBytes(header, OLE_DOC_MAGIC)) {
        Log.d(TAG, "FileRouter: matched LEGACY_DOC via magic bytes (OLE CFB)")
        return Pair(FileType.LEGACY_DOC, "magic_bytes (legacy .doc OLE)")
      }
    }

    // 3. MIME inspection (text/*)
    if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream" && mimeType != "*/*") {
      val normalizedMime = mimeType.lowercase(Locale.ROOT)
      if (normalizedMime.startsWith("text/")) {
        Log.d(TAG, "FileRouter: matched TEXT via MIME ($mimeType)")
        return Pair(FileType.TEXT, "MIME ($mimeType)")
      }
    }

    // 4. First 4KB UTF-8 text sniffing (no null bytes, valid UTF-8)
    if (bytesRead > 0 && isValidUtf8(header, bytesRead)) {
      Log.d(TAG, "FileRouter: matched TEXT via 4KB UTF-8 sniffing (no null bytes)")
      return Pair(FileType.TEXT, "sniffing (valid UTF-8 text)")
    }

    // 5. Fallback to Unsupported
    Log.d(TAG, "FileRouter: no match found, fallback to UNSUPPORTED")
    return Pair(FileType.UNSUPPORTED, "fallback (unsupported)")
  }

  private fun zipContainsWordDocument(context: Context, uri: Uri): Boolean {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        ZipInputStream(stream).use { zis ->
          var entry = zis.nextEntry
          var count = 0
          while (entry != null && count < 30) {
            val name = entry.name
            if (name == "word/document.xml" || name.endsWith("/word/document.xml")) {
              return true
            }
            zis.closeEntry()
            entry = zis.nextEntry
            count++
          }
          false
        }
      } ?: false
    } catch (e: Exception) {
      Log.d(TAG, "FileRouter: zip inspection error: ${e.message}")
      false
    }
  }

  private fun startsWithBytes(buffer: ByteArray, prefix: ByteArray): Boolean {
    if (buffer.size < prefix.size) return false
    for (i in prefix.indices) {
      if (buffer[i] != prefix[i]) return false
    }
    return true
  }

  private fun isValidUtf8(bytes: ByteArray, length: Int): Boolean {
    if (length <= 0) return false
    for (i in 0 until length) {
      if (bytes[i] == 0.toByte()) return false
    }
    return try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
      decoder.onMalformedInput(CodingErrorAction.REPORT)
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
      decoder.decode(ByteBuffer.wrap(bytes, 0, length))
      true
    } catch (_: CharacterCodingException) {
      false
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
