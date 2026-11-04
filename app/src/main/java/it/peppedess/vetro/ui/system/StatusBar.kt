package it.peppedess.vetro.ui.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import it.peppedess.vetro.ui.components.Glyph
import it.peppedess.vetro.ui.components.GlyphIcon
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun IosStatusBar(height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val time by rememberClock()
    var battery by remember { mutableFloatStateOf(1f) }
    var charging by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(false) }
    var cellular by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) battery = level / scale.toFloat()
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiver.onReceive(context, sticky)

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }

            override fun onLost(network: Network) {
                wifi = false
                cellular = false
            }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { cm?.unregisterNetworkCallback(callback) }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time,
            color = Color.White,
            fontFamily = VetroFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(
                Glyph.CELL,
                if (cellular || !wifi) Color.White else Color.White.copy(alpha = 0.35f),
                Modifier.size(17.dp),
            )
            if (wifi) GlyphIcon(Glyph.WIFI, Color.White, Modifier.size(17.dp))
            BatteryGlyph(level = battery, charging = charging)
        }
    }
}

@Composable
private fun BatteryGlyph(level: Float, charging: Boolean) {
    Canvas(
        Modifier
            .width(27.dp)
            .height(13.dp),
    ) {
        val bodyW = size.width - 3.dp.toPx()
        val radius = CornerRadius(3.5.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset.Zero,
            size = Size(bodyW, size.height),
            cornerRadius = radius,
            style = Stroke(1.dp.toPx()),
        )
        val pad = 2.dp.toPx()
        val fillColor = when {
            charging -> IosColors.Green
            level <= 0.2f -> IosColors.Red
            else -> Color.White
        }
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(pad, pad),
            size = Size((bodyW - 2 * pad) * level.coerceIn(0.05f, 1f), size.height - 2 * pad),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(bodyW + 1.dp.toPx(), size.height * 0.32f),
            size = Size(1.6.dp.toPx(), size.height * 0.36f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

@Composable
fun rememberClock(): androidx.compose.runtime.State<String> {
    val state = remember { mutableStateOf(formatNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            state.value = formatNow()
            val now = System.currentTimeMillis()
            delay(60_000 - now % 60_000 + 50)
        }
    }
    return state
}

private fun formatNow(): String {
    val c = Calendar.getInstance()
    return "%d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}
