package com.voronkova.tilory.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One photo of the Tilory collection: `assets/collection/<id>.jpg`, with a small copy in `thumbs/` (#57). */
@Serializable
data class CollectionItem(val id: String, val width: Int, val height: Int)

/**
 * The owner's photos built into the game (#57). They were prepared before going in: upright, at most 1600 px on the
 * long side, no EXIF at all. A picked photo is copied into the app's photo folder like any other, so History,
 * "Play again" and the puzzle itself work unchanged.
 */
class TiloryCollection(private val context: Context) {
    val items: List<CollectionItem> by lazy {
        runCatching {
            val text = context.assets.open("$DIR/index.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<List<CollectionItem>>(text)
        }.getOrDefault(emptyList())
    }

    /** The small copy for the grid. Blocking: call off the main thread. */
    fun thumbnail(item: CollectionItem): Bitmap? =
        runCatching { context.assets.open("$DIR/thumbs/${item.id}.jpg").use(BitmapFactory::decodeStream) }.getOrNull()

    fun assetPath(item: CollectionItem) = "$DIR/${item.id}.jpg"

    companion object {
        private const val DIR = "collection"
        private const val PREFIX = "col-"

        /** The stored file name of a collection photo; the prefix tells collection photos apart (#57). */
        fun fileName(item: CollectionItem) = "$PREFIX${item.id}.jpg"

        /** The collection id of a stored photo, or null for the owner's own photos. */
        fun idOf(photo: Photo): String? =
            photo.file.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.removeSuffix(".jpg")

        /** Unsolved items, or all of them once every one is solved. */
        fun candidates(items: List<CollectionItem>, solved: Set<String>): List<CollectionItem> =
            items.filter { it.id !in solved }.ifEmpty { items }
    }
}
