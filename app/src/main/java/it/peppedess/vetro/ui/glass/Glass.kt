package it.peppedess.vetro.ui.glass

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.peppedess.vetro.data.WallpaperImages
import kotlin.math.roundToInt

val LocalWallpaper = staticCompositionLocalOf<WallpaperImages?> { null }

object GlassTint {
    val Light = Color.White.copy(alpha = 0.16f)
    val Dock = Color.White.copy(alpha = 0.20f)
    val Dark = Color(0xFF1C1C1E).copy(alpha = 0.55f)
    val Menu = Color(0xFF2C2C2E).copy(alpha = 0.72f)
}

/**
 * Vetro smerigliato vero: disegna la porzione sfocata dello sfondo che sta
 * esattamente dietro l'elemento, poi una velatura e un bordo sottile.
 */
fun Modifier.glass(
    shape: Shape,
    tint: Color = GlassTint.Light,
    border: Boolean = true,
): Modifier = composed {
    val wallpaper = LocalWallpaper.current
    var position by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    val base = this
        .onGloballyPositioned { coords ->
            position = coords.positionInRoot()
            rootSize = coords.findRootCoordinates().size
        }
        .clip(shape)
        .drawBehind {
            val image = wallpaper?.blurred
            if (image != null && rootSize.width > 0) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(-position.x.roundToInt(), -position.y.roundToInt()),
                    dstSize = rootSize,
                    filterQuality = FilterQuality.Low,
                )
            }
            drawRect(tint)
        }
    if (border) base.border(0.5.dp, Color.White.copy(alpha = 0.22f), shape) else base
}
