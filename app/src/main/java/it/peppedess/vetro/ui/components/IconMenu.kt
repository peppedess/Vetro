package it.peppedess.vetro.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.ui.glass.GlassTint
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont
import kotlin.math.roundToInt

data class IconAction(
    val label: String,
    val glyph: Glyph = Glyph.NONE,
    val destructive: Boolean = false,
    val run: () -> Unit,
)

@Immutable
data class MenuRequest(
    val bounds: Rect,
    val actions: List<IconAction>,
    val icon: @Composable () -> Unit,
)

class MenuController {
    var request by mutableStateOf<MenuRequest?>(null)
        private set

    fun show(request: MenuRequest) {
        this.request = request
    }

    fun dismiss() {
        request = null
    }
}

val LocalMenuController = staticCompositionLocalOf { MenuController() }

/** Ultima icona toccata: serve per l'animazione di apertura che parte dall'icona. */
object LaunchSource {
    var bounds: Rect? = null
}

@Composable
fun IconMenuOverlay(request: MenuRequest, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(request) {
        appear.snapTo(0f)
        appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
    }
    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        val b = request.bounds
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val menuW = 250.dp
        val itemH = 46.dp
        val menuWpx = with(density) { menuW.toPx() }
        val menuHpx = with(density) { (itemH * request.actions.size).toPx() }
        val gap = with(density) { 14.dp.toPx() }
        val margin = with(density) { 14.dp.toPx() }
        val below = b.bottom + gap + menuHpx < screenH - margin * 4
        val menuTop = if (below) b.bottom + gap else b.top - gap - menuHpx
        val menuLeft = (if (b.center.x < screenW / 2) b.left else b.right - menuWpx)
            .coerceIn(margin, screenW - menuWpx - margin)
        val originX = ((b.center.x - menuLeft) / menuWpx).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .offset { IntOffset(b.left.roundToInt(), b.top.roundToInt()) }
                .size(with(density) { b.width.toDp() }, with(density) { b.height.toDp() })
                .graphicsLayer {
                    val s = 1f + 0.08f * appear.value
                    scaleX = s
                    scaleY = s
                }
                .clip(IconShape),
        ) {
            request.icon()
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(menuLeft.roundToInt(), menuTop.roundToInt()) }
                .width(menuW)
                .graphicsLayer {
                    val v = appear.value
                    scaleX = 0.5f + 0.5f * v
                    scaleY = 0.5f + 0.5f * v
                    alpha = v.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(originX, if (below) 0f else 1f)
                }
                .glass(RoundedCornerShape(14.dp), GlassTint.Menu, border = false)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            request.actions.forEachIndexed { index, action ->
                if (index > 0) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(IosColors.Separator),
                    )
                }
                val color = if (action.destructive) IosColors.Red else Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemH)
                        .clickable {
                            onDismiss()
                            action.run()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = action.label,
                        color = color,
                        fontFamily = VetroFont,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    GlyphIcon(action.glyph, color, Modifier.size(19.dp))
                }
            }
        }
    }
}
