package it.peppedess.vetro.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.peppedess.vetro.data.AppInfo
import it.peppedess.vetro.data.LauncherViewModel
import it.peppedess.vetro.ui.components.Dock
import it.peppedess.vetro.ui.components.IconAction
import it.peppedess.vetro.ui.components.PageDots
import it.peppedess.vetro.ui.components.launchApp
import it.peppedess.vetro.ui.components.openAppInfo
import it.peppedess.vetro.ui.components.uninstallApp
import it.peppedess.vetro.ui.components.webSearch
import it.peppedess.vetro.ui.library.AppLibrary
import it.peppedess.vetro.ui.search.SpotlightSearch
import kotlinx.coroutines.launch

private const val COLUMNS = 4
private const val ROWS = 6
private const val MAX_DOCK = 4

@Composable
fun LauncherScreen(
    homeSignal: Int,
    vm: LauncherViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }

    val pages = remember(state.home) {
        state.home.chunked(COLUMNS * ROWS).ifEmpty { listOf(emptyList()) }
    }
    val pageCount = pages.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val onLibrary = pagerState.currentPage == pageCount - 1

    val launch: (AppInfo) -> Unit = { app ->
        searchOpen = false
        vm.onLaunched(app)
        context.launchApp(app)
    }

    fun inHome(app: AppInfo) = state.home.any { it.key == app.key }
    fun inDock(app: AppInfo) = state.dock.any { it.key == app.key }

    fun homeActions(app: AppInfo) = listOf(
        IconAction("Modifica schermata Home") { editing = true },
        IconAction("Info app") { context.openAppInfo(app) },
        IconAction("Rimuovi da Home") { vm.removeFromHome(app) },
        IconAction("Disinstalla", destructive = true) { context.uninstallApp(app) },
    )

    fun dockActions(app: AppInfo) = listOf(
        IconAction("Modifica schermata Home") { editing = true },
        IconAction("Info app") { context.openAppInfo(app) },
        IconAction("Rimuovi dal Dock") { vm.removeFromDock(app) },
        IconAction("Disinstalla", destructive = true) { context.uninstallApp(app) },
    )

    fun libraryActions(app: AppInfo) = buildList {
        if (!inHome(app) && !inDock(app)) add(IconAction("Aggiungi a Home") { vm.addToHome(app) })
        if (state.dock.size < MAX_DOCK && !inDock(app)) add(IconAction("Aggiungi al Dock") { vm.addToDock(app) })
        add(IconAction("Info app") { context.openAppInfo(app) })
        add(IconAction("Disinstalla", destructive = true) { context.uninstallApp(app) })
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            searchOpen = false
            editing = false
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler {
        when {
            searchOpen -> searchOpen = false
            editing -> editing = false
            else -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 16.dp),
            ) {
                if (editing) {
                    Text(
                        text = "Fine",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable { editing = false }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                if (page < pages.size) {
                    HomeGrid(
                        apps = pages[page],
                        columns = COLUMNS,
                        rows = ROWS,
                        editing = editing,
                        onLaunch = launch,
                        actionsFor = { homeActions(it) },
                        onRemove = { vm.removeFromHome(it) },
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

            PageDots(
                count = pageCount,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
            )

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
            visible = searchOpen,
            enter = fadeIn() + slideInVertically { -it / 8 },
            exit = fadeOut(),
        ) {
            SpotlightSearch(
                apps = state.all,
                onLaunch = launch,
                actionsFor = { libraryActions(it) },
                onWebSearch = { query ->
                    searchOpen = false
                    context.webSearch(query)
                },
                onClose = { searchOpen = false },
            )
        }
    }
}
