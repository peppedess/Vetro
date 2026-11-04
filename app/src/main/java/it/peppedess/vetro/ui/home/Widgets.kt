package it.peppedess.vetro.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.ui.system.rememberClock
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val WidgetShape = RoundedCornerShape(22.dp)

@Composable
fun CalendarWidget(modifier: Modifier = Modifier) {
    val minute by rememberClock()
    val (dayName, dayNumber, month) = remember(minute) {
        val now = Date()
        Triple(
            SimpleDateFormat("EEEE", Locale.ITALIAN).format(now).uppercase(Locale.ITALIAN),
            SimpleDateFormat("d", Locale.ITALIAN).format(now),
            SimpleDateFormat("MMMM", Locale.ITALIAN).format(now).replaceFirstChar { it.uppercase() },
        )
    }
    Column(
        modifier = modifier
            .shadow(10.dp, WidgetShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(WidgetShape)
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(dayName, color = IosColors.Red, fontFamily = VetroFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(
            text = dayNumber,
            color = Color.Black,
            fontFamily = VetroFont,
            fontWeight = FontWeight.Normal,
            fontSize = 50.sp,
            lineHeight = 52.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(month, color = Color.Black, fontFamily = VetroFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("Nessun evento oggi", color = IosColors.Gray, fontFamily = VetroFont, fontSize = 12.sp)
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val measurer = rememberTextMeasurer()
    val numberStyle = TextStyle(color = Color.Black, fontFamily = VetroFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

    Box(
        modifier = modifier
            .shadow(10.dp, WidgetShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(WidgetShape)
            .background(Color(0xFF1C1C1E))
            .padding(10.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = min(size.width, size.height) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.White, r, c)

            for (h in 1..12) {
                val angle = Math.toRadians(h * 30.0 - 90.0)
                val layout = measurer.measure(h.toString(), numberStyle)
                val pos = Offset(
                    c.x + (r * 0.78f * cos(angle)).toFloat() - layout.size.width / 2f,
                    c.y + (r * 0.78f * sin(angle)).toFloat() - layout.size.height / 2f,
                )
                drawText(layout, topLeft = pos)
            }

            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val seconds = cal.get(Calendar.SECOND)
            val minutes = cal.get(Calendar.MINUTE) + seconds / 60f
            val hours = (cal.get(Calendar.HOUR) + minutes / 60f)

            rotate(hours * 30f, c) {
                drawLine(Color.Black, c, Offset(c.x, c.y - r * 0.5f), r * 0.07f, StrokeCap.Round)
            }
            rotate(minutes * 6f, c) {
                drawLine(Color.Black, c, Offset(c.x, c.y - r * 0.74f), r * 0.05f, StrokeCap.Round)
            }
            rotate(seconds * 6f, c) {
                drawLine(IosColors.Orange, Offset(c.x, c.y + r * 0.14f), Offset(c.x, c.y - r * 0.82f), r * 0.022f, StrokeCap.Round)
            }
            drawCircle(IosColors.Orange, r * 0.05f, c)
            drawCircle(Color.White, r * 0.022f, c)
        }
    }
}
