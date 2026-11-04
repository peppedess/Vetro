package it.peppedess.vetro.ui.system

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.peppedess.vetro.ui.components.Glyph
import it.peppedess.vetro.ui.components.GlyphIcon
import it.peppedess.vetro.ui.components.openAirplaneSettings
import it.peppedess.vetro.ui.components.openBluetoothSettings
import it.peppedess.vetro.ui.components.openCalculator
import it.peppedess.vetro.ui.components.openCamera
import it.peppedess.vetro.ui.components.openInternetPanel
import it.peppedess.vetro.ui.components.openTimers
import it.peppedess.vetro.ui.components.openWifiPanel
import it.peppedess.vetro.ui.components.openWriteSettingsPermission
import it.peppedess.vetro.ui.glass.GlassTint
import it.peppedess.vetro.ui.glass.glass
import it.peppedess.vetro.ui.theme.IosColors
import it.peppedess.vetro.ui.theme.VetroFont
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val TileShape = RoundedCornerShape(22.dp)

@Composable
fun ControlCenter(topPadding: Dp, onClose: () -> Unit) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val camera = remember { context.getSystemService(CameraManager::class.java) }
    val torchId = remember {
        runCatching {
            camera?.cameraIdList?.firstOrNull { id ->
                camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    var torchOn by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(false) }
    var cellular by remember { mutableStateOf(false) }
    var airplane by remember { mutableStateOf(false) }
    var bluetooth by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(readVolume(audio)) }
    var brightness by remember { mutableFloatStateOf(readBrightness(context)) }
    var askedWritePermission by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) torchOn = enabled
            }
        }
        runCatching { camera?.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { camera?.unregisterTorchCallback(callback) } }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
            wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
            bluetooth = runCatching {
                context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
            }.getOrDefault(false)
            delay(1500)
        }
    }

    BackHandler(onBack = onClose)
    val close by rememberUpdatedState(onClose)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
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
        val gap = 14.dp
        val tile = minOf(80.dp, (maxWidth - 56.dp - gap * 3) / 4)
        val big = tile * 2 + gap

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding + 28.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(
                    modifier = Modifier
                        .size(big)
                        .glass(TileShape, GlassTint.Dark, border = false)
                        .padding(tile * 0.14f),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundToggle(Glyph.PLANE, airplane, IosColors.Orange, tile * 0.62f) { context.openAirplaneSettings(); close() }
                        RoundToggle(Glyph.CELL, cellular, IosColors.Green, tile * 0.62f) { context.openInternetPanel(); close() }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundToggle(Glyph.WIFI, wifi, IosColors.Blue, tile * 0.62f) { context.openWifiPanel(); close() }
                        RoundToggle(Glyph.BLUETOOTH, bluetooth, IosColors.Blue, tile * 0.62f) { context.openBluetoothSettings(); close() }
                    }
                }
                MediaTile(big) { code -> sendMediaKey(audio, code) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        SquareTile(Glyph.FLASHLIGHT, tile, active = torchOn) {
                            torchId?.let { id -> runCatching { camera?.setTorchMode(id, !torchOn) } }
                        }
                        SquareTile(Glyph.TIMER, tile) { context.openTimers(); close() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        SquareTile(Glyph.CALC, tile) { context.openCalculator(); close() }
                        SquareTile(Glyph.CAMERA, tile) { context.openCamera(); close() }
                    }
                }
                VerticalSlider(
                    value = brightness,
                    glyph = Glyph.SUN,
                    modifier = Modifier
                        .width(tile)
                        .height(big),
                    onValueChange = { v ->
                        if (Settings.System.canWrite(context)) {
                            brightness = v
                            writeBrightness(context, v)
                        } else if (!askedWritePermission) {
                            askedWritePermission = true
                            context.openWriteSettingsPermission()
                        }
                    },
                )
                VerticalSlider(
                    value = volume,
                    glyph = Glyph.SPEAKER,
                    modifier = Modifier
                        .width(tile)
                        .height(big),
                    onValueChange = { v ->
                        volume = v
                        writeVolume(audio, v)
                    },
                )
            }
        }
    }
}

@Composable
private fun RoundToggle(glyph: Glyph, active: Boolean, activeColor: Color, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) activeColor else Color.White.copy(alpha = 0.2f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, Color.White, Modifier.size(size * 0.46f))
    }
}

@Composable
private fun SquareTile(glyph: Glyph, size: Dp, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .glass(TileShape, if (active) Color.White.copy(alpha = 0.92f) else GlassTint.Dark, border = false)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, if (active) Color(0xFF1C1C1E) else Color.White, Modifier.size(size * 0.38f))
    }
}

@Composable
private fun MediaTile(size: Dp, onKey: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .size(size)
            .glass(TileShape, GlassTint.Dark, border = false)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(16.dp),
    ) {
        Text(
            text = "Non in riproduzione",
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = VetroFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaButton(Glyph.PREV, 22.dp) { onKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
            MediaButton(Glyph.PLAY, 26.dp) { onKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
            MediaButton(Glyph.NEXT, 22.dp) { onKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MediaButton(glyph: Glyph, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size + 16.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, Color.White, Modifier.size(size))
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    glyph: Glyph,
    modifier: Modifier,
    onValueChange: (Float) -> Unit,
) {
    val current by rememberUpdatedState(onValueChange)
    Box(
        modifier = modifier
            .glass(TileShape, GlassTint.Dark, border = false)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    current((1f - down.position.y / size.height).coerceIn(0f, 1f))
                    drag(down.id) { change ->
                        change.consume()
                        current((1f - change.position.y / size.height).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .background(Color.White),
        )
        GlyphIcon(
            glyph,
            if (value > 0.18f) Color(0xFF6E6E73) else Color.White,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(24.dp),
        )
    }
}

private fun readVolume(audio: AudioManager?): Float {
    audio ?: return 0.5f
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
}

private fun writeVolume(audio: AudioManager?, v: Float) {
    audio ?: return
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt(), 0) }
}

private fun sendMediaKey(audio: AudioManager?, code: Int) {
    audio ?: return
    runCatching {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

private fun brightnessMax(): Int {
    val res = Resources.getSystem()
    val id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android")
    return if (id != 0) runCatching { res.getInteger(id) }.getOrDefault(255).coerceAtLeast(1) else 255
}

private fun readBrightness(context: Context): Float {
    val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
    return (raw / brightnessMax().toFloat()).coerceIn(0f, 1f)
}

private fun writeBrightness(context: Context, v: Float) {
    runCatching {
        val cr = context.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (v * brightnessMax()).roundToInt().coerceAtLeast(1))
    }
}
