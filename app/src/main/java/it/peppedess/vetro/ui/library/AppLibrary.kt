package it.peppedess.vetro.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.LibraryGroup
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.IconAction
import it.peppedess.vetro.ui.components.IconShape
import it.peppedess.vetro.ui.components.SearchPill

@Composable
fun AppLibrary(
    groups: List<LibraryGroup>,
    all: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
) {
    var query by remember { mutableStateOf("") }
    var openGroup by remember { mutableStateOf<LibraryGroup?>(null) }
    var showGroup by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            SearchPill(
                value = query,
                onValueChange = { query = it },
                placeholder = "Libreria app",
                onSubmit = { focus.clearFocus() },
            )
            Spacer(Modifier.height(16.dp))

            if (query.isBlank()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(groups, key = { it.title }) { group ->
                        LibraryCard(
                            group = group,
                            onLaunch = onLaunch,
                            onExpand = {
                                openGroup = group
                                showGroup = true
                            },
                        )
                    }
                }
            } else {
                val results = remember(query, all) {
                    val q = query.trim()
                    all.filter { it.label.contains(q, ignoreCase = true) }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results, key = { it.key }) { app ->
                        LibraryRow(app) { onLaunch(app) }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showGroup,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            openGroup?.let { group ->
                GroupOverlay(
                    group = group,
                    onLaunch = onLaunch,
                    actionsFor = actionsFor,
                    onDismiss = { showGroup = false },
                )
            }
        }
    }

    BackHandler(enabled = showGroup) { showGroup = false }
}

@Composable
private fun LibraryCard(
    group: LibraryGroup,
    onLaunch: (AppInfo) -> Unit,
    onExpand: () -> Unit,
) {
    val overflow = group.apps.size > 4
    val big = group.apps.take(if (overflow) 3 else 4)
    val small = if (overflow) group.apps.drop(3).take(4) else emptyList()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) { col ->
                        val index = row * 2 + col
                        val cell = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                        val app = big.getOrNull(index)
                        if (index == 3 && overflow) {
                            MiniCluster(apps = small, modifier = cell, onExpand = onExpand)
                        } else if (app != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = cell
                                    .clip(IconShape)
                                    .clickable { onLaunch(app) },
                            )
                        } else {
                            Spacer(cell)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = group.title,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniCluster(
    apps: List<AppInfo>,
    modifier: Modifier,
    onExpand: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onExpand)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(2) { r ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(2) { c ->
                    val app = apps.getOrNull(r * 2 + c)
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
private fun LibraryRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(IconShape),
        )
        Spacer(Modifier.width(14.dp))
        Text(app.label, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GroupOverlay(
    group: LibraryGroup,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xE62C2C2E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(20.dp),
        ) {
            Text(group.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(group.apps, key = { it.key }) { app ->
                    AppIcon(
                        app = app,
                        actions = actionsFor(app),
                        onClick = {
                            onDismiss()
                            onLaunch(app)
                        },
                        iconSize = 56.dp,
                    )
                }
            }
        }
    }
}
