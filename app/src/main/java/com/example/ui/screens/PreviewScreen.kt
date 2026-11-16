package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.SpeedCategory
import com.example.domain.model.SpeedPresets
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperTarget
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.LiveMediaPreview
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RonyGradients
import com.example.ui.theme.RosePulse
import com.example.ui.theme.SapphireBlue
import com.example.util.WallpaperHelper
import kotlin.math.roundToInt

private enum class PreviewTab { SPEED, TRIM, FRAMING, APPLY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewScreen(
    wallpaper: WallpaperItem?,
    initialTarget: WallpaperTarget,
    onBack: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onSaveCustomization: (
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
    onApplyWallpaper: (
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
        trimEndMs: Long,
        target: WallpaperTarget
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current

    if (wallpaper == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(text = strings.noActiveWallpaperTitle, color = Color.White)
        }
        return
    }

    var speed by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.playbackSpeed) }
    var speedInputText by remember(wallpaper.id) {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", wallpaper.playbackSpeed))
    }
    var isLooping by remember(wallpaper.id) { mutableStateOf(wallpaper.isLooping) }
    var isMuted by remember(wallpaper.id) { mutableStateOf(wallpaper.isMuted) }
    var brightness by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.brightness) }
    var scaleMode by remember(wallpaper.id) { mutableStateOf(wallpaper.scaleMode) }
    var zoom by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.zoom) }
    var offsetX by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.offsetX) }
    var offsetY by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.offsetY) }
    var trimStartMs by remember(wallpaper.id) { mutableLongStateOf(wallpaper.trimStartMs.coerceAtLeast(0L)) }
    var trimEndMs by remember(wallpaper.id) {
        mutableLongStateOf(
            if (wallpaper.trimEndMs > 0L) wallpaper.trimEndMs else wallpaper.durationMs
        )
    }
    var target by remember(wallpaper.id) { mutableStateOf(initialTarget) }

    var isPlaying by remember { mutableStateOf(true) }
    var showPhoneFrame by remember { mutableStateOf(true) }
    val availableTabs = remember(wallpaper.mediaType) {
        when (wallpaper.mediaType) {
            MediaType.VIDEO -> listOf(PreviewTab.SPEED, PreviewTab.TRIM, PreviewTab.FRAMING, PreviewTab.APPLY)
            MediaType.GIF -> listOf(PreviewTab.SPEED, PreviewTab.FRAMING, PreviewTab.APPLY)
            MediaType.IMAGE -> listOf(PreviewTab.FRAMING, PreviewTab.APPLY)
        }
    }
    var selectedTabIndex by remember(wallpaper.id) { mutableIntStateOf(0) }
    val activeTab = availableTabs.getOrElse(selectedTabIndex) { availableTabs.first() }

    // Persist edits reactively so active wallpaper updates live too
    LaunchedEffect(
        wallpaper.id,
        speed,
        isLooping,
        isMuted,
        brightness,
        scaleMode,
        zoom,
        offsetX,
        offsetY,
        trimStartMs,
        trimEndMs
    ) {
        onSaveCustomization(
            wallpaper.id,
            speed,
            isLooping,
            isMuted,
            brightness,
            scaleMode,
            zoom,
            offsetX,
            offsetY,
            trimStartMs,
            trimEndMs
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Studio Header
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
                    .testTag("preview_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${wallpaper.mediaType.name} • ${SpeedPresets.format(speed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            IconButton(
                onClick = { showPhoneFrame = !showPhoneFrame },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("preview_frame_toggle")
            ) {
                Icon(
                    imageVector = if (showPhoneFrame) Icons.Filled.Fullscreen else Icons.Filled.PhoneAndroid,
                    contentDescription = if (showPhoneFrame) strings.fullScreenPreview else strings.framePhonePreview
                )
            }

            IconButton(
                onClick = { onToggleFavorite(wallpaper.id) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("preview_favorite_btn")
            ) {
                Icon(
                    imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (wallpaper.isFavorite) strings.unfavorite else strings.favorite,
                    tint = if (wallpaper.isFavorite) RosePulse else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Live Interactive Wallpaper Canvas (Realistic Phone Frame or Full Bleed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = if (showPhoneFrame) 24.dp else 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val frameModifier = if (showPhoneFrame) {
                Modifier
                    .aspectRatio(9f / 18.5f)
                    .clip(RoundedCornerShape(32.dp))
                    .border(3.dp, RonyGradients.PurpleCyanAccent, RoundedCornerShape(32.dp))
            } else {
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            }

            Box(modifier = frameModifier.background(ObsidianBlack)) {
                LiveMediaPreview(
                    filePath = wallpaper.filePath,
                    mediaType = wallpaper.mediaType,
                    playbackSpeed = speed,
                    isLooping = isLooping,
                    isMuted = isMuted,
                    brightness = brightness,
                    scaleMode = scaleMode,
                    zoom = zoom,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    isPlaying = isPlaying,
                    trimStartMs = trimStartMs,
                    trimEndMs = trimEndMs,
                    allowDragPosition = true,
                    onOffsetChange = { nx, ny ->
                        offsetX = nx
                        offsetY = ny
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Realistic Lock/Home Clock Mock Overlay when in Phone Frame mode
                if (showPhoneFrame) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "09:41",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Playback Quick Controls Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("preview_play_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) strings.pause else strings.play,
                                    tint = Color.White
                                )
                            }
                        }

                        if (wallpaper.mediaType == MediaType.VIDEO) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = CircleShape
                            ) {
                                IconButton(
                                    onClick = { isMuted = !isMuted },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("preview_mute_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = strings.muteAudio,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    val categoryText = when (SpeedPresets.classify(speed)) {
                        SpeedCategory.SLOW_MOTION -> strings.slowMotion
                        SpeedCategory.NORMAL -> strings.normalSpeed
                        SpeedCategory.FAST_MOTION -> strings.fastMotion
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "${SpeedPresets.format(speed)} • $categoryText",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Studio Customization Deck
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex.coerceIn(0, availableTabs.lastIndex),
                containerColor = Color.Transparent
            ) {
                availableTabs.forEachIndexed { idx, tab ->
                    val (label, icon, tag) = when (tab) {
                        PreviewTab.SPEED -> Triple(strings.speedControl, Icons.Filled.Speed, "preview_tab_speed")
                        PreviewTab.TRIM -> Triple(strings.trimCutTitle, Icons.Filled.ContentCut, "preview_tab_trim")
                        PreviewTab.FRAMING -> Triple(strings.scaleModeTitle, Icons.Filled.AspectRatio, "preview_tab_framing")
                        PreviewTab.APPLY -> Triple(strings.applyWallpaper, Icons.Filled.Tune, "preview_tab_apply")
                    }
                    Tab(
                        selected = idx == selectedTabIndex,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeTab) {
                    PreviewTab.SPEED -> {
                        val applySpeedValue: (Float) -> Unit = { v ->
                            val clamped = SpeedPresets.clamp(v)
                            speed = clamped
                            speedInputText = String.format(java.util.Locale.US, "%.2f", clamped)
                        }
                        val commitManualSpeed: () -> Unit = {
                            val parsed = speedInputText.toFloatOrNull()
                            val clamped = if (parsed != null) SpeedPresets.clamp(parsed) else speed
                            speed = clamped
                            speedInputText = String.format(java.util.Locale.US, "%.2f", clamped)
                            focusManager.clearFocus()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.speedPresetsTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { applySpeedValue(SpeedPresets.DEFAULT_SPEED) },
                                modifier = Modifier.testTag("reset_speed_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.resetSpeed)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = speedInputText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 5) {
                                        speedInputText = filtered
                                        filtered.toFloatOrNull()?.let { parsed ->
                                            if (parsed in SpeedPresets.MIN_SPEED..SpeedPresets.MAX_SPEED) {
                                                speed = SpeedPresets.clamp(parsed)
                                            }
                                        }
                                    }
                                },
                                label = { Text(strings.customSpeedInputLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                suffix = { Text("x") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { commitManualSpeed() }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preview_manual_speed_input")
                            )
                            FilledTonalButton(
                                onClick = commitManualSpeed,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("preview_manual_speed_apply"),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = strings.applyLabel, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.applyLabel, maxLines = 1)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SpeedPresets.values.forEach { preset ->
                                val selected = kotlin.math.abs(speed - preset) < 0.015f
                                FilterChip(
                                    selected = selected,
                                    onClick = { applySpeedValue(preset) },
                                    label = { Text(SpeedPresets.format(preset)) },
                                    modifier = Modifier.testTag("speed_preset_${preset}")
                                )
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.customSpeedSlider,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = SpeedPresets.format(speed),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = speed,
                                onValueChange = applySpeedValue,
                                valueRange = SpeedPresets.MIN_SPEED..SpeedPresets.MAX_SPEED,
                                modifier = Modifier.testTag("custom_speed_slider")
                            )
                        }
                    }

                    PreviewTab.TRIM -> {
                        val totalDurationMs = wallpaper.durationMs.coerceAtLeast(1000L)
                        val effectiveEndMs = trimEndMs.coerceIn(
                            (trimStartMs + 500L).coerceAtMost(totalDurationMs),
                            totalDurationMs
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.trimCutTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    trimStartMs = 0L
                                    trimEndMs = wallpaper.durationMs
                                },
                                modifier = Modifier.testTag("reset_trim_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.resetFullVideo)
                            }
                        }

                        Text(
                            text = strings.trimUnlimitedHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (wallpaper.durationMs > 1000L) {
                            RangeSlider(
                                value = trimStartMs.toFloat().coerceIn(0f, (totalDurationMs - 500L).toFloat())..effectiveEndMs.toFloat(),
                                onValueChange = { range ->
                                    val minGap = 500L
                                    val newStart = range.start.toLong().coerceIn(0L, totalDurationMs - minGap)
                                    val newEnd = range.endInclusive.toLong().coerceIn(newStart + minGap, totalDurationMs)
                                    trimStartMs = newStart
                                    trimEndMs = newEnd
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                modifier = Modifier.testTag("video_trim_range_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.trimStartLabel}: ${WallpaperHelper.formatDuration(trimStartMs)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${strings.trimDurationLabel}: ${WallpaperHelper.formatDuration((effectiveEndMs - trimStartMs).coerceAtLeast(0L))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.trimEndLabel}: ${WallpaperHelper.formatDuration(effectiveEndMs)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { trimStartMs = (trimStartMs - 5000L).coerceAtLeast(0L) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("${strings.trimStartLabel} -5s", maxLines = 1) }
                                OutlinedButton(
                                    onClick = { trimStartMs = (trimStartMs + 5000L).coerceAtMost((effectiveEndMs - 500L).coerceAtLeast(0L)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("${strings.trimStartLabel} +5s", maxLines = 1) }
                                OutlinedButton(
                                    onClick = { trimEndMs = (effectiveEndMs - 5000L).coerceAtLeast(trimStartMs + 500L) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("${strings.trimEndLabel} -5s", maxLines = 1) }
                                OutlinedButton(
                                    onClick = { trimEndMs = (effectiveEndMs + 5000L).coerceAtMost(totalDurationMs) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) { Text("${strings.trimEndLabel} +5s", maxLines = 1) }
                            }
                        }
                    }

                    PreviewTab.FRAMING -> {
                        Text(
                            text = strings.scaleModeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                ScaleMode.CROP to strings.scaleCrop,
                                ScaleMode.FIT to strings.scaleFit,
                                ScaleMode.FILL to strings.scaleFill,
                                ScaleMode.CENTER to strings.scaleCenter
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = scaleMode == mode,
                                    onClick = { scaleMode = mode },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.zoomLabel, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "${((zoom * 100).roundToInt() / 100f)}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Slider(
                                value = zoom,
                                onValueChange = { zoom = it },
                                valueRange = 1.0f..3.0f,
                                modifier = Modifier.testTag("zoom_slider")
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.brightnessLabel, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "${(brightness * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
                                valueRange = 0.3f..1.2f,
                                modifier = Modifier.testTag("brightness_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.loopPlayback, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isLooping,
                                onCheckedChange = { isLooping = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.resetPosition, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    speed = SpeedPresets.DEFAULT_SPEED
                                    speedInputText = "1.00"
                                    isLooping = true
                                    isMuted = true
                                    brightness = 1.0f
                                    scaleMode = ScaleMode.CROP
                                    zoom = 1.0f
                                    offsetX = 0f
                                    offsetY = 0f
                                    trimStartMs = 0L
                                    trimEndMs = wallpaper.durationMs
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_all_settings_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.resetAllSettings, maxLines = 1)
                            }
                        }
                    }

                    PreviewTab.APPLY -> {
                        Text(
                            text = strings.targetScreenTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                WallpaperTarget.HOME to strings.targetHome,
                                WallpaperTarget.LOCK to strings.targetLock,
                                WallpaperTarget.BOTH to strings.targetBoth
                            ).forEach { (itemTarget, label) ->
                                FilterChip(
                                    selected = target == itemTarget,
                                    onClick = { target = itemTarget },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            text = strings.androidPickerNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            GradientPrimaryButton(
                text = strings.applyNow,
                icon = Icons.Filled.CheckCircle,
                onClick = {
                    onApplyWallpaper(
                        wallpaper.id,
                        speed,
                        isLooping,
                        isMuted,
                        brightness,
                        scaleMode,
                        zoom,
                        offsetX,
                        offsetY,
                        trimStartMs,
                        trimEndMs,
                        target
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "preview_apply_wallpaper_btn"
            )
        }
    }
}
