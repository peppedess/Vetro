package it.peppedess.vetro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.FolderItem
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.VetroFont

val IconShape = RoundedCornerShape(percent = 23)

/** Numero di notifiche per pacchetto, per i badge rossi. */
val LocalBadges = staticCompositionLocalOf<Map<String, Int>> { emptyMap() }

@Composable
fun AppIcon(
    app: AppInfo,
    actions: List<IconAction>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp,
    showLabel: Boolean = true,
    editing: Boolean = false,
    emphasized: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    IconTile(
        label = app.label,
        seed = app.key.hashCode(),
        actions = actions,
        onClick = onClick,
        modifier = modifier,
        iconSize = iconSize,
        showLabel = showLabel,
        editing = editing,
        emphasized = emphasized,
        clickableInEdit = false,
        badge = LocalBadges.current[app.packageName] ?: 0,
        onRemove = onRemove,
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun FolderIcon(
    folder: FolderItem,
    actions: List<IconAction>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp,
    editing: Boolean = false,
    emphasized: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    IconTile(
        label = folder.name,
        seed = folder.id.hashCode(),
        actions = actions,
        onClick = onClick,
        modifier = modifier,
        iconSize = iconSize,
        showLabel = true,
        editing = editing,
        emphasized = emphasized,
        clickableInEdit = true,
        badge = LocalBadges.current.let { badges -> folder.apps.distinctBy { it.packageName }.sumOf { badges[it.packageName] ?: 0 } },
        onRemove = onRemove,
    ) {
        FolderPreview(folder, iconSize)
    }
}

@Composable
private fun FolderPreview(folder: FolderItem, iconSize: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glass(IconShape, Color.White.copy(alpha = 0.22f), border = false)
            .padding(iconSize * 0.13f),
        verticalArrangement = Arrangement.spacedBy(iconSize * 0.05f),
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(iconSize * 0.05f),
            ) {
                repeat(3) { col ->
                    val app = folder.apps.getOrNull(row * 3 + col)
                    val mini = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    if (app != null) {
                        Image(bitmap = app.icon, contentDescription = null, modifier = mini.clip(IconShape))
                    } else {
                        Spacer(mini)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(
    label: String,
    seed: Int,
    actions: List<IconAction>,
    onClick: () -> Unit,
    modifier: Modifier,
    iconSize: Dp,
    showLabel: Boolean,
    editing: Boolean,
    emphasized: Boolean,
    clickableInEdit: Boolean,
    badge: Int,
    onRemove: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val menu = LocalMenuController.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bounds = remember { mutableStateOf(Rect.Zero) }

    val scale = animateFloatAsState(
        targetValue = when {
            emphasized -> 1.18f
            pressed -> 0.9f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "scale",
    )

    val wiggle: State<Float>? = if (editing) {
        rememberInfiniteTransition(label = "wiggle").animateFloat(
            initialValue = -1.6f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 120 + (seed and 31),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "angle",
        )
    } else {
        null
    }

    Column(
        modifier = modifier.width(iconSize + 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
                rotationZ = wiggle?.value ?: 0f
                alpha = if (menu.request?.bounds == bounds.value) 0f else 1f
            },
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .onGloballyPositioned { bounds.value = it.boundsInRoot() }
                    .clip(IconShape)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {
                            if (!editing || clickableInEdit) {
                                LaunchSource.bounds = bounds.value
                                onClick()
                            }
                        },
                        onLongClick = {
                            if (!editing && actions.isNotEmpty()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menu.show(MenuRequest(bounds.value, actions, content))
                            }
                        },
                    ),
            ) {
                content()
            }

            if (badge > 0 && !editing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .height(22.dp)
                        .widthIn(min = 22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF3B30))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        color = Color.White,
                        style = TextStyle(fontFamily = VetroFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    )
                }
            }

            if (editing && onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-7).dp, y = (-7).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .glass(CircleShape, Color(0xFF8E8E93).copy(alpha = 0.55f), border = false)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 10.dp, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White),
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = label,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = VetroFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 3f),
                ),
            )
        }
    }
}
