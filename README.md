# OpenView — Offline File Viewer for Android

OpenView is an open source, fast, clean, and sleek offline file viewer for Android. It has **no ads, no accounts, no analytics, no tracking, and no internet permission**.

Existing free mobile file openers are cluttered with interstitial video ads, trackers, and mandatory account registrations. OpenView was created to provide a distraction-free, privacy-first reader designed for speed and stability.

---

## ⚡ Key Highlights
- **100% Offline & Private:** Zero `android.permission.INTERNET` declared. Files are accessed strictly via Android Storage Access Framework (SAF) and never leave your phone.
- **No Ads & No Telemetry:** No third-party ad networks, no analytics SDKs, zero background data harvesting.
- **Fast & Responsive:** Jetpack Compose UI built on Material 3 with asynchronous file handling using Kotlin Coroutines.
- **Format Support (Phase 1):**
  - **Plain text (`.txt`, `.log`, `.csv`, `.text`):** Fast background chunked streaming for 50MB+ files, BOM and UTF-8/Windows-1252 auto-detection, word wrap toggle, DataStore typography scaling, pinch zoom, and non-blocking in-file search with match navigation.
  - **Code files (`.py`, `.java`, `.kt`, `.cpp`, `.c`, `.js`, `.html`, `.css`, `.json`, `.xml`, `.md`, etc.):** Monospace layout with left line numbers gutter, language indicator chip, lightweight regex syntax highlighting on demand, horizontal scroll, and in-file search.
  - **PDF documents (`.pdf`):** Offline `PdfRenderer` engine with automatic seekable stream caching, LazyColumn page rendering with LRU bitmap cache, pinch-to-zoom, page counter indicator, and jump-to-page dialog.
  - **Microsoft Word documents (`.docx`):** Pure offline parser via `ZipInputStream` and `XmlPullParser` on `word/document.xml`, extracting headings, formatted paragraphs (bold, italic, underline, strikethrough), bullet lists, and clean bordered tables.
  - **Graceful "not supported yet" handling** for legacy `.doc` with conversion guidance.

---

## 🛠️ Tech Stack & Architecture
- **Language:** Kotlin 2.2.10
- **UI Toolkit:** Jetpack Compose with Material 3 (pure Kotlin, no XML layouts)
- **Architecture:** MVVM with Unidirectional Data Flow (`ViewModel` + `StateFlow`)
- **Navigation:** Navigation Compose (`NavHost`)
- **Local Database:** Room Database with KSP (`AppDatabase`, `RecentFileDao`)
- **Preferences:** Jetpack DataStore Preferences
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36

---

## 📂 Project Structure
```
app/src/main/java/io/github/hiharis/openview/
  MainActivity.kt          -> Application entry point with Edge-to-Edge and Theme setup
  navigation/              -> NavGraph and Screen route definitions
  ui/theme/                -> Color, Type, Theme, and Dimens (central design tokens)
  ui/components/           -> Shared reusable components (OpenViewTopAppBar, FileCard, SettingsRow)
  ui/home/                 -> Home screen and HomeViewModel
  ui/settings/             -> Settings screen and SettingsViewModel
  ui/about/                -> About screen with open source license credits
  viewers/
    router/                -> FileRouter (detects file type and selects viewer)
    text/                  -> Text viewer screen and ViewModel
    code/                  -> Code viewer screen and ViewModel
    pdf/                   -> PDF viewer screen and ViewModel
    docx/                  -> DOCX viewer screen and ViewModel
    unsupported/           -> Unsupported format & legacy .doc screen
  data/
    settings/              -> Jetpack DataStore wrapper (SettingsManager)
    recent/                -> Room entities, DAO, AppDatabase, and repository
  core/                    -> Core utilities (FileUtils, FileType detection, size formatting)
```

---

## 👤 Author
Created and maintained by **hiharis**
- GitHub: [hiharis](https://github.com/hiharis)
- Source code: [https://github.com/hiharis/openview](https://github.com/hiharis/openview)

---

## 📄 License
Licensed under the [Apache License, Version 2.0](LICENSE). Copyright 2026 hiharis.
