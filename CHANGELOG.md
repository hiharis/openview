# Changelog

OpenView is created and maintained by **hiharis** (GitHub: [hiharis](https://github.com/hiharis/openview)).
All notable changes to the OpenView project will be documented in this file. Format follows Keep a Changelog.

---

## [Unreleased] - 2026-10-14

### Added
- Implemented core document viewing engine for all supported formats in one pass:
  - **Precision 5-tier FileRouter**: Extension evaluation (`txt`, `code`, `pdf`, `docx`, `doc`), magic bytes sniffing (`%PDF`, `PK` + `word/document.xml`, `D0 CF 11 E0 A1 B1 1A E1`), MIME fallback (`text/*`), 4KB UTF-8 text validation without null bytes, and graceful unsupported routing.
  - **TXT Reader**: Full read for <= 2MB, background chunked streaming for > 2MB files, BOM detection (`UTF-8`, `UTF-16LE`, `UTF-16BE`) with `windows-1252` fallback, word wrap toggle, DataStore font sizing, pinch-to-zoom scaling, cancellable in-file search with match counter and next/prev navigation, and clean empty file states.
  - **Code Reader**: Monospace typography, left line number gutter with vertical divider, language detection from extension with chip indicator, lightweight regex syntax highlighting (keywords, strings, comments, numbers) on demand, horizontal scroll, word wrap toggle, and in-file search.
  - **PDF Reader**: `PdfRenderer` integration with seekable stream fallback copying to `cacheDir`, LazyColumn visible page rendering, LRU bitmap cache, pinch-to-zoom, jump-to-page dialog, floating page counter pill, and password-protected PDF error handling.
  - **DOCX Parser**: Pure zero-external-dependency parser via `ZipInputStream` + `XmlPullParser` on `word/document.xml`, extracting headings (`Heading1..6`, `Title`), styled paragraphs (`bold`, `italic`, `underline`, `strikethrough`), bullet lists, and bordered tables (`DocxTableCard`).
  - **Unsupported & Legacy Screen**: User-friendly advice for legacy Word `.doc` files and unsupported formats with 100% externalized string resources.

### Changed
- Rebranded visible project identity to **OpenView** across `strings.xml`, `TopAppBar`, About screen, README, LICENSES, and documentation.
- UI Redesign: Upgraded from strict two-color theme to modern, accessible Material 3 design system:
  - Confident accent color: Modern Blue (`#1D63ED` / `AccentBlue`).
  - Dark mode surfaces: `#121418` (Background) and `#1C1F26` (Cards/Surfaces) with `#F0F2F5` text.
  - Light mode surfaces: `#F8F9FA` (Background) and `#FFFFFF` (Cards/Surfaces) with `#1E2022` text.
  - Generous 16–24dp rounded corners, 56dp elderly-friendly minimum touch targets, subtle card elevation/depth, and color-coded format badges (PDF, Word, Text, Code).
  - Renamed theme composables and top app bars to `OpenViewTheme` and `OpenViewTopAppBar`.

### Added
- Initial project skeleton with MVVM architecture, Jetpack Compose, Material 3, Navigation Compose, Room, and DataStore.
- Design system tokens in `ui/theme/`: `Color.kt`, `Dimens.kt`, `Type.kt`, and `Theme.kt` (`OpenViewTheme`).
- Reusable UI components: `OpenViewTopAppBar`, `FileCard`, and `SettingsRow`.
- Home screen with hero SAF file picker, format filter tabs, and recent files empty state.
- Settings screen with theme selection (System / Light / Dark), text size preview slider, and privacy breakdown.
- About screen displaying version info, mission, zero-permission statement, and third-party library licenses.
- Skeletons for file viewing engine: `FileRouter`, `TextViewerScreen`, `CodeViewerScreen`, `PdfViewerScreen`, `DocxViewerScreen`, and `UnsupportedFileScreen`.
- Local Room persistence setup (`RecentFile`, `RecentFileDao`, `AppDatabase`, `RecentFileRepository`).
- DataStore preference wrapper (`SettingsManager`).
- Intent filter for `android.intent.action.VIEW` in `AndroidManifest.xml` supporting offline "Open with".
- Project documentation: `README.md`, `LICENSES.md`, `CHANGELOG.md`, and `PROJECT_CONTEXT.txt`.
