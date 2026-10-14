package com.example.core

import java.util.Locale

object FileUtils {
  private val CODE_EXTENSIONS = setOf(
    "py", "java", "kt", "kts", "cpp", "c", "cc", "h", "hpp",
    "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss",
    "json", "xml", "md", "markdown", "sh", "bash", "yaml",
    "yml", "sql", "gradle", "rs", "go", "php", "swift",
  )

  fun detectFileType(extensionOrFileName: String): FileType {
    val ext = extensionOrFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
      ext == "txt" || ext == "log" || ext == "csv" || ext == "tsv" -> FileType.TEXT
      CODE_EXTENSIONS.contains(ext) -> FileType.CODE
      ext == "pdf" -> FileType.PDF
      ext == "docx" -> FileType.DOCX
      ext == "doc" -> FileType.LEGACY_DOC
      else -> FileType.UNSUPPORTED
    }
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
      size /= 1024
      unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
  }
}
