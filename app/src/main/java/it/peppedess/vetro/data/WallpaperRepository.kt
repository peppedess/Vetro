package it.peppedess.vetro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
data class WallpaperImages(
    val sharp: ImageBitmap,
    val blurred: ImageBitmap,
)

class WallpaperRepository(private val context: Context) {

    private val file = File(context.filesDir, "wallpaper.jpg")

    fun load(width: Int, height: Int): WallpaperImages {
        val custom = if (file.exists()) BitmapFactory.decodeFile(file.path)?.let { centerCrop(it, width, height) } else null
        val sharp = custom ?: generateDefault(width, height)
        val small = Bitmap.createScaledBitmap(sharp, max(1, width / 10), max(1, height / 10), true)
        val blurred = boxBlur(small, radius = 5)
        return WallpaperImages(sharp.asImageBitmap(), blurred.asImageBitmap())
    }

    fun save(uri: Uri, width: Int, height: Int): Boolean {
        val bitmap = decodeSampled(uri, width, height) ?: return false
        val cropped = centerCrop(bitmap, width, height)
        FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return true
    }

    fun reset() {
        file.delete()
    }

    private fun decodeSampled(uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqW && bounds.outHeight / (sample * 2) >= reqH) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun centerCrop(src: Bitmap, width: Int, height: Int): Bitmap {
        val scale = max(width / src.width.toFloat(), height / src.height.toFloat())
        val cropW = (width / scale).roundToInt().coerceAtMost(src.width)
        val cropH = (height / scale).roundToInt().coerceAtMost(src.height)
        val left = (src.width - cropW) / 2
        val top = (src.height - cropH) / 2
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            src,
            Rect(left, top, left + cropW, top + cropH),
            Rect(0, 0, width, height),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return out
    }

    /** Sfondo originale: gradiente profondo con macchie di colore morbide. */
    private fun generateDefault(width: Int, height: Int): Bitmap {
        val w = width.toFloat()
        val h = height.toFloat()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, w * 0.3f, h,
            intArrayOf(0xFF0A1440.toInt(), 0xFF1B2A7A.toInt(), 0xFF3B1E6E.toInt(), 0xFF140B2E.toInt()),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        fun blob(cx: Float, cy: Float, r: Float, color: Int) {
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(color, color and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, paint)
        }
        blob(w * 0.15f, h * 0.18f, w * 0.9f, 0xAA2F6BFF.toInt())
        blob(w * 0.95f, h * 0.42f, w * 0.85f, 0x99B04CFF.toInt())
        blob(w * 0.25f, h * 0.78f, w * 0.95f, 0x88FF5E8A.toInt())
        blob(w * 0.85f, h * 0.95f, w * 0.7f, 0x77FF9F45.toInt())
        blob(w * 0.55f, h * 0.05f, w * 0.6f, 0x6638D3FF)

        val small = Bitmap.createScaledBitmap(bmp, max(1, width / 6), max(1, height / 6), true)
        val soft = boxBlur(small, radius = 8)
        return Bitmap.createScaledBitmap(soft, width, height, true)
    }
}

private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    repeat(3) {
        blurPass(pixels, w, h, radius, horizontal = true)
        blurPass(pixels, w, h, radius, horizontal = false)
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

private fun blurPass(p: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val length = if (horizontal) w else h
    val lines = if (horizontal) h else w
    val out = IntArray(length)
    val div = 2 * r + 1
    for (line in 0 until lines) {
        fun at(i: Int): Int {
            val c = i.coerceIn(0, length - 1)
            return if (horizontal) p[line * w + c] else p[c * w + line]
        }
        var a = 0
        var red = 0
        var green = 0
        var blue = 0
        for (i in -r..r) {
            val c = at(i)
            a += c ushr 24
            red += (c shr 16) and 0xFF
            green += (c shr 8) and 0xFF
            blue += c and 0xFF
        }
        for (i in 0 until length) {
            out[i] = ((a / div) shl 24) or ((red / div) shl 16) or ((green / div) shl 8) or (blue / div)
            val cOut = at(i - r)
            val cIn = at(i + r + 1)
            a += (cIn ushr 24) - (cOut ushr 24)
            red += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF)
            green += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF)
            blue += (cIn and 0xFF) - (cOut and 0xFF)
        }
        for (i in 0 until length) {
            if (horizontal) p[line * w + i] = out[i] else p[i * w + line] = out[i]
        }
    }
}
