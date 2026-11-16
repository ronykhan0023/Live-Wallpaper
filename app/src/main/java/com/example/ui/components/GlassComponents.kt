package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.MediaType
import com.example.domain.model.SpeedPresets
import com.example.domain.model.WallpaperItem
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RonyGradients
import com.example.ui.theme.RosePulse
import com.example.ui.theme.SapphireBlue
import com.example.util.WallpaperHelper
import java.io.File

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    }
    val borderBrush = if (isDark) RonyGradients.GlassBorderDark else RonyGradients.GlassBorderLight

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = if (isDark) 2.dp else 4.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "gradient_button"
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RonyGradients.PurpleBluePrimary, MaterialTheme.shapes.medium)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RonyBrandMark(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "brandGlow")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Box(
        modifier = modifier
            .scale(pulse)
            .clip(RoundedCornerShape(14.dp))
            .background(RonyGradients.PurpleCyanAccent)
            .padding(2.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_rony_logo_photo),
            contentDescription = "Rony Live Wallpaper Logo",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DeveloperConnectSupportCard(
    onConnectWhatsApp: () -> Unit,
    onOpenSupportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        // Connect with Developer (WhatsApp - NO phone number shown!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onConnectWhatsApp)
                .padding(vertical = 4.dp)
                .testTag("connect_developer_whatsapp_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_whatsapp),
                contentDescription = strings.connectWithDeveloper,
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.connectWithDeveloper,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.connectWhatsAppSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = Color(0xFF25D366).copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF25D366))
            ) {
                Text(
                    text = "WhatsApp",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF25D366),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Support the Developer (bKash & Nagad Donation)
        Surface(
            onClick = onOpenSupportDialog,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("support_developer_card"),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = strings.supportDeveloperTitle,
                    tint = Color(0xFFE2136E),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.supportDeveloperTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.supportDeveloperSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_bkash_logo),
                            contentDescription = strings.bkashName,
                            modifier = Modifier
                                .height(28.dp)
                                .width(56.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nagad_logo),
                            contentDescription = strings.nagadName,
                            modifier = Modifier
                                .height(28.dp)
                                .width(56.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperDonationDialog(
    donationNumber: String,
    onCopyNumber: () -> Unit,
    onOpenBkash: () -> Unit,
    onOpenNagad: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    tint = Color(0xFFE2136E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.supportDeveloperTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings.supportDeveloperSubtitle,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Visible donation number card with 1-tap copy
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.personalNumberLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = donationNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onCopyNumber,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_donation_number_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = strings.copyNumber
                            )
                        }
                    }
                }

                // bKash & Nagad branded buttons with user-uploaded official logos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBkash,
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .testTag("donate_bkash_btn"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, Color(0xFFE2136E)),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_bkash_logo),
                                contentDescription = strings.bkashName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenNagad,
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .testTag("donate_nagad_btn"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, Color(0xFFF26522)),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_nagad_logo),
                                contentDescription = strings.nagadName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Text(
                    text = strings.donationInstructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCopyNumber,
                modifier = Modifier.testTag("dialog_copy_number_confirm")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.copyNumber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
fun WallpaperMediaCard(
    item: WallpaperItem,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    val borderStroke = if (item.isActive) {
        BorderStroke(2.dp, RonyGradients.PurpleCyanAccent)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onPreviewClick)
            .testTag("wallpaper_card_${item.id}"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = borderStroke,
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .background(ObsidianBlack)
            ) {
                val thumbFile = remember(item.thumbnailPath, item.filePath) {
                    val t = File(item.thumbnailPath)
                    if (t.exists()) t else File(item.filePath)
                }
                AsyncImage(
                    model = thumbFile,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top gradient scrim for badge readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Type badge (VIDEO / GIF / PHOTO)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    color = when (item.mediaType) {
                        MediaType.GIF -> ElectricViolet.copy(alpha = 0.88f)
                        MediaType.VIDEO -> SapphireBlue.copy(alpha = 0.88f)
                        MediaType.IMAGE -> CyanGlow.copy(alpha = 0.88f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (item.mediaType) {
                                MediaType.GIF -> Icons.Filled.GifBox
                                MediaType.VIDEO -> Icons.Filled.PlayArrow
                                MediaType.IMAGE -> Icons.Filled.Image
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (item.mediaType == MediaType.IMAGE) "PHOTO" else item.mediaType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Favorite button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("fav_toggle_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (item.isFavorite) strings.unfavorite else strings.favorite,
                        tint = if (item.isFavorite) RosePulse else Color.White
                    )
                }

                // Active pill or speed pill at bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = SpeedPresets.format(item.playbackSpeed),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    if (item.isActive) {
                        Surface(
                            color = EmeraldLive.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = strings.activeBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else if (item.durationMs > 0) {
                        Text(
                            text = WallpaperHelper.formatDuration(item.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${WallpaperHelper.formatFileSize(item.fileSizeBytes)} • ${item.width}×${item.height}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("card_menu_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.previewAndCustomize) },
                            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPreviewClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.applyWallpaper) },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onApplyClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.rename) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.delete,
                                    color = if (item.isActive) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = if (item.isActive) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashIntroOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(420)),
        modifier = modifier
    ) {
        val infinite = rememberInfiniteTransition(label = "splash")
        val glowScale by infinite.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF090714), ObsidianBlack, Color(0xFF060A17))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ElectricViolet.copy(alpha = 0.35f),
                                    SapphireBlue.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    RonyBrandMark(
                        modifier = Modifier
                            .size(96.dp)
                            .border(2.dp, RonyGradients.PurpleCyanAccent, RoundedCornerShape(16.dp))
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.slogan,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CyanGlow
                )
            }
        }
    }
}
