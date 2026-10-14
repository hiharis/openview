# Changelog

All notable changes to the OpenView project will be documented in this file. Format follows Keep a Changelog.

---

## [Unreleased] - 2026-10-14

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
