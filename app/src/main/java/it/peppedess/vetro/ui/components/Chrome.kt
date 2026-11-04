package it.peppedess.vetro.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.ui.glass.GlassTint
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.VetroFont

private val DockShape = RoundedCornerShape(34.dp)

@Composable
fun Dock(
    apps: List<AppInfo>,
    editing: Boolean,
    onLaunch: (AppInfo) -> Unit,
    actionsFor: (AppInfo) -> List<IconAction>,
    onRemove: (AppInfo) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 6.dp)
            .glass(DockShape, GlassTint.Dock)
            .padding(horizontal = 6.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (apps.isEmpty()) Spacer(Modifier.height(60.dp))
        apps.forEach { app ->
            AppIcon(
                app = app,
                actions = actionsFor(app),
                onClick = { onLaunch(app) },
                showLabel = false,
                editing = editing,
                onRemove = { onRemove(app) },
            )
        }
    }
}

/** Pillola "Cerca" sopra il Dock; durante lo scorrimento mostra i puntini delle pagine. */
@Composable
fun HomeSearchPill(
    pageCount: Int,
    current: Int,
    scrolling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .glass(RoundedCornerShape(50), GlassTint.Light, border = false)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = scrolling,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "pill",
        ) { showDots ->
            if (showDots) {
                PageDots(count = pageCount, current = current)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.SEARCH, Color.White, Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Cerca",
                        color = Color.White,
                        fontFamily = VetroFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val alpha by animateFloatAsState(if (index == current) 1f else 0.4f, label = "dot")
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha)),
            )
        }
    }
}

/** Pulsante a pillola in vetro, come "Fine" e "Modifica" su iOS. */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: Glyph = Glyph.NONE,
) {
    Row(
        modifier = modifier
            .glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.26f), border = false)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != Glyph.NONE) {
            GlyphIcon(glyph, Color.White, Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Color.White, fontFamily = VetroFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSubmit: () -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontFamily = VetroFont),
        cursorBrush = SolidColor(Color(0xFF0A84FF)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .glass(RoundedCornerShape(12.dp), Color.White.copy(alpha = 0.16f), border = false)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.SEARCH, Color.White.copy(alpha = 0.65f), Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = VetroFont,
                            fontSize = 17.sp,
                        )
                    }
                    inner()
                }
            }
        },
    )
}
