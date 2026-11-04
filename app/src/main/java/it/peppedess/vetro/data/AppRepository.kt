package it.peppedess.vetro.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconPx = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)

    fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val activities = pm.queryIntentActivities(intent, 0)
        return activities
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                val component = ComponentName(ai.packageName, ai.name)
                val installedAt = runCatching {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(ai.packageName, 0).firstInstallTime
                }.getOrDefault(0L)
                AppInfo(
                    key = component.flattenToShortString(),
                    label = ri.loadLabel(pm).toString(),
                    component = component,
                    icon = ri.loadIcon(pm).toSquareBitmap(iconPx).asImageBitmap(),
                    category = categoryName(ai.applicationInfo.category),
                    installedAt = installedAt,
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    fun defaultDockPackages(): List<String> {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        return intents.mapNotNull { intent ->
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
                ?.takeIf { it != "android" }
        }
    }

    private fun categoryName(category: Int): String = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Giochi"
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Intrattenimento"
        ApplicationInfo.CATEGORY_IMAGE -> "Foto e video"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "Notizie"
        ApplicationInfo.CATEGORY_MAPS -> "Viaggi"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Produttività"
        else -> "Utility"
    }
}

/**
 * Disegna l'icona come quadrato pieno: per le adaptive icon espande i layer
 * oltre la maschera di sistema, così la forma "squircle" la applica Compose.
 */
private fun Drawable.toSquareBitmap(px: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (this is AdaptiveIconDrawable) {
        val pad = px / 4
        background?.let {
            it.setBounds(-pad, -pad, px + pad, px + pad)
            it.draw(canvas)
        }
        foreground?.let {
            it.setBounds(-pad, -pad, px + pad, px + pad)
            it.draw(canvas)
        }
    } else {
        canvas.drawColor(Color.WHITE)
        val inset = px / 8
        setBounds(inset, inset, px - inset, px - inset)
        draw(canvas)
    }
    return bitmap
}
