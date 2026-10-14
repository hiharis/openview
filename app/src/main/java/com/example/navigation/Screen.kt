package com.example.navigation

sealed class Screen(val route: String) {
  object Home : Screen("home")
  object Settings : Screen("settings")
  object About : Screen("about")
  object TextViewer : Screen("viewer_text")
  object CodeViewer : Screen("viewer_code")
  object PdfViewer : Screen("viewer_pdf")
  object DocxViewer : Screen("viewer_docx")
  object Unsupported : Screen("viewer_unsupported")
}
