package it.peppedess.vetro.data

import android.app.Application
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherState(
    val loading: Boolean = true,
    val all: List<AppInfo> = emptyList(),
    val home: List<HomeItem> = emptyList(),
    val dock: List<AppInfo> = emptyList(),
    val library: List<LibraryGroup> = emptyList(),
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val store = LayoutStore(app)
    private val wallpapers = WallpaperRepository(app)
    private var wallpaperSize = 0 to 0
    private val launcherApps = app.getSystemService(LauncherApps::class.java)

    private var apps: List<AppInfo> = emptyList()
    private var loadJob: Job? = null

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private val _wallpaper = MutableStateFlow<WallpaperImages?>(null)
    val wallpaper: StateFlow<WallpaperImages?> = _wallpaper.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    override fun onCleared() {
        launcherApps?.unregisterCallback(callback)
        super.onCleared()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val needsDefaults = !store.initialized
            val (loaded, defaults) = withContext(Dispatchers.IO) {
                repo.loadApps() to if (needsDefaults) repo.defaultDockPackages() else emptyList()
            }
            apps = loaded
            reconcile(defaults)
        }
    }

    // ---- Sfondo ----

    fun prepareWallpaper(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || wallpaperSize == (width to height)) return
        wallpaperSize = width to height
        viewModelScope.launch {
            _wallpaper.value = withContext(Dispatchers.Default) { wallpapers.load(width, height) }
        }
    }

    fun setWallpaper(uri: Uri) {
        val (width, height) = wallpaperSize
        if (width <= 0) return
        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) {
                if (wallpapers.save(uri, width, height)) wallpapers.load(width, height) else null
            }
            if (images != null) _wallpaper.value = images
        }
    }

    fun resetWallpaper() {
        val (width, height) = wallpaperSize
        if (width <= 0) return
        viewModelScope.launch {
            _wallpaper.value = withContext(Dispatchers.IO) {
                wallpapers.reset()
                wallpapers.load(width, height)
            }
        }
    }

    fun onLaunched(app: AppInfo) {
        store.incrementLaunch(app.key)
        publish()
    }

    // ---- Home ----

    fun moveHome(key: String, toIndex: Int) {
        val list = store.home.toMutableList()
        if (!list.remove(key)) return
        list.add(toIndex.coerceIn(0, list.size), key)
        store.home = list
        publish()
    }

    fun removeFromHome(app: AppInfo) {
        store.home = store.home - app.key
        store.hidden = store.hidden + app.key
        publish()
    }

    fun addToHome(app: AppInfo) {
        if (app.key in store.home) return
        detachFromFolders(app.key)
        store.dock = store.dock - app.key
        store.hidden = store.hidden - app.key
        store.home = store.home + app.key
        publish()
    }

    // ---- Dock ----

    fun removeFromDock(app: AppInfo) {
        store.dock = store.dock - app.key
        if (app.key !in store.home) store.home = store.home + app.key
        publish()
    }

    fun moveHomeToDock(key: String) {
        apps.firstOrNull { it.key == key }?.let { addToDock(it) }
    }

    fun addToDock(app: AppInfo) {
        val dock = store.dock
        if (dock.size >= MAX_DOCK || app.key in dock) return
        detachFromFolders(app.key)
        store.home = store.home - app.key
        store.hidden = store.hidden - app.key
        store.dock = store.dock + app.key
        publish()
    }

    // ---- Cartelle ----

    /** Rilascio di un elemento sopra un altro: crea una cartella o aggiunge a quella esistente. */
    fun dropOnto(draggedKey: String, targetKey: String) {
        if (draggedKey == targetKey || FolderKeys.isFolder(draggedKey)) return
        if (FolderKeys.isFolder(targetKey)) {
            val id = FolderKeys.id(targetKey)
            store.setFolderApps(id, store.folderApps(id) + draggedKey)
            store.home = store.home - draggedKey
        } else {
            val id = java.lang.Long.toString(System.currentTimeMillis(), 36)
            val byKey = apps.associateBy { it.key }
            val a = byKey[targetKey]
            val b = byKey[draggedKey]
            val name = if (a != null && b != null && a.category == b.category) a.category else LayoutStore.DEFAULT_FOLDER
            store.setFolderApps(id, listOf(targetKey, draggedKey))
            store.setFolderName(id, name)
            store.home = store.home
                .filter { it != draggedKey }
                .map { if (it == targetKey) FolderKeys.key(id) else it }
        }
        publish()
    }

    fun removeFromFolder(folderId: String, app: AppInfo) {
        val folderKey = FolderKeys.key(folderId)
        val content = store.folderApps(folderId) - app.key
        val home = store.home.toMutableList()
        val index = home.indexOf(folderKey)
        if (index < 0) return
        if (content.size >= 2) {
            store.setFolderApps(folderId, content)
        } else {
            home[index] = content.firstOrNull() ?: return
            store.deleteFolder(folderId)
        }
        home.add(index + 1, app.key)
        store.home = home
        publish()
    }

    fun renameFolder(folderId: String, name: String) {
        store.setFolderName(folderId, name.trim().ifEmpty { LayoutStore.DEFAULT_FOLDER })
        publish()
    }

    fun removeFolder(folderId: String) {
        store.hidden = store.hidden + store.folderApps(folderId)
        store.home = store.home - FolderKeys.key(folderId)
        store.deleteFolder(folderId)
        publish()
    }

    private fun detachFromFolders(appKey: String) {
        store.home.filter { FolderKeys.isFolder(it) }.forEach { folderKey ->
            val id = FolderKeys.id(folderKey)
            if (appKey in store.folderApps(id)) {
                val content = store.folderApps(id) - appKey
                if (content.size >= 2) {
                    store.setFolderApps(id, content)
                } else {
                    store.home = store.home.map { if (it == folderKey) content.firstOrNull().orEmpty() else it }
                        .filter { it.isNotEmpty() }
                    store.deleteFolder(id)
                }
            }
        }
    }

    // ---- Sincronizzazione ----

    private fun reconcile(defaultDock: List<String>) {
        val valid = apps.map { it.key }.toSet()
        if (!store.initialized) {
            val dock = defaultDock
                .mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg }?.key }
                .distinct()
                .take(MAX_DOCK)
            store.dock = dock
            store.home = apps.map { it.key }.filter { it !in dock }
            store.hidden = emptySet()
            store.initialized = true
        }

        val home = mutableListOf<String>()
        val inFolders = mutableSetOf<String>()
        for (key in store.home) {
            if (FolderKeys.isFolder(key)) {
                val id = FolderKeys.id(key)
                val content = store.folderApps(id).filter { it in valid }
                when {
                    content.size >= 2 -> {
                        store.setFolderApps(id, content)
                        home += key
                        inFolders += content
                    }
                    content.size == 1 -> {
                        home += content.first()
                        store.deleteFolder(id)
                    }
                    else -> store.deleteFolder(id)
                }
            } else if (key in valid && key !in home) {
                home += key
            }
        }

        val dock = store.dock.filter { it in valid }
        val hidden = store.hidden.filter { it in valid }.toSet()
        val known = home.toSet() + inFolders + dock + hidden
        home += apps.map { it.key }.filter { it !in known }

        store.dock = dock
        store.hidden = hidden
        store.home = home
        publish()
    }

    private fun publish() {
        val byKey = apps.associateBy { it.key }
        val home = store.home.mapNotNull { key ->
            if (FolderKeys.isFolder(key)) {
                val id = FolderKeys.id(key)
                val content = store.folderApps(id).mapNotNull { byKey[it] }
                if (content.isEmpty()) null else FolderItem(id, store.folderName(id), content)
            } else {
                byKey[key]?.let { AppItem(it) }
            }
        }
        _state.value = LauncherState(
            loading = false,
            all = apps,
            home = home,
            dock = store.dock.mapNotNull { byKey[it] },
            library = buildLibrary(),
        )
    }

    private fun buildLibrary(): List<LibraryGroup> {
        val groups = mutableListOf<LibraryGroup>()
        val suggested = apps
            .filter { store.launchCount(it.key) > 0 }
            .sortedByDescending { store.launchCount(it.key) }
            .take(8)
        if (suggested.isNotEmpty()) groups += LibraryGroup("Suggerimenti", suggested)
        val recent = apps
            .filter { it.installedAt > 0 }
            .sortedByDescending { it.installedAt }
            .take(8)
        if (recent.isNotEmpty()) groups += LibraryGroup("Aggiunte di recente", recent)
        groups += apps
            .groupBy { it.category }
            .toSortedMap()
            .map { (title, list) -> LibraryGroup(title, list) }
        return groups
    }

    private companion object {
        const val MAX_DOCK = 4
    }
}
