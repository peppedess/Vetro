package it.peppedess.vetro.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.IconAction

@Composable
fun HomeGrid(
    apps: List<AppInfo>,
    columns: Int,
    rows: Int,
    editing: Boolean,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
    onRemove: (AppInfo) -> Unit,
    onEnterEdit: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val enterEdit by rememberUpdatedState(onEnterEdit)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 100.dp.toPx()) swipeDown() },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { enterEdit() })
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
            ) {
                repeat(columns) { col ->
                    val app = apps.getOrNull(row * columns + col)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (app != null) {
                            key(app.key) {
                                AppIcon(
                                    app = app,
                                    actions = actionsFor(app),
                                    onClick = { onLaunch(app) },
                                    editing = editing,
                                    onRemove = { onRemove(app) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
