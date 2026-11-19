package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.SpeedPresets
import com.example.domain.model.WallpaperItem
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RonyGradients
import com.example.util.WallpaperHelper
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCustomizerDialog(
    wallpaper: WallpaperItem,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        speed: Float,
        isLooping: Boolean,
        isMuted: Boolean,
        volume: Float,
        brightness: Float,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        trimStartMs: Long,
        trimEndMs: Long
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current

    var speed by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.playbackSpeed) }
    var speedInputText by remember(wallpaper.id) {
        mutableStateOf(String.format(Locale.US, "%.2f", wallpaper.playbackSpeed))
    }
    var isLooping by remember(wallpaper.id) { mutableStateOf(wallpaper.isLooping) }
    var isMuted by remember(wallpaper.id) { mutableStateOf(wallpaper.isMuted) }
    var volume by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.volume) }
    var brightness by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.brightness) }
    var scaleMode by remember(wallpaper.id) { mutableStateOf(wallpaper.scaleMode) }
    var zoom by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.zoom) }
    var offsetX by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.offsetX) }
    var offsetY by remember(wallpaper.id) { mutableFloatStateOf(wallpaper.offsetY) }
    var trimStartMs by remember(wallpaper.id) { mutableLongStateOf(wallpaper.trimStartMs.coerceAtLeast(0L)) }
    var trimEndMs by remember(wallpaper.id) {
        mutableLongStateOf(if (wallpaper.trimEndMs > 0L) wallpaper.trimEndMs else wallpaper.durationMs)
    }
    var isPlaying by remember { mutableStateOf(true) }

    val applySpeedFromSliderOrChip: (Float) -> Unit = { value ->
        val clamped = SpeedPresets.clamp(value)
        speed = clamped
        speedInputText = String.format(Locale.US, "%.2f", clamped)
    }

    val commitManualSpeedInput: () -> Unit = {
        val parsed = speedInputText.toFloatOrNull()
        val finalSpeed = if (parsed != null) SpeedPresets.clamp(parsed) else speed
        speed = finalSpeed
        speedInputText = String.format(Locale.US, "%.2f", finalSpeed)
        focusManager.clearFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, RonyGradients.GlassBorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Fixed Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.adjustWallpaperTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = wallpaper.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("customizer_close_btn")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = strings.close)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Compact Fixed Live Preview (Drag to pan!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, RonyGradients.PurpleCyanAccent, RoundedCornerShape(20.dp))
                            .background(ObsidianBlack)
                    ) {
                        LiveMediaPreview(
                            filePath = wallpaper.filePath,
                            mediaType = wallpaper.mediaType,
                            playbackSpeed = speed,
                            isLooping = isLooping,
                            isMuted = isMuted,
                            volume = volume,
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

                        if (wallpaper.mediaType != MediaType.IMAGE) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ) {
                                    IconButton(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) strings.pause else strings.play,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (wallpaper.mediaType == MediaType.VIDEO) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (isMuted) {
                                                    if (volume <= 0.01f) volume = 1.0f
                                                    isMuted = false
                                                } else {
                                                    isMuted = true
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted || volume <= 0.005f) {
                                                    Icons.AutoMirrored.Filled.VolumeOff
                                                } else {
                                                    Icons.AutoMirrored.Filled.VolumeUp
                                                },
                                                contentDescription = strings.muteAudio,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.positionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Flexible Scrollable Middle Controls — weight(1f, fill = false) guarantees Footer NEVER gets squashed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Scale & Fit Mode
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
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customizer_scale_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Zoom Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.zoomLabel, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${((zoom * 100).roundToInt() / 100f)}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1.0f..3.0f,
                            modifier = Modifier.testTag("customizer_zoom_slider")
                        )
                    }

                    // Brightness Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.brightnessLabel, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${(brightness * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanGlow
                            )
                        }
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0.3f..1.2f,
                            modifier = Modifier.testTag("customizer_brightness_slider")
                        )
                    }

                    // Video & GIF Speed Controls + Manual Keyboard Input
                    if (wallpaper.mediaType != MediaType.IMAGE) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.speedControl}: ${SpeedPresets.format(speed)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { applySpeedFromSliderOrChip(SpeedPresets.DEFAULT_SPEED) }) {
                                Text(strings.resetSpeed)
                            }
                        }

                        // Manual keyboard numeric input (e.g. 0.40) + Set button
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
                                    onDone = { commitManualSpeedInput() }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customizer_speed_input")
                            )
                            FilledTonalButton(
                                onClick = commitManualSpeedInput,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("customizer_speed_apply_btn"),
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
                                    onClick = { applySpeedFromSliderOrChip(preset) },
                                    label = { Text(SpeedPresets.format(preset)) }
                                )
                            }
                        }
                        Slider(
                            value = speed,
                            onValueChange = applySpeedFromSliderOrChip,
                            valueRange = SpeedPresets.MIN_SPEED..SpeedPresets.MAX_SPEED,
                            modifier = Modifier.testTag("customizer_speed_slider")
                        )

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

                        if (wallpaper.mediaType == MediaType.VIDEO) {
                            VideoSoundControlSection(
                                isMuted = isMuted,
                                volume = volume,
                                onMuteChange = { isMuted = it },
                                onVolumeChange = { volume = it },
                                testTagPrefix = "customizer"
                            )
                        }
                    }

                    // Video Trim / Duration Control
                    if (wallpaper.mediaType == MediaType.VIDEO && wallpaper.durationMs > 1000L) {
                        HorizontalDivider()
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
                                }
                            ) {
                                Text(strings.resetFullVideo)
                            }
                        }
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
                            modifier = Modifier.testTag("customizer_trim_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.trimStartLabel}: ${WallpaperHelper.formatDuration(trimStartMs)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${strings.trimDurationLabel}: ${WallpaperHelper.formatDuration((effectiveEndMs - trimStartMs).coerceAtLeast(0L))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.trimEndLabel}: ${WallpaperHelper.formatDuration(effectiveEndMs)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Reset Position & All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetPosition, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                applySpeedFromSliderOrChip(SpeedPresets.DEFAULT_SPEED)
                                isLooping = true
                                isMuted = true
                                volume = 1.0f
                                brightness = 1.0f
                                scaleMode = ScaleMode.CROP
                                zoom = 1.0f
                                offsetX = 0f
                                offsetY = 0f
                                trimStartMs = 0L
                                trimEndMs = wallpaper.durationMs
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetAllSettings, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fixed Full-Height Footer Save / Cancel Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("customizer_cancel_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = strings.cancel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            commitManualSpeedInput()
                            onSave(
                                wallpaper.id,
                                speed,
                                isLooping,
                                isMuted,
                                volume,
                                brightness,
                                scaleMode,
                                zoom,
                                offsetX,
                                offsetY,
                                trimStartMs,
                                trimEndMs
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("customizer_save_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.save,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
