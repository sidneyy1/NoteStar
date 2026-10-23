# NoteStar data model

Proposed before the v0.4 UI work, per the project spec. All serialization
lives in one place: `data/NoteJson.kt`. Every note file carries a `version`
field so the format can evolve without breaking saved notes.

## Entities

- **NoteDocument** — one note.
  - `id`, `title`, `tags: [String]`, `createdAt`, `updatedAt`
  - `pdf: PdfAttachment?` — set when the note annotates an imported PDF
  - `pages: [Page]` — a plain note starts with one page; a PDF note has one
    page per PDF page.
- **PdfAttachment** — `fileName` (inside `files/pdfs/`), `pageCount`.
  The PDF file itself is never modified; annotations live in the note JSON.
- **Page**
  - `id`, `template` (BLANK, LINED, DOTTED, PLANNER, LECTURE)
  - `pdfPageIndex: Int?` — which PDF page this page annotates, if any
  - `layers: [Layer]` — at least one.
- **Layer**
  - `id`, `name`, `visible`, `locked`
  - `strokes: [Stroke]`, `texts: [TextElement]`
  - Hidden layers are not rendered; locked layers cannot be edited.
  - Layer order in the list is back-to-front (first = bottom).
- **Stroke** — `points: [StrokePoint]`, `color` (ARGB ULong as string),
  `thickness` (page units), `highlighter` (rendered translucent).
- **StrokePoint** — `x`, `y`, `pressure` (0..1, 1 when the device reports
  none). Coordinates are *page coordinates*: for plain notes, the canvas
  space before pan/zoom is applied; for PDF pages, PDF points (the page's
  own width/height), so annotations stay correct at any zoom.
- **TextElement** — `id`, `x`, `y`, `text`, `color`, `size` (sp at scale 1),
  `bold`, `italic`, `font` (default/serif/sans/mono/cursive).

## JSON format (version 2)

```json
{
  "version": 2,
  "id": "1729...", "title": "Biology", "tags": ["school"],
  "createdAt": 1729..., "updatedAt": 1729...,
  "pdf": { "fileName": "1729....pdf", "pageCount": 12 },
  "pages": [
    { "id": "...", "template": "LINED", "pdfPageIndex": null,
      "layers": [
        { "id": "...", "name": "Layer 1", "visible": true, "locked": false,
          "strokes": [ { "color": "428...", "thickness": 6.0,
                         "highlighter": false,
                         "points": [[x, y, pressure], ...] } ],
          "texts": [ { "id": "...", "x": 0, "y": 0, "text": "Hi",
                       "color": "428...", "size": 20.0,
                       "bold": false, "italic": false, "font": "default" } ] }
      ] }
  ]
}
```

## Migration

- **v1** files (no `version`, strokes/texts at the root, written by
  NoteStar ≤ 0.3) load as a v2 document with a single page and a single
  layer named "Layer 1", preserving title, template, strokes (pressure
  defaults to 1) and texts. The next save writes v2.
- Future versions must keep `NoteJson.fromJson` able to read every older
  version.

## Tests

`app/src/test/` contains JVM unit tests for the JSON round-trip and the
v1 → v2 migration, plus stroke geometry (smoothing/beautifier). They use
`org.json:json` as a test-only dependency because the Android framework's
`org.json` is a stub on the JVM. Run with `gradle testDebugUnitTest`.
