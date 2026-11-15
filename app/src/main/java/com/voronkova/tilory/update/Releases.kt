package com.voronkova.tilory.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Versions like "0.10.2" or "v0.10.2", compared as numbers so "1.9" is older than "1.50". */
object Versions {
    fun parse(version: String): List<Int>? {
        val parts = version.trim().removePrefix("v").split('.')
        if (parts.isEmpty()) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    /** Negative when [a] is older than [b], zero when equal, positive when newer. Unparsable counts as oldest. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a) ?: return if (parse(b) == null) 0 else -1
        val pb = parse(b) ?: return 1
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}

@Serializable
data class GhAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val url: String,
)

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tag: String,
    val draft: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

data class AvailableUpdate(val version: String, val url: String, val size: Long)

/**
 * The newest release that is newer than [installed] and has an APK attached. Pre-releases count: all Tilory
 * releases are pre-releases for now.
 */
fun pickUpdate(releases: List<GhRelease>, installed: String): AvailableUpdate? =
    releases
        .asSequence()
        .filter { !it.draft && Versions.parse(it.tag) != null }
        .mapNotNull { r ->
            val apk = r.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@mapNotNull null
            AvailableUpdate(r.tag.removePrefix("v"), apk.url, apk.size)
        }
        .filter { Versions.compare(it.version, installed) > 0 }
        .maxWithOrNull { a, b -> Versions.compare(a.version, b.version) }
