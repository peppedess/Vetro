package it.peppedess.vetro.data

import android.content.Context
import androidx.core.content.edit

class LayoutStore(context: Context) {

    private val prefs = context.getSharedPreferences("vetro_layout", Context.MODE_PRIVATE)

    var initialized: Boolean
        get() = prefs.getBoolean("initialized", false)
        set(value) = prefs.edit { putBoolean("initialized", value) }

    var home: List<String>
        get() = readList("home")
        set(value) = writeList("home", value)

    var dock: List<String>
        get() = readList("dock")
        set(value) = writeList("dock", value)

    var hidden: Set<String>
        get() = readList("hidden").toSet()
        set(value) = writeList("hidden", value.toList())

    fun launchCount(key: String): Int = prefs.getInt("launch:$key", 0)

    fun incrementLaunch(key: String) = prefs.edit { putInt("launch:$key", launchCount(key) + 1) }

    fun folderApps(id: String): List<String> = readList("folder_apps:$id")

    fun setFolderApps(id: String, apps: List<String>) = writeList("folder_apps:$id", apps)

    fun folderName(id: String): String = prefs.getString("folder_name:$id", null) ?: DEFAULT_FOLDER

    fun setFolderName(id: String, name: String) = prefs.edit { putString("folder_name:$id", name) }

    fun deleteFolder(id: String) = prefs.edit {
        remove("folder_apps:$id")
        remove("folder_name:$id")
    }

    private fun readList(name: String): List<String> =
        prefs.getString(name, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }

    private fun writeList(name: String, value: List<String>) =
        prefs.edit { putString(name, value.joinToString(SEPARATOR)) }

    companion object {
        const val DEFAULT_FOLDER = "Cartella"
        private const val SEPARATOR = "|"
    }
}
