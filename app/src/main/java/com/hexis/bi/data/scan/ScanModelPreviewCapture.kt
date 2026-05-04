package com.hexis.bi.data.scan

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.PixelCopy
import android.view.SurfaceView
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Grabs a **low-resolution** PNG of the 3D preview (for scan history), from an on-screen
 * [SurfaceView] after the GL mesh has been drawn.
 */
object ScanModelPreviewCapture {

    /** Longer edge of the output image — keeps Firestore payload small. */
    private const val MAX_EDGE_PX = 160

    suspend fun captureToPngBase64(surfaceView: SurfaceView): String? {
        val w = surfaceView.width
        val h = surfaceView.height
        if (w <= 0 || h <= 0) {
            Timber.w("ScanModelPreviewCapture: invalid surface size %dx%d", w, h)
            return null
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val success = suspendCancellableCoroutine { cont ->
            PixelCopy.request(
                surfaceView,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) cont.resume(true)
                    else {
                        Timber.w("ScanModelPreviewCapture: PixelCopy failed code=%s", copyResult)
                        bitmap.recycle()
                        cont.resume(false)
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
        if (!success) return null
        val scaled = scaleDown(bitmap, MAX_EDGE_PX)
        if (scaled != bitmap) bitmap.recycle()
        return try {
            ByteArrayOutputStream().use { os ->
                scaled.compress(Bitmap.CompressFormat.PNG, 92, os)
                Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
            }
        } finally {
            scaled.recycle()
        }
    }

    private fun scaleDown(src: Bitmap, maxEdge: Int): Bitmap {
        val iw = src.width
        val ih = src.height
        val scale = minOf(maxEdge.toFloat() / iw, maxEdge.toFloat() / ih, 1f)
        if (scale >= 1f) return src
        val nw = max(1, (iw * scale).roundToInt())
        val nh = max(1, (ih * scale).roundToInt())
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
