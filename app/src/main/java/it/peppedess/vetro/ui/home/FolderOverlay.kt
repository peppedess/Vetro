package it.peppedess.vetro.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.FolderItem
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.IconAction

@Composable
fun FolderOverlay(
    folder: FolderItem,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val focusManager = LocalFocusManager.current

    val close = {
        focusManager.clearFocus()
        if (name.trim() != folder.name) onRename(name)
        onDismiss()
    }

    BackHandler { close() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = close,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onRename(name)
                        focusManager.clearFocus()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(18.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(folder.apps, key = { it.key }) { app ->
                        AppIcon(
                            app = app,
                            actions = actionsFor(app),
                            onClick = {
                                onDismiss()
                                onLaunch(app)
                            },
                        )
                    }
                }
            }
        }
    }
}
