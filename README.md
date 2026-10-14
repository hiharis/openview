# OpenView — Offline File Viewer for Android

OpenView is an open source, fast, clean, and sleek offline file viewer for Android. It has **no ads, no accounts, no analytics, no tracking, and no internet permission**.

Existing free mobile file openers are cluttered with interstitial video ads, trackers, and mandatory account registrations. OpenView was created to provide a distraction-free, privacy-first reader designed for speed and stability.

---

## ⚡ Key Highlights
- **100% Offline & Private:** Zero `android.permission.INTERNET` declared. Files are accessed strictly via Android Storage Access Framework (SAF) and never leave your phone.
- **No Ads & No Telemetry:** No third-party ad networks, no analytics SDKs, zero background data harvesting.
- **Fast & Responsive:** Jetpack Compose UI built on Material 3 with asynchronous file handling using Kotlin Coroutines.
- **Format Support (Phase 1):**
  - Plain text (`.txt`, `.log`, `.csv`, `.tsv`)
  - Code files (`.py`, `.java`, `.kt`, `.cpp`, `.c`, `.js`, `.ts`, `.html`, `.css`, `.json`, `.xml`, `.md`, etc.)
  - PDF documents (`.pdf`)
  - Microsoft Word documents (`.docx`)
  - Graceful "not supported yet" handling for legacy `.doc`

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
app/src/main/java/com/example/
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

## 📄 License
Licensed under the [Apache License, Version 2.0](LICENSES.md).
