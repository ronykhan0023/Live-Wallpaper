package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.R
import com.example.domain.model.WallpaperItem
import com.example.ui.components.FrostedBarSurface
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.WallpaperMediaCard
import com.example.ui.i18n.LocalAppStrings

@Composable
fun FavoritesScreen(
    favorites: List<WallpaperItem>,
    onBrowseLibrary: () -> Unit,
    onPreviewClick: (Long) -> Unit,
    onApplyClick: (WallpaperItem) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onReplace: (WallpaperItem) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var renameTarget by remember { mutableStateOf<WallpaperItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<WallpaperItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        FrostedBarSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = strings.favorites,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(contentPadding = PaddingValues(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_vault),
                            contentDescription = strings.emptyFavoritesTitle,
                            modifier = Modifier
                                .size(170.dp)
                                .clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.emptyFavoritesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.emptyFavoritesSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientPrimaryButton(
                            text = strings.wallpaperLibrary,
                            icon = Icons.Filled.PhotoLibrary,
                            onClick = onBrowseLibrary,
                            testTag = "favorites_browse_library_btn"
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.id }) { item ->
                    WallpaperMediaCard(
                        item = item,
                        onPreviewClick = { onPreviewClick(item.id) },
                        onApplyClick = { onApplyClick(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onRenameClick = {
                            renameTarget = item
                            renameText = item.title
                        },
                        onReplaceClick = { onReplace(item) },
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
                    singleLine = true
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
