package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.MediaType
import com.example.domain.model.SpeedPresets
import com.example.domain.model.StorageSummary
import com.example.domain.model.ThemeMode
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperPlaylist
import com.example.ui.components.DeveloperConnectSupportCard
import com.example.ui.components.FrostedBarSurface
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.LiveMediaPreview
import com.example.ui.components.RonyBrandMark
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RonyGradients
import com.example.ui.theme.SapphireBlue
import com.example.util.WallpaperHelper
import java.io.File

@Composable
fun HomeScreen(
    activeWallpaper: WallpaperItem?,
    activePlaylist: WallpaperPlaylist?,
    playlistCount: Int,
    isSystemWallpaperActive: Boolean,
    recentlyUsed: List<WallpaperItem>,
    favorites: List<WallpaperItem>,
    totalLibraryCount: Int,
    storageSummary: StorageSummary,
    settings: AppSettings,
    onImportVideo: () -> Unit,
    onImportGif: () -> Unit,
    onImportPhotos: () -> Unit,
    onSetNewWallpaperClick: () -> Unit,
    onOpenPreview: (Long) -> Unit,
    onApplyWallpaperDirect: (WallpaperItem) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onSkipPlaylistNext: () -> Unit,
    onStopPlaylist: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onConnectWhatsApp: () -> Unit,
    onOpenSupportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand Header
        item {
            FrostedBarSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RonyBrandMark(modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.slogan,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = if (isSystemWallpaperActive) {
                            EmeraldLive.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        },
                        contentColor = if (isSystemWallpaperActive) {
                            EmeraldLive
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSystemWallpaperActive) EmeraldLive else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSystemWallpaperActive) EmeraldLive else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSystemWallpaperActive) {
                                    strings.systemLiveActive
                                } else {
                                    strings.systemLiveInactive
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Current Live Wallpaper Preview Hero Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.currentLiveWallpaper,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeWallpaper != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${activeWallpaper.mediaType.name} • ${SpeedPresets.format(activeWallpaper.playbackSpeed)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ObsidianBlack)
                        .border(
                            1.dp,
                            RonyGradients.PurpleCyanAccent,
                            MaterialTheme.shapes.medium
                        )
                        .clickable {
                            if (activeWallpaper != null) {
                                onOpenPreview(activeWallpaper.id)
                            } else {
                                onSetNewWallpaperClick()
                            }
                        }
                        .testTag("hero_active_preview")
                ) {
                    if (activeWallpaper != null && File(activeWallpaper.filePath).exists()) {
                        LiveMediaPreview(
                            filePath = activeWallpaper.filePath,
                            mediaType = activeWallpaper.mediaType,
                            playbackSpeed = activeWallpaper.playbackSpeed,
                            isLooping = activeWallpaper.isLooping,
                            isMuted = true,
                            brightness = activeWallpaper.brightness,
                            scaleMode = activeWallpaper.scaleMode,
                            zoom = activeWallpaper.zoom,
                            offsetX = activeWallpaper.offsetX,
                            offsetY = activeWallpaper.offsetY,
                            trimStartMs = activeWallpaper.trimStartMs,
                            trimEndMs = activeWallpaper.trimEndMs,
                            isPlaying = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_aurora),
                            contentDescription = strings.currentLiveWallpaper,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = activeWallpaper?.title ?: strings.noActiveWallpaperTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeWallpaper != null) {
                                "${activeWallpaper.width}×${activeWallpaper.height} • ${WallpaperHelper.formatFileSize(activeWallpaper.fileSizeBytes)}"
                            } else {
                                strings.noActiveWallpaperSubtitle
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeWallpaper != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenPreview(activeWallpaper.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hero_customize_btn"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.previewAndCustomize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        GradientPrimaryButton(
                            text = strings.applyNow,
                            icon = Icons.Filled.CheckCircle,
                            onClick = { onApplyWallpaperDirect(activeWallpaper) },
                            modifier = Modifier.weight(1.2f),
                            testTag = "hero_apply_btn"
                        )
                    }
                } else {
                    GradientPrimaryButton(
                        text = strings.setNewWallpaper,
                        icon = Icons.Filled.Wallpaper,
                        onClick = onSetNewWallpaperClick,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "set_new_wallpaper_btn"
                    )
                }
            }
        }

        // Import Action Row (Videos, GIFs, Photos)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImportActionTile(
                    title = strings.importVideo,
                    subtitle = "MP4 • MKV",
                    icon = Icons.Filled.VideoLibrary,
                    accent = SapphireBlue,
                    onClick = onImportVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_video_btn")
                )
                ImportActionTile(
                    title = strings.importGif,
                    subtitle = "GIF",
                    icon = Icons.Filled.GifBox,
                    accent = ElectricViolet,
                    onClick = onImportGif,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_gif_btn")
                )
                ImportActionTile(
                    title = strings.importPhotos,
                    subtitle = "JPG • PNG",
                    icon = Icons.Filled.Image,
                    accent = CyanGlow,
                    onClick = onImportPhotos,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_photos_btn")
                )
            }
        }

        // Wallpaper Playlists Card (Time-Based Auto-Rotation)
        item {
            GlassCard(
                onClick = onOpenPlaylists,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("home_playlists_card"),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (activePlaylist != null) EmeraldLive.copy(alpha = 0.18f)
                                    else ElectricViolet.copy(alpha = 0.18f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistPlay,
                                contentDescription = strings.playlistsTitle,
                                tint = if (activePlaylist != null) EmeraldLive else ElectricViolet
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activePlaylist?.name ?: strings.playlistsTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activePlaylist != null) {
                                    "${activePlaylist.intervalValue} ${unitLabel(activePlaylist.intervalUnit, strings)} • ${activePlaylist.currentIndex + 1}/${activePlaylist.items.size}"
                                } else {
                                    strings.playlistsSubtitle
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (activePlaylist != null) {
                        Surface(
                            color = EmeraldLive.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLive)
                        ) {
                            Text(
                                text = strings.activePlaylistBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLive,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = strings.itemsCount(playlistCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (activePlaylist != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSkipPlaylistNext,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onStopPlaylist,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.deactivatePlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Quick Settings Strip
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = strings.quickSettings,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.batterySaverEnabled,
                        onClick = { onToggleBatterySaver(!settings.batterySaverEnabled) },
                        label = { Text(strings.batterySaverLabel, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.BatterySaver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("quick_battery_saver")
                    )

                    FilterChip(
                        selected = false,
                        onClick = onToggleTheme,
                        label = {
                            Text(
                                if (settings.themeMode == ThemeMode.LIGHT) strings.themeLight else strings.themeDark,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (settings.themeMode == ThemeMode.LIGHT) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("quick_theme_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.language == AppLanguage.BENGALI,
                        onClick = onToggleLanguage,
                        label = {
                            Text(
                                if (settings.language == AppLanguage.BENGALI) "বাংলা" else "English",
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("quick_language_toggle")
                    )

                    FilterChip(
                        selected = false,
                        onClick = onOpenSettings,
                        label = {
                            Text(
                                "${strings.defaultSpeedLabel}: ${SpeedPresets.format(settings.defaultSpeed)}",
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("quick_speed_settings")
                    )
                }
            }
        }

        // Recently Used Carousel
        if (recentlyUsed.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    SectionHeader(
                        title = strings.recentlyUsed,
                        actionLabel = strings.viewAll,
                        onActionClick = onOpenLibrary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentlyUsed, key = { it.id }) { item ->
                            CompactWallpaperThumb(
                                item = item,
                                onClick = { onOpenPreview(item.id) }
                            )
                        }
                    }
                }
            }
        }

        // Quick Library & Favorites Summary Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_library_card"),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = strings.wallpaperLibrary,
                        tint = SapphireBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.wallpaperLibrary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.itemsCount(totalLibraryCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlassCard(
                    onClick = onOpenFavorites,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_favorites_card"),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = strings.favorites,
                        tint = ElectricViolet
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.favorites,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.itemsCount(favorites.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Storage Usage Indicator
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = CyanGlow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.storageUsage,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_manage_storage_btn")
                    ) {
                        Text(strings.manageStorage)
                    }
                }

                val total = storageSummary.totalBytes.coerceAtLeast(1L).toFloat()
                val videoRatio = (storageSummary.videoBytes / total).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { if (storageSummary.totalBytes == 0L) 0f else videoRatio.coerceAtLeast(0.08f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SapphireBlue,
                    trackColor = ElectricViolet.copy(alpha = 0.25f)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${strings.videosLabel}: ${WallpaperHelper.formatFileSize(storageSummary.videoBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.photosLabel}: ${WallpaperHelper.formatFileSize(storageSummary.imageBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.totalUsed}: ${WallpaperHelper.formatFileSize(storageSummary.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Connect with Developer (WhatsApp) & Support Developer (bKash / Nagad)
        item {
            DeveloperConnectSupportCard(
                onConnectWhatsApp = onConnectWhatsApp,
                onOpenSupportDialog = onOpenSupportDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            )
        }
    }
}

@Composable
private fun ImportActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = accent)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    FrostedBarSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun CompactWallpaperThumb(
    item: WallpaperItem,
    onClick: () -> Unit
) {
    val thumbFile = remember(item.thumbnailPath, item.filePath) {
        val t = File(item.thumbnailPath)
        if (t.exists()) t else File(item.filePath)
    }
    Surface(
        modifier = Modifier
            .width(108.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBlack)
            ) {
                AsyncImage(
                    model = thumbFile,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    color = if (item.mediaType == MediaType.GIF) ElectricViolet else SapphireBlue,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.mediaType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = SpeedPresets.format(item.playbackSpeed),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
