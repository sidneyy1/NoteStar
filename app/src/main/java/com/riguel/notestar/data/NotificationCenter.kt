package com.riguel.notestar.data

/**
 * The single place quiet status messages live. Editor screens never show
 * them: they hand an event to [queue] and keep working silently. Only the
 * All notes (main menu) screen drains the queue, through its own
 * SnackbarHost, when it is in the foreground — so entering, leaving or
 * switching tabs inside an editor can never flash a toast.
 *
 * Rules (all testable here, no Android needed):
 * - Only events for an actual change are queued; callers decide that by
 *   comparing content before calling [queueSave]. A save that changed
 *   nothing queues nothing, so opening and closing a note 10 times with
 *   no edit produces no message at all.
 * - The same message for the same note is not shown twice within
 *   [DEDUP_WINDOW_MS] (30 seconds): a second event in the window is
 *   dropped if an identical one is already queued or was just shown.
 * - When the menu drains, several pending save events collapse into one
 *   ("2 notes saved") instead of a parade of snackbars. Non-save events
 *   (import, backup, excerpt) keep their own text and are not merged.
 * - "Show save confirmations" (Settings > Notifications, default on)
 *   gates only save/annotation events. Failures and export results never
 *   go through here at all: they are shown immediately, wherever the user
 *   is, via dialogs in MainActivity.
 */
object NotificationCenter {

    const val DEDUP_WINDOW_MS: Long = 30_000L

    enum class Kind { SAVE, INFO }

    data class Event(
        val kind: Kind,
        val noteId: String?,
        val text: String,
        val queuedAtMs: Long
    )

    private val queue = mutableListOf<Event>()
    private val lastShownAt = mutableMapOf<String, Long>()

    private fun dedupKey(kind: Kind, noteId: String?, text: String): String =
        "${kind.name}|${noteId ?: "-"}|$text"

    /** Queue a save/annotation confirmation for [noteId], if not a repeat. */
    fun queueSave(noteId: String, isPdf: Boolean, nowMs: Long) {
        val text = if (isPdf) "Annotations saved" else "Note saved"
        queueDeduped(Kind.SAVE, noteId, text, nowMs)
    }

    /** Queue a one-off info message (import, backup, excerpt, ...). */
    fun queueInfo(text: String, nowMs: Long) {
        queueDeduped(Kind.INFO, null, text, nowMs)
    }

    private fun queueDeduped(kind: Kind, noteId: String?, text: String, nowMs: Long) {
        val key = dedupKey(kind, noteId, text)
        val last = lastShownAt[key]
        if (last != null && nowMs - last < DEDUP_WINDOW_MS) return
        if (queue.any { dedupKey(it.kind, it.noteId, it.text) == key }) return
        queue.add(Event(kind, noteId, text, nowMs))
    }

    /** Mark [text] as shown at [nowMs], so a repeat within the window drops. */
    fun markShown(kind: Kind, noteId: String?, text: String, nowMs: Long) {
        lastShownAt[dedupKey(kind, noteId, text)] = nowMs
    }

    /**
     * Drain the queue into the messages the menu should show, in order.
     * Save events collapse: 1 save shows its own text, 2+ show
     * "N notes saved". Draining clears the queue and stamps the 30s
     * window for every contributing event (so a note folded into a
     * combined message is still not repeated on its own within 30s).
     * When save confirmations are off, save events are dropped here.
     */
    fun drain(nowMs: Long, showSaveConfirmations: Boolean): List<Event> {
        if (queue.isEmpty()) return emptyList()
        val pending = queue.toList()
        queue.clear()
        val saves = pending.filter { it.kind == Kind.SAVE }
        val infos = pending.filter { it.kind != Kind.SAVE }
        val out = mutableListOf<Event>()
        if (saves.isNotEmpty() && showSaveConfirmations) {
            if (saves.size == 1) {
                out.add(saves.first())
            } else {
                out.add(
                    Event(
                        Kind.SAVE, null,
                        "${saves.size} notes saved", nowMs
                    )
                )
            }
            saves.forEach {
                lastShownAt[dedupKey(it.kind, it.noteId, it.text)] = nowMs
            }
        }
        infos.forEach {
            lastShownAt[dedupKey(it.kind, it.noteId, it.text)] = nowMs
        }
        out.addAll(infos)
        return out
    }

    fun pendingCount(): Int = queue.size

    /** Test/support hook: forget all queued and shown state. */
    fun resetForTest() {
        queue.clear()
        lastShownAt.clear()
    }
}
