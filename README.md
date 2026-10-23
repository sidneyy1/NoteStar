# NoteStar

A free, offline handwritten note-taking app for Android tablets and phones,
built from your phone — no PC needed. No accounts, no registration: notes
stay on the device unless you export them.

> NoteStar is an original project inspired by the features of modern
> handwriting apps (like StarNote). It does not use any other app's code
> or assets.

## Features (v0.4)

- **Handwriting** — pen and highlighter with pressure-sensitive strokes
  (uses stylus pressure when the device provides it), stroke smoothing,
  and an optional beautifier (wand icon) that smooths strokes and aligns
  near-straight lines without changing their meaning.
- **Canvas** — a large pan-and-zoom canvas per page (Move tool), multiple
  pages per note, and **layers** that can be added, renamed, reordered,
  hidden, locked, and deleted.
- **PDF annotation** — import a PDF from the notebook list; it opens
  page-by-page for handwriting, highlighting, and typed text on top.
  Annotations are stored in the note JSON, so the original PDF file is
  never modified.
- **Inline rich text** — tap the page and type right on the paper (no
  popup), pinch to resize, move by dragging, bold / italic / font choice
  (Default, Serif, Sans, Mono, Cursive), swatch palette + hex color picker.
- **Organization** — tags on every note with a tag filter on the home
  screen, an outline view built from a page's text blocks (tap to jump),
  and one-tap "excerpt page into a new note".
- **Templates** — Blank, Lined, Planner, Lecture notes, and Mind map
  (dot grid) for new notes.
- **Appearance** — light and dark themes with a choice of accent colors,
  plus a distraction-free full-screen mode in the editor.
- **Multitasking** — picture-in-picture button keeps the note visible in
  a floating window while another app plays video.
- Notebook list: create, open, delete notes. Everything works offline.

## Data format

Notes are JSON files in the app's private storage, with a `version`
field (current: 2). All serialization lives in `data/NoteJson.kt`; the
format, entities (notes, pages, layers, annotations), and the v1 → v2
migration are documented in `docs/DATA_MODEL.md`.

## Tests

JVM unit tests cover the JSON round-trip, the v1 → v2 migration, and the
stroke geometry (smoothing / beautifier):

```
gradle testDebugUnitTest
```

To run them in GitHub Actions, change the build command in
`.github/workflows/builds.yml` to `gradle assembleDebug testDebugUnitTest`.

## Tech stack

- Kotlin 2.0.20, Jetpack Compose + Material 3 (BOM 2024.09.00),
  single-activity, no XML layouts
- Android Gradle Plugin 8.5.2, compileSdk/targetSdk 34, minSdk 26
- Storage: JSON files via `org.json` (no database, no backend)
- Build: Gradle 8.7 on Java 17 (Temurin) via GitHub Actions

## How to build the APK (no PC needed)

1. Push this project to GitHub.
2. Add the workflow file `.github/workflows/builds.yml` (GitHub website →
   Add file → Create new file) — it installs Gradle 8.7, runs
   `gradle assembleDebug`, and uploads the APK as an artifact.
3. Open the repo's **Actions** tab, pick the latest **Build APK** run.
4. Download the **NoteStar-debug-apk** artifact, unzip it, and install
   `app-debug.apk` on your Android device (allow "Install unknown apps"
   for your browser/files app if Android asks).

## Roadmap

- Image import (PDF import is in; the import path is built to extend)
- User-created templates (save a page style as a reusable template)
- Region lasso excerpt (v0.4 excerpts a whole page)
