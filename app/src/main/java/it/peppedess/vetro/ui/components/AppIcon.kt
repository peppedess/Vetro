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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.FolderItem

val IconShape = RoundedCornerShape(percent = 23)

data class IconAction(
    val label: String,
    val destructive: Boolean = false,
    val run: () -> Unit,
)

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
        onRemove = onRemove,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.28f))
                .padding(iconSize * 0.12f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(3) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
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
    onRemove: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = when {
            emphasized -> 1.18f
            pressed || menuOpen -> 0.9f
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
            initialValue = -1.8f,
            targetValue = 1.8f,
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
        modifier = modifier.width(iconSize + 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
                rotationZ = wiggle?.value ?: 0f
            },
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(IconShape)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { if (!editing || clickableInEdit) onClick() },
                        onLongClick = {
                            if (!editing && actions.isNotEmpty()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuOpen = true
                            }
                        },
                    ),
            ) {
                content()
            }

            if (editing && onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-7).dp, y = (-7).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xF2D1D1D6))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 10.dp, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.Black),
                    )
                }
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(14.dp),
            ) {
                actions.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = action.label,
                                color = if (action.destructive) Color(0xFFFF453A) else Color.Unspecified,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            action.run()
                        },
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
                    fontSize = 12.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 4f),
                ),
            )
        }
    }
}
