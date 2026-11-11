package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomLevel
import com.example.data.model.DifferenceSpot
import com.example.data.model.GameTheme
import com.example.data.model.PatternComplexity
import com.example.data.model.VariationType
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.CustomDesignerUiState
import com.example.ui.viewmodel.SpotDiffViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun LevelDesignerScreen(
    viewModel: SpotDiffViewModel,
    designerState: CustomDesignerUiState,
    sharedLevelsState: UiState<List<CustomLevel>>,
    myLevelsState: UiState<List<CustomLevel>>,
    onPlayCustomLevel: (CustomLevel) -> Unit,
    onBackToPlay: () -> Unit
) {
    BackHandler(onBack = onBackToPlay)
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 700.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Custom Level Designer",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Create AI-powered puzzle levels, place custom difference hotspots, and share with friends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Create & Share") },
                modifier = Modifier.testTag("tab_create_level")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Friend & Community Levels") },
                modifier = Modifier.testTag("tab_community_levels")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            DesignerEditorTab(
                state = designerState,
                viewModel = viewModel,
                onShareLevel = { level ->
                    viewModel.openChallengeGeneratorForCustomLevel(level)
                    shareLevelViaIntent(context, level)
                },
                onGenerateDraftChallengeLink = {
                    val draftSpots = designerState.spots
                    val code = com.example.data.model.ChallengeLinkPayload.computeDeterministicCode(
                        title = designerState.title.ifBlank { "${designerState.theme.displayName} Custom" },
                        theme = designerState.theme,
                        variationCount = designerState.variationCount,
                        complexity = designerState.patternComplexity,
                        spots = draftSpots
                    )
                    val tempLevel = CustomLevel(
                        id = "draft_$code",
                        userId = viewModel.currentUserId,
                        authorName = "Puzzle Architect",
                        title = designerState.title.ifBlank { "${designerState.theme.displayName} Custom" },
                        scenePrompt = designerState.scenePrompt.ifBlank { designerState.theme.promptStyle },
                        theme = designerState.theme.id,
                        variationCount = designerState.variationCount.toLong(),
                        timeLimitSeconds = designerState.timeLimitSeconds.toLong(),
                        patternComplexity = designerState.patternComplexity.id,
                        visibility = designerState.visibility,
                        shareCode = code,
                        spotsData = DifferenceSpot.serializeList(draftSpots)
                    )
                    viewModel.openChallengeGeneratorForCustomLevel(tempLevel)
                },
                onTestPlayDraft = {
                    val tempLevel = CustomLevel(
                        id = "draft_preview",
                        userId = viewModel.currentUserId,
                        authorName = "You",
                        title = designerState.title.ifBlank { "${designerState.theme.displayName} Custom" },
                        scenePrompt = designerState.scenePrompt.ifBlank { designerState.theme.promptStyle },
                        theme = designerState.theme.id,
                        variationCount = designerState.variationCount.toLong(),
                        timeLimitSeconds = designerState.timeLimitSeconds.toLong(),
                        patternComplexity = designerState.patternComplexity.id,
                        visibility = designerState.visibility,
                        shareCode = "PREVIEW",
                        spotsData = DifferenceSpot.serializeList(designerState.spots)
                    )
                    onPlayCustomLevel(tempLevel)
                }
            )
        } else {
            CommunityLevelsTab(
                designerState = designerState,
                sharedLevelsState = sharedLevelsState,
                myLevelsState = myLevelsState,
                currentUserId = viewModel.currentUserId,
                onSearchCodeChange = viewModel::updateShareCodeSearch,
                onPlayLevel = onPlayCustomLevel,
                onShareLevel = { level ->
                    viewModel.openChallengeGeneratorForCustomLevel(level)
                },
                onDeleteLevel = viewModel::deleteCustomLevel
            )
        }
    }
}

private fun shareLevelViaIntent(context: android.content.Context, level: CustomLevel) {
    val theme = GameTheme.fromId(level.theme)
    val complexity = PatternComplexity.fromId(level.patternComplexity)
    val spots = DifferenceSpot.deserializeList(level.spotsData).takeIf { it.isNotEmpty() }
        ?: com.example.data.model.ChallengeLinkPayload.deterministicSpotsForSeed(
            level.shareCode.ifBlank { "LVL001" },
            level.variationCount.toInt(),
            complexity
        )
    val payload = com.example.data.model.ChallengeLinkPayload(
        challengeCode = level.shareCode.ifBlank { "LVL001" },
        title = level.title,
        scenePrompt = level.scenePrompt,
        theme = theme,
        variationCount = spots.size,
        timeLimitSeconds = level.timeLimitSeconds.toInt().coerceIn(20, 300),
        patternComplexity = complexity,
        spots = spots,
        challengerName = level.authorName.ifBlank { "Puzzle Architect" }
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SpotDiff AI Challenge Link: ${level.title}")
        putExtra(Intent.EXTRA_TEXT, payload.toShareMessage())
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Custom Level Challenge Link"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignerEditorTab(
    state: CustomDesignerUiState,
    viewModel: SpotDiffViewModel,
    onShareLevel: (CustomLevel) -> Unit,
    onGenerateDraftChallengeLink: () -> Unit,
    onTestPlayDraft: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!state.statusMessage.isNullOrBlank()) {
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldSuccess,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Card 1: Title & AI Scene Description
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Level Identity & AI Prompt", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateDesignerTitle,
                    label = { Text("Custom Level Title") },
                    placeholder = { Text("e.g., Floating Lantern Sanctuary") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_title_input")
                )

                OutlinedTextField(
                    value = state.scenePrompt,
                    onValueChange = viewModel::updateDesignerPrompt,
                    label = { Text("Visual Scene Prompt for AI Generator") },
                    placeholder = { Text("Describe the objects, colors, and atmosphere...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_prompt_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = viewModel::enhanceDesignerPromptWithAi,
                        enabled = !state.isEnhancingPrompt,
                        modifier = Modifier.testTag("ai_enhance_prompt_button")
                    ) {
                        if (state.isEnhancingPrompt) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Enhance Scene Prompt")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = state.visibility == "SHARED",
                            onClick = { viewModel.updateDesignerVisibility("SHARED") },
                            label = { Text("Shareable") }
                        )
                        FilterChip(
                            selected = state.visibility == "PRIVATE",
                            onClick = { viewModel.updateDesignerVisibility("PRIVATE") },
                            label = { Text("Private") }
                        )
                    }
                }
            }
        }

        // Card 2: Theme, Variations, Time Limit & Complexity
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Theme & Difficulty Parameters", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = state.theme == theme,
                            onClick = { viewModel.updateDesignerTheme(theme) },
                            label = { Text(theme.displayName) }
                        )
                    }
                }

                Text(
                    text = "Variations Count: ${state.variationCount} differences",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.variationCount.toFloat(),
                    onValueChange = { viewModel.updateDesignerVariationCount(it.roundToInt()) },
                    valueRange = 3f..10f,
                    steps = 6
                )

                Text(
                    text = "Time Limit: ${state.timeLimitSeconds} seconds",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.timeLimitSeconds.toFloat(),
                    onValueChange = { viewModel.updateDesignerTimeLimit(it.roundToInt()) },
                    valueRange = 30f..240f,
                    steps = 13
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PatternComplexity.entries.forEach { comp ->
                        FilterChip(
                            selected = state.patternComplexity == comp,
                            onClick = { viewModel.updateDesignerComplexity(comp) },
                            label = { Text(comp.displayName) }
                        )
                    }
                }
            }
        }

        // Card 3: Interactive Difference Spot Placement Blueprint
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. Place Difference Hotspots (Tap Grid to Position)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Select a spot index below, tap the blueprint canvas to place it, and pick its visual anomaly type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Spot selector pills
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.spots.forEachIndexed { idx, spot ->
                        FilterChip(
                            selected = state.selectedSpotIndex == idx,
                            onClick = { viewModel.selectDesignerSpot(idx) },
                            label = { Text("#${idx + 1}: ${spot.variationType.displayName.substringBefore(" ")}") }
                        )
                    }
                }

                // Variation Type selector for the currently active spot
                val activeSpot = state.spots.getOrNull(state.selectedSpotIndex)
                if (activeSpot != null) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VariationType.entries.forEach { vType ->
                            FilterChip(
                                selected = activeSpot.variationType == vType,
                                onClick = { viewModel.updateSelectedDesignerSpotType(vType) },
                                label = { Text(vType.displayName) }
                            )
                        }
                    }
                }

                // Interactive Blueprint Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0B1120),
                                    Color(state.theme.primaryHex).copy(alpha = 0.25f),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(state.selectedSpotIndex) {
                            detectTapGestures { offset ->
                                val nx = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.12f, 0.88f)
                                val ny = (offset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.12f, 0.88f)
                                viewModel.moveSelectedDesignerSpot(nx, ny)
                            }
                        }
                        .testTag("designer_spot_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Subtle blueprint grid lines
                        val stepX = size.width / 6f
                        val stepY = size.height / 6f
                        for (i in 1..5) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(i * stepX, 0f),
                                end = Offset(i * stepX, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, i * stepY),
                                end = Offset(size.width, i * stepY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw each difference hotspot
                        state.spots.forEachIndexed { idx, spot ->
                            val isSelected = idx == state.selectedSpotIndex
                            val center = Offset(spot.normalizedX * size.width, spot.normalizedY * size.height)
                            val radius = spot.normalizedRadius * size.minDimension
                            val ringColor = if (isSelected) RadiantAmber else Color(0xFF38BDF8)

                            drawCircle(
                                color = ringColor.copy(alpha = if (isSelected) 0.32f else 0.16f),
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = ringColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx())
                            )
                        }
                    }

                    Text(
                        text = "Spot #${state.selectedSpotIndex + 1} Selected — Tap anywhere to reposition",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    )
                }
            }
        }

        // Action Buttons: Test Play, Generate Challenge Link, & Publish to Cloud
        FilledTonalButton(
            onClick = onGenerateDraftChallengeLink,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("designer_generate_challenge_link_button")
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Challenge Link for This Layout", fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onTestPlayDraft,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("test_play_custom_level_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Play")
            }

            Button(
                onClick = {
                    viewModel.publishCustomLevel { createdLevel ->
                        if (createdLevel.visibility == "SHARED") {
                            onShareLevel(createdLevel)
                        }
                    }
                },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(54.dp)
                    .testTag("publish_custom_level_button")
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish & Share")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CommunityLevelsTab(
    designerState: CustomDesignerUiState,
    sharedLevelsState: UiState<List<CustomLevel>>,
    myLevelsState: UiState<List<CustomLevel>>,
    currentUserId: String,
    onSearchCodeChange: (String) -> Unit,
    onPlayLevel: (CustomLevel) -> Unit,
    onShareLevel: (CustomLevel) -> Unit,
    onDeleteLevel: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = designerState.shareCodeSearchQuery,
            onValueChange = onSearchCodeChange,
            label = { Text("Search by Friend Share Code or Title") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_code_search_input")
        )

        val combinedLevels = remember(sharedLevelsState, myLevelsState, designerState.shareCodeSearchQuery) {
            val shared = (sharedLevelsState as? UiState.Success)?.data.orEmpty()
            val mine = (myLevelsState as? UiState.Success)?.data.orEmpty()
            val merged = (mine + shared).distinctBy { it.id }
            val q = designerState.shareCodeSearchQuery.trim()
            if (q.isBlank()) {
                merged
            } else {
                merged.filter {
                    it.shareCode.contains(q, ignoreCase = true) ||
                        it.title.contains(q, ignoreCase = true) ||
                        it.authorName.contains(q, ignoreCase = true)
                }
            }
        }

        when {
            sharedLevelsState is UiState.Loading && myLevelsState is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            combinedLevels.isEmpty() -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Custom Levels Found Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Switch to 'Create & Share' to publish your first custom AI level and share the code with friends!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(combinedLevels, key = { it.id }) { level ->
                        val theme = GameTheme.fromId(level.theme)
                        val isOwner = level.userId == currentUserId

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(theme.primaryHex))
                                        )
                                        Text(
                                            text = level.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "CODE: ${level.shareCode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = level.scenePrompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "By ${level.authorName} • ${level.variationCount} spots • ${level.timeLimitSeconds}s",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { onShareLevel(level) },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share level with friends",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (isOwner) {
                                            IconButton(
                                                onClick = { onDeleteLevel(level.id) },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete level",
                                                    tint = CoralError
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { onPlayLevel(level) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
