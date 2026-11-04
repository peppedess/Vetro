package it.peppedess.vetro.data

import android.app.Application
import android.content.pm.LauncherApps
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
    val home: List<AppInfo> = emptyList(),
    val dock: List<AppInfo> = emptyList(),
    val library: List<LibraryGroup> = emptyList(),
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val store = LayoutStore(app)
    private val launcherApps = app.getSystemService(LauncherApps::class.java)

    private var apps: List<AppInfo> = emptyList()
    private var loadJob: Job? = null

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

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

    fun onLaunched(app: AppInfo) {
        store.incrementLaunch(app.key)
        publish()
    }

    fun removeFromHome(app: AppInfo) {
        store.home = store.home - app.key
        store.hidden = store.hidden + app.key
        publish()
    }

    fun addToHome(app: AppInfo) {
        if (app.key in store.home) return
        store.dock = store.dock - app.key
        store.hidden = store.hidden - app.key
        store.home = store.home + app.key
        publish()
    }

    fun removeFromDock(app: AppInfo) {
        store.dock = store.dock - app.key
        if (app.key !in store.home) store.home = store.home + app.key
        publish()
    }

    fun addToDock(app: AppInfo) {
        val dock = store.dock
        if (dock.size >= MAX_DOCK || app.key in dock) return
        store.home = store.home - app.key
        store.hidden = store.hidden - app.key
        store.dock = dock + app.key
        publish()
    }

    private fun reconcile(defaultDock: List<String>) {
        val byKey = apps.associateBy { it.key }
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
        val dock = store.dock.filter { it in byKey }
        val hidden = store.hidden.filter { it in byKey }.toSet()
        val known = store.home.toSet() + dock + hidden
        val home = store.home.filter { it in byKey } + apps.map { it.key }.filter { it !in known }
        store.dock = dock
        store.hidden = hidden
        store.home = home
        publish()
    }

    private fun publish() {
        val byKey = apps.associateBy { it.key }
        _state.value = LauncherState(
            loading = false,
            all = apps,
            home = store.home.mapNotNull { byKey[it] },
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
