package com.hexis.bi.ui.avatar

import java.io.BufferedReader
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Disk-backed cache for downloaded OBJ files. Returns a reader for [url], downloading it (atomically,
 * via a temp file) on a cache miss and trimming the on-disk cache to its size/count budget. Pure file
 * I/O — no mesh parsing (see [ObjParser]).
 */
internal object ObjDiskCache {
    private const val OBJ_URL_CONNECT_TIMEOUT_MS = 10_000
    private const val OBJ_URL_READ_TIMEOUT_MS = 15_000

    private const val OBJ_DISK_CACHE_DIR = "metric_avatar_obj"
    private const val OBJ_DISK_CACHE_MAX_FILES = 16
    private const val OBJ_DISK_CACHE_MAX_BYTES = 64L * 1024L * 1024L

    fun objFileReader(url: String, cacheDir: File): BufferedReader {
        val dir = File(cacheDir, OBJ_DISK_CACHE_DIR).apply { mkdirs() }
        val cacheFile = File(dir, cacheFileName(url))
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            downloadObj(url, dir, cacheFile)
        }
        cacheFile.setLastModified(System.currentTimeMillis())
        val reader = cacheFile.bufferedReader()
        trimDiskCache(dir, cacheFile)
        return reader
    }

    @Synchronized
    private fun trimDiskCache(dir: File, activeFile: File) {
        val cachedFiles = dir.listFiles { file ->
            file.isFile && file.extension == "obj"
        }.orEmpty().sortedWith(
            compareByDescending<File> { it == activeFile }
                .thenByDescending { it.lastModified() },
        )
        var retainedFiles = 0
        var retainedBytes = 0L

        cachedFiles.forEach { file ->
            val fileSize = file.length()
            val retain = file == activeFile ||
                    (retainedFiles < OBJ_DISK_CACHE_MAX_FILES &&
                            retainedBytes + fileSize <= OBJ_DISK_CACHE_MAX_BYTES)
            if (retain) {
                retainedFiles += 1
                retainedBytes += fileSize
            } else {
                file.delete()
            }
        }
    }

    private fun downloadObj(url: String, dir: File, dest: File) {
        val connection = URL(url).openConnection().apply {
            connectTimeout = OBJ_URL_CONNECT_TIMEOUT_MS
            readTimeout = OBJ_URL_READ_TIMEOUT_MS
        }
        // Do not expose partial downloads to concurrent readers.
        val tmp = File.createTempFile("obj", ".tmp", dir)
        try {
            connection.getInputStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
            }
        } finally {
            tmp.delete()
        }
    }

    private fun cacheFileName(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".obj"
    }
}
