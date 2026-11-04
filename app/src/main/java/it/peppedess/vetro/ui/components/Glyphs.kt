package it.peppedess.vetro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** Glifi disegnati a mano nello spirito degli SF Symbols (tratto arrotondato, forme semplici). */
enum class Glyph {
    NONE, INFO, MINUS, PLUS, TRASH, EDIT, PENCIL, DOCK, SEARCH, PHOTO,
    WIFI, BLUETOOTH, PLANE, CELL, FLASHLIGHT, TIMER, CALC, CAMERA,
    SUN, SPEAKER, PLAY, PREV, NEXT,
}

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) { drawGlyph(glyph, color) }
}

fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val sw = s * 0.085f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = sw) =
        drawLine(color, p(x1, y1), p(x2, y2), w, StrokeCap.Round)
    fun circle(cx: Float, cy: Float, r: Float, fill: Boolean = false) =
        if (fill) drawCircle(color, r * s, p(cx, cy)) else drawCircle(color, r * s, p(cx, cy), style = stroke)
    fun path(fill: Boolean, close: Boolean, vararg pts: Pair<Float, Float>) {
        val path = Path()
        pts.forEachIndexed { i, (x, y) ->
            val o = p(x, y)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        if (close) path.close()
        if (fill) drawPath(path, color) else drawPath(path, color, style = stroke)
    }
    fun rrect(l: Float, t: Float, r: Float, b: Float, radius: Float, fill: Boolean = false) {
        val tl = p(l, t)
        val sz = Size((r - l) * s, (b - t) * s)
        val cr = CornerRadius(radius * s)
        if (fill) drawRoundRect(color, tl, sz, cr) else drawRoundRect(color, tl, sz, cr, style = stroke)
    }
    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) =
        drawArc(color, start, sweep, false, p(cx - r, cy - r), Size(2 * r * s, 2 * r * s), style = stroke)

    when (glyph) {
        Glyph.NONE -> Unit
        Glyph.INFO -> {
            circle(0.5f, 0.5f, 0.42f)
            circle(0.5f, 0.3f, 0.055f, fill = true)
            line(0.5f, 0.45f, 0.5f, 0.72f)
        }
        Glyph.MINUS -> {
            circle(0.5f, 0.5f, 0.42f)
            line(0.3f, 0.5f, 0.7f, 0.5f)
        }
        Glyph.PLUS -> {
            circle(0.5f, 0.5f, 0.42f)
            line(0.3f, 0.5f, 0.7f, 0.5f)
            line(0.5f, 0.3f, 0.5f, 0.7f)
        }
        Glyph.TRASH -> {
            line(0.16f, 0.24f, 0.84f, 0.24f)
            path(false, false, 0.38f to 0.24f, 0.4f to 0.12f, 0.6f to 0.12f, 0.62f to 0.24f)
            path(false, false, 0.25f to 0.24f, 0.3f to 0.88f, 0.7f to 0.88f, 0.75f to 0.24f)
            line(0.43f, 0.4f, 0.44f, 0.74f)
            line(0.57f, 0.4f, 0.56f, 0.74f)
        }
        Glyph.EDIT -> {
            rrect(0.12f, 0.12f, 0.44f, 0.44f, 0.09f)
            rrect(0.56f, 0.12f, 0.88f, 0.44f, 0.09f)
            rrect(0.12f, 0.56f, 0.44f, 0.88f, 0.09f)
            rrect(0.56f, 0.56f, 0.88f, 0.88f, 0.09f)
        }
        Glyph.PENCIL -> {
            path(false, true, 0.2f to 0.8f, 0.24f to 0.62f, 0.68f to 0.18f, 0.82f to 0.32f, 0.38f to 0.76f)
            line(0.6f, 0.26f, 0.74f, 0.4f)
        }
        Glyph.DOCK -> {
            rrect(0.1f, 0.62f, 0.9f, 0.88f, 0.08f)
            rrect(0.36f, 0.14f, 0.64f, 0.42f, 0.07f)
        }
        Glyph.SEARCH -> {
            circle(0.43f, 0.43f, 0.28f)
            line(0.64f, 0.64f, 0.86f, 0.86f, sw * 1.2f)
        }
        Glyph.PHOTO -> {
            rrect(0.1f, 0.18f, 0.9f, 0.82f, 0.1f)
            path(false, false, 0.14f to 0.74f, 0.4f to 0.48f, 0.58f to 0.64f, 0.7f to 0.54f, 0.86f to 0.7f)
            circle(0.68f, 0.36f, 0.06f, fill = true)
        }
        Glyph.WIFI -> {
            circle(0.5f, 0.8f, 0.07f, fill = true)
            arc(0.5f, 0.8f, 0.22f, 225f, 90f)
            arc(0.5f, 0.8f, 0.42f, 225f, 90f)
            arc(0.5f, 0.8f, 0.62f, 228f, 84f)
        }
        Glyph.BLUETOOTH -> {
            path(false, false, 0.28f to 0.32f, 0.72f to 0.68f, 0.5f to 0.88f, 0.5f to 0.12f, 0.72f to 0.32f, 0.28f to 0.68f)
        }
        Glyph.PLANE -> rotate(45f, pivot = p(0.5f, 0.5f)) {
            path(
                true, true,
                0.47f to 0.08f, 0.53f to 0.08f, 0.56f to 0.4f, 0.92f to 0.6f, 0.92f to 0.68f,
                0.56f to 0.58f, 0.55f to 0.8f, 0.66f to 0.88f, 0.66f to 0.93f, 0.5f to 0.88f,
                0.34f to 0.93f, 0.34f to 0.88f, 0.45f to 0.8f, 0.44f to 0.58f, 0.08f to 0.68f,
                0.08f to 0.6f, 0.44f to 0.4f,
            )
        }
        Glyph.CELL -> {
            rrect(0.1f, 0.62f, 0.24f, 0.86f, 0.03f, fill = true)
            rrect(0.31f, 0.48f, 0.45f, 0.86f, 0.03f, fill = true)
            rrect(0.52f, 0.32f, 0.66f, 0.86f, 0.03f, fill = true)
            rrect(0.73f, 0.14f, 0.87f, 0.86f, 0.03f, fill = true)
        }
        Glyph.FLASHLIGHT -> {
            path(false, true, 0.3f to 0.1f, 0.7f to 0.1f, 0.7f to 0.22f, 0.62f to 0.42f, 0.38f to 0.42f, 0.3f to 0.22f)
            rrect(0.38f, 0.42f, 0.62f, 0.9f, 0.05f)
            circle(0.5f, 0.6f, 0.04f, fill = true)
        }
        Glyph.TIMER -> {
            circle(0.5f, 0.56f, 0.36f)
            line(0.5f, 0.56f, 0.5f, 0.36f)
            line(0.4f, 0.08f, 0.6f, 0.08f)
            line(0.82f, 0.22f, 0.88f, 0.28f)
        }
        Glyph.CALC -> {
            rrect(0.2f, 0.08f, 0.8f, 0.92f, 0.1f)
            rrect(0.3f, 0.18f, 0.7f, 0.34f, 0.03f)
            for (r in 0 until 3) for (c in 0 until 3) {
                circle(0.34f + c * 0.16f, 0.5f + r * 0.14f, 0.035f, fill = true)
            }
        }
        Glyph.CAMERA -> {
            rrect(0.08f, 0.3f, 0.92f, 0.84f, 0.1f)
            path(false, false, 0.34f to 0.3f, 0.4f to 0.18f, 0.6f to 0.18f, 0.66f to 0.3f)
            circle(0.5f, 0.57f, 0.16f)
        }
        Glyph.SUN -> {
            circle(0.5f, 0.5f, 0.17f, fill = true)
            for (i in 0 until 8) {
                rotate(i * 45f, pivot = p(0.5f, 0.5f)) { line(0.5f, 0.08f, 0.5f, 0.18f) }
            }
        }
        Glyph.SPEAKER -> {
            path(true, true, 0.1f to 0.38f, 0.28f to 0.38f, 0.5f to 0.18f, 0.5f to 0.82f, 0.28f to 0.62f, 0.1f to 0.62f)
            arc(0.5f, 0.5f, 0.18f, -45f, 90f)
            arc(0.5f, 0.5f, 0.34f, -50f, 100f)
        }
        Glyph.PLAY -> path(true, true, 0.26f to 0.14f, 0.84f to 0.5f, 0.26f to 0.86f)
        Glyph.PREV -> {
            path(true, true, 0.5f to 0.2f, 0.5f to 0.8f, 0.08f to 0.5f)
            path(true, true, 0.92f to 0.2f, 0.92f to 0.8f, 0.5f to 0.5f)
        }
        Glyph.NEXT -> {
            path(true, true, 0.08f to 0.2f, 0.08f to 0.8f, 0.5f to 0.5f)
            path(true, true, 0.5f to 0.2f, 0.5f to 0.8f, 0.92f to 0.5f)
        }
    }
}
