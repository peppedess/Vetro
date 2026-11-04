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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.ui.components.AppIcon
import it.peppedess.vetro.ui.components.IconAction
import it.peppedess.vetro.ui.components.SearchPill

@Composable
fun SpotlightSearch(
    apps: List<AppInfo>,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp)
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
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onClose),
                )
            }

            if (results.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionTitle("App")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    results.chunked(4).forEach { row ->
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
                Spacer(Modifier.height(20.dp))
                SectionTitle("Web")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onWebSearch(query.trim()) }
                        .padding(16.dp),
                ) {
                    Text("Cerca “${query.trim()}” sul web", color = Color.White, fontSize = 16.sp)
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
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}
