// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OpenView", appName)
  }

  @Test
  fun `test docx parsing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val file = java.io.File(context.cacheDir, "test.docx")
    java.util.zip.ZipOutputStream(file.outputStream()).use { zos ->
      val entry = java.util.zip.ZipEntry("word/document.xml")
      zos.putNextEntry(entry)
      val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
      <w:r><w:t>Title Test</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:t>Paragraph content here.</w:t></w:r>
    </w:p>
  </w:body>
</w:document>"""
      zos.write(xml.toByteArray(Charsets.UTF_8))
      zos.closeEntry()
    }

    val uri = android.net.Uri.fromFile(file)
    val vm = io.github.hiharis.openview.viewers.docx.DocxViewerViewModel()
    vm.loadDocx(context, uri, "test.docx")
    
    // Give coroutine time or check state
    val state = vm.uiState.value
    println("DOCX test state: " + state)
  }
}
