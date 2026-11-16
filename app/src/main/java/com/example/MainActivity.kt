package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.domain.model.AppLanguage
import com.example.domain.model.MediaType
import com.example.domain.model.ThemeMode
import com.example.ui.components.DeveloperDonationDialog
import com.example.ui.components.SplashIntroOverlay
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OptimizationScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WallpapersScreen
import com.example.ui.theme.RonyLiveWallpaperTheme
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.WallpaperViewModel
import com.example.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object WallpapersRoute
@Serializable data object FavoritesRoute
@Serializable data object SettingsRoute
@Serializable data class PreviewRoute(val wallpaperId: Long)
@Serializable data object OptimizationRoute
@Serializable data object PlaylistsRoute

private data class TopLevelTab(
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as RonyWallpaperApp).container

        setContent {
            val viewModel: WallpaperViewModel = viewModel(
                factory = WallpaperViewModel.Factory(appContainer.repository)
            )
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val localizedStrings = remember(settings.language) { stringsFor(settings.language) }

            val gifImageLoader = remember {
                ImageLoader.Builder(applicationContext)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()
            }

            @Suppress("DEPRECATION")
            CompositionLocalProvider(
                LocalAppStrings provides localizedStrings,
                LocalImageLoader provides gifImageLoader
            ) {
                RonyLiveWallpaperTheme(themeMode = settings.themeMode) {
                    RonyLiveWallpaperAppRoot(
                        viewModel = viewModel,
                        appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                    )
                }
            }
        }
    }
}

@Composable
private fun RonyLiveWallpaperAppRoot(
    viewModel: WallpaperViewModel,
    appVersion: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val allWallpapers by viewModel.allWallpapers.collectAsStateWithLifecycle()
    val filteredWallpapers by viewModel.filteredWallpapers.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyUsed by viewModel.recentlyUsed.collectAsStateWithLifecycle()
    val activeWallpaper by viewModel.activeWallpaper.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val storageSummary by viewModel.storageSummary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    var isSystemWallpaperActive by remember {
        mutableStateOf(WallpaperHelper.isRonyWallpaperActive(context))
    }
    var showImportChooserDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val handleConnectWhatsApp: () -> Unit = {
        val opened = WallpaperHelper.openDeveloperWhatsApp(context)
        if (!opened) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(strings.whatsappOpenError)
            }
        }
    }

    val handleOpenSupportDialog: () -> Unit = {
        showDonationDialog = true
    }

    LaunchedEffect(Unit) {
        delay(950L)
        showSplash = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSystemWallpaperActive = WallpaperHelper.isRonyWallpaperActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(viewModel, strings) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Imported -> {
                    snackbarHostState.showSnackbar(strings.importSuccess)
                    if (navController.currentDestination?.hasRoute<PlaylistsRoute>() != true) {
                        navController.navigate(PreviewRoute(event.wallpaperId))
                    }
                }
                is UiEvent.BatchImported -> {
                    snackbarHostState.showSnackbar("${strings.importSuccess} (${strings.itemsCount(event.count)})")
                }
                UiEvent.ImportFailed -> {
                    snackbarHostState.showSnackbar(strings.importError)
                }
                UiEvent.ActiveWallpaperDeleteBlocked -> {
                    snackbarHostState.showSnackbar(strings.cannotDeleteActive)
                }
                is UiEvent.CacheCleared -> {
                    snackbarHostState.showSnackbar(strings.cacheCleared)
                }
                UiEvent.SettingsSynced -> {
                    snackbarHostState.showSnackbar(strings.settingsSavedBanner)
                }
                UiEvent.PlaylistActivated -> {
                    snackbarHostState.showSnackbar(strings.playlistActivatedToast)
                }
                UiEvent.PlaylistUpdated -> {
                    snackbarHostState.showSnackbar(strings.playlistUpdatedToast)
                }
            }
        }
    }

    // Zero-permission Storage Access Framework multi-select launchers (Play Policy compliant)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importMultipleMedia(uris, MediaType.VIDEO)
    }

    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importMultipleMedia(uris, MediaType.GIF)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importMultipleMedia(uris, MediaType.IMAGE)
    }

    val launchImportVideo = { videoPickerLauncher.launch(arrayOf("video/*")) }
    val launchImportGif = { gifPickerLauncher.launch(arrayOf("image/gif")) }
    val launchImportPhotos = { photoPickerLauncher.launch(arrayOf("image/*")) }
    val isTopLevelRoute = currentDestination == null ||
        currentDestination.hasRoute<HomeRoute>() ||
        currentDestination.hasRoute<WallpapersRoute>() ||
        currentDestination.hasRoute<FavoritesRoute>() ||
        currentDestination.hasRoute<SettingsRoute>()

    val topLevelTabs = listOf(
        TopLevelTab(HomeRoute, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        TopLevelTab(WallpapersRoute, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_wallpapers"),
        TopLevelTab(FavoritesRoute, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites"),
        TopLevelTab(SettingsRoute, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val tabLabel: (Any) -> String = { route ->
        when (route) {
            HomeRoute -> strings.navHome
            WallpapersRoute -> strings.navWallpapers
            FavoritesRoute -> strings.navFavorites
            SettingsRoute -> strings.navSettings
            else -> ""
        }
    }

    val navigateToTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (isTopLevelRoute && !useNavRail) {
                        NavigationBar {
                            topLevelTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    WallpapersRoute -> currentDestination?.hasRoute<WallpapersRoute>() == true
                                    FavoritesRoute -> currentDestination?.hasRoute<FavoritesRoute>() == true
                                    SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tabLabel(tab.route)
                                        )
                                    },
                                    label = { Text(tabLabel(tab.route), maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTopLevelRoute && useNavRail) {
                        NavigationRail {
                            Spacer(modifier = Modifier.height(12.dp))
                            topLevelTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    WallpapersRoute -> currentDestination?.hasRoute<WallpapersRoute>() == true
                                    FavoritesRoute -> currentDestination?.hasRoute<FavoritesRoute>() == true
                                    SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tabLabel(tab.route)
                                        )
                                    },
                                    label = { Text(tabLabel(tab.route), maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (isImporting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    activeWallpaper = activeWallpaper,
                                    activePlaylist = activePlaylist,
                                    playlistCount = playlists.size,
                                    isSystemWallpaperActive = isSystemWallpaperActive,
                                    recentlyUsed = recentlyUsed,
                                    favorites = favorites,
                                    totalLibraryCount = allWallpapers.size,
                                    storageSummary = storageSummary,
                                    settings = settings,
                                    onImportVideo = launchImportVideo,
                                    onImportGif = launchImportGif,
                                    onImportPhotos = launchImportPhotos,
                                    onSetNewWallpaperClick = {
                                        if (allWallpapers.isNotEmpty()) {
                                            navigateToTopLevel(WallpapersRoute)
                                        } else {
                                            showImportChooserDialog = true
                                        }
                                    },
                                    onOpenPreview = { id -> navController.navigate(PreviewRoute(id)) },
                                    onApplyWallpaperDirect = { item ->
                                        viewModel.activateAndPrepareWallpaper(item.id, settings.preferredTarget) {
                                            WallpaperHelper.launchLiveWallpaperPicker(context)
                                        }
                                    },
                                    onOpenLibrary = { navigateToTopLevel(WallpapersRoute) },
                                    onOpenFavorites = { navigateToTopLevel(FavoritesRoute) },
                                    onOpenPlaylists = { navController.navigate(PlaylistsRoute) },
                                    onSkipPlaylistNext = viewModel::skipPlaylistNext,
                                    onStopPlaylist = viewModel::deactivatePlaylist,
                                    onOpenSettings = { navigateToTopLevel(SettingsRoute) },
                                    onToggleBatterySaver = viewModel::setBatterySaverEnabled,
                                    onToggleTheme = {
                                        val next = if (settings.themeMode == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
                                        viewModel.setThemeMode(next)
                                    },
                                    onToggleLanguage = {
                                        val next = if (settings.language == AppLanguage.ENGLISH) {
                                            AppLanguage.BENGALI
                                        } else {
                                            AppLanguage.ENGLISH
                                        }
                                        viewModel.setLanguage(next)
                                    },
                                    onConnectWhatsApp = handleConnectWhatsApp,
                                    onOpenSupportDialog = handleOpenSupportDialog,
                                    modifier = Modifier.statusBarsPadding()
                                )
                            }

                            composable<WallpapersRoute> {
                                WallpapersScreen(
                                    wallpapers = filteredWallpapers,
                                    searchQuery = searchQuery,
                                    mediaFilter = mediaFilter,
                                    sortOption = sortOption,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onMediaFilterChange = viewModel::setMediaFilter,
                                    onSortOptionChange = viewModel::setSortOption,
                                    onImportVideo = launchImportVideo,
                                    onImportGif = launchImportGif,
                                    onImportPhotos = launchImportPhotos,
                                    onOpenPlaylists = { navController.navigate(PlaylistsRoute) },
                                    onPreviewClick = { id -> navController.navigate(PreviewRoute(id)) },
                                    onApplyClick = { item ->
                                        viewModel.activateAndPrepareWallpaper(item.id, settings.preferredTarget) {
                                            WallpaperHelper.launchLiveWallpaperPicker(context)
                                        }
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRename = viewModel::renameWallpaper,
                                    onDelete = viewModel::deleteWallpaper,
                                    modifier = Modifier.statusBarsPadding()
                                )
                            }

                            composable<FavoritesRoute> {
                                FavoritesScreen(
                                    favorites = favorites,
                                    onBrowseLibrary = { navigateToTopLevel(WallpapersRoute) },
                                    onPreviewClick = { id -> navController.navigate(PreviewRoute(id)) },
                                    onApplyClick = { item ->
                                        viewModel.activateAndPrepareWallpaper(item.id, settings.preferredTarget) {
                                            WallpaperHelper.launchLiveWallpaperPicker(context)
                                        }
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onRename = viewModel::renameWallpaper,
                                    onDelete = viewModel::deleteWallpaper,
                                    modifier = Modifier.statusBarsPadding()
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    settings = settings,
                                    storageSummary = storageSummary,
                                    appVersion = appVersion,
                                    onSetThemeMode = viewModel::setThemeMode,
                                    onSetLanguage = viewModel::setLanguage,
                                    onSetDefaultSpeed = viewModel::setDefaultSpeed,
                                    onSetDefaultLooping = viewModel::setDefaultLooping,
                                    onSetBatterySaver = viewModel::setBatterySaverEnabled,
                                    onSetTargetFps = viewModel::setTargetFps,
                                    onSetPreferHardwareDecoder = viewModel::setPreferHardwareDecoder,
                                    onClearCache = viewModel::cleanOrphanStorage,
                                    onOpenOptimizationGuide = { navController.navigate(OptimizationRoute) },
                                    onConnectWhatsApp = handleConnectWhatsApp,
                                    onOpenSupportDialog = handleOpenSupportDialog,
                                    modifier = Modifier.statusBarsPadding()
                                )
                            }

                            composable<PreviewRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<PreviewRoute>()
                                val wallpaper by viewModel.observeWallpaper(route.wallpaperId)
                                    .collectAsStateWithLifecycle(initialValue = null)

                                PreviewScreen(
                                    wallpaper = wallpaper,
                                    initialTarget = settings.preferredTarget,
                                    onBack = { navController.popBackStack() },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSaveCustomization = { id, speed, looping, muted, brightness, scale, zoom, ox, oy, trimStart, trimEnd ->
                                        viewModel.updateWallpaperCustomization(
                                            id = id,
                                            playbackSpeed = speed,
                                            isLooping = looping,
                                            isMuted = muted,
                                            brightness = brightness,
                                            scaleMode = scale,
                                            zoom = zoom,
                                            offsetX = ox,
                                            offsetY = oy,
                                            trimStartMs = trimStart,
                                            trimEndMs = trimEnd
                                        )
                                    },
                                    onApplyWallpaper = { id, speed, looping, muted, brightness, scale, zoom, ox, oy, trimStart, trimEnd, target ->
                                        viewModel.updateWallpaperCustomization(
                                            id = id,
                                            playbackSpeed = speed,
                                            isLooping = looping,
                                            isMuted = muted,
                                            brightness = brightness,
                                            scaleMode = scale,
                                            zoom = zoom,
                                            offsetX = ox,
                                            offsetY = oy,
                                            trimStartMs = trimStart,
                                            trimEndMs = trimEnd
                                        )
                                        viewModel.activateAndPrepareWallpaper(id, target) {
                                            WallpaperHelper.launchLiveWallpaperPicker(context)
                                        }
                                    }
                                )
                            }

                            composable<OptimizationRoute> {
                                OptimizationScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable<PlaylistsRoute> {
                                PlaylistsScreen(
                                    playlists = playlists,
                                    activePlaylist = activePlaylist,
                                    allWallpapers = allWallpapers,
                                    preferredTarget = settings.preferredTarget,
                                    onBack = { navController.popBackStack() },
                                    onImportPhotos = launchImportPhotos,
                                    onImportVideos = launchImportVideo,
                                    onImportGifs = launchImportGif,
                                    onCreateAndActivatePlaylist = { name, ids, intervalVal, unit, shuffle, target ->
                                        viewModel.createPlaylist(
                                            name = name,
                                            wallpaperIds = ids,
                                            intervalValue = intervalVal,
                                            intervalUnit = unit,
                                            isShuffle = shuffle,
                                            activateImmediately = true,
                                            target = target,
                                            onActivated = {
                                                WallpaperHelper.launchLiveWallpaperPicker(context)
                                            }
                                        )
                                    },
                                    onUpdatePlaylist = viewModel::updatePlaylist,
                                    onSaveItemCustomization = { id, speed, looping, muted, brightness, scale, zoom, ox, oy, trimStart, trimEnd ->
                                        viewModel.updateWallpaperCustomization(
                                            id = id,
                                            playbackSpeed = speed,
                                            isLooping = looping,
                                            isMuted = muted,
                                            brightness = brightness,
                                            scaleMode = scale,
                                            zoom = zoom,
                                            offsetX = ox,
                                            offsetY = oy,
                                            trimStartMs = trimStart,
                                            trimEndMs = trimEnd,
                                            notifyIfActive = true
                                        )
                                    },
                                    onActivatePlaylist = { playlistId, target ->
                                        viewModel.activatePlaylist(playlistId, target) {
                                            WallpaperHelper.launchLiveWallpaperPicker(context)
                                        }
                                    },
                                    onDeactivatePlaylist = viewModel::deactivatePlaylist,
                                    onSkipNext = viewModel::skipPlaylistNext,
                                    onDeletePlaylist = viewModel::deletePlaylist
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isImporting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.importingMedia,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = onboardingCompleted == false,
            enter = fadeIn(tween(320)),
            exit = fadeOut(tween(260)) + slideOutVertically(
                animationSpec = tween(260),
                targetOffsetY = { -it / 10 }
            )
        ) {
            OnboardingScreen(
                language = settings.language,
                onToggleLanguage = {
                    val next = if (settings.language == AppLanguage.ENGLISH) {
                        AppLanguage.BENGALI
                    } else {
                        AppLanguage.ENGLISH
                    }
                    viewModel.setLanguage(next)
                },
                onFinishOnboarding = viewModel::completeOnboarding
            )
        }

        SplashIntroOverlay(visible = showSplash)
    }

    if (showImportChooserDialog) {
        AlertDialog(
            onDismissRequest = { showImportChooserDialog = false },
            title = { Text(strings.setNewWallpaper) },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text(strings.emptyLibrarySubtitle)
                    Button(
                        onClick = {
                            showImportChooserDialog = false
                            launchImportVideo()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.importVideo)
                    }
                    Button(
                        onClick = {
                            showImportChooserDialog = false
                            launchImportPhotos()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.importPhotos)
                    }
                    Button(
                        onClick = {
                            showImportChooserDialog = false
                            launchImportGif()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.importGif)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportChooserDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showDonationDialog) {
        DeveloperDonationDialog(
            donationNumber = WallpaperHelper.DONATION_NUMBER,
            onCopyNumber = {
                WallpaperHelper.copyDonationNumber(context)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(strings.numberCopiedToast)
                }
            },
            onOpenBkash = {
                val opened = WallpaperHelper.openDonationApp(context, WallpaperHelper.BKASH_PACKAGE)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (opened) strings.numberCopiedToast else strings.appNotInstalledCopied
                    )
                }
            },
            onOpenNagad = {
                val opened = WallpaperHelper.openDonationApp(context, WallpaperHelper.NAGAD_PACKAGE)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (opened) strings.numberCopiedToast else strings.appNotInstalledCopied
                    )
                }
            },
            onDismiss = { showDonationDialog = false }
        )
    }
}
