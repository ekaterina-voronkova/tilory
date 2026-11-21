package com.voronkova.tilory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TiloryCollectionTest {
    private val a = CollectionItem("c001", 1600, 1200)
    private val b = CollectionItem("c002", 1200, 1600)

    @Test
    fun storedFileNameRoundTrips() {
        val photo = Photo(TiloryCollection.fileName(a), a.width, a.height)
        assertEquals("c001", TiloryCollection.idOf(photo))
    }

    @Test
    fun ownPhotosHaveNoCollectionId() {
        assertNull(TiloryCollection.idOf(Photo("0a1b2c3d4e5f.jpg", 100, 100)))
    }

    @Test
    fun randomSkipsSolvedUntilAllAreSolved() {
        assertEquals(listOf(b), TiloryCollection.candidates(listOf(a, b), setOf("c001")))
        assertEquals(listOf(a, b), TiloryCollection.candidates(listOf(a, b), setOf("c001", "c002")))
    }
}
