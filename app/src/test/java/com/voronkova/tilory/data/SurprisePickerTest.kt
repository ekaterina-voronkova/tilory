package com.voronkova.tilory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurprisePickerTest {
    private val tree = "tree"
    private fun images(vararg ids: String) = ids.map { FolderImage(it, tree) }
    private val photo = Photo("h.jpg", 10, 10)

    @Test
    fun leavesOutSolvedAndHidden() {
        val memory = SurpriseMemory(solvedDocuments = setOf("a"), hidden = listOf(HiddenPhoto(photo, "b")))
        val result = SurprisePicker.candidates(images("a", "b", "c", "d"), memory, justSkipped = null)
        assertEquals(setOf("c", "d"), result.map { it.document }.toSet())
    }

    @Test
    fun justSkippedComesBackOnlyWhenNothingElseIsLeft() {
        val memory = SurpriseMemory()
        assertEquals(listOf("b"), SurprisePicker.candidates(images("a", "b"), memory, "a").map { it.document })
        assertEquals(listOf("a"), SurprisePicker.candidates(images("a"), memory, "a").map { it.document })
    }

    @Test
    fun sameDocumentFromTwoFoldersCountsOnce() {
        val all = listOf(FolderImage("a", "t1"), FolderImage("a", "t2"))
        assertEquals(1, SurprisePicker.candidates(all, SurpriseMemory(), null).size)
    }

    @Test
    fun emptyWhenEverythingIsUsed() {
        val memory = SurpriseMemory(solvedDocuments = setOf("a", "b"))
        assertTrue(SurprisePicker.candidates(images("a", "b"), memory, null).isEmpty())
    }
}
