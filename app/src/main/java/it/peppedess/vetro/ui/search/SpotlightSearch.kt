package it.peppedess.vetro.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.Glyph
import it.peppedess.vetro.ui.components.GlyphIcon
import it.peppedess.vetro.ui.components.IconAction
import it.peppedess.vetro.ui.components.SearchPill
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont

@Composable
fun SpotlightSearch(
    apps: List<AppInfo>,
    suggestions: List<AppInfo>,
    topPadding: Dp,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
    onWebSearch: (String) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)

    val results = remember(query, apps) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { if (it.label.startsWith(q, ignoreCase = true)) 0 else 1 }
                .take(12)
        }
    }
    val shown = if (query.isBlank()) suggestions.take(8) else results

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchPill(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Cerca",
                    modifier = Modifier.weight(1f),
                    focusRequester = focus,
                    onSubmit = {
                        val first = results.firstOrNull()
                        if (first != null) {
                            onLaunch(first)
                        } else if (query.isNotBlank()) {
                            onWebSearch(query.trim())
                        }
                    },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Annulla",
                    color = Color.White,
                    fontFamily = VetroFont,
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onClose),
                )
            }

            if (shown.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                SectionTitle(if (query.isBlank()) "Suggerimenti" else "App")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.14f), border = false)
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    shown.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                    AppIcon(
                                        app = app,
                                        actions = actionsFor(app),
                                        onClick = { onLaunch(app) },
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            if (query.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                SectionTitle("Web")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(16.dp), Color.White.copy(alpha = 0.14f), border = false)
                        .clickable { onWebSearch(query.trim()) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(Glyph.SEARCH, IosColors.Blue, Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Cerca “${query.trim()}” sul web",
                        color = Color.White,
                        fontFamily = VetroFont,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontFamily = VetroFont,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}
