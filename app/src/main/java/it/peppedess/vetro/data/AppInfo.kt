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

sealed interface HomeItem {
    val key: String
}

@Immutable
data class AppItem(val app: AppInfo) : HomeItem {
    override val key: String get() = app.key
}

@Immutable
data class FolderItem(
    val id: String,
    val name: String,
    val apps: List<AppInfo>,
) : HomeItem {
    override val key: String get() = FolderKeys.key(id)
}

object FolderKeys {
    private const val PREFIX = "folder:"
    fun key(id: String): String = PREFIX + id
    fun isFolder(key: String): Boolean = key.startsWith(PREFIX)
    fun id(key: String): String = key.removePrefix(PREFIX)
}
