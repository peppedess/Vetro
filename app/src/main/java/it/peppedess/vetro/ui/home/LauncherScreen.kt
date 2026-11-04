package it.peppedess.vetro.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.AppItem
import it.peppedess.vetro.data.FolderItem
import it.peppedess.vetro.data.HomeItem
import it.peppedess.vetro.data.LauncherViewModel
import it.peppedess.vetro.ui.components.Dock
import it.peppedess.vetro.ui.components.GlassPillButton
import it.peppedess.vetro.ui.components.Glyph
import it.peppedess.vetro.ui.components.HomeSearchPill
import it.peppedess.vetro.ui.components.IconAction
import it.peppedess.vetro.ui.components.IconMenuOverlay
import it.peppedess.vetro.ui.components.LocalBadges
import it.peppedess.vetro.ui.components.LocalMenuController
import it.peppedess.vetro.notifications.NotificationHub
import it.peppedess.vetro.ui.system.NotificationCenter
import it.peppedess.vetro.ui.components.MenuController
import it.peppedess.vetro.ui.components.launchApp
import it.peppedess.vetro.ui.components.openAppInfo
import it.peppedess.vetro.ui.components.uninstallApp
import it.peppedess.vetro.ui.components.webSearch
import it.peppedess.vetro.ui.glass.LocalWallpaper
import it.peppedess.vetro.ui.library.AppLibrary
import it.peppedess.vetro.ui.search.SpotlightSearch
import it.peppedess.vetro.ui.system.ControlCenter
import it.peppedess.vetro.ui.system.IosStatusBar
import kotlinx.coroutines.launch

private const val COLUMNS = 4
private const val ROWS = 6
private const val PAGE_SIZE = COLUMNS * ROWS
private const val WIDGET_ROWS = 2
private const val FIRST_PAGE_SIZE = PAGE_SIZE - WIDGET_ROWS * COLUMNS
private const val MAX_DOCK = 4

private fun buildPages(home: List<HomeItem>): List<List<HomeItem>> {
    val pages = mutableListOf(home.take(FIRST_PAGE_SIZE))
    pages += home.drop(FIRST_PAGE_SIZE).chunked(PAGE_SIZE)
    return pages
}

private fun pageOffset(page: Int) = if (page == 0) 0 else FIRST_PAGE_SIZE + (page - 1) * PAGE_SIZE

@Composable
fun LauncherScreen(
    homeSignal: Int,
    vm: LauncherViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val menu = remember { MenuController() }

    var editing by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var controlOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    val notifications by NotificationHub.items.collectAsStateWithLifecycle()
    val badges = remember(notifications) { notifications.groupingBy { it.packageName }.eachCount() }
    val appsByPackage = remember(state.all) { state.all.associateBy { it.packageName } }
    var openFolderId by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setWallpaper(uri)
    }

    val pages = remember(state.home) { buildPages(state.home) }
    val pageCount = pages.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val onLibrary = pagerState.currentPage == pageCount - 1
    val openFolder = state.home.filterIsInstance<FolderItem>().firstOrNull { it.id == openFolderId }

    val overlayOpen = searchOpen || controlOpen || notificationsOpen || openFolder != null || menu.request != null
    val blurRadius by animateDpAsState(if (overlayOpen) 24.dp else 0.dp, label = "blur")

    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val statusHeight = maxOf(cutoutTop, 30.dp) + 6.dp

    val launch: (AppInfo) -> Unit = { app ->
        searchOpen = false
        openFolderId = null
        menu.dismiss()
        vm.onLaunched(app)
        context.launchApp(app, view)
    }

    fun inHome(app: AppInfo) = state.home.any { item ->
        when (item) {
            is AppItem -> item.app.key == app.key
            is FolderItem -> item.apps.any { it.key == app.key }
        }
    }

    fun inDock(app: AppInfo) = state.dock.any { it.key == app.key }

    fun homeActions(item: HomeItem): List<IconAction> = when (item) {
        is AppItem -> listOf(
            IconAction("Modifica schermata Home", Glyph.EDIT) { editing = true },
            IconAction("Info app", Glyph.INFO) { context.openAppInfo(item.app) },
            IconAction("Rimuovi da Home", Glyph.MINUS) { vm.removeFromHome(item.app) },
            IconAction("Disinstalla app", Glyph.TRASH, destructive = true) { context.uninstallApp(item.app) },
        )
        is FolderItem -> listOf(
            IconAction("Modifica schermata Home", Glyph.EDIT) { editing = true },
            IconAction("Rinomina", Glyph.PENCIL) { openFolderId = item.id },
            IconAction("Rimuovi cartella", Glyph.TRASH, destructive = true) { vm.removeFolder(item.id) },
        )
    }

    fun dockActions(app: AppInfo) = listOf(
        IconAction("Modifica schermata Home", Glyph.EDIT) { editing = true },
        IconAction("Info app", Glyph.INFO) { context.openAppInfo(app) },
        IconAction("Rimuovi dal Dock", Glyph.MINUS) { vm.removeFromDock(app) },
        IconAction("Disinstalla app", Glyph.TRASH, destructive = true) { context.uninstallApp(app) },
    )

    fun libraryActions(app: AppInfo) = buildList {
        if (!inHome(app) && !inDock(app)) add(IconAction("Aggiungi a Home", Glyph.PLUS) { vm.addToHome(app) })
        if (state.dock.size < MAX_DOCK && !inDock(app)) add(IconAction("Aggiungi al Dock", Glyph.DOCK) { vm.addToDock(app) })
        add(IconAction("Info app", Glyph.INFO) { context.openAppInfo(app) })
        add(IconAction("Disinstalla app", Glyph.TRASH, destructive = true) { context.uninstallApp(app) })
    }

    fun folderAppActions(folder: FolderItem, app: AppInfo) = listOf(
        IconAction("Rimuovi dalla cartella", Glyph.MINUS) { vm.removeFromFolder(folder.id, app) },
        IconAction("Info app", Glyph.INFO) { context.openAppInfo(app) },
        IconAction("Disinstalla app", Glyph.TRASH, destructive = true) { context.uninstallApp(app) },
    )

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            searchOpen = false
            controlOpen = false
            notificationsOpen = false
            editing = false
            openFolderId = null
            menu.dismiss()
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler {
        if (editing) {
            editing = false
        } else {
            scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        LaunchedEffect(widthPx, heightPx) { vm.prepareWallpaper(widthPx, heightPx) }

        CompositionLocalProvider(
            LocalWallpaper provides wallpaper,
            LocalMenuController provides menu,
            LocalBadges provides badges,
        ) {
            val wp = wallpaper
            if (wp != null) {
                Image(
                    bitmap = wp.sharp,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF101A44)),
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .padding(top = statusHeight)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    if (editing) {
                        GlassPillButton(
                            text = "Sfondo",
                            glyph = Glyph.PHOTO,
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                        GlassPillButton(
                            text = "Fine",
                            onClick = { editing = false },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !overlayOpen,
                ) { page ->
                    if (page < pages.size) {
                        HomeGrid(
                            items = pages[page],
                            pageOffset = pageOffset(page),
                            columns = COLUMNS,
                            rows = ROWS,
                            reservedRows = if (page == 0) WIDGET_ROWS else 0,
                            editing = editing,
                            onLaunch = launch,
                            onOpenFolder = { openFolderId = it.id },
                            actionsFor = { homeActions(it) },
                            onRemove = { item ->
                                when (item) {
                                    is AppItem -> vm.removeFromHome(item.app)
                                    is FolderItem -> vm.removeFolder(item.id)
                                }
                            },
                            onMove = { key, index -> vm.moveHome(key, index) },
                            onDropOnto = { dragged, target -> vm.dropOnto(dragged, target) },
                            onDropToDock = { key -> vm.moveHomeToDock(key) },
                            onMoveToPage = { key, direction ->
                                val target = (page + direction).coerceIn(0, pages.size - 1)
                                if (target != page) {
                                    val index = if (direction > 0) pageOffset(target) else pageOffset(page) - 1
                                    vm.moveHome(key, index)
                                    scope.launch { pagerState.animateScrollToPage(target) }
                                }
                            },
                            onEnterEdit = { editing = true },
                            onSwipeDown = { if (!editing) searchOpen = true },
                        )
                    } else {
                        AppLibrary(
                            groups = state.library,
                            all = state.all,
                            onLaunch = launch,
                            actionsFor = { libraryActions(it) },
                        )
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!onLibrary) {
                        HomeSearchPill(
                            pageCount = pageCount,
                            current = pagerState.currentPage,
                            scrolling = pagerState.isScrollInProgress,
                            onClick = { searchOpen = true },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !onLibrary,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Dock(
                        apps = state.dock,
                        editing = editing,
                        onLaunch = launch,
                        actionsFor = { dockActions(it) },
                        onRemove = { vm.removeFromDock(it) },
                    )
                }
            }

            AnimatedVisibility(
                visible = openFolder != null,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
            ) {
                openFolder?.let { folder ->
                    FolderOverlay(
                        folder = folder,
                        onLaunch = launch,
                        actionsFor = { app -> folderAppActions(folder, app) },
                        onRename = { name -> vm.renameFolder(folder.id, name) },
                        onDismiss = { openFolderId = null },
                    )
                }
            }

            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn() + slideInVertically { -it / 10 },
                exit = fadeOut(),
            ) {
                SpotlightSearch(
                    apps = state.all,
                    suggestions = state.library.firstOrNull { it.title == "Suggerimenti" }?.apps ?: state.all,
                    topPadding = statusHeight,
                    onLaunch = launch,
                    actionsFor = { libraryActions(it) },
                    onWebSearch = { query ->
                        searchOpen = false
                        context.webSearch(query)
                    },
                    onClose = { searchOpen = false },
                )
            }

            AnimatedVisibility(
                visible = controlOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
            ) {
                ControlCenter(topPadding = statusHeight, onClose = { controlOpen = false })
            }

            AnimatedVisibility(
                visible = notificationsOpen,
                enter = fadeIn() + slideInVertically { -it / 3 },
                exit = fadeOut() + slideOutVertically { -it / 3 },
            ) {
                NotificationCenter(
                    notifications = notifications,
                    hasAccess = remember(notificationsOpen) { NotificationHub.hasAccess(context) },
                    topPadding = statusHeight,
                    iconFor = { pkg -> appsByPackage[pkg]?.icon },
                    labelFor = { pkg -> appsByPackage[pkg]?.label ?: pkg },
                    onClose = { notificationsOpen = false },
                )
            }

            menu.request?.let { request ->
                IconMenuOverlay(request = request, onDismiss = { menu.dismiss() })
            }

            IosStatusBar(
                height = statusHeight,
                modifier = Modifier.pointerInput(Unit) {
                    var startX = 0f
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            total = 0f
                        },
                        onDragEnd = {
                            if (total > 30.dp.toPx()) {
                                if (startX < size.width / 2f) {
                                    notificationsOpen = true
                                } else {
                                    controlOpen = true
                                }
                            }
                        },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            total += amount
                        },
                    )
                },
            )
        }
    }
}
