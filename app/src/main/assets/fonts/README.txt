Fallback fonts for PDF export only (never used on screen).
Roboto Regular/Bold, Apache License 2.0, Copyright Google.
Source: github.com/googlefonts/roboto (hinted static TTFs).
Used ONLY when the device's own system font file for a text block cannot
be found at export time. The normal path embeds the device's system font
file (e.g. /system/fonts/Roboto-Regular.ttf), which is the font Compose
renders on screen. No built-in PDF (Standard 14) font is ever used for
normal-note text export.
