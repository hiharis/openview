// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.docx

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

sealed interface DocxElement {
  data class Heading(val text: String, val level: Int) : DocxElement
  data class Paragraph(val text: AnnotatedString) : DocxElement
  data class ListItem(val text: AnnotatedString, val indentLevel: Int = 0) : DocxElement
  data class Table(val rows: List<List<AnnotatedString>>) : DocxElement
}

data class DocxSearchMatch(
  val elementIndex: Int,
  val startIndex: Int,
  val length: Int,
)

sealed interface DocxUiState {
  object Loading : DocxUiState

  data class Content(
    val fileName: String,
    val elements: List<DocxElement>,
    val isEmpty: Boolean = false,
    val zoomScale: Float = 1.0f,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<DocxSearchMatch> = emptyList(),
    val currentMatchIndex: Int = 0,
  ) : DocxUiState

  data class Error(val message: String) : DocxUiState
}

class DocxViewerViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<DocxUiState>(DocxUiState.Loading)
  val uiState: StateFlow<DocxUiState> = _uiState.asStateFlow()

  companion object {
    private const val TAG = "FileOpen"
  }

  fun loadDocx(context: Context, uri: Uri, fileName: String) {
    viewModelScope.launch {
      _uiState.value = DocxUiState.Loading

      withContext(Dispatchers.IO) {
        val fileSize = try {
          context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        } catch (_: Exception) {
          -1L
        }
        Log.d(TAG, "FileOpen: starting load DOCX uri=$uri, name=$fileName, size=$fileSize bytes")

        val zipEntries = mutableListOf<String>()
        try {
          val elements = context.contentResolver.openInputStream(uri)?.use { rawStream ->
            ZipInputStream(rawStream).use { zis ->
              var entry = zis.nextEntry
              var documentXmlFound = false
              var parsedElements: List<DocxElement> = emptyList()

              while (entry != null) {
                val rawName = entry.name
                zipEntries.add(rawName)
                val normalizedName = rawName.trim().lowercase(java.util.Locale.ROOT).replace('\\', '/')
                if (normalizedName == "word/document.xml" || normalizedName.endsWith("/word/document.xml")) {
                  documentXmlFound = true
                  Log.d(TAG, "FileOpen: found word/document.xml in zip stream")
                  parsedElements = parseDocumentXml(zis)
                  break
                }
                zis.closeEntry()
                entry = zis.nextEntry
              }

              if (!documentXmlFound) {
                null
              } else {
                parsedElements
              }
            }
          }

          Log.d(TAG, "FileOpen: zip entries checked (${zipEntries.size}): ${zipEntries.take(15)}")

          if (elements == null) {
            Log.e(TAG, "FileOpen: word/document.xml not found in zip for uri=$uri, entries checked=${zipEntries.size}")
            _uiState.value = DocxUiState.Error("This file does not appear to be a valid Word document.")
            return@withContext
          }

          val isEmpty = elements.isEmpty()
          Log.d(TAG, "FileOpen: parsed ${elements.size} elements, isEmpty=$isEmpty for uri=$uri")

          _uiState.value = DocxUiState.Content(
            fileName = fileName,
            elements = elements,
            isEmpty = isEmpty,
          )
        } catch (ce: CancellationException) {
          throw ce
        } catch (e: Exception) {
          Log.e(TAG, "FileOpen: exception parsing DOCX for uri=$uri, name=$fileName", e)
          _uiState.value = DocxUiState.Error("This file does not appear to be a valid Word document.")
        }
      }
    }
  }

  fun setError(message: String) {
    _uiState.value = DocxUiState.Error(message)
  }

  fun setZoomScale(scale: Float) {
    val current = _uiState.value as? DocxUiState.Content ?: return
    _uiState.value = current.copy(zoomScale = scale.coerceIn(0.5f, 4.0f))
  }

  fun toggleDoubleTapZoom() {
    val current = _uiState.value as? DocxUiState.Content ?: return
    val nextScale = if (current.zoomScale < 1.5f) 2.0f else 1.0f
    _uiState.value = current.copy(zoomScale = nextScale)
  }

  fun toggleSearch() {
    val current = _uiState.value as? DocxUiState.Content ?: return
    val nextIsOpen = !current.isSearchOpen
    _uiState.value = current.copy(
      isSearchOpen = nextIsOpen,
      searchQuery = if (nextIsOpen) current.searchQuery else "",
      searchMatches = if (nextIsOpen) current.searchMatches else emptyList(),
      currentMatchIndex = 0,
    )
  }

  fun updateSearchQuery(query: String) {
    val current = _uiState.value as? DocxUiState.Content ?: return
    if (query.isBlank()) {
      _uiState.value = current.copy(
        searchQuery = query,
        searchMatches = emptyList(),
        currentMatchIndex = 0,
      )
      return
    }

    val matches = mutableListOf<DocxSearchMatch>()
    current.elements.forEachIndexed { elemIndex, element ->
      when (element) {
        is DocxElement.Heading -> {
          findQueryOccurrences(element.text, query) { start, len ->
            matches.add(DocxSearchMatch(elemIndex, start, len))
          }
        }
        is DocxElement.Paragraph -> {
          findQueryOccurrences(element.text.text, query) { start, len ->
            matches.add(DocxSearchMatch(elemIndex, start, len))
          }
        }
        is DocxElement.ListItem -> {
          findQueryOccurrences(element.text.text, query) { start, len ->
            matches.add(DocxSearchMatch(elemIndex, start, len))
          }
        }
        is DocxElement.Table -> {
          element.rows.forEach { row ->
            row.forEach { cell ->
              findQueryOccurrences(cell.text, query) { start, len ->
                matches.add(DocxSearchMatch(elemIndex, start, len))
              }
            }
          }
        }
      }
    }

    _uiState.value = current.copy(
      searchQuery = query,
      searchMatches = matches,
      currentMatchIndex = 0,
    )
  }

  private inline fun findQueryOccurrences(text: String, query: String, onMatch: (start: Int, length: Int) -> Unit) {
    var startIndex = 0
    while (startIndex < text.length) {
      val found = text.indexOf(query, startIndex, ignoreCase = true)
      if (found == -1) break
      onMatch(found, query.length)
      startIndex = found + query.length.coerceAtLeast(1)
    }
  }

  fun nextMatch() {
    val current = _uiState.value as? DocxUiState.Content ?: return
    if (current.searchMatches.isEmpty()) return
    val nextIndex = (current.currentMatchIndex + 1) % current.searchMatches.size
    _uiState.value = current.copy(currentMatchIndex = nextIndex)
  }

  fun prevMatch() {
    val current = _uiState.value as? DocxUiState.Content ?: return
    if (current.searchMatches.isEmpty()) return
    val prevIndex = (current.currentMatchIndex - 1 + current.searchMatches.size) % current.searchMatches.size
    _uiState.value = current.copy(currentMatchIndex = prevIndex)
  }

  private fun parseDocumentXml(stream: InputStream): List<DocxElement> {
    val elements = mutableListOf<DocxElement>()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(stream, "UTF-8")

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        val tagName = parser.name.substringAfterLast(':')
        when (tagName) {
          "p" -> {
            val element = parseParagraph(parser)
            if (element != null) {
              elements.add(element)
            }
          }
          "tbl" -> {
            val table = parseTable(parser)
            if (table != null) {
              elements.add(table)
            }
          }
        }
      }
      eventType = parser.next()
    }

    return elements
  }

  private fun parseParagraph(parser: XmlPullParser): DocxElement? {
    var isHeading = false
    var headingLevel = 1
    var isListItem = false
    val builder = AnnotatedString.Builder()

    var depth = 1
    while (depth > 0) {
      val eventType = parser.next()
      if (eventType == XmlPullParser.START_TAG) {
        depth++
        val tagName = parser.name.substringAfterLast(':')
        when (tagName) {
          "pStyle" -> {
            val styleVal = getAttributeValue(parser, "val") ?: ""
            if (styleVal.startsWith("Heading", ignoreCase = true)) {
              isHeading = true
              val digit = styleVal.filter { it.isDigit() }.toIntOrNull()
              headingLevel = digit?.coerceIn(1, 6) ?: 1
            } else if (styleVal.equals("Title", ignoreCase = true)) {
              isHeading = true
              headingLevel = 1
            } else if (styleVal.equals("Subtitle", ignoreCase = true)) {
              isHeading = true
              headingLevel = 2
            }
          }
          "numPr" -> {
            isListItem = true
          }
          "r" -> {
            parseRun(parser, builder)
            depth-- // parseRun consumed its end tag
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        depth--
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        break
      }
    }

    val text = builder.toAnnotatedString()
    if (text.text.isBlank()) return null

    return when {
      isHeading -> DocxElement.Heading(text.text.trim(), headingLevel)
      isListItem -> DocxElement.ListItem(text)
      else -> DocxElement.Paragraph(text)
    }
  }

  private fun parseRun(parser: XmlPullParser, builder: AnnotatedString.Builder) {
    var isBold = false
    var isItalic = false
    var isUnderline = false
    var isStrike = false
    var runText = ""

    var depth = 1
    while (depth > 0) {
      val eventType = parser.next()
      if (eventType == XmlPullParser.START_TAG) {
        depth++
        val tagName = parser.name.substringAfterLast(':')
        when (tagName) {
          "b" -> {
            val v = getAttributeValue(parser, "val")
            isBold = v == null || (v != "0" && !v.equals("false", ignoreCase = true))
          }
          "i" -> {
            val v = getAttributeValue(parser, "val")
            isItalic = v == null || (v != "0" && !v.equals("false", ignoreCase = true))
          }
          "u" -> {
            val v = getAttributeValue(parser, "val")
            isUnderline = v != "none"
          }
          "strike" -> {
            isStrike = true
          }
          "t" -> {
            var tDepth = 1
            while (tDepth > 0) {
              val tEvent = parser.next()
              if (tEvent == XmlPullParser.TEXT) {
                runText += parser.text ?: ""
              } else if (tEvent == XmlPullParser.START_TAG) {
                tDepth++
              } else if (tEvent == XmlPullParser.END_TAG) {
                tDepth--
              } else if (tEvent == XmlPullParser.END_DOCUMENT) {
                break
              }
            }
            depth-- // consumed "t" element end tag
          }
          "br" -> {
            runText += "\n"
          }
          "tab" -> {
            runText += "    "
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        depth--
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        break
      }
    }

    if (runText.isNotEmpty()) {
      val textDec = when {
        isUnderline && isStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        isUnderline -> TextDecoration.Underline
        isStrike -> TextDecoration.LineThrough
        else -> null
      }

      val style = SpanStyle(
        fontWeight = if (isBold) FontWeight.Bold else null,
        fontStyle = if (isItalic) FontStyle.Italic else null,
        textDecoration = textDec,
      )

      val start = builder.length
      builder.append(runText)
      builder.addStyle(style, start, builder.length)
    }
  }

  private fun parseTable(parser: XmlPullParser): DocxElement? {
    val rows = mutableListOf<List<AnnotatedString>>()

    var depth = 1
    while (depth > 0) {
      val eventType = parser.next()
      if (eventType == XmlPullParser.START_TAG) {
        depth++
        val tagName = parser.name.substringAfterLast(':')
        if (tagName == "tr") {
          val row = parseTableRow(parser)
          if (row.isNotEmpty()) {
            rows.add(row)
          }
          depth-- // parseTableRow consumed end tag
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        depth--
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        break
      }
    }

    if (rows.isEmpty()) return null
    return DocxElement.Table(rows)
  }

  private fun parseTableRow(parser: XmlPullParser): List<AnnotatedString> {
    val cells = mutableListOf<AnnotatedString>()

    var depth = 1
    while (depth > 0) {
      val eventType = parser.next()
      if (eventType == XmlPullParser.START_TAG) {
        depth++
        val tagName = parser.name.substringAfterLast(':')
        if (tagName == "tc") {
          val cellText = parseTableCell(parser)
          cells.add(cellText)
          depth-- // parseTableCell consumed end tag
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        depth--
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        break
      }
    }

    return cells
  }

  private fun parseTableCell(parser: XmlPullParser): AnnotatedString {
    val builder = AnnotatedString.Builder()

    var depth = 1
    while (depth > 0) {
      val eventType = parser.next()
      if (eventType == XmlPullParser.START_TAG) {
        depth++
        val tagName = parser.name.substringAfterLast(':')
        if (tagName == "p") {
          val p = parseParagraph(parser)
          if (p != null) {
            val text = when (p) {
              is DocxElement.Heading -> AnnotatedString(p.text)
              is DocxElement.Paragraph -> p.text
              is DocxElement.ListItem -> p.text
              else -> AnnotatedString("")
            }
            if (builder.length > 0) builder.append("\n")
            builder.append(text)
          }
          depth--
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        depth--
      } else if (eventType == XmlPullParser.END_DOCUMENT) {
        break
      }
    }

    return builder.toAnnotatedString()
  }

  private fun getAttributeValue(parser: XmlPullParser, localName: String): String? {
    for (i in 0 until parser.attributeCount) {
      val name = parser.getAttributeName(i).substringAfterLast(':')
      if (name.equals(localName, ignoreCase = true)) {
        return parser.getAttributeValue(i)
      }
    }
    return null
  }
}
