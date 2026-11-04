package it.peppedess.vetro.ui.system

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.notifications.NotifItem
import it.peppedess.vetro.notifications.NotificationHub
import it.peppedess.vetro.ui.components.IconShape
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun NotificationCenter(
    notifications: List<NotifItem>,
    hasAccess: Boolean,
    topPadding: Dp,
    iconFor: (String) -> ImageBitmap?,
    labelFor: (String) -> String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val time by rememberClock()
    val date = remember(time) {
        SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date()).replaceFirstChar { it.uppercase() }
    }
    val close by rememberUpdatedState(onClose)
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -80.dp.toPx()) close() },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                )
            },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = topPadding + 24.dp, start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "clock") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date,
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = VetroFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                    )
                    Text(
                        text = time,
                        color = Color.White.copy(alpha = 0.92f),
                        fontFamily = VetroFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 92.sp,
                        lineHeight = 96.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!hasAccess) {
                item(key = "access") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.18f), border = false)
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                            .padding(16.dp),
                    ) {
                        Text(
                            "Attiva le notifiche",
                            color = Color.White,
                            fontFamily = VetroFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Concedi a Vetro l'accesso alle notifiche per vederle qui e mostrare i badge sulle icone.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = VetroFont,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Apri Impostazioni",
                            color = IosColors.Blue,
                            fontFamily = VetroFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            } else if (notifications.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nessuna notifica precedente",
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = VetroFont,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                    )
                }
            } else {
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Centro Notifiche",
                            color = Color.White,
                            fontFamily = VetroFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Cancella tutto",
                            color = Color.White,
                            fontFamily = VetroFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.2f), border = false)
                                .clickable { NotificationHub.dismissAll() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                items(notifications, key = { it.key }) { item ->
                    NotificationCard(
                        item = item,
                        icon = iconFor(item.packageName),
                        appName = labelFor(item.packageName),
                        onOpen = {
                            close()
                            NotificationHub.open(context, item)
                        },
                        onDismiss = { NotificationHub.dismiss(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotifItem,
    icon: ImageBitmap?,
    appName: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val offset = remember(item.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (abs(offset.value) / (size.width.coerceAtLeast(1f))).coerceIn(0f, 0.8f)
            }
            .pointerInput(item.key) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (item.clearable && abs(offset.value) > size.width * 0.35f) {
                                offset.animateTo(if (offset.value > 0) size.width.toFloat() else -size.width.toFloat())
                                onDismiss()
                            } else {
                                offset.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                )
            }
            .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.22f), border = false)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(IconShape),
            )
        } else {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(IconShape)
                    .background(IosColors.Gray),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title.ifBlank { appName },
                    color = Color.White,
                    fontFamily = VetroFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(item.postTime),
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = VetroFont,
                    fontSize = 13.sp,
                )
            }
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    color = Color.White.copy(alpha = 0.92f),
                    fontFamily = VetroFont,
                    fontSize = 15.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun relativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "ora"
        minutes < 60 -> "$minutes min fa"
        minutes < 24 * 60 -> "${minutes / 60} h fa"
        else -> SimpleDateFormat("d MMM", Locale.ITALIAN).format(Date(time))
    }
}
