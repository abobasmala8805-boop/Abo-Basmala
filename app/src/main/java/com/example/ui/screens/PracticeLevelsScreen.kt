package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OfflinePracticeLevel
import com.example.data.model.OfflineReplayRecord
import com.example.ui.components.OfflineReplayGalleryView
import com.example.ui.components.buildEffectiveReplayGallery
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import java.util.Locale

@Composable
fun PracticeLevelsScreen(
    levels: List<OfflinePracticeLevel>,
    completedLevels: Set<Int>,
    savedReplayRecords: List<OfflineReplayRecord> = emptyList(),
    initialSectionTab: Int = 0,
    onSelectOfflineLevel: (Int) -> Unit,
    onWatchReplay: (OfflineReplayRecord) -> Unit = {},
    onPlayFromReplay: (OfflineReplayRecord) -> Unit = {},
    onBackToPlay: () -> Unit
) {
    BackHandler(onBack = onBackToPlay)
    var selectedSection by rememberSaveable { mutableIntStateOf(initialSectionTab) }
    var selectedChapter by rememberSaveable { mutableIntStateOf(0) } // 0 = All, 1..5 = Chapters

    LaunchedEffect(initialSectionTab) {
        selectedSection = initialSectionTab
    }

    val effectiveReplays = remember(savedReplayRecords, completedLevels, levels) {
        buildEffectiveReplayGallery(
            savedReplays = savedReplayRecords,
            completedOfflineLevels = completedLevels,
            allOfflineLevels = levels
        )
    }
    val personalSavedIds = remember(savedReplayRecords, completedLevels) {
        val fromRecords = savedReplayRecords.map { it.replayId.lowercase() }
        val fromCompleted = completedLevels.map { "offline_$it" }
        (fromRecords + fromCompleted).toSet()
    }
    val replayByOfflineLevel = remember(effectiveReplays) {
        effectiveReplays.mapNotNull { rec ->
            rec.offlineLevelNumber?.let { num -> num to rec }
        }.toMap()
    }

    val chapters = listOf(
        0 to "All 100 Levels",
        1 to "1–20: Mystic Nature",
        2 to "21–40: Deep Space",
        3 to "41–60: Geometric",
        4 to "61–80: Synthwave",
        5 to "81–100: Relics"
    )

    val filteredLevels = remember(levels, selectedChapter) {
        when (selectedChapter) {
            1 -> levels.filter { it.levelNumber in 1..20 }
            2 -> levels.filter { it.levelNumber in 21..40 }
            3 -> levels.filter { it.levelNumber in 41..60 }
            4 -> levels.filter { it.levelNumber in 61..80 }
            5 -> levels.filter { it.levelNumber in 81..100 }
            else -> levels
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (selectedSection == 0) Icons.Default.OfflineBolt else Icons.Default.SlowMotionVideo,
                    contentDescription = null,
                    tint = if (selectedSection == 0) EmeraldSuccess else RadiantAmber,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (selectedSection == 0) {
                            "100 Offline Practice Levels"
                        } else {
                            "Offline Replay Gallery"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = if (selectedSection == 0) {
                            "Pre-loaded visual scenes — instant play with zero internet required."
                        } else {
                            "Review your best saved local performances & per-difference timestamps."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Mode Switcher: 100 Offline Levels vs Offline Replay Gallery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PracticeModeSegmentTab(
                title = "100 Offline Levels (${completedLevels.size}/100)",
                icon = Icons.Default.OfflineBolt,
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("practice_mode_levels")
            )
            PracticeModeSegmentTab(
                title = "Replay Gallery (${effectiveReplays.size})",
                icon = Icons.Default.SlowMotionVideo,
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("practice_mode_replays")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedSection == 1) {
            OfflineReplayGalleryView(
                replays = effectiveReplays,
                personalSavedIds = personalSavedIds,
                onWatchReplay = onWatchReplay,
                onPlayLevelAgain = onPlayFromReplay,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Completion Progress Bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Practice Mastery Progress",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${completedLevels.size} / 100 Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (completedLevels.size / 100f).coerceIn(0f, 1f) },
                        color = EmeraldSuccess,
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chapter Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chapters.forEach { (chapterIdx, label) ->
                    FilterChip(
                        selected = selectedChapter == chapterIdx,
                        onClick = { selectedChapter = chapterIdx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("practice_chapter_$chapterIdx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 100 Offline Levels Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 155.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("offline_levels_grid")
            ) {
                items(filteredLevels, key = { it.levelNumber }) { level ->
                    val isCompleted = level.levelNumber in completedLevels
                    val savedReplay = replayByOfflineLevel[level.levelNumber]
                    val themeColor = Color(level.theme.primaryHex)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) {
                                EmeraldSuccess.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.dp,
                                color = if (isCompleted) EmeraldSuccess else themeColor.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectOfflineLevel(level.levelNumber) }
                            .testTag("offline_level_card_${level.levelNumber}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = themeColor.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "LEVEL #${level.levelNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = themeColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (isCompleted) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (savedReplay != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(RadiantAmber.copy(alpha = 0.22f))
                                                    .clickable { onWatchReplay(savedReplay) }
                                                    .testTag("offline_card_watch_replay_${level.levelNumber}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SlowMotionVideo,
                                                    contentDescription = "Watch Replay",
                                                    tint = RadiantAmber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = themeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = level.title.substringAfter(": "),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = level.theme.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${level.variationCount} Spots",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadiantAmber
                                )
                                if (savedReplay != null && isCompleted) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Best %.1fs",
                                                savedReplay.completionTimeMillis / 1000.0
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${level.timeLimitSeconds}s • ${level.complexity.id.take(3)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF94A3B8)
                                    )
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
private fun PracticeModeSegmentTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
