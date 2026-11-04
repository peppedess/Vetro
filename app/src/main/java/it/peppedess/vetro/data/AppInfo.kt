package it.peppedess.vetro.data

import android.content.ComponentName
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppInfo(
    val key: String,
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    val category: String,
    val installedAt: Long,
) {
    val packageName: String get() = component.packageName
}

@Immutable
data class LibraryGroup(
    val title: String,
    val apps: List<AppInfo>,
)
