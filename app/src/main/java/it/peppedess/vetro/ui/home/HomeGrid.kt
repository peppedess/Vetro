package it.peppedess.vetro.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.AppItem
import it.peppedess.vetro.data.FolderItem
import it.peppedess.vetro.data.HomeItem
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.FolderIcon
import it.peppedess.vetro.ui.components.IconAction
import kotlin.math.roundToInt

val HOME_ICON_SIZE = 60.dp

@Composable
fun HomeGrid(
    items: List<HomeItem>,
    pageOffset: Int,
    columns: Int,
    rows: Int,
    reservedRows: Int,
    editing: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    actionsFor: (HomeItem) -> List<IconAction>,
    onRemove: (HomeItem) -> Unit,
    onMove: (key: String, toIndex: Int) -> Unit,
    onDropOnto: (draggedKey: String, targetKey: String) -> Unit,
    onDropToDock: (key: String) -> Unit,
    onMoveToPage: (key: String, direction: Int) -> Unit,
    onEnterEdit: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val reserved = reservedRows * columns

    val currentItems by rememberUpdatedState(items)
    val currentOffset by rememberUpdatedState(pageOffset)
    val move by rememberUpdatedState(onMove)
    val dropOnto by rememberUpdatedState(onDropOnto)
    val dropToDock by rememberUpdatedState(onDropToDock)
    val moveToPage by rememberUpdatedState(onMoveToPage)
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val enterEdit by rememberUpdatedState(onEnterEdit)

    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverIndex by remember { mutableIntStateOf(0) }
    var folderTarget by remember { mutableStateOf<String?>(null) }
    var dockHover by remember { mutableStateOf(false) }
    var edgeHover by remember { mutableIntStateOf(0) }

    val display = remember(items, draggingKey, hoverIndex) {
        val key = draggingKey
        val dragged = items.firstOrNull { it.key == key }
        if (key == null || dragged == null) {
            items
        } else {
            items.filter { it.key != key }.toMutableList().apply {
                add(hoverIndex.coerceIn(0, size), dragged)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editing) {
                if (editing) return@pointerInput
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 90.dp.toPx()) swipeDown() },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                )
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val cellW = constraints.maxWidth / columns.toFloat()
        val cellH = constraints.maxHeight / rows.toFloat()
        val iconPx = with(density) { HOME_ICON_SIZE.toPx() }
        val cellWdp = with(density) { cellW.toDp() }
        val cellHdp = with(density) { cellH.toDp() }

        fun slotOrigin(slot: Int) = Offset((slot % columns) * cellW, (slot / columns) * cellH)
        fun itemOrigin(index: Int) = slotOrigin(index + reserved)
        fun iconCenter(index: Int) = itemOrigin(index) + Offset(cellW / 2f, iconPx / 2f)

        /** Indice elemento sotto il punto (può essere negativo se cade sui widget). */
        fun indexAt(p: Offset): Int {
            val col = (p.x / cellW).toInt().coerceIn(0, columns - 1)
            val row = (p.y / cellH).toInt().coerceIn(0, rows - 1)
            return row * columns + col - reserved
        }

        if (reservedRows > 0) {
            val inset = (cellW - iconPx) / 2f
            val widgetW = with(density) { (cellW + iconPx).toDp() }
            val widgetH = with(density) { (cellH * (reservedRows - 1) + iconPx).toDp() }
            CalendarWidget(
                Modifier
                    .offset { IntOffset(inset.roundToInt(), 0) }
                    .size(widgetW, widgetH),
            )
            ClockWidget(
                Modifier
                    .offset { IntOffset((cellW * 2 + inset).roundToInt(), 0) }
                    .size(widgetW, widgetH),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cellW, cellH, reserved) {
                    detectTapGestures(
                        onLongPress = { p ->
                            val idx = indexAt(p)
                            if (idx >= 0 && currentItems.getOrNull(idx) == null) enterEdit()
                        },
                    )
                }
                .pointerInput(editing, cellW, cellH, reserved) {
                    if (!editing) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = indexAt(down.position)
                        val item = currentItems.getOrNull(start) ?: return@awaitEachGesture
                        val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            ?: return@awaitEachGesture

                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggingKey = item.key
                        grabOffset = down.position - iconCenter(start)
                        dragPos = slop.position
                        hoverIndex = start
                        folderTarget = null
                        dockHover = false
                        edgeHover = 0
                        val edge = 22.dp.toPx()
                        val dockLine = size.height + 30.dp.toPx()

                        val completed = drag(slop.id) { change ->
                            change.consume()
                            dragPos = change.position
                            dockHover = item is AppItem && change.position.y > dockLine
                            edgeHover = when {
                                dockHover -> 0
                                change.position.x < edge -> -1
                                change.position.x > size.width - edge -> 1
                                else -> 0
                            }
                            if (dockHover || edgeHover != 0) {
                                folderTarget = null
                                return@drag
                            }
                            val others = currentItems.filter { it.key != item.key }
                            val idx = indexAt(change.position).coerceAtLeast(0)
                            val shown = others.toMutableList().apply {
                                add(hoverIndex.coerceIn(0, size), item)
                            }
                            val under = shown.getOrNull(idx)
                            val iconPos = change.position - grabOffset
                            val nearCenter = (iconPos - iconCenter(idx)).getDistance() < iconPx * 0.35f
                            if (item is AppItem && under != null && under.key != item.key && nearCenter) {
                                if (folderTarget != under.key) {
                                    folderTarget = under.key
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            } else {
                                folderTarget = null
                                hoverIndex = idx.coerceAtMost(others.size)
                            }
                        }

                        val key = draggingKey
                        val target = folderTarget
                        if (completed && key != null) {
                            when {
                                dockHover -> dropToDock(key)
                                edgeHover != 0 -> moveToPage(key, edgeHover)
                                target != null -> dropOnto(key, target)
                                else -> move(key, currentOffset + hoverIndex)
                            }
                        }
                        draggingKey = null
                        folderTarget = null
                        dockHover = false
                        edgeHover = 0
                    }
                },
        ) {
            display.forEachIndexed { index, item ->
                key(item.key) {
                    val position by animateOffsetAsState(
                        targetValue = itemOrigin(index),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cell",
                    )
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                            .size(cellWdp, cellHdp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (item.key != draggingKey) {
                            HomeItemView(
                                item = item,
                                editing = editing,
                                emphasized = item.key == folderTarget,
                                actions = actionsFor(item),
                                onLaunch = onLaunch,
                                onOpenFolder = onOpenFolder,
                                onRemove = { onRemove(item) },
                            )
                        }
                    }
                }
            }

            if (edgeHover != 0) {
                Box(
                    Modifier
                        .align(if (edgeHover < 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .width(5.dp)
                        .fillMaxHeight(0.5f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.45f)),
                )
            }

            val draggedItem = draggingKey?.let { k -> items.firstOrNull { it.key == k } }
            if (draggedItem != null) {
                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .offset {
                            val p = dragPos - grabOffset - Offset(cellW / 2f, iconPx / 2f)
                            IntOffset(p.x.roundToInt(), p.y.roundToInt())
                        }
                        .size(cellWdp, cellHdp)
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    HomeItemView(
                        item = draggedItem,
                        editing = false,
                        emphasized = false,
                        actions = emptyList(),
                        onLaunch = {},
                        onOpenFolder = {},
                        onRemove = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeItemView(
    item: HomeItem,
    editing: Boolean,
    emphasized: Boolean,
    actions: List<IconAction>,
    onLaunch: (AppInfo) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onRemove: () -> Unit,
) {
    when (item) {
        is AppItem -> AppIcon(
            app = item.app,
            actions = actions,
            onClick = { onLaunch(item.app) },
            iconSize = HOME_ICON_SIZE,
            editing = editing,
            emphasized = emphasized,
            onRemove = onRemove,
        )
        is FolderItem -> FolderIcon(
            folder = item,
            actions = actions,
            onClick = { onOpenFolder(item) },
            iconSize = HOME_ICON_SIZE,
            editing = editing,
            emphasized = emphasized,
            onRemove = onRemove,
        )
    }
}
