package com.voronkova.tilory.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasesTest {
    @Test
    fun versionsCompareAsNumbers() {
        assertTrue(Versions.compare("1.50", "1.9") > 0)
        assertTrue(Versions.compare("v0.3.0", "0.2.0") > 0)
        assertEquals(0, Versions.compare("0.3", "0.3.0"))
        assertTrue(Versions.compare("0.2.10", "0.2.9") > 0)
        assertTrue(Versions.compare("garbage", "0.1.0") < 0)
    }

    private fun release(tag: String, apk: Boolean = true, draft: Boolean = false) = GhRelease(
        tag,
        draft,
        if (apk) listOf(GhAsset("tilory-$tag.apk", 25_000_000, "https://example.invalid/$tag.apk")) else emptyList(),
    )

    @Test
    fun picksNewestNewerReleaseWithApk() {
        val releases = listOf(release("v0.2.0"), release("v0.4.0", apk = false), release("v0.3.1"), release("v0.3.0"))
        val update = pickUpdate(releases, "0.2.0")
        assertEquals("0.3.1", update?.version)
        assertEquals("https://example.invalid/v0.3.1.apk", update?.url)
    }

    @Test
    fun ignoresDraftsAndOlderVersions() {
        assertNull(pickUpdate(listOf(release("v0.3.0", draft = true), release("v0.2.0")), "0.2.0"))
    }
}
