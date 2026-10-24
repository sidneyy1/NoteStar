package com.riguel.notestar.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.io.OutputStream

/**
 * Mirrors notes into shared storage — Documents/NoteStar — so they survive
 * uninstalling the app (app-private files are deleted on uninstall). Layout:
 *   Documents/NoteStar/<noteId>.json
 *   Documents/NoteStar/pdfs/<fileName>
 * On Android 10+ this uses MediaStore (no permission needed, and files made
 * by this package stay readable by it after a reinstall). On Android 8/9 it
 * writes to the public Documents folder, which needs WRITE_EXTERNAL_STORAGE.
 * Everything here is best-effort: a failed backup never breaks saving.
 */
object BackupManager {

    private val NOTES_PATH = Environment.DIRECTORY_DOCUMENTS + "/NoteStar"
    private val PDFS_PATH = NOTES_PATH + "/pdfs"

    fun needsLegacyPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

    // ---- writes -------------------------------------------------------------

    fun backupNote(context: Context, doc: NoteDocument) {
        try {
            val bytes = NoteJson.toJson(doc).toString().toByteArray(Charsets.UTF_8)
            writeShared(context, doc.id + ".json", NOTES_PATH, "application/json") { out ->
                out.write(bytes)
            }
        } catch (e: Exception) {
            // Best effort only.
        }
    }

    fun backupPdf(context: Context, fileName: String) {
        try {
            val src = NoteStorage.pdfFile(context, fileName)
            if (!src.exists()) return
            writeShared(context, fileName, PDFS_PATH, "application/pdf") { out ->
                src.inputStream().use { input -> input.copyTo(out) }
            }
        } catch (e: Exception) {
            // Best effort only.
        }
    }

    fun deleteBackup(context: Context, noteId: String, pdfFileName: String?) {
        try {
            deleteShared(context, noteId + ".json", NOTES_PATH)
            if (pdfFileName != null) deleteShared(context, pdfFileName, PDFS_PATH)
        } catch (e: Exception) {
            // Best effort only.
        }
    }

    /** Back up every note (and its PDF). Returns how many notes were processed. */
    fun backupAll(context: Context): Int {
        var count = 0
        for (meta in NoteStorage.listNotes(context)) {
            val doc = NoteStorage.loadNote(context, meta.id) ?: continue
            backupNote(context, doc)
            doc.pdf?.let { backupPdf(context, it.fileName) }
            count++
        }
        return count
    }

    // ---- restore ------------------------------------------------------------

    /**
     * Copy any backed-up notes that are missing from internal storage back
     * in (used after a reinstall). Returns how many notes were restored.
     */
    fun restoreAll(context: Context): Int {
        val texts = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaNoteJsonTexts(context)
            } else {
                legacyNoteJsonTexts(context)
            }
        } catch (e: Exception) {
            emptyList()
        }
        var restored = 0
        for (text in texts) {
            try {
                val doc = NoteJson.fromJson(JSONObject(text))
                if (NoteStorage.loadNote(context, doc.id) == null) {
                    doc.pdf?.let { pdf ->
                        val target = NoteStorage.pdfFile(context, pdf.fileName)
                        if (!target.exists()) {
                            readSharedPdf(context, pdf.fileName)?.let { bytes ->
                                target.writeBytes(bytes)
                            }
                        }
                    }
                    NoteStorage.writeNoteInternal(context, doc)
                    restored++
                }
            } catch (e: Exception) {
                // Skip a corrupt backup entry.
            }
        }
        return restored
    }

    // ---- shared-storage plumbing ---------------------------------------------

    private fun collectionUri(): Uri =
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    private fun findUri(context: Context, displayName: String, relativePath: String): Uri? {
        val idColumn = MediaStore.Files.FileColumns._ID
        val selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ? AND " +
            MediaStore.Files.FileColumns.DISPLAY_NAME + " = ?"
        val args = arrayOf(relativePath + "%", displayName)
        context.contentResolver.query(
            collectionUri(), arrayOf(idColumn), selection, args, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn))
                return Uri.withAppendedPath(collectionUri(), id.toString())
            }
        }
        return null
    }

    private fun writeShared(
        context: Context,
        displayName: String,
        relativePath: String,
        mime: String,
        write: (OutputStream) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val existing = findUri(context, displayName, relativePath)
            if (existing != null) {
                try {
                    resolver.openOutputStream(existing, "wt")?.use { write(it) }
                    return
                } catch (e: Exception) {
                    try {
                        resolver.delete(existing, null, null)
                    } catch (ignored: Exception) {
                        // Fall through and insert a fresh entry.
                    }
                }
            }
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, mime)
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
            }
            val uri = resolver.insert(collectionUri(), values) ?: return
            resolver.openOutputStream(uri, "wt")?.use { write(it) }
        } else {
            if (needsLegacyPermission(context)) return
            val dir = legacyDir(relativePath)
            if (!dir.exists()) dir.mkdirs()
            File(dir, displayName).outputStream().use { write(it) }
        }
    }

    private fun deleteShared(context: Context, displayName: String, relativePath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = findUri(context, displayName, relativePath) ?: return
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                // Best effort only.
            }
        } else {
            if (needsLegacyPermission(context)) return
            val file = File(legacyDir(relativePath), displayName)
            if (file.exists()) file.delete()
        }
    }

    private fun readSharedPdf(context: Context, fileName: String): ByteArray? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findUri(context, fileName, PDFS_PATH) ?: return null
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                val file = File(legacyDir(PDFS_PATH), fileName)
                if (file.exists()) file.readBytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mediaNoteJsonTexts(context: Context): List<String> {
        val resolver = context.contentResolver
        val idColumn = MediaStore.Files.FileColumns._ID
        val selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ? AND " +
            MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"
        val args = arrayOf(NOTES_PATH + "%", "%.json")
        val texts = mutableListOf<String>()
        resolver.query(collectionUri(), arrayOf(idColumn), selection, args, null)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn))
                    val uri = Uri.withAppendedPath(collectionUri(), id.toString())
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            texts.add(input.bufferedReader().readText())
                        }
                    } catch (e: Exception) {
                        // Skip unreadable entries.
                    }
                }
            }
        return texts
    }

    private fun legacyNoteJsonTexts(context: Context): List<String> {
        if (needsLegacyPermission(context)) return emptyList()
        val dir = legacyDir(NOTES_PATH)
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun legacyDir(relativePath: String): File {
        val root = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS
        )
        val sub = relativePath.removePrefix(Environment.DIRECTORY_DOCUMENTS).trim('/')
        return if (sub.isEmpty()) root else File(root, sub)
    }
}
