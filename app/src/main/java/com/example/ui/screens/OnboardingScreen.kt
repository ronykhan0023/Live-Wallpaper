package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.RonyGradients
import com.example.ui.theme.SapphireBlue
import com.example.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun OnboardingScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var isBatteryUnrestricted by remember {
        mutableStateOf(WallpaperHelper.isIgnoringBatteryOptimizations(context))
    }
    var revealContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60L)
        revealContent = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryUnrestricted = WallpaperHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Predictive-back-friendly step navigation inside onboarding
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_aurora")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated ambient aurora glow orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val violetAlpha = if (isDark) 0.24f else 0.14f
            val cyanAlpha = if (isDark) 0.20f else 0.12f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricViolet.copy(alpha = violetAlpha), Color.Transparent),
                    center = Offset(w * (0.2f + 0.12f * driftPhase), h * 0.18f),
                    radius = w * 0.72f * pulsePhase
                ),
                radius = w * 0.72f * pulsePhase,
                center = Offset(w * (0.2f + 0.12f * driftPhase), h * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanGlow.copy(alpha = cyanAlpha), Color.Transparent),
                    center = Offset(w * (0.85f - 0.12f * driftPhase), h * 0.78f),
                    radius = w * 0.65f * pulsePhase
                ),
                radius = w * 0.65f * pulsePhase,
                center = Offset(w * (0.85f - 0.12f * driftPhase), h * 0.78f)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Language Toggle Pill + Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleLanguage,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, RonyGradients.GlassBorderDark),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_language_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = strings.languageLabel,
                                tint = CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.BENGALI) "বাংলা" else "EN",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = onFinishOnboarding,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_skip_btn")
                    ) {
                        Text(
                            text = strings.onboardingSkip,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Multi-Step Interactive Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val offsetFraction = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue.coerceIn(0f, 1f)
                    val pageScale = 1f - (offsetFraction * 0.06f)
                    val pageAlpha = 1f - (offsetFraction * 0.35f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pageScale)
                            .alpha(pageAlpha)
                    ) {
                        when (page) {
                            0 -> WelcomeCapabilitiesPage(
                                revealContent = revealContent,
                                pulseScale = pulsePhase
                            )
                            1 -> PermissionsSetupPage(
                                isBatteryUnrestricted = isBatteryUnrestricted,
                                pulseScale = pulsePhase,
                                onOpenBatterySettings = {
                                    WallpaperHelper.openBatteryOptimizationSettings(context)
                                    Toast.makeText(context, strings.autoStartOpenedToast, Toast.LENGTH_LONG).show()
                                },
                                onOpenAutoStart = {
                                    WallpaperHelper.openAutoStartSettings(context)
                                    Toast.makeText(context, strings.autoStartOpenedToast, Toast.LENGTH_LONG).show()
                                },
                                onOpenAppInfo = {
                                    WallpaperHelper.openAppInfoSettings(context)
                                },
                                onSkipPermissions = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                }
                            )
                            else -> ReadyToStartPage(
                                pulseScale = pulsePhase
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val selected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 30.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        RonyGradients.PurpleCyanAccent
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            )
                                        )
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("onboarding_back_btn"),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.onboardingBack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.onboardingBack, maxLines = 1)
                        }
                    }

                    AnimatedContent(
                        targetState = pagerState.currentPage == 2,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                        modifier = Modifier.weight(1f),
                        label = "cta_transition"
                    ) { isLastPage ->
                        if (isLastPage) {
                            GradientPrimaryButton(
                                text = strings.onboardingGetStarted,
                                icon = Icons.Filled.RocketLaunch,
                                onClick = onFinishOnboarding,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "onboarding_start_btn"
                            )
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(2))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("onboarding_next_btn"),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(RonyGradients.PurpleBluePrimary, MaterialTheme.shapes.medium)
                                        .padding(horizontal = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = strings.onboardingNext,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = strings.onboardingNext,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun WelcomeCapabilitiesPage(
    revealContent: Boolean,
    pulseScale: Float
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Glowing Brand Icon Emblem
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.45f),
                                CyanGlow.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_neon),
                contentDescription = strings.appName,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, RonyGradients.PurpleCyanAccent, RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = CircleShape,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.45f))
        ) {
            Text(
                text = strings.onboardingCompatibilityBadge,
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.onboardingWelcomeTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.onboardingWelcomeSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val features = listOf(
            Triple(Icons.Filled.VideoLibrary, strings.onboardingFeature1Title, strings.onboardingFeature1Desc),
            Triple(Icons.AutoMirrored.Filled.PlaylistPlay, strings.onboardingFeature2Title, strings.onboardingFeature2Desc),
            Triple(Icons.Filled.Widgets, strings.onboardingFeature3Title, strings.onboardingFeature3Desc),
            Triple(Icons.Filled.VerifiedUser, strings.onboardingFeature4Title, strings.onboardingFeature4Desc)
        )

        features.forEachIndexed { idx, (icon, title, desc) ->
            AnimatedVisibility(
                visible = revealContent,
                enter = fadeIn(tween(300, delayMillis = idx * 80)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { it / 3 }
                    )
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            ElectricViolet.copy(alpha = 0.25f),
                                            SapphireBlue.copy(alpha = 0.25f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = CyanGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsSetupPage(
    isBatteryUnrestricted: Boolean,
    pulseScale: Float,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onSkipPermissions: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            ElectricViolet.copy(alpha = 0.28f),
                            CyanGlow.copy(alpha = 0.24f)
                        )
                    )
                )
                .border(1.5.dp, RonyGradients.PurpleCyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = strings.onboardingPermissionsTitle,
                tint = CyanGlow,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.onboardingPermissionsTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.onboardingPermissionsSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Battery Unrestricted Card with live status
        PermissionActionCard(
            icon = Icons.Filled.BatteryChargingFull,
            title = strings.onboardingBatteryCardTitle,
            description = strings.onboardingBatteryCardDesc,
            buttonText = strings.onboardingBatteryGrantBtn,
            statusBadge = if (isBatteryUnrestricted) strings.batteryOptStatusIgnored else strings.batteryOptStatusRestricted,
            isOptimal = isBatteryUnrestricted,
            onClick = onOpenBatterySettings,
            testTag = "onboarding_battery_btn"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Auto-Start on Boot Card
        PermissionActionCard(
            icon = Icons.Filled.PowerSettingsNew,
            title = strings.onboardingAutoStartCardTitle,
            description = strings.onboardingAutoStartCardDesc,
            buttonText = strings.onboardingAutoStartBtn,
            statusBadge = null,
            isOptimal = false,
            onClick = onOpenAutoStart,
            testTag = "onboarding_autostart_btn"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Startup & Background Activity (App Info)
        PermissionActionCard(
            icon = Icons.Filled.Settings,
            title = strings.onboardingStartupCardTitle,
            description = strings.onboardingStartupCardDesc,
            buttonText = strings.onboardingStartupBtn,
            statusBadge = strings.bootSyncRegisteredStatus,
            isOptimal = true,
            onClick = onOpenAppInfo,
            testTag = "onboarding_app_info_btn"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkipPermissions,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("onboarding_skip_permissions_btn")
        ) {
            Text(
                text = strings.onboardingSetLater,
                style = MaterialTheme.typography.labelLarge,
                color = CyanGlow
            )
        }
    }
}

@Composable
private fun PermissionActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    statusBadge: String?,
    isOptimal: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = CyanGlow, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (statusBadge != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isOptimal) EmeraldLive else AmberWarm,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOptimal) EmeraldLive else AmberWarm,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(testTag),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReadyToStartPage(pulseScale: Float) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                EmeraldLive.copy(alpha = 0.30f),
                                CyanGlow.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(2.dp, RonyGradients.PurpleCyanAccent),
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = strings.onboardingReadyTitle,
                        tint = CyanGlow,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = strings.onboardingReadyTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.onboardingReadySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            listOf(
                strings.onboardingReadyStep1,
                strings.onboardingReadyStep2,
                strings.onboardingReadyStep3
            ).forEachIndexed { idx, step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(RonyGradients.PurpleBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (idx == 2) Icons.Filled.Wallpaper else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
