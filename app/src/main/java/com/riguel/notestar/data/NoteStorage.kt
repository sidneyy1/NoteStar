package com.riguel.notestar.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.json.JSONObject
import java.io.File
import java.io.InputStream

/**
 * File IO for notes. Format knowledge lives in [NoteJson]; this object only
 * knows where files live:
 *   files/notes/<id>.json  — one JSON file per note
 *   files/pdfs/<fileName>  — imported PDFs (originals, never modified)
 */
object NoteStorage {

    private fun notesDir(context: Context): File {
        val dir = File(context.filesDir, "notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun pdfsDir(context: Context): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun noteFile(context: Context, id: String): File =
        File(notesDir(context), "$id.json")

    fun pdfFile(context: Context, fileName: String): File =
        File(pdfsDir(context), fileName)

    fun newId(): String = System.currentTimeMillis().toString()

    fun listNotes(context: Context): List<NoteMeta> {
        val files = notesDir(context).listFiles { f -> f.extension == "json" }
            ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                val doc = NoteJson.fromJson(JSONObject(file.readText()))
                NoteMeta(
                    id = doc.id,
                    title = doc.title,
                    updatedAt = doc.updatedAt,
                    template = doc.pages.firstOrNull()?.template ?: PageTemplate.BLANK,
                    tags = doc.tags,
                    isPdf = doc.pdf != null,
                    pageCount = doc.pages.size,
                    coverColorValue = doc.coverColorValue
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun loadNote(context: Context, id: String): NoteDocument? {
        val file = noteFile(context, id)
        if (!file.exists()) return null
        return try {
            NoteJson.fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            null
        }
    }

    fun saveNote(context: Context, doc: NoteDocument) {
        writeNoteInternal(context, doc)
        // Best-effort mirror into shared storage so notes survive uninstall.
        BackupManager.backupNote(context, doc)
        doc.pdf?.let { BackupManager.backupPdf(context, it.fileName) }
    }

    /** Internal-storage write only (no backup) — used when restoring. */
    fun writeNoteInternal(context: Context, doc: NoteDocument) {
        noteFile(context, doc.id).writeText(NoteJson.toJson(doc).toString())
    }

    fun deleteNote(context: Context, id: String) {
        // Clean up the imported PDF copy too, if this note had one.
        val pdfName = loadNote(context, id)?.pdf?.fileName
        if (pdfName != null) {
            val pdf = pdfFile(context, pdfName)
            if (pdf.exists()) pdf.delete()
        }
        val file = noteFile(context, id)
        if (file.exists()) file.delete()
        BackupManager.deleteBackup(context, id, pdfName)
    }

    /**
     * Copy an imported PDF into private storage and create a note that can
     * annotate it: one page per PDF page, each with an empty annotation
     * layer. Returns the new document, or null when the file is not a
     * readable PDF.
     */
    fun importPdf(context: Context, input: InputStream, displayName: String): NoteDocument? {
        val fileName = System.currentTimeMillis().toString() + ".pdf"
        val target = pdfFile(context, fileName)
        try {
            input.use { stream ->
                target.outputStream().use { out -> stream.copyTo(out) }
            }
            val pageCount = ParcelFileDescriptor.open(
                target, ParcelFileDescriptor.MODE_READ_ONLY
            ).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
            }
            if (pageCount <= 0) {
                target.delete()
                return null
            }
            val now = System.currentTimeMillis()
            val pages = (0 until pageCount).map { index ->
                Page(
                    id = "p" + System.nanoTime().toString() + index,
                    pdfPageIndex = index,
                    layers = listOf(Layer.create("Annotations"))
                )
            }
            val doc = NoteDocument(
                id = newId(),
                title = displayName.removeSuffix(".pdf").removeSuffix(".PDF"),
                createdAt = now,
                updatedAt = now,
                pdf = PdfAttachment(fileName = fileName, pageCount = pageCount),
                pages = pages
            )
            saveNote(context, doc)
            return doc
        } catch (e: Exception) {
            if (target.exists()) target.delete()
            return null
        }
    }
}
