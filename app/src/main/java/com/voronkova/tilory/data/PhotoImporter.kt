package com.voronkova.tilory.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Copies a picked or shared photo into the app (design §9): downscaled so the longest side is at most [MAX_SIDE],
 * EXIF orientation applied (ImageDecoder does this), HEIC supported (ImageDecoder, API 28+), saved as JPEG under a
 * content hash so the same photo is stored once.
 */
class PhotoImporter(private val context: Context) {
    private val dir = File(context.filesDir, "photos").apply { mkdirs() }

    fun import(uri: Uri): Photo {
        val resolver = context.contentResolver
        val hash = resolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
            digest.digest().take(12).joinToString("") { "%02x".format(it) }
        } ?: error("Cannot open $uri")

        val name = "$hash.jpg"
        val out = File(dir, name)
        if (!out.exists()) {
            val source = ImageDecoder.createSource(resolver, uri)
            var bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                var sample = 1
                while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
                decoder.setTargetSampleSize(sample)
            }
            val longest = max(bitmap.width, bitmap.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                    (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                    true,
                )
            }
            val tmp = File(dir, "$name.tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            tmp.renameTo(out)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.path, bounds)
        return Photo(name, bounds.outWidth, bounds.outHeight, taken(uri))
    }

    /**
     * The shooting date from the original's EXIF; the stored copy has no EXIF (#43). The file date is not used: it is
     * often the date of a copy or download.
     */
    private fun taken(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            listOf(ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED)
                .firstNotNullOfOrNull { tag -> exif.getAttribute(tag)?.trim()?.takeIf(::isDate) }
        }
    }.getOrNull()

    private fun isDate(value: String) = TAKEN_FORMAT.matches(value) && !value.startsWith("0000")

    fun file(photo: Photo) = File(dir, photo.file)

    fun load(photo: Photo): Bitmap? = BitmapFactory.decodeFile(file(photo).path)

    /** A smaller copy for lists: the longest side is about [targetSide]. */
    fun loadThumbnail(photo: Photo, targetSide: Int = 480): Bitmap? {
        var sample = 1
        while (max(photo.width, photo.height) / (sample * 2) >= targetSide) sample *= 2
        return BitmapFactory.decodeFile(file(photo).path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Deletes stored photos that nothing refers to any more. */
    fun cleanUp(keep: Set<String>) {
        dir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    companion object {
        const val MAX_SIDE = 2048
        private val TAKEN_FORMAT = Regex("""\d{4}:\d{2}:\d{2} \d{2}:\d{2}:\d{2}""")
    }
}
