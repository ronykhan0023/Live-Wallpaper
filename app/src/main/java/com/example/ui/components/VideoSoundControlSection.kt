package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CyanGlow
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSoundControlSection(
    isMuted: Boolean,
    volume: Float,
    onMuteChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val effectiveVolume = if (isMuted) 0f else volume.coerceIn(0f, 1f)
    val volumePercent = (effectiveVolume * 100f).roundToInt()
    val soundEnabled = !isMuted && volume > 0.005f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (soundEnabled) CyanGlow.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = strings.videoSoundTitle,
                        tint = if (soundEnabled) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.videoSoundTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (soundEnabled) {
                                "${strings.videoVolumeLabel}: $volumePercent%"
                            } else {
                                strings.soundMutedState
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (soundEnabled) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (volume <= 0.01f) {
                                onVolumeChange(1.0f)
                            }
                            onMuteChange(false)
                        } else {
                            onMuteChange(true)
                        }
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_sound_switch")
                )
            }

            Slider(
                value = effectiveVolume,
                onValueChange = { newValue ->
                    val clamped = newValue.coerceIn(0f, 1f)
                    if (clamped <= 0.01f) {
                        onVolumeChange(0f)
                        onMuteChange(true)
                    } else {
                        onMuteChange(false)
                        onVolumeChange(clamped)
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_volume_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0% (${strings.soundLowHint})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "100% (${strings.soundHighHint})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f).forEach { preset ->
                    val isSelected = if (preset == 0.0f) {
                        !soundEnabled
                    } else {
                        soundEnabled && abs(effectiveVolume - preset) < 0.04f
                    }
                    val label = if (preset == 0.0f) "0%" else "${(preset * 100).roundToInt()}%"
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (preset == 0.0f) {
                                onMuteChange(true)
                            } else {
                                onMuteChange(false)
                                onVolumeChange(preset)
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("${testTagPrefix}_volume_preset_${(preset * 100).roundToInt()}")
                    )
                }
            }
        }
    }
}
