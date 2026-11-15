package com.voronkova.tilory.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps [AppData] in one JSON file in the app's private storage. Writes go to a temporary file first and then
 * replace the old one, so a killed app never leaves a half-written state.
 */
class Store(context: Context) {
    private val file = File(context.filesDir, "state.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppData =
        try {
            if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
        } catch (e: Exception) {
            AppData()
        }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, "state.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
