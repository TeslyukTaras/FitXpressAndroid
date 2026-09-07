package com.hexis.bi.data.store

import android.content.Context
import java.io.File

object PendingFirestoreCacheWipe {

    private const val MARKER_FILE_NAME = "pending_firestore_cache_wipe"

    fun arm(context: Context) {
        runCatching { markerFile(context).createNewFile() }
    }

    fun isArmed(context: Context): Boolean = markerFile(context).exists()

    fun disarm(context: Context) {
        runCatching { markerFile(context).delete() }
    }

    private fun markerFile(context: Context): File = File(context.filesDir, MARKER_FILE_NAME)
}
