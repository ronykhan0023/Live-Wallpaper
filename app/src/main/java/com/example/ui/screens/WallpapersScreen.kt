package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.MediaFilter
import com.example.domain.model.SortOption
import com.example.domain.model.WallpaperItem
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.WallpaperMediaCard
import com.example.ui.i18n.LocalAppStrings

@Composable
fun WallpapersScreen(
    wallpapers: List<WallpaperItem>,
    searchQuery: String,
    mediaFilter: MediaFilter,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onMediaFilterChange: (MediaFilter) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onImportVideo: () -> Unit,
    onImportGif: () -> Unit,
    onImportPhotos: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onPreviewClick: (Long) -> Unit,
    onApplyClick: (WallpaperItem) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WallpaperItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<WallpaperItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = strings.wallpaperLibrary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOpenPlaylists,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("library_playlists_btn")
            ) {
                Icon(Icons.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.playlistsTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Box {
                OutlinedButton(
                    onClick = { sortMenuOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sort_button")
                ) {
                    Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (sortOption) {
                            SortOption.DATE_DESC -> strings.sortNewest
                            SortOption.NAME_ASC -> strings.sortName
                            SortOption.SIZE_DESC -> strings.sortSize
                        },
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.sortNewest) },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_DESC)
                            sortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.sortName) },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_ASC)
                            sortMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.sortSize) },
                        onClick = {
                            onSortOptionChange(SortOption.SIZE_DESC)
                            sortMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mediaFilter == MediaFilter.ALL,
                onClick = { onMediaFilterChange(MediaFilter.ALL) },
                label = { Text(strings.filterAll) },
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = mediaFilter == MediaFilter.VIDEOS,
                onClick = { onMediaFilterChange(MediaFilter.VIDEOS) },
                label = { Text(strings.filterVideos) },
                modifier = Modifier.testTag("filter_videos")
            )
            FilterChip(
                selected = mediaFilter == MediaFilter.GIFS,
                onClick = { onMediaFilterChange(MediaFilter.GIFS) },
                label = { Text(strings.filterGifs) },
                modifier = Modifier.testTag("filter_gifs")
            )
            FilterChip(
                selected = mediaFilter == MediaFilter.PHOTOS,
                onClick = { onMediaFilterChange(MediaFilter.PHOTOS) },
                label = { Text(strings.filterPhotos) },
                modifier = Modifier.testTag("filter_photos")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (wallpapers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_vault),
                        contentDescription = strings.emptyLibraryTitle,
                        modifier = Modifier
                            .size(190.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = strings.emptyLibraryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.emptyLibrarySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientPrimaryButton(
                            text = strings.importVideo,
                            icon = Icons.Filled.VideoLibrary,
                            onClick = onImportVideo,
                            modifier = Modifier.weight(1f, fill = false),
                            testTag = "empty_import_video"
                        )
                        OutlinedButton(
                            onClick = onImportGif,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("empty_import_gif"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.GifBox, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.importGif)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onImportPhotos,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("empty_import_photos"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.importPhotos)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(wallpapers, key = { it.id }) { item ->
                    WallpaperMediaCard(
                        item = item,
                        onPreviewClick = { onPreviewClick(item.id) },
                        onApplyClick = { onApplyClick(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onRenameClick = {
                            renameTarget = item
                            renameText = item.title
                        },
                        onDeleteClick = { deleteTarget = item }
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.renameWallpaperTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(strings.wallpaperNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(target.id, renameText)
                        renameTarget = null
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(target.id)
                        deleteTarget = null
                    }
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
