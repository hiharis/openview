# OpenView: Free Offline PDF, DOCX, TXT & Code Viewer for Android

**OpenView is a free, open source, offline document viewer for Android.** It opens PDF, DOCX, TXT and source code files fast, with **no ads, no accounts, no tracking and no internet permission**.

[**Download the latest APK**](https://github.com/hiharis/openview/releases/latest) | [Report a bug](https://github.com/hiharis/openview/issues) | [Source code](https://github.com/hiharis/openview)

<p align="center">
  <img src="docs/screenshots/home.png" width="22%" alt="OpenView home screen with recent files and format tabs">
  <img src="docs/screenshots/pdf.png" width="22%" alt="OpenView PDF viewer with page indicator and night mode">
  <img src="docs/screenshots/docx.png" width="22%" alt="OpenView DOCX viewer showing headings and a table">
  <img src="docs/screenshots/code-dark.png" width="22%" alt="OpenView code viewer in dark mode with line numbers and syntax highlighting">
</p>

## Why OpenView?

Most free file openers on mobile are full of video ads, trackers and forced sign-ups. OpenView is the opposite: a small, fast, distraction-free reader that respects your privacy. If you just want to open a PDF, a Word file or a text file on your phone, this does exactly that and nothing else.

## Features

- **100% offline and private.** The app declares zero `android.permission.INTERNET`. Files are opened through Android's Storage Access Framework and never leave your phone.
- **No ads, no analytics, no telemetry.** No third-party ad networks, no tracking SDKs, no background data collection.
- **Fast and responsive.** Jetpack Compose with Material 3, and all file loading runs off the main thread with Kotlin Coroutines.
- **Light and dark themes** with a clean, accessible design.
- **Recent files list** and settings stored locally on your device.
- **Opens from anywhere.** Use "Open with" from your file manager, WhatsApp, Gmail or any app that shares documents.

## Supported formats

| Format | Extensions | What you get |
|---|---|---|
| **PDF viewer** | `.pdf` | Page rendering with Android's `PdfRenderer`, pinch zoom, double-tap zoom, jump to page, page indicator, night mode (invert colors), fit width / fit page, fast scroll bar |
| **Word (DOCX) viewer** | `.docx` | Headings, paragraphs with bold, italic, underline and strikethrough, bullet lists, bordered tables, pinch and double-tap zoom, in-document search |
| **Text viewer** | `.txt`, `.log`, `.csv`, `.text` | Smooth chunked loading of 50 MB+ files, automatic encoding detection (UTF-8, UTF-16, Windows-1252), word wrap toggle, font scaling, pinch zoom, in-file search |
| **Code viewer** | `.py`, `.kt`, `.java`, `.cpp`, `.c`, `.js`, `.html`, `.css`, `.json`, `.xml`, `.md`, `.sh`, `.yml` and more | Monospace font, line numbers, syntax highlighting, horizontal scroll, in-file search |
| **Legacy Word** | `.doc` | Friendly "not supported yet" screen with an option to open in another app |

## Privacy

OpenView is built so that privacy is enforced by design, not by promise:

- No `INTERNET` permission in `AndroidManifest.xml`, so the app is technically unable to send your data anywhere.
- No analytics, crash reporting or advertising libraries.
- All dependencies are open source (Apache 2.0). See [LICENSES.md](LICENSES.md).

You can check this yourself by reading the manifest or by viewing the app's permissions in Android settings.

## Install

1. **GitHub Releases:** download the latest signed APK from the [Releases page](https://github.com/hiharis/openview/releases/latest).
2. **F-Droid:** submission is planned.

Requires **Android 8.0 (API 26) or newer**.

## Build from source

```bash
git clone https://github.com/hiharis/openview.git
cd openview
./gradlew assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/`. You need JDK 17 or newer and the Android SDK (target SDK 36).

## Tech stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with unidirectional data flow (`ViewModel` + `StateFlow`)
- **Navigation:** Navigation Compose
- **Database:** Room (KSP) for the recent files list and reading positions
- **Preferences:** Jetpack DataStore
- **PDF:** `android.graphics.pdf.PdfRenderer`
- **DOCX:** custom parser using `ZipInputStream` and `XmlPullParser`, with no heavy libraries
- **Min SDK:** 26 | **Target SDK:** 36

<details>
<summary>Project structure</summary>

```
app/src/main/java/io/github/hiharis/openview/
  MainActivity.kt          -> App entry point, edge-to-edge, VIEW intent handling
  navigation/              -> NavGraph and Screen route definitions
  ui/theme/                -> Color, Type, Theme, Dimens (design tokens)
  ui/components/           -> Reusable components (top bar, file card, fast scroll bar)
  ui/home/                 -> Home screen and ViewModel
  ui/settings/             -> Settings screen and ViewModel
  ui/about/                -> About screen and license credits
  viewers/
    router/                -> FileRouter (detects file type by extension, magic bytes, MIME)
    text/                  -> Text viewer
    code/                  -> Code viewer
    pdf/                   -> PDF viewer
    docx/                  -> DOCX viewer
    unsupported/           -> Unsupported format and legacy .doc screen
  data/
    settings/              -> DataStore wrapper
    recent/                -> Room entities, DAO, database, repository
  core/                    -> Utilities (FileUtils, FileType, OpenFileSession)
```

</details>

## Roadmap

- [ ] Image viewer (JPG, PNG, WebP) with zoom and pan
- [ ] Better DOCX fidelity (images, numbered lists, alignment, styles)
- [ ] Saved reading position for every format
- [ ] F-Droid release

Have an idea? [Open an issue](https://github.com/hiharis/openview/issues).

## FAQ

**Is OpenView really free?**
Yes. It is free forever, with no ads, no in-app purchases and no premium tier.

**Does OpenView need internet or any permissions?**
No. It has no internet permission and does not request storage permissions. Files are opened through the Android system file picker.

**Can OpenView open .doc files?**
Not yet. Legacy `.doc` files show a friendly message. Save the file as `.docx` or PDF and it will open.

**Why does my DOCX look different from Word?**
OpenView uses a lightweight reader focused on speed and readable content, so complex layouts (text boxes, columns, floating images) may not match Word exactly.

**Is it an alternative to ad-supported PDF readers?**
Yes. If you want an ad-free, offline PDF and document viewer for Android, that is exactly what OpenView is for.

## Contributing

Pull requests and bug reports are welcome. Please keep the project's core promise intact: no internet permission, no trackers, no ads.

## Support the project

OpenView is free and always will be. The best way to support it is to **star the repo** and **share it with friends and family**. #SupportOpenSource

## Author

Created and maintained by **hiharis**.

- GitHub: [@hiharis](https://github.com/hiharis)
- Source code: [github.com/hiharis/openview](https://github.com/hiharis/openview)

## License

Licensed under the [Apache License, Version 2.0](LICENSE). Copyright 2026 hiharis.
