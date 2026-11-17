package com.voronkova.tilory.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** An image found in a Surprise folder. */
data class FolderImage(val document: String, val tree: String)

/**
 * Reads the folders picked for Surprise through the Storage Access Framework (design §9 stage 1, #4). The folders
 * are picked with ACTION_OPEN_DOCUMENT_TREE and the read permission is persisted, so no media permission is needed.
 */
class Folders(private val context: Context) {
    private val resolver get() = context.contentResolver

    /** Keeps read access to a picked folder across restarts and returns it as a source. */
    fun add(tree: Uri): SurpriseSource {
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return SurpriseSource(tree.toString(), name(tree) ?: tree.lastPathSegment.orEmpty())
    }

    fun release(source: SurpriseSource) {
        runCatching {
            resolver.releasePersistableUriPermission(Uri.parse(source.tree), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun name(tree: Uri): String? {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return runCatching {
            resolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()
    }

    /** All images in [source] and its subfolders; hidden folders (".thumbnails" and the like) are skipped. */
    fun images(source: SurpriseSource): List<FolderImage> {
        val tree = Uri.parse(source.tree)
        val out = mutableListOf<FolderImage>()
        val pending = ArrayDeque<String>()
        runCatching { pending.add(DocumentsContract.getTreeDocumentId(tree)) }
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        while (pending.isNotEmpty()) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, pending.removeFirst())
            runCatching {
                resolver.query(children, columns, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1).orEmpty()
                        val mime = c.getString(2).orEmpty()
                        if (name.startsWith(".")) continue
                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR -> pending.add(id)
                            mime.startsWith("image/") -> out.add(
                                FolderImage(DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(), source.tree),
                            )
                        }
                    }
                }
            }
        }
        return out
    }
}

/** Plain choice logic for Surprise, kept free of Android so it can be unit-tested. */
object SurprisePicker {
    /**
     * The images that may come next: not solved, not hidden; the photo just skipped only if nothing else is left.
     * Returned in the order to try them: each step picks a random folder among those with images left, then a random
     * image in it, so every folder is equally likely whatever its size (#42).
     */
    fun candidates(
        images: List<FolderImage>,
        memory: SurpriseMemory,
        justSkipped: String?,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): List<FolderImage> {
        val hidden = memory.hidden.map { it.document }.toSet()
        val open = images.distinctBy { it.document }
            .filter { it.document !in memory.solvedDocuments && it.document !in hidden }
        val fresh = open.filter { it.document != justSkipped }
        val folders = fresh.ifEmpty { open }
            .groupBy { it.tree }
            .values
            .map { it.shuffled(random).toMutableList() }
            .toMutableList()
        val order = mutableListOf<FolderImage>()
        while (folders.isNotEmpty()) {
            val i = random.nextInt(folders.size)
            order += folders[i].removeAt(folders[i].lastIndex)
            if (folders[i].isEmpty()) folders.removeAt(i)
        }
        return order
    }
}
