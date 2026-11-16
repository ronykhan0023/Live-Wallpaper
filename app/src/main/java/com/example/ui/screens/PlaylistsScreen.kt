package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.ScheduleUnit
import com.example.domain.model.SpeedPresets
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperPlaylist
import com.example.domain.model.WallpaperTarget
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.ItemCustomizerDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SapphireBlue
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistsScreen(
    playlists: List<WallpaperPlaylist>,
    activePlaylist: WallpaperPlaylist?,
    allWallpapers: List<WallpaperItem>,
    preferredTarget: WallpaperTarget,
    onBack: () -> Unit,
    onImportPhotos: () -> Unit,
    onImportVideos: () -> Unit,
    onImportGifs: () -> Unit,
    onCreateAndActivatePlaylist: (
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean,
        target: WallpaperTarget
    ) -> Unit,
    onUpdatePlaylist: (
        playlistId: Long,
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean
    ) -> Unit,
    onSaveItemCustomization: (
        id: Long,
        speed: Float,
        isLooping: Boolean,
        isMuted: Boolean,
        brightness: Float,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        trimStartMs: Long,
        trimEndMs: Long
    ) -> Unit,
    onActivatePlaylist: (Long, WallpaperTarget) -> Unit,
    onDeactivatePlaylist: () -> Unit,
    onSkipNext: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<WallpaperPlaylist?>(null) }
    var customizingItemId by remember { mutableStateOf<Long?>(null) }
    val customizingItem = remember(customizingItemId, allWallpapers) {
        allWallpapers.find { it.id == customizingItemId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("playlists_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.playlistsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.playlistsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Multi-import bar for bulk selection of photos, videos, GIFs
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onImportPhotos,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("playlist_import_photos_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.importPhotos, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onImportVideos,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("playlist_import_videos_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.importVideo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onImportGifs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("playlist_import_gifs_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.GifBox, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.importGif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GradientPrimaryButton(
                        text = strings.createPlaylist,
                        icon = Icons.Filled.PlaylistPlay,
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "open_create_playlist_btn"
                    )
                }
            }

            // Active Playlist Status Card
            if (activePlaylist != null) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .testTag("active_playlist_card"),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = EmeraldLive.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EmeraldLive)
                            ) {
                                Text(
                                    text = strings.activePlaylistBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLive,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activePlaylist.currentIndex + 1}/${activePlaylist.items.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { editingPlaylist = activePlaylist },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_active_playlist_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = strings.editPlaylist,
                                        tint = CyanGlow
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activePlaylist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val nextEpoch = activePlaylist.intervalUnit.computeNextSwitchEpochMs(
                            activePlaylist.lastSwitchEpochMs,
                            activePlaylist.intervalValue
                        )
                        val nextFormatted = remember(nextEpoch) {
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(nextEpoch))
                        }
                        Text(
                            text = "${activePlaylist.intervalValue} ${unitLabel(activePlaylist.intervalUnit, strings)} • ${strings.nextChangeLabel}: $nextFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("playlist_skip_next_btn")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Next", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onDeactivatePlaylist,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("playlist_stop_btn")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.deactivatePlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_vault),
                                contentDescription = strings.emptyPlaylistsTitle,
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(MaterialTheme.shapes.large),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = strings.emptyPlaylistsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.emptyPlaylistsSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        strings = strings,
                        onActivate = { onActivatePlaylist(playlist.id, preferredTarget) },
                        onDeactivate = onDeactivatePlaylist,
                        onEdit = { editingPlaylist = playlist },
                        onCustomizeItem = { item -> customizingItemId = item.id },
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistEditorDialog(
            existingPlaylist = null,
            allWallpapers = allWallpapers,
            initialTarget = preferredTarget,
            strings = strings,
            onImportPhotos = onImportPhotos,
            onImportVideos = onImportVideos,
            onImportGifs = onImportGifs,
            onCustomizeItem = { item -> customizingItemId = item.id },
            onDismiss = { showCreateDialog = false },
            onConfirmCreate = { name, ids, intervalVal, unit, shuffle, target ->
                showCreateDialog = false
                onCreateAndActivatePlaylist(name, ids, intervalVal, unit, shuffle, target)
            },
            onConfirmUpdate = { _, _, _, _, _, _ -> }
        )
    }

    editingPlaylist?.let { targetPlaylist ->
        PlaylistEditorDialog(
            existingPlaylist = targetPlaylist,
            allWallpapers = allWallpapers,
            initialTarget = preferredTarget,
            strings = strings,
            onImportPhotos = onImportPhotos,
            onImportVideos = onImportVideos,
            onImportGifs = onImportGifs,
            onCustomizeItem = { item -> customizingItemId = item.id },
            onDismiss = { editingPlaylist = null },
            onConfirmCreate = { _, _, _, _, _, _ -> },
            onConfirmUpdate = { playlistId, name, ids, intervalVal, unit, shuffle ->
                editingPlaylist = null
                onUpdatePlaylist(playlistId, name, ids, intervalVal, unit, shuffle)
            }
        )
    }

    customizingItem?.let { itemToCustomize ->
        ItemCustomizerDialog(
            wallpaper = itemToCustomize,
            onDismiss = { customizingItemId = null },
            onSave = { id, speed, looping, muted, brightness, scale, zoom, ox, oy, trimStart, trimEnd ->
                onSaveItemCustomization(
                    id,
                    speed,
                    looping,
                    muted,
                    brightness,
                    scale,
                    zoom,
                    ox,
                    oy,
                    trimStart,
                    trimEnd
                )
                customizingItemId = null
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: WallpaperPlaylist,
    strings: AppStrings,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onEdit: () -> Unit,
    onCustomizeItem: (WallpaperItem) -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("playlist_card_${playlist.id}"),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CyanGlow
                    )
                    Text(
                        text = "${playlist.intervalValue} ${unitLabel(playlist.intervalUnit, strings)} • ${strings.itemsCount(playlist.items.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (playlist.isShuffle) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = strings.shuffleOrder,
                            modifier = Modifier.size(14.dp),
                            tint = ElectricViolet
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = strings.editPlaylist,
                        tint = CyanGlow
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_playlist_${playlist.id}")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.delete)
                }
            }
        }

        if (playlist.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlist.items, key = { it.id }) { item ->
                    val thumbFile = remember(item.thumbnailPath, item.filePath) {
                        val t = File(item.thumbnailPath)
                        if (t.exists()) t else File(item.filePath)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBlack)
                            .clickable { onCustomizeItem(item) }
                            .testTag("playlist_card_item_${playlist.id}_${item.id}")
                    ) {
                        AsyncImage(
                            model = thumbFile,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = strings.adjustBtnLabel,
                                tint = CyanGlow,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(3.dp)
                        ) {
                            Text(
                                text = when (item.mediaType) {
                                    MediaType.VIDEO -> "VID"
                                    MediaType.GIF -> "GIF"
                                    MediaType.IMAGE -> "IMG"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.tapAdjustHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_playlist_btn_${playlist.id}")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.editPlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (playlist.isActive) {
                OutlinedButton(
                    onClick = onDeactivate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.deactivatePlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Button(
                    onClick = onActivate,
                    enabled = playlist.items.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("activate_playlist_${playlist.id}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.activatePlaylist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistEditorDialog(
    existingPlaylist: WallpaperPlaylist?,
    allWallpapers: List<WallpaperItem>,
    initialTarget: WallpaperTarget,
    strings: AppStrings,
    onImportPhotos: () -> Unit,
    onImportVideos: () -> Unit,
    onImportGifs: () -> Unit,
    onCustomizeItem: (WallpaperItem) -> Unit,
    onDismiss: () -> Unit,
    onConfirmCreate: (
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean,
        target: WallpaperTarget
    ) -> Unit,
    onConfirmUpdate: (
        playlistId: Long,
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean
    ) -> Unit
) {
    val isEditing = existingPlaylist != null
    var name by remember(existingPlaylist?.id) {
        mutableStateOf(existingPlaylist?.name ?: "")
    }
    var intervalValue by remember(existingPlaylist?.id) {
        mutableIntStateOf(existingPlaylist?.intervalValue ?: 1)
    }
    var intervalUnit by remember(existingPlaylist?.id) {
        mutableStateOf(existingPlaylist?.intervalUnit ?: ScheduleUnit.DAYS)
    }
    var isShuffle by remember(existingPlaylist?.id) {
        mutableStateOf(existingPlaylist?.isShuffle ?: false)
    }
    var target by remember { mutableStateOf(initialTarget) }

    val selectedIds = remember(existingPlaylist?.id) {
        mutableStateListOf<Long>().apply {
            if (existingPlaylist != null) {
                addAll(existingPlaylist.items.map { it.id })
            }
        }
    }
    val initialLibraryIds = remember(existingPlaylist?.id) {
        allWallpapers.map { it.id }.toMutableSet()
    }

    // Auto-select newly imported photos/videos/GIFs added while editing or creating
    LaunchedEffect(allWallpapers) {
        allWallpapers.forEach { item ->
            if (initialLibraryIds.add(item.id) && !selectedIds.contains(item.id)) {
                selectedIds.add(item.id)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) strings.editPlaylist else strings.createPlaylist,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistNameHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )

                Text(
                    text = strings.selectPlaylistItems,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Always allow quick importing new photos, videos, or GIFs straight into this playlist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportPhotos,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_import_photos_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(strings.filterPhotos, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onImportVideos,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_import_videos_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(strings.filterVideos, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onImportGifs,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_import_gifs_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(strings.filterGifs, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (allWallpapers.isEmpty()) {
                    Text(
                        text = strings.emptyLibrarySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size}/${allWallpapers.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanGlow
                        )
                        TextButton(
                            onClick = {
                                if (selectedIds.size == allWallpapers.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(allWallpapers.map { it.id })
                                }
                            }
                        ) {
                            Text(if (selectedIds.size == allWallpapers.size) "Clear" else "Select All")
                        }
                    }

                    Text(
                        text = strings.tapAdjustHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allWallpapers, key = { it.id }) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            val orderIndex = selectedIds.indexOf(item.id)
                            val thumbFile = remember(item.thumbnailPath, item.filePath) {
                                val t = File(item.thumbnailPath)
                                if (t.exists()) t else File(item.filePath)
                            }
                            Column(
                                modifier = Modifier.width(80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(9f / 13f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianBlack)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                        }
                                        .testTag("playlist_select_item_${item.id}")
                                ) {
                                    AsyncImage(
                                        model = thumbFile,
                                        contentDescription = item.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(SapphireBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${orderIndex + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = when (item.mediaType) {
                                                MediaType.VIDEO -> "VID"
                                                MediaType.GIF -> "GIF"
                                                MediaType.IMAGE -> "IMG"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    if (item.zoom > 1.02f || item.scaleMode != ScaleMode.CROP || kotlin.math.abs(item.playbackSpeed - 1f) > 0.02f || item.trimStartMs > 0L) {
                                        Surface(
                                            color = SapphireBlue.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = if (item.mediaType == MediaType.IMAGE) {
                                                    "${((item.zoom * 10).toInt() / 10f)}x"
                                                } else {
                                                    SpeedPresets.format(item.playbackSpeed)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    onClick = { onCustomizeItem(item) },
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_adjust_item_${item.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Tune,
                                            contentDescription = strings.adjustBtnLabel,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = strings.adjustBtnLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interval Stepper & Unit Chips (Seconds / Minutes / Hours / Days / Months)
                Text(
                    text = strings.changeEveryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (intervalValue > 1) intervalValue-- },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$intervalValue ${unitLabel(intervalUnit, strings)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { if (intervalValue < 365) intervalValue++ },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 5, 10, 15, 30).forEach { preset ->
                        FilterChip(
                            selected = intervalValue == preset,
                            onClick = { intervalValue = preset },
                            label = { Text("$preset") }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScheduleUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = intervalUnit == unit,
                            onClick = { intervalUnit = unit },
                            label = { Text(unitLabel(unit, strings)) },
                            modifier = Modifier.testTag("playlist_unit_${unit.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.shuffleOrder, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isShuffle,
                        onCheckedChange = { isShuffle = it }
                    )
                }

                if (!isEditing) {
                    Text(
                        text = strings.targetScreenTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            WallpaperTarget.HOME to strings.targetHome,
                            WallpaperTarget.LOCK to strings.targetLock,
                            WallpaperTarget.BOTH to strings.targetBoth
                        ).forEach { (itemTarget, label) ->
                            FilterChip(
                                selected = target == itemTarget,
                                onClick = { target = itemTarget },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (existingPlaylist != null) {
                        onConfirmUpdate(
                            existingPlaylist.id,
                            name.ifBlank { existingPlaylist.name },
                            selectedIds.toList(),
                            intervalValue,
                            intervalUnit,
                            isShuffle
                        )
                    } else {
                        onConfirmCreate(
                            name.ifBlank { "Playlist $intervalValue ${unitLabel(intervalUnit, strings)}" },
                            selectedIds.toList(),
                            intervalValue,
                            intervalUnit,
                            isShuffle,
                            target
                        )
                    }
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.testTag("playlist_create_apply_btn")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEditing) strings.saveChanges else strings.activatePlaylist)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

fun unitLabel(unit: ScheduleUnit, strings: AppStrings): String = when (unit) {
    ScheduleUnit.SECONDS -> strings.unitSeconds
    ScheduleUnit.MINUTES -> strings.unitMinutes
    ScheduleUnit.HOURS -> strings.unitHours
    ScheduleUnit.DAYS -> strings.unitDays
    ScheduleUnit.MONTHS -> strings.unitMonths
}
