package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.SpeedPresets
import com.example.domain.model.StorageSummary
import com.example.domain.model.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.DeveloperConnectSupportCard
import com.example.ui.components.FrostedBarSurface
import com.example.ui.components.GlassCard
import com.example.ui.i18n.LocalAppStrings
import com.example.util.WallpaperHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    storageSummary: StorageSummary,
    appVersion: String,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetDefaultSpeed: (Float) -> Unit,
    onSetDefaultLooping: (Boolean) -> Unit,
    onSetBatterySaver: (Boolean) -> Unit,
    onSetTargetFps: (Int) -> Unit,
    onSetPreferHardwareDecoder: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onOpenOptimizationGuide: () -> Unit,
    onConnectWhatsApp: () -> Unit,
    onOpenSupportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            FrostedBarSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = strings.navSettings,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // Appearance & Language
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionTitle(Icons.Filled.Palette, strings.settingsAppearance)
                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.themeLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ThemeMode.DARK to strings.themeDark,
                        ThemeMode.LIGHT to strings.themeLight,
                        ThemeMode.SYSTEM to strings.themeSystem
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_theme_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { onSetLanguage(AppLanguage.ENGLISH) },
                        label = { Text(strings.languageEnglish) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_lang_en")
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.BENGALI,
                        onClick = { onSetLanguage(AppLanguage.BENGALI) },
                        label = { Text(strings.languageBengali) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_lang_bn")
                    )
                }
            }
        }

        // Playback Defaults
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionTitle(Icons.Filled.Speed, strings.settingsPlaybackDefaults)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.defaultSpeedLabel, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = SpeedPresets.format(settings.defaultSpeed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.defaultSpeed,
                    onValueChange = { onSetDefaultSpeed(SpeedPresets.clamp(it)) },
                    valueRange = SpeedPresets.MIN_SPEED..SpeedPresets.MAX_SPEED
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.50f, 1.00f, 1.50f, 2.00f, 3.00f).forEach { preset ->
                        FilterChip(
                            selected = kotlin.math.abs(settings.defaultSpeed - preset) < 0.02f,
                            onClick = { onSetDefaultSpeed(preset) },
                            label = { Text(SpeedPresets.format(preset)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.defaultLoopingLabel, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.defaultLooping,
                        onCheckedChange = onSetDefaultLooping
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(strings.hardwareDecoderLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.hardwareDecoderDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.preferHardwareDecoder,
                        onCheckedChange = onSetPreferHardwareDecoder
                    )
                }
            }
        }

        // Battery & Performance
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionTitle(Icons.Filled.BatterySaver, strings.settingsBatteryPerf)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(strings.batterySaverLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.batterySaverDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.batterySaverEnabled,
                        onCheckedChange = onSetBatterySaver,
                        modifier = Modifier.testTag("settings_battery_saver_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.targetFpsLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 45, 60).forEach { fps ->
                        FilterChip(
                            selected = settings.targetFps == fps,
                            onClick = { onSetTargetFps(fps) },
                            label = { Text("$fps FPS") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Storage Management
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionTitle(Icons.Filled.Storage, strings.settingsStorage)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${strings.totalUsed}: ${WallpaperHelper.formatFileSize(storageSummary.totalBytes)} (${strings.itemsCount(storageSummary.wallpaperCount)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${strings.videosLabel}: ${WallpaperHelper.formatFileSize(storageSummary.videoBytes)} • ${strings.gifsLabel}: ${WallpaperHelper.formatFileSize(storageSummary.gifBytes)} • ${strings.photosLabel}: ${WallpaperHelper.formatFileSize(storageSummary.imageBytes)} • ${strings.thumbnailsLabel}: ${WallpaperHelper.formatFileSize(storageSummary.thumbnailBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_cache_btn")
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.clearThumbCache)
                }
            }
        }

        // System Optimization & Auto-Start Guide
        item {
            GlassCard(
                onClick = onOpenOptimizationGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("open_optimization_guide_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SecurityUpdateGood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.optimizationGuideTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.optimizationGuideSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
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

        // About & Privacy
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionTitle(Icons.Filled.Info, strings.settingsAbout)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.privacyPolicyTitle, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.aboutAppTitle, maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${strings.appVersionLabel}: $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicyTitle) },
            text = { Text(strings.privacyPolicyBody) },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutAppTitle) },
            text = { Text("${strings.aboutAppBody}\n\n${strings.appVersionLabel}: $appVersion") },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
