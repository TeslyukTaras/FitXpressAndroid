package com.hexis.bi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object ImageCompressor {

    fun readAsJpeg(
        context: Context,
        uri: Uri,
        targetBytes: Long,
        startQuality: Int,
        minQuality: Int,
        qualityStep: Int,
        hardCapBytes: Long = targetBytes,
        maxDimensionPx: Int? = null,
        skipIfAtMostBytes: Long = 0L,
    ): ByteArray {
        val original = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error("Unable to open photo: $uri")
        if (skipIfAtMostBytes > 0L && original.size <= skipIfAtMostBytes) return original

        var bitmap = decodeOriented(context, uri)
        maxDimensionPx?.let { max ->
            val scaled = bitmap.scaleToFit(max)
            if (scaled !== bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }
        try {
            var quality = startQuality
            var bytes = bitmap.compressJpeg(quality)
            while (bytes.size > targetBytes && quality > minQuality) {
                quality -= qualityStep
                bytes = bitmap.compressJpeg(quality)
            }
            while (bytes.size > hardCapBytes && bitmap.width > 1 && bitmap.height > 1) {
                val halved = bitmap.scaleToFit(maxOf(bitmap.width, bitmap.height) / 2)
                if (halved === bitmap) break
                bitmap.recycle()
                bitmap = halved
                bytes = bitmap.compressJpeg(minQuality)
            }
            return bytes
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeOriented(context: Context, uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri),
        ) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }

    private fun Bitmap.scaleToFit(maxDimension: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxDimension) return this

        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    }

    private fun Bitmap.compressJpeg(quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }
}
