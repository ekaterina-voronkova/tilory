package com.voronkova.tilory.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTakenTest {
    @Test
    fun exifDateIsFormatted() {
        val text = formatTaken("2019:07:12 14:32:05")
        assertNotNull(text)
        assertTrue(text!!, text.contains("2019"))
    }

    @Test
    fun missingOrBrokenDateGivesNothing() {
        assertNull(formatTaken(null))
        assertNull(formatTaken("2019-07-12"))
        assertNull(formatTaken("2019:13:40 25:61:00"))
    }
}
