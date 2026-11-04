package it.peppedess.vetro.ui.components

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import it.peppedess.vetro.data.AppInfo
import kotlin.math.roundToInt

fun Context.launchApp(app: AppInfo, view: View? = null) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    val b = LaunchSource.bounds
    LaunchSource.bounds = null
    val options = if (view != null && b != null && b.width > 0f) {
        intent.sourceBounds = android.graphics.Rect(
            b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt(),
        )
        ActivityOptions.makeScaleUpAnimation(
            view, b.left.roundToInt(), b.top.roundToInt(), b.width.roundToInt(), b.height.roundToInt(),
        ).toBundle()
    } else {
        null
    }
    runCatching { startActivity(intent, options) }
}

fun Context.openAppInfo(app: AppInfo) {
    start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null)))
}

fun Context.uninstallApp(app: AppInfo) {
    start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
}

fun Context.webSearch(query: String) {
    val ok = start(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))
    if (!ok) start(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
}

@SuppressLint("WrongConstant")
fun Context.expandNotifications() {
    runCatching {
        val service = getSystemService("statusbar")
        service.javaClass.getMethod("expandNotificationsPanel").invoke(service)
    }
}

fun Context.openWifiPanel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) start(Intent(Settings.Panel.ACTION_WIFI))
    else start(Intent(Settings.ACTION_WIFI_SETTINGS))
}

fun Context.openInternetPanel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) start(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    else start(Intent(Settings.ACTION_WIRELESS_SETTINGS))
}

fun Context.openBluetoothSettings() = start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

fun Context.openAirplaneSettings() = start(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))

fun Context.openTimers() {
    if (!start(Intent(AlarmClock.ACTION_SHOW_TIMERS))) start(Intent(AlarmClock.ACTION_SET_TIMER))
}

fun Context.openCalculator() =
    start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))

fun Context.openCamera() = start(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

fun Context.openWriteSettingsPermission() =
    start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))

private fun Context.start(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
