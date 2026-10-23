# NoteStar

A handwritten note-taking app for Android, built from your phone — no PC needed.
Write naturally with your finger or a stylus, organize notes in a simple
notebook list, and build installable APKs in the cloud with GitHub Actions.

> NoteStar is an original project inspired by the features of modern
> handwriting apps (like StarNote): a drawing canvas, pen tools, and a
> notebook library. It does not use any other app's code or assets.

## Features (v0.1)

- Notebook list: create, open, rename (title is edited in the editor), delete
- Handwriting canvas with smooth strokes
- Tools: pen, highlighter (translucent), eraser (removes strokes it touches)
- 5 ink colors and adjustable stroke thickness
- Undo / redo / clear page
- Notes are saved on-device as JSON files (no account, works offline)

## Build the APK without a PC

This repo builds itself on GitHub:

1. Push any change to `main`, or open the **Actions** tab and run
   **Build APK** manually (Run workflow).
2. Wait for the run to finish (green check).
3. Open the run, scroll to **Artifacts**, and download **NoteStar-debug-apk**.
4. Unzip it on your phone and install `app-debug.apk`
   (you may need to allow "Install unknown apps" for your browser/files app).

The workflow uses Gradle 8.7 on GitHub's servers, so no Gradle wrapper
binary is stored in this repo.

## Project layout

- `app/src/main/java/com/riguel/notestar/` — Kotlin + Jetpack Compose source
  - `MainActivity.kt` — entry point and screen navigation
  - `data/` — stroke/note models and on-device JSON storage
  - `ui/` — notebook list screen and handwriting editor screen
- `.github/workflows/build.yml` — cloud APK build

## Roadmap ideas

- PDF import and annotation
- Infinite / expandable canvas, page templates
- Layers and lasso selection
- Export notes to PDF / PNG
- Audio recording with AI summary
