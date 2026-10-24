package com.riguel.notestar

import com.riguel.notestar.data.NotificationCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationCenterTest {

    @Before
    fun setUp() {
        NotificationCenter.resetForTest()
    }

    @Test
    fun singleSaveShowsItsOwnTextOnMenu() {
        NotificationCenter.queueSave("n1", false, 1000L)
        val shown = NotificationCenter.drain(2000L, true)
        assertEquals(1, shown.size)
        assertEquals("Note saved", shown[0].text)
    }

    @Test
    fun repeatedSaveWithinThirtySecondsIsDropped() {
        NotificationCenter.queueSave("n1", false, 1000L)
        NotificationCenter.drain(2000L, true)
        // Same note saved again 10s later: no second message.
        NotificationCenter.queueSave("n1", false, 12_000L)
        assertEquals(0, NotificationCenter.drain(13_000L, true).size)
        // After the window passes, it may show again.
        NotificationCenter.queueSave("n1", false, 40_000L)
        assertEquals(1, NotificationCenter.drain(41_000L, true).size)
    }

    @Test
    fun severalPendingSavesCombineIntoOne() {
        NotificationCenter.queueSave("n1", false, 1000L)
        NotificationCenter.queueSave("n2", true, 1100L)
        NotificationCenter.queueSave("n3", false, 1200L)
        val shown = NotificationCenter.drain(2000L, true)
        assertEquals(1, shown.size)
        assertEquals("3 notes saved", shown[0].text)
    }

    @Test
    fun combinedSaveStillDedupesEachNote() {
        NotificationCenter.queueSave("n1", false, 1000L)
        NotificationCenter.queueSave("n2", false, 1100L)
        NotificationCenter.drain(2000L, true)
        NotificationCenter.queueSave("n1", false, 5000L)
        assertEquals(0, NotificationCenter.drain(6000L, true).size)
    }

    @Test
    fun saveConfirmationsOffDropsSavesButKeepsInfo() {
        NotificationCenter.queueSave("n1", false, 1000L)
        NotificationCenter.queueInfo("Imported \"Doc\" — tap it to annotate", 1100L)
        val shown = NotificationCenter.drain(2000L, false)
        assertEquals(1, shown.size)
        assertTrue(shown[0].text.startsWith("Imported"))
    }

    @Test
    fun queueingTwiceBeforeDrainShowsOnce() {
        // Back + dispose double-save: the second identical queue is a no-op.
        NotificationCenter.queueSave("n1", false, 1000L)
        NotificationCenter.queueSave("n1", false, 1001L)
        assertEquals(1, NotificationCenter.pendingCount())
        val shown = NotificationCenter.drain(2000L, true)
        assertEquals(1, shown.size)
    }

    @Test
    fun emptyQueueDrainsToNothing() {
        assertTrue(NotificationCenter.drain(1000L, true).isEmpty())
    }
}
