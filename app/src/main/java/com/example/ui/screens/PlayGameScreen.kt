package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.ChallengeLinkPayload
import com.example.data.model.ChallengeTimeRecord
import com.example.data.model.DifferenceSpot
import com.example.data.model.DifficultyTensionCalculator
import com.example.data.model.GameTheme
import com.example.data.model.PatternComplexity
import com.example.data.model.StreakComplexityScaler
import com.example.ui.components.ChallengeLinkHubCard
import com.example.ui.components.ChallengeStandingsCompactList
import com.example.ui.components.DailyLoginHubBanner
import com.example.ui.components.LevelRewardBreakdownCard
import com.example.ui.components.RewardCurrencyHubBanner
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.ChallengeLinkUiState
import com.example.ui.viewmodel.GameSessionUiState
import com.example.ui.viewmodel.SpotDiffViewModel
import com.example.ui.viewmodel.TapFeedback
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PlayGameScreen(
    viewModel: SpotDiffViewModel,
    preferences: AppPreferences,
    sessionState: GameSessionUiState,
    onNavigateToPractice: () -> Unit,
    onNavigateToReplayGallery: () -> Unit = onNavigateToPractice,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToDesigner: () -> Unit
) {
    val challengeLinkState by viewModel.challengeLinkState.collectAsStateWithLifecycle()
    val globalLeaderboardState by viewModel.globalLeaderboardState.collectAsStateWithLifecycle()

    when (sessionState) {
        is GameSessionUiState.Idle -> {
            val parsedCode = challengeLinkState.parsedChallenge?.challengeCode.orEmpty()
            val standingsForParsed = remember(parsedCode, globalLeaderboardState, preferences.challengeTimeRecords) {
                viewModel.getChallengeStandings(parsedCode)
            }
            LevelSetupHub(
                preferences = preferences,
                challengeState = challengeLinkState,
                standingsForParsed = standingsForParsed,
                onChallengeInputChange = viewModel::updateChallengeLinkInput,
                onGenerateSetupChallengeLink = viewModel::openChallengeGeneratorForSetup,
                onShareStreakSnapshot = viewModel::openSocialSnapshotForProfileOrSetup,
                onOpenDailyLoginCalendar = viewModel::openDailyLoginModal,
                onOpenRewardShop = { tab -> viewModel.openRewardShop(initialTab = tab) },
                onAcceptChallenge = { payload -> viewModel.acceptAndLaunchChallenge(payload) },
                onThemeSelect = viewModel::setTheme,
                onVariationCountChange = viewModel::setVariationCount,
                onTimeLimitChange = viewModel::setTimeLimitSeconds,
                onComplexitySelect = viewModel::setPatternComplexity,
                onSideBySideToggle = viewModel::setSideBySideMode,
                onIndependentZoomToggle = viewModel::setIndependentZoom,
                onScreenShakeToggle = viewModel::setScreenShakeEnabled,
                onStartAiGame = { viewModel.startNewAiLevel(null) },
                onOpenPractice = onNavigateToPractice,
                onOpenReplayGallery = onNavigateToReplayGallery,
                onOpenDesigner = onNavigateToDesigner
            )
        }

        is GameSessionUiState.GeneratingAiLevel -> {
            BackHandler { viewModel.exitCurrentGame() }
            AiGeneratingLoadingView(
                state = sessionState,
                onCancel = viewModel::exitCurrentGame
            )
        }

        is GameSessionUiState.Error -> {
            BackHandler { viewModel.exitCurrentGame() }
            AiGenerationErrorView(
                state = sessionState,
                onRetry = { viewModel.startNewAiLevel(null) },
                onPlayOfflineInstead = {
                    viewModel.exitCurrentGame()
                    onNavigateToPractice()
                },
                onBack = viewModel::exitCurrentGame
            )
        }

        is GameSessionUiState.Playing -> {
            BackHandler { viewModel.exitCurrentGame() }
            val challengeStandings = remember(
                sessionState.challengeCode,
                sessionState.isVictory,
                globalLeaderboardState,
                preferences.challengeTimeRecords
            ) {
                viewModel.getChallengeStandings(sessionState.challengeCode)
            }
            ActiveGameplayView(
                state = sessionState,
                currentShardBalance = preferences.shardBalance,
                challengeStandings = challengeStandings,
                sideBySideMode = preferences.sideBySideMode,
                independentZoom = preferences.independentZoom,
                screenShakeEnabled = preferences.screenShakeEnabled,
                onToggleLayoutMode = { viewModel.setSideBySideMode(!preferences.sideBySideMode) },
                onToggleIndependentZoom = { viewModel.setIndependentZoom(!preferences.independentZoom) },
                onToggleScreenShake = { viewModel.setScreenShakeEnabled(!preferences.screenShakeEnabled) },
                onImageTap = viewModel::onImageTapped,
                onUseHint = viewModel::useHint,
                onOpenRewardShop = { tab -> viewModel.openRewardShop(initialTab = tab) },
                onOpenChallengeLinkGenerator = viewModel::openChallengeGeneratorForPlaying,
                onOpenSocialSnapshot = viewModel::openSocialSnapshotForPlaying,
                onWatchInstantReplay = viewModel::openReplayTheaterForCurrentVictory,
                onNextLevel = {
                    val offlineNum = sessionState.offlineLevelNumber
                    if (offlineNum != null) {
                        val nextNum = if (offlineNum < 100) offlineNum + 1 else 1
                        viewModel.startOfflinePracticeLevel(nextNum)
                    } else {
                        viewModel.startNewAiLevel(null)
                    }
                },
                onExitToSetup = viewModel::exitCurrentGame,
                onViewLeaderboard = onNavigateToLeaderboard
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelSetupHub(
    preferences: AppPreferences,
    challengeState: ChallengeLinkUiState,
    standingsForParsed: List<ChallengeTimeRecord>,
    onChallengeInputChange: (String) -> Unit,
    onGenerateSetupChallengeLink: () -> Unit,
    onShareStreakSnapshot: () -> Unit,
    onOpenDailyLoginCalendar: () -> Unit,
    onOpenRewardShop: (Int) -> Unit,
    onAcceptChallenge: (ChallengeLinkPayload) -> Unit,
    onThemeSelect: (GameTheme) -> Unit,
    onVariationCountChange: (Int) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onComplexitySelect: (PatternComplexity) -> Unit,
    onSideBySideToggle: (Boolean) -> Unit,
    onIndependentZoomToggle: (Boolean) -> Unit,
    onScreenShakeToggle: (Boolean) -> Unit,
    onStartAiGame: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenReplayGallery: () -> Unit,
    onOpenDesigner: () -> Unit
) {
    val timePresets = listOf(45, 60, 90, 120, 180)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Launch Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1794391386542),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x44090E1A),
                                        Color(0xEE090E1A)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "GEMINI AI GENERATOR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF041019),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${preferences.variationCount} Variations • ${preferences.timeLimitSeconds}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = preferences.selectedTheme.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = preferences.selectedTheme.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Primary Start Buttons: AI Generator & 100 Offline Practice Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartAiGame,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF041019)
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(58.dp)
                        .testTag("generate_ai_level_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play AI Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = onOpenPractice,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("open_practice_levels_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = EmeraldSuccess
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100 Offline (${preferences.completedOfflineLevels.size}/100)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Offline Replay Gallery Quick Launch Banner
            OutlinedButton(
                onClick = onOpenReplayGallery,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_offline_replay_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SlowMotionVideo,
                    contentDescription = null,
                    tint = RadiantAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Replay Gallery & Difference Timestamps (${maxOf(preferences.offlineReplayRecords.size, preferences.completedOfflineLevels.size, 3)} Best Runs)",
                    fontWeight = FontWeight.Bold
                )
            }

            // Calendar-Based Daily Login Streak Banner
            DailyLoginHubBanner(
                preferences = preferences,
                onOpenCalendar = onOpenDailyLoginCalendar
            )

            // Starlight Shards Currency & Reward Emporium Banner
            RewardCurrencyHubBanner(
                preferences = preferences,
                onOpenShop = { onOpenRewardShop(0) }
            )

            // Consecutive Win Streak & Dynamic AI Complexity Escalation Card
            WinStreakComplexityCard(
                preferences = preferences,
                onShareSnapshot = onShareStreakSnapshot
            )

            // Friend Challenge Link & Fastest Time Duel Card
            ChallengeLinkHubCard(
                challengeState = challengeState,
                standingsForParsed = standingsForParsed,
                onInputChange = onChallengeInputChange,
                onGenerateSetupChallengeLink = onGenerateSetupChallengeLink,
                onAcceptChallenge = onAcceptChallenge
            )

            // 1. Visual Theme Selector with Animated Art Style Transition Preview
            com.example.ui.components.AnimatedThemeStylePreviewCard(
                selectedTheme = preferences.selectedTheme,
                onSelectTheme = onThemeSelect,
                title = "1. Choose Visual Theme & Art Style",
                unlockedThemeIds = preferences.unlockedThemeIds,
                onOpenRewardShop = { onOpenRewardShop(0) }
            )

            // 2. Number of Variations & Time Limit Difficulty
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. Variations Difficulty",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${preferences.variationCount} Differences",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = preferences.variationCount.toFloat(),
                        onValueChange = { onVariationCountChange(it.roundToInt()) },
                        valueRange = 3f..10f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("variation_count_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3 (Casual)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                        Text("6 (Skilled)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                        Text("10 (Expert)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = RadiantAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Time Limit Control",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            color = RadiantAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${preferences.timeLimitSeconds} sec",
                                style = MaterialTheme.typography.labelMedium,
                                color = RadiantAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timePresets.forEach { sec ->
                            val selected = preferences.timeLimitSeconds == sec
                            FilterChip(
                                selected = selected,
                                onClick = { onTimeLimitChange(sec) },
                                label = {
                                    Text(
                                        when (sec) {
                                            45 -> "45s Blitz"
                                            60 -> "60s Fast"
                                            90 -> "90s Standard"
                                            120 -> "120s Relaxed"
                                            else -> "180s Zen"
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadiantAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = RadiantAmber
                                )
                            )
                        }
                    }
                }
            }

            // 3. Visual Pattern Complexity, Layout Mode & Single-Image Zoom Option
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Pattern Complexity & Zoom Controls",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PatternComplexity.entries.forEach { comp ->
                            val isSelected = preferences.patternComplexity == comp
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color(0xFF111827),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onComplexitySelect(comp) }
                                    .testTag("complexity_${comp.id.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comp.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = comp.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "x${comp.scoreMultiplier} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comparison Layout",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = preferences.sideBySideMode,
                                onClick = { onSideBySideToggle(true) },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Side-by-Side") }
                            )
                            FilterChip(
                                selected = !preferences.sideBySideMode,
                                onClick = { onSideBySideToggle(false) },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Stacked") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pinch-to-Zoom Mode",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = preferences.independentZoom,
                                onClick = { onIndependentZoomToggle(true) },
                                leadingIcon = {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Single Image") },
                                modifier = Modifier.testTag("chip_single_image_zoom")
                            )
                            FilterChip(
                                selected = !preferences.independentZoom,
                                onClick = { onIndependentZoomToggle(false) },
                                leadingIcon = {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Both Sync") },
                                modifier = Modifier.testTag("chip_sync_zoom")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // High-Difficulty Countdown Screen-Shake Tension Preview & Toggle
                    val setupTensionProfile = remember(
                        preferences.patternComplexity,
                        preferences.variationCount,
                        preferences.timeLimitSeconds,
                        preferences.currentWinStreak
                    ) {
                        preferences.currentDifficultyTensionProfile()
                    }
                    val tensionAccent = when {
                        !setupTensionProfile.isHighDifficulty -> Color(0xFF94A3B8)
                        setupTensionProfile.difficultyScore >= 6 -> CoralError
                        else -> RadiantAmber
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = tensionAccent.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("setup_difficulty_tension_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = tensionAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Countdown Tension: ${setupTensionProfile.tierBadge}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tensionAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = setupTensionProfile.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                FilterChip(
                                    selected = preferences.screenShakeEnabled,
                                    onClick = { onScreenShakeToggle(!preferences.screenShakeEnabled) },
                                    label = {
                                        Text(
                                            if (preferences.screenShakeEnabled) "Shake ON" else "Shake OFF",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tensionAccent.copy(alpha = 0.22f),
                                        selectedLabelColor = tensionAccent
                                    ),
                                    modifier = Modifier.testTag("chip_screen_shake_toggle")
                                )
                            }
                        }
                    }
                }
            }

            // Custom Level Designer Quick Card
            OutlinedButton(
                onClick = onOpenDesigner,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Open Custom Level Designer & Play Friends' Levels")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AiGeneratingLoadingView(
    state: GameSessionUiState.GeneratingAiLevel,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = state.customTitle ?: "Synthesizing ${state.theme.displayName} Level...",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gemini Nano Banana 2.1 is generating your custom visual scene and weaving ${state.variationCount} ${state.complexity.displayName.lowercase()} differences (typically 15–45s).",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (state.activeWinStreak > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = RadiantAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = RadiantAmber.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = RadiantAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isStreakEscalated) {
                                    "Win Streak ${state.activeWinStreak} (${state.streakTierBadge}) • AI Complexity Escalated!"
                                } else {
                                    "Active Win Streak: ${state.activeWinStreak} • ${state.streakTierBadge}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = RadiantAmber,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel Generation")
                }
            }
        }
    }
}

@Composable
private fun AiGenerationErrorView(
    state: GameSessionUiState.Error,
    onRetry: () -> Unit,
    onPlayOfflineInstead: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = CoralError,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "AI Level Generation Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoralError,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("retry_ai_level_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry AI")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onPlayOfflineInstead,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play 100 Offline Practice Levels Instead")
                }
            }
        }
    }
}

@Composable
private fun ActiveGameplayView(
    state: GameSessionUiState.Playing,
    currentShardBalance: Int,
    challengeStandings: List<ChallengeTimeRecord>,
    sideBySideMode: Boolean,
    independentZoom: Boolean,
    screenShakeEnabled: Boolean,
    onToggleLayoutMode: () -> Unit,
    onToggleIndependentZoom: () -> Unit,
    onToggleScreenShake: () -> Unit,
    onImageTap: (Float, Float) -> Unit,
    onUseHint: () -> Unit,
    onOpenRewardShop: (Int) -> Unit,
    onOpenChallengeLinkGenerator: () -> Unit,
    onOpenSocialSnapshot: () -> Unit,
    onWatchInstantReplay: () -> Unit,
    onNextLevel: () -> Unit,
    onExitToSetup: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    // Separate zoom & pan state for Image 1 (Original) and Image 2 (Modified)
    // When independentZoom == true (default), zooming/panning one image leaves the other untouched!
    var zoomScaleA by remember(state.levelTitle) { mutableFloatStateOf(1f) }
    var panOffsetXA by remember(state.levelTitle) { mutableFloatStateOf(0f) }
    var panOffsetYA by remember(state.levelTitle) { mutableFloatStateOf(0f) }

    var zoomScaleB by remember(state.levelTitle) { mutableFloatStateOf(1f) }
    var panOffsetXB by remember(state.levelTitle) { mutableFloatStateOf(0f) }
    var panOffsetYB by remember(state.levelTitle) { mutableFloatStateOf(0f) }

    val secondsLeft = (state.remainingMillis / 1000L).toInt()
    val tenthsLeft = ((state.remainingMillis % 1000L) / 100L).toInt()
    val timerColor = when {
        secondsLeft <= 15 -> CoralError
        secondsLeft <= 30 -> RadiantAmber
        else -> EmeraldSuccess
    }

    // Evaluate High-Difficulty Tension & Subtle Countdown Screen-Shake
    val tensionProfile = remember(
        state.patternComplexity,
        state.variationCount,
        state.timeLimitSeconds,
        state.activeWinStreak
    ) {
        DifficultyTensionCalculator.evaluate(
            complexity = state.patternComplexity,
            variationCount = state.variationCount,
            timeLimitSeconds = state.timeLimitSeconds,
            activeWinStreak = state.activeWinStreak
        )
    }

    val baseCountdownShakeIntensity = if (!state.isVictory && !state.isTimeOut) {
        DifficultyTensionCalculator.computeCountdownShakeIntensity(
            profile = tensionProfile,
            remainingMillis = state.remainingMillis,
            elapsedMillis = state.elapsedMillis,
            timeLimitSeconds = state.timeLimitSeconds,
            screenShakeEnabled = screenShakeEnabled
        )
    } else 0f

    // Brief extra tremor if player misses a tap on a high-difficulty level
    val missTapShakeBoost = remember(state.lastTapFeedback, state.elapsedMillis, tensionProfile.isHighDifficulty, screenShakeEnabled) {
        val feedback = state.lastTapFeedback
        if (screenShakeEnabled && tensionProfile.isHighDifficulty && feedback != null && !feedback.isHit) {
            val ageMs = (System.currentTimeMillis() - feedback.timestamp).coerceAtLeast(0L)
            if (ageMs < 420L) {
                (1f - ageMs / 420f) * 0.45f
            } else 0f
        } else 0f
    }

    val effectiveShakeIntensity = (baseCountdownShakeIntensity + missTapShakeBoost).coerceIn(0f, 1f)

    val shakeTransition = rememberInfiniteTransition(label = "countdown_tension_shake")
    val shakeWavePhase by shakeTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shake_wave_phase"
    )

    val density = LocalDensity.current
    val maxShakeTranslationPx = with(density) { 3.6.dp.toPx() }
    val shakeTranslationX = if (effectiveShakeIntensity > 0.01f) {
        sin(shakeWavePhase * 2f) * maxShakeTranslationPx * effectiveShakeIntensity
    } else 0f
    val shakeTranslationY = if (effectiveShakeIntensity > 0.01f) {
        cos(shakeWavePhase * 3f) * (maxShakeTranslationPx * 0.72f) * effectiveShakeIntensity
    } else 0f
    val shakeRotationZ = if (effectiveShakeIntensity > 0.01f) {
        sin(shakeWavePhase) * 0.38f * effectiveShakeIntensity
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeTranslationX
                translationY = shakeTranslationY
                rotationZ = shakeRotationZ
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Gameplay HUD
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (tensionProfile.isHighDifficulty && effectiveShakeIntensity > 0.08f) {
                        Modifier.border(
                            width = 1.2.dp,
                            color = timerColor.copy(alpha = (0.35f + effectiveShakeIntensity * 0.55f).coerceIn(0f, 1f)),
                            shape = RoundedCornerShape(18.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onExitToSetup,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Level Setup"
                            )
                        }
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = state.levelTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(state.theme.displayName)
                                    if (state.challengeCode.isNotBlank()) {
                                        append(" • #${state.challengeCode}")
                                    }
                                    if (state.challengerTimeMillis != null && state.challengerTimeMillis > 0L) {
                                        append(
                                            String.format(
                                                Locale.US,
                                                " • Beat %.1fs",
                                                state.challengerTimeMillis / 1000.0
                                            )
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onOpenSocialSnapshot,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("hud_share_snapshot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Generate Social Snapshot of Time & Streak",
                                tint = RadiantAmber
                            )
                        }

                        IconButton(
                            onClick = onOpenChallengeLinkGenerator,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("hud_generate_challenge_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Generate Challenge Link for this puzzle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Countdown Timer Pill with subtle high-difficulty tension pulse
                        Surface(
                            color = timerColor.copy(alpha = (0.18f + effectiveShakeIntensity * 0.16f).coerceIn(0f, 0.45f)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .graphicsLayer {
                                    val timerPulse = 1f + (effectiveShakeIntensity * 0.05f * sin(shakeWavePhase * 2f))
                                    scaleX = timerPulse
                                    scaleY = timerPulse
                                }
                                .border(1.dp, timerColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .testTag("countdown_timer_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = timerColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d.%d", secondsLeft / 60, secondsLeft % 60, tenthsLeft),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = timerColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress & Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Spotted: ${state.foundSpotIndices.size}/${state.spots.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${state.score} pts",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        // Consecutive Win Streak Pill in HUD
                        Surface(
                            color = if (state.activeWinStreak >= 2) {
                                RadiantAmber.copy(alpha = 0.22f)
                            } else {
                                Color(0xFF1E293B)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (state.activeWinStreak >= 2) RadiantAmber.copy(alpha = 0.65f) else Color(0xFF334155),
                                    shape = RoundedCornerShape(50)
                                )
                                .testTag("hud_win_streak_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Consecutive Win Streak",
                                    tint = if (state.activeWinStreak > 0) RadiantAmber else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${state.activeWinStreak}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (state.activeWinStreak > 0) RadiantAmber else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFF451A03).copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(1.dp, RadiantAmber.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .clickable { onOpenRewardShop(0) }
                                .testTag("hud_shards_shop_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Starlight Shards & Shop",
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$currentShardBalance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // High-Difficulty Countdown Tension Badge (clickable to toggle screen shake)
                        if (tensionProfile.isHighDifficulty) {
                            val badgeColor = if (effectiveShakeIntensity > 0.15f) CoralError else RadiantAmber
                            Surface(
                                color = if (screenShakeEnabled) {
                                    badgeColor.copy(alpha = 0.2f)
                                } else {
                                    Color(0xFF1E293B)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (screenShakeEnabled) badgeColor.copy(alpha = 0.65f) else Color(0xFF334155),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { onToggleScreenShake() }
                                    .testTag("hud_tension_shake_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = "Toggle High-Difficulty Countdown Screen Shake",
                                        tint = if (screenShakeEnabled) badgeColor else Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (screenShakeEnabled) {
                                            if (effectiveShakeIntensity > 0.08f) "TENSION" else "HIGH DIFF"
                                        } else {
                                            "CALM"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (screenShakeEnabled) badgeColor else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Hint, Single/Sync Zoom Toggle & Layout Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (zoomScaleA > 1.05f || zoomScaleB > 1.05f) {
                            IconButton(
                                onClick = {
                                    zoomScaleA = 1f; panOffsetXA = 0f; panOffsetYA = 0f
                                    zoomScaleB = 1f; panOffsetXB = 0f; panOffsetYB = 0f
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Reset All Zoom",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Toggle between Single-Image Independent Zoom and Synced Zoom
                        IconButton(
                            onClick = onToggleIndependentZoom,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("toggle_zoom_mode_button")
                        ) {
                            Icon(
                                imageVector = if (independentZoom) Icons.Default.LockOpen else Icons.Default.Sync,
                                contentDescription = if (independentZoom) {
                                    "Single-Image Independent Zoom Active"
                                } else {
                                    "Synced Both-Images Zoom Active"
                                },
                                tint = if (independentZoom) EmeraldSuccess else RadiantAmber
                            )
                        }

                        val isHintOnCooldown = state.hintCooldownMillis > 0L && state.hintsRemaining > 0
                        val hintCooldownSeconds = ceil(state.hintCooldownMillis / 1000.0).toInt()
                        val cooldownFraction = if (state.hintTotalCooldownMillis > 0L) {
                            (1f - (state.hintCooldownMillis.toFloat() / state.hintTotalCooldownMillis.toFloat())).coerceIn(0f, 1f)
                        } else 1f

                        FilledTonalButton(
                            onClick = {
                                if (state.hintsRemaining <= 0) {
                                    onOpenRewardShop(2)
                                } else {
                                    onUseHint()
                                }
                            },
                            enabled = (!isHintOnCooldown) && !state.isVictory && !state.isTimeOut,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("use_hint_button")
                        ) {
                            if (isHintOnCooldown) {
                                CircularProgressIndicator(
                                    progress = { cooldownFraction },
                                    modifier = Modifier.size(15.dp),
                                    color = RadiantAmber,
                                    trackColor = RadiantAmber.copy(alpha = 0.25f),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hint ${hintCooldownSeconds}s (${state.hintsRemaining})")
                            } else if (state.hintsRemaining <= 0) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Get Bonus Hints in Shop",
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+Hints")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Use Hint",
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hint (${state.hintsRemaining})")
                            }
                        }

                        IconButton(
                            onClick = onToggleLayoutMode,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("toggle_layout_mode_button")
                        ) {
                            Icon(
                                imageVector = if (sideBySideMode) Icons.Default.ViewAgenda else Icons.Default.ViewColumn,
                                contentDescription = "Switch Side-by-Side or Stacked layout"
                            )
                        }
                    }
                }

                val hintedSpot = state.activeHintIndex?.let { idx -> state.spots.firstOrNull { it.index == idx } }
                AnimatedVisibility(visible = hintedSpot != null) {
                    if (hintedSpot != null) {
                        Surface(
                            color = RadiantAmber.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("active_hint_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Faint circle active: ${hintedSpot.hint}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RadiantAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.hintCooldownMillis > 0L && state.hintsRemaining > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Next in ${ceil(state.hintCooldownMillis / 1000.0).toInt()}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Dual-Image Comparison Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val useSideBySide = sideBySideMode || maxWidth > 620.dp

            val handleTransformA: (Float, Float, Float) -> Unit = { zoomChange, panX, panY ->
                zoomScaleA = (zoomScaleA * zoomChange).coerceIn(1f, 3.5f)
                if (zoomScaleA > 1.02f) {
                    panOffsetXA = (panOffsetXA + panX).coerceIn(-450f, 450f)
                    panOffsetYA = (panOffsetYA + panY).coerceIn(-450f, 450f)
                } else {
                    panOffsetXA = 0f
                    panOffsetYA = 0f
                }
                if (!independentZoom) {
                    zoomScaleB = zoomScaleA
                    panOffsetXB = panOffsetXA
                    panOffsetYB = panOffsetYA
                }
            }

            val handleTransformB: (Float, Float, Float) -> Unit = { zoomChange, panX, panY ->
                zoomScaleB = (zoomScaleB * zoomChange).coerceIn(1f, 3.5f)
                if (zoomScaleB > 1.02f) {
                    panOffsetXB = (panOffsetXB + panX).coerceIn(-450f, 450f)
                    panOffsetYB = (panOffsetYB + panY).coerceIn(-450f, 450f)
                } else {
                    panOffsetXB = 0f
                    panOffsetYB = 0f
                }
                if (!independentZoom) {
                    zoomScaleA = zoomScaleB
                    panOffsetXA = panOffsetXB
                    panOffsetYA = panOffsetYB
                }
            }

            if (useSideBySide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InteractiveSpotPanel(
                        titleBadge = "IMAGE 1 (ORIGINAL)",
                        bitmap = state.originalBitmap,
                        spots = state.spots,
                        foundSpotIndices = state.foundSpotIndices,
                        activeHintIndex = state.activeHintIndex,
                        hintOffsetNormalizedX = state.hintOffsetNormalizedX,
                        hintOffsetNormalizedY = state.hintOffsetNormalizedY,
                        lastTapFeedback = state.lastTapFeedback,
                        zoomScale = zoomScaleA,
                        panOffsetX = panOffsetXA,
                        panOffsetY = panOffsetYA,
                        onTransform = handleTransformA,
                        onResetPanelZoom = {
                            zoomScaleA = 1f; panOffsetXA = 0f; panOffsetYA = 0f
                        },
                        onTapNormalized = onImageTap,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("image_panel_original")
                    )

                    InteractiveSpotPanel(
                        titleBadge = "IMAGE 2 (${state.spots.size - state.foundSpotIndices.size} LEFT)",
                        bitmap = state.modifiedBitmap,
                        spots = state.spots,
                        foundSpotIndices = state.foundSpotIndices,
                        activeHintIndex = state.activeHintIndex,
                        hintOffsetNormalizedX = state.hintOffsetNormalizedX,
                        hintOffsetNormalizedY = state.hintOffsetNormalizedY,
                        lastTapFeedback = state.lastTapFeedback,
                        zoomScale = zoomScaleB,
                        panOffsetX = panOffsetXB,
                        panOffsetY = panOffsetYB,
                        onTransform = handleTransformB,
                        onResetPanelZoom = {
                            zoomScaleB = 1f; panOffsetXB = 0f; panOffsetYB = 0f
                        },
                        onTapNormalized = onImageTap,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("image_panel_modified")
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InteractiveSpotPanel(
                        titleBadge = "IMAGE 1 (ORIGINAL — PINCH TO ZOOM THIS IMAGE ONLY)",
                        bitmap = state.originalBitmap,
                        spots = state.spots,
                        foundSpotIndices = state.foundSpotIndices,
                        activeHintIndex = state.activeHintIndex,
                        hintOffsetNormalizedX = state.hintOffsetNormalizedX,
                        hintOffsetNormalizedY = state.hintOffsetNormalizedY,
                        lastTapFeedback = state.lastTapFeedback,
                        zoomScale = zoomScaleA,
                        panOffsetX = panOffsetXA,
                        panOffsetY = panOffsetYA,
                        onTransform = handleTransformA,
                        onResetPanelZoom = {
                            zoomScaleA = 1f; panOffsetXA = 0f; panOffsetYA = 0f
                        },
                        onTapNormalized = onImageTap,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("image_panel_original")
                    )

                    InteractiveSpotPanel(
                        titleBadge = "IMAGE 2 (MODIFIED — PINCH TO ZOOM THIS IMAGE ONLY)",
                        bitmap = state.modifiedBitmap,
                        spots = state.spots,
                        foundSpotIndices = state.foundSpotIndices,
                        activeHintIndex = state.activeHintIndex,
                        hintOffsetNormalizedX = state.hintOffsetNormalizedX,
                        hintOffsetNormalizedY = state.hintOffsetNormalizedY,
                        lastTapFeedback = state.lastTapFeedback,
                        zoomScale = zoomScaleB,
                        panOffsetX = panOffsetXB,
                        panOffsetY = panOffsetYB,
                        onTransform = handleTransformB,
                        onResetPanelZoom = {
                            zoomScaleB = 1f; panOffsetXB = 0f; panOffsetYB = 0f
                        },
                        onTapNormalized = onImageTap,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("image_panel_modified")
                    )
                }
            }

            // Victory or Time-Out Overlay Modal
            if (state.isVictory || state.isTimeOut) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC060B14)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .padding(16.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (state.isVictory) EmeraldSuccess else CoralError,
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (state.isVictory) Icons.Default.EmojiEvents else Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (state.isVictory) RadiantAmber else CoralError,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (state.isVictory) "All Differences Spotted!" else "Time's Up!",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val elapsedSec = state.elapsedMillis / 1000.0
                            Text(
                                text = if (state.isVictory) {
                                    String.format(
                                        Locale.US,
                                        "Completed in %.1fs • Score: %d pts • #%s",
                                        elapsedSec,
                                        state.score,
                                        state.challengeCode
                                    )
                                } else {
                                    "You spotted ${state.foundSpotIndices.size} of ${state.spots.size} differences (#${state.challengeCode})."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (state.isVictory && state.challengerTimeMillis != null && state.challengerTimeMillis > 0L) {
                                val beatChallenger = state.elapsedMillis <= state.challengerTimeMillis
                                val diffSec = kotlin.math.abs(state.challengerTimeMillis - state.elapsedMillis) / 1000.0
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = (if (beatChallenger) EmeraldSuccess else RadiantAmber).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (beatChallenger) {
                                            String.format(
                                                Locale.US,
                                                "⚡ You beat %s's target time (%.1fs) by %.1fs!",
                                                state.challengerName ?: "Challenger",
                                                state.challengerTimeMillis / 1000.0,
                                                diffSec
                                            )
                                        } else {
                                            String.format(
                                                Locale.US,
                                                "⏱ %.1fs behind %s's %.1fs — Share or Replay to claim #1!",
                                                diffSec,
                                                state.challengerName ?: "Challenger",
                                                state.challengerTimeMillis / 1000.0
                                            )
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (beatChallenger) EmeraldSuccess else RadiantAmber,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (state.isVictory && state.scoreSubmitted) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Recorded on Global & Challenge Leaderboard!",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (state.rewardSummary != null) {
                                LevelRewardBreakdownCard(
                                    rewardSummary = state.rewardSummary,
                                    currentShardBalance = currentShardBalance,
                                    onOpenRewardShop = { onOpenRewardShop(0) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Win Streak Counter & Next AI Level Complexity Scaling Banner
                            val nextEsc = StreakComplexityScaler.resolve(
                                winStreak = state.activeWinStreak,
                                baseComplexity = state.patternComplexity,
                                baseVariationCount = state.variationCount
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (state.isVictory) {
                                    RadiantAmber.copy(alpha = 0.16f)
                                } else {
                                    CoralError.copy(alpha = 0.14f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (state.isVictory) RadiantAmber.copy(alpha = 0.5f) else CoralError.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("victory_win_streak_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = if (state.isVictory) RadiantAmber else CoralError,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (state.isVictory) {
                                                "Consecutive Win Streak: ${state.activeWinStreak} (${nextEsc.tierBadge})"
                                            } else {
                                                "Win Streak Reset to 0"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (state.isVictory) RadiantAmber else CoralError,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (state.isVictory) {
                                                if (nextEsc.isComplexityEscalated) {
                                                    "Next AI level scales to ${nextEsc.effectiveComplexity.displayName} (${nextEsc.effectiveVariationCount} variations • x${String.format(Locale.US, "%.2f", nextEsc.scoreBonusMultiplier)} streak score)"
                                                } else {
                                                    "Win 1 more level in a row to boost AI puzzle complexity!"
                                                }
                                            } else {
                                                "AI puzzle generator returned to your base difficulty settings."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (state.isVictory && state.spotTimestamps.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .testTag("victory_timestamps_summary_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Per-Difference Timestamps Saved to Replay Gallery",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            state.spotTimestamps.forEach { ts ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(
                                                            Locale.US,
                                                            "#%d @ %.1fs (+%.1fs)",
                                                            ts.orderFound,
                                                            ts.foundAtElapsedMillis / 1000.0,
                                                            ts.splitDeltaMillis / 1000.0
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = onWatchInstantReplay,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("victory_watch_replay_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SlowMotionVideo,
                                                contentDescription = null,
                                                tint = RadiantAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Watch Instant Replay & Timestamp Timeline",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Live Shortest Completion Times for this specific puzzle layout
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0B1120).copy(alpha = 0.9f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    ChallengeStandingsCompactList(
                                        challengeCode = state.challengeCode,
                                        standings = challengeStandings
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Share Snapshot of Final Time & Streak + Challenge Link Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onOpenSocialSnapshot,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("victory_share_snapshot_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = RadiantAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share Snapshot",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                FilledTonalButton(
                                    onClick = onOpenChallengeLinkGenerator,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("victory_generate_challenge_link_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Challenge Link",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = onNextLevel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("next_ai_level_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.offlineLevelNumber != null) Icons.Default.OfflineBolt else Icons.Default.AutoAwesome,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.offlineLevelNumber != null) {
                                        "Next Offline Practice Level"
                                    } else {
                                        "Generate Next AI Level"
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onExitToSetup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Back")
                                }
                                OutlinedButton(
                                    onClick = onViewLeaderboard,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Leaderboard")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveSpotPanel(
    titleBadge: String,
    bitmap: Bitmap,
    spots: List<DifferenceSpot>,
    foundSpotIndices: Set<Int>,
    activeHintIndex: Int?,
    hintOffsetNormalizedX: Float,
    hintOffsetNormalizedY: Float,
    lastTapFeedback: TapFeedback?,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onTransform: (Float, Float, Float) -> Unit,
    onResetPanelZoom: () -> Unit,
    onTapNormalized: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spot_ring")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_pulse"
    )
    val hintAlphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha_pulse"
    )

    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = if (zoomScale > 1.05f) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1f || zoomScale > 1.02f) {
                                onTransform(zoom, pan.x, pan.y)
                            }
                        }
                    }
                    .pointerInput(zoomScale, panOffsetX, panOffsetY) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val centerX = w / 2f
                            val centerY = h / 2f
                            val unscaledX = ((tapOffset.x - centerX - panOffsetX) / zoomScale) + centerX
                            val unscaledY = ((tapOffset.y - centerY - panOffsetY) / zoomScale) + centerY
                            val normX = (unscaledX / w).coerceIn(0f, 1f)
                            val normY = (unscaledY / h).coerceIn(0f, 1f)
                            onTapNormalized(normX, normY)
                        }
                    }
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                    }
            ) {
                Image(
                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                    contentDescription = titleBadge,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minDim = size.minDimension

                    // 1. Draw rings around all found spots
                    spots.filter { it.index in foundSpotIndices }.forEach { spot ->
                        val center = Offset(spot.normalizedX * size.width, spot.normalizedY * size.height)
                        val r = spot.normalizedRadius * minDim
                        drawCircle(
                            color = EmeraldSuccess.copy(alpha = 0.25f),
                            radius = r,
                            center = center
                        )
                        drawCircle(
                            color = EmeraldSuccess,
                            radius = r,
                            center = center,
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }

                    // 2. Draw faint circle highlight around the hidden difference (fair region halo)
                    if (activeHintIndex != null) {
                        val hintSpot = spots.firstOrNull { it.index == activeHintIndex && it.index !in foundSpotIndices }
                        if (hintSpot != null) {
                            val hintNormX = (hintSpot.normalizedX + hintOffsetNormalizedX).coerceIn(0.08f, 0.92f)
                            val hintNormY = (hintSpot.normalizedY + hintOffsetNormalizedY).coerceIn(0.08f, 0.92f)
                            val center = Offset(hintNormX * size.width, hintNormY * size.height)
                            val baseRadius = hintSpot.normalizedRadius * minDim * 1.65f
                            val faintRadius = (baseRadius * hintPulse).coerceAtLeast(18f)

                            // Soft radial glow halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        RadiantAmber.copy(alpha = 0.19f * hintAlphaPulse),
                                        RadiantAmber.copy(alpha = 0.08f * hintAlphaPulse),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = faintRadius * 1.18f
                                ),
                                radius = faintRadius * 1.18f,
                                center = center
                            )

                            // Faint dashed outer boundary circle
                            drawCircle(
                                color = RadiantAmber.copy(alpha = 0.42f * hintAlphaPulse),
                                radius = faintRadius,
                                center = center,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                )
                            )

                            // Subtle ultra-faint inner ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.22f * hintAlphaPulse),
                                radius = faintRadius * 0.68f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                    // 3. Draw recent tap feedback marker
                    if (lastTapFeedback != null && !lastTapFeedback.isHit) {
                        val center = Offset(
                            lastTapFeedback.normalizedX * size.width,
                            lastTapFeedback.normalizedY * size.height
                        )
                        val crossSize = 10.dp.toPx()
                        drawLine(
                            color = CoralError,
                            start = Offset(center.x - crossSize, center.y - crossSize),
                            end = Offset(center.x + crossSize, center.y + crossSize),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawLine(
                            color = CoralError,
                            start = Offset(center.x + crossSize, center.y - crossSize),
                            end = Offset(center.x - crossSize, center.y + crossSize),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }

            // Top-Start Corner Label Badge
            Surface(
                color = Color(0xCC090E1A),
                shape = RoundedCornerShape(bottomEnd = 14.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = titleBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Per-Image Zoom Indicator & Reset Pill (Top-End)
            if (zoomScale > 1.05f) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 14.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { onResetPanelZoom() }
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx (Tap to Reset)", zoomScale),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF041019),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WinStreakComplexityCard(
    preferences: AppPreferences,
    onShareSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = remember(
        preferences.currentWinStreak,
        preferences.patternComplexity,
        preferences.variationCount
    ) {
        preferences.currentStreakProfile()
    }
    val bestStreak = kotlin.math.max(preferences.bestWinStreak, preferences.currentWinStreak)
    val accentColor = when {
        profile.tierIndex >= 3 -> Color(0xFFEC4899)
        profile.tierIndex == 2 -> RadiantAmber
        profile.tierIndex == 1 -> EmeraldSuccess
        else -> MaterialTheme.colorScheme.primary
    }
    val progressToMax = (preferences.currentWinStreak.coerceIn(0, 6) / 6f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (preferences.currentWinStreak > 0) 0.55f else 0.28f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("win_streak_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Win Streak: ${preferences.currentWinStreak}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                color = accentColor.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = profile.tierBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = profile.tierDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onShareSnapshot,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("setup_share_streak_snapshot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Share Time & Win Streak Snapshot",
                            tint = accentColor
                        )
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BEST",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$bestStreak",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = RadiantAmber
                            )
                        }
                    }
                }
            }

            // Progress bar across Streak Tiers (0 -> 2 -> 4 -> 6+)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progressToMax },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accentColor,
                    trackColor = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0 Base",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "2+ Heating Up",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (preferences.currentWinStreak >= 2) EmeraldSuccess else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "4+ Blazing",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (preferences.currentWinStreak >= 4) RadiantAmber else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "6+ Overdrive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (preferences.currentWinStreak >= 6) Color(0xFFEC4899) else Color(0xFF94A3B8)
                    )
                }
            }

            // Live Next AI Level Scaling Summary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0B1120).copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (profile.isComplexityEscalated) {
                                "Next AI Level: ${profile.effectiveComplexity.displayName} • ${profile.effectiveVariationCount} spots"
                            } else {
                                "Next AI Level: ${profile.baseComplexity.displayName} • ${profile.baseVariationCount} spots"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "x%.2f Streak Pts", profile.scoreBonusMultiplier),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

