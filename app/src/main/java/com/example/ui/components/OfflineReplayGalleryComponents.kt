package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OfflinePracticeLevel
import com.example.data.model.OfflineReplayRecord
import com.example.data.model.ReplaySpotTimestamp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import kotlinx.coroutines.delay
import java.util.Locale

enum class ReplaySortOption(val label: String) {
    FASTEST_TIME("Fastest Time"),
    FASTEST_SPLIT("Quickest Spot Split"),
    HIGHEST_SCORE("Highest Score"),
    MOST_RECENT("Most Recent")
}

fun buildEffectiveReplayGallery(
    savedReplays: List<OfflineReplayRecord>,
    completedOfflineLevels: Set<Int>,
    allOfflineLevels: List<OfflinePracticeLevel>
): List<OfflineReplayRecord> {
    val byId = LinkedHashMap<String, OfflineReplayRecord>()
    savedReplays.forEach { rec ->
        byId[rec.replayId.lowercase()] = rec
    }

    // Ensure every completed offline level has a replay record in the gallery
    completedOfflineLevels.sorted().forEach { levelNum ->
        val key = "offline_$levelNum"
        if (key !in byId) {
            val spec = allOfflineLevels.firstOrNull { it.levelNumber == levelNum }
            if (spec != null) {
                byId[key] = OfflineReplayRecord.createSyntheticReplayForOfflineLevel(spec)
            }
        }
    }

    // If the player hasn't recorded any replays yet, provide 3 pre-loaded local replay showcases
    // from Offline Practice levels #1, #21, and #61 so the gallery and timestamp scrubber work immediately.
    if (byId.isEmpty() && allOfflineLevels.isNotEmpty()) {
        listOf(1, 21, 61).forEachIndexed { idx, levelNum ->
            val spec = allOfflineLevels.firstOrNull { it.levelNumber == levelNum } ?: allOfflineLevels.first()
            val sampleTime = when (idx) {
                0 -> 14_800L
                1 -> 18_400L
                else -> 21_600L
            }
            val rec = OfflineReplayRecord.createSyntheticReplayForOfflineLevel(
                spec = spec,
                completionTimeMillis = sampleTime,
                winStreak = idx + 2
            )
            byId[rec.replayId.lowercase()] = rec
        }
    }

    return byId.values.toList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineReplayGalleryView(
    replays: List<OfflineReplayRecord>,
    personalSavedIds: Set<String>,
    onWatchReplay: (OfflineReplayRecord) -> Unit,
    onPlayLevelAgain: (OfflineReplayRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOption by rememberSaveable { mutableStateOf(ReplaySortOption.FASTEST_TIME) }

    val sortedReplays = remember(replays, sortOption) {
        when (sortOption) {
            ReplaySortOption.FASTEST_TIME -> replays.sortedBy { it.completionTimeMillis }
            ReplaySortOption.FASTEST_SPLIT -> replays.sortedBy { it.fastestSplitMillis }
            ReplaySortOption.HIGHEST_SCORE -> replays.sortedByDescending { it.score }
            ReplaySortOption.MOST_RECENT -> replays.sortedByDescending { it.recordedAtEpochMillis }
        }
    }

    val bestOverallTimeMs = remember(replays) {
        replays.minOfOrNull { it.completionTimeMillis } ?: 0L
    }
    val fastestSingleSplitMs = remember(replays) {
        replays.minOfOrNull { it.fastestSplitMillis } ?: 0L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("offline_replay_gallery_view")
    ) {
        // Gallery Summary Telemetry Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReplayMetricStat(
                    label = "Saved Replays",
                    value = "${replays.size}",
                    accentColor = MaterialTheme.colorScheme.primary
                )
                ReplayMetricStat(
                    label = "Best Clear Time",
                    value = formatMillisSec(bestOverallTimeMs),
                    accentColor = EmeraldSuccess
                )
                ReplayMetricStat(
                    label = "Fastest Spot Split",
                    value = "+${formatMillisSec(fastestSingleSplitMs)}",
                    accentColor = RadiantAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sort Options Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReplaySortOption.entries.forEach { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { sortOption = option },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag("replay_sort_${option.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("offline_replay_list")
        ) {
            itemsIndexed(sortedReplays, key = { _, item -> item.replayId }) { index, record ->
                val isPersonalSaved = record.replayId.lowercase() in personalSavedIds
                OfflineReplayPerformanceCard(
                    rank = index + 1,
                    record = record,
                    isPersonalSaved = isPersonalSaved,
                    onWatchReplay = { onWatchReplay(record) },
                    onPlayLevelAgain = { onPlayLevelAgain(record) }
                )
            }
        }
    }
}

@Composable
private fun ReplayMetricStat(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineReplayPerformanceCard(
    rank: Int,
    record: OfflineReplayRecord,
    isPersonalSaved: Boolean,
    onWatchReplay: () -> Unit,
    onPlayLevelAgain: () -> Unit
) {
    val themeColor = Color(record.theme.primaryHex)
    val fastestSplit = record.fastestSplitMillis

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = if (rank == 1) 1.5.dp else 1.dp,
            color = if (rank == 1) RadiantAmber.copy(alpha = 0.75f) else themeColor.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("replay_card_${record.replayId.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Rank + Title + Total Completion Time Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (rank == 1) RadiantAmber.copy(alpha = 0.22f) else themeColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (rank == 1) "#1 BEST" else "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (rank == 1) RadiantAmber else themeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.levelTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(record.theme.displayName)
                                append(" • ")
                                append(record.complexity.displayName)
                                append(" • ")
                                append("${record.variationCount} Spots")
                                if (record.challengeCode.isNotBlank()) {
                                    append(" • #${record.challengeCode}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatMillisSec(record.completionTimeMillis),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            // Secondary Metadata Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${record.score} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = RadiantAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = RadiantAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Fastest Split: +${formatMillisSec(fastestSplit)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RadiantAmber
                        )
                    }
                }

                if (isPersonalSaved) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Personal Best",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }

            // Difference Identification Timestamps Strip
            Surface(
                color = Color(0xFF0B1120).copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Difference Identification Timestamps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "Avg ${formatMillisSec(record.averageSplitMillis)} / spot",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        record.spotTimestamps.forEach { ts ->
                            val isFastest = ts.splitDeltaMillis == fastestSplit
                            val spotColor = parseHexColorOrDefault(ts.accentHex, themeColor)
                            Surface(
                                color = spotColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isFastest) RadiantAmber else spotColor.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${ts.orderFound}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = spotColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${formatMillisSec(ts.foundAtElapsedMillis)} (+${formatMillisSec(ts.splitDeltaMillis)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = if (isFastest) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Watch Replay & Play Level Again
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onWatchReplay,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF041019)
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("watch_replay_${record.replayId.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Replay & Timestamps",
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                FilledTonalButton(
                    onClick = onPlayLevelAgain,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("replay_play_again_${record.replayId.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Replay Level",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineReplayTheaterModal(
    record: OfflineReplayRecord,
    originalBitmap: Bitmap?,
    modifiedBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onPlayLevelNow: (OfflineReplayRecord) -> Unit
) {
    val totalDurationMs = record.completionTimeMillis.coerceAtLeast(1000L)
    var playheadMillis by remember(record.replayId) { mutableLongStateOf(0L) }
    var isPlaying by remember(record.replayId) { mutableStateOf(true) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    var selectedSpotOrder by remember(record.replayId) { mutableIntStateOf(0) }

    // Advance playhead smoothly while playing
    LaunchedEffect(isPlaying, playbackSpeed, record.replayId) {
        val frameMs = 50L
        while (isPlaying) {
            delay(frameMs)
            val delta = (frameMs * playbackSpeed).toLong()
            val next = playheadMillis + delta
            if (next >= totalDurationMs) {
                playheadMillis = totalDurationMs
                isPlaying = false
            } else {
                playheadMillis = next
            }
        }
    }

    val revealedTimestamps = remember(playheadMillis, record.spotTimestamps) {
        record.spotTimestamps.filter { it.foundAtElapsedMillis <= playheadMillis }
    }
    val activeHighlightTimestamp = remember(playheadMillis, record.spotTimestamps, selectedSpotOrder) {
        record.spotTimestamps.firstOrNull { it.orderFound == selectedSpotOrder }
            ?: revealedTimestamps.lastOrNull()
    }

    val pulseTransition = rememberInfiniteTransition(label = "replay_reticle_pulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "replay_pulse_progress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp)
                .testTag("offline_replay_theater_modal"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(record.theme.primaryHex),
                        RadiantAmber,
                        Color(record.theme.secondaryHex)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(record.theme.primaryHex).copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = null,
                                    tint = Color(record.theme.primaryHex),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline Replay Theater",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${record.levelTitle} • Cleared in ${formatMillisSec(record.completionTimeMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_replay_theater_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Replay Theater"
                        )
                    }
                }

                // Side-by-Side Local Scene Replay Viewport
                if (originalBitmap == null || modifiedBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0B1120)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rendering saved local puzzle scene...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReplayScenePanel(
                            badgeLabel = "ORIGINAL",
                            bitmap = originalBitmap,
                            revealedTimestamps = revealedTimestamps,
                            activeTimestamp = activeHighlightTimestamp,
                            pulseProgress = pulseProgress,
                            onSpotTap = { ts ->
                                isPlaying = false
                                selectedSpotOrder = ts.orderFound
                                playheadMillis = ts.foundAtElapsedMillis
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ReplayScenePanel(
                            badgeLabel = "MODIFIED (${revealedTimestamps.size}/${record.spotTimestamps.size} SPOTTED)",
                            bitmap = modifiedBitmap,
                            revealedTimestamps = revealedTimestamps,
                            activeTimestamp = activeHighlightTimestamp,
                            pulseProgress = pulseProgress,
                            onSpotTap = { ts ->
                                isPlaying = false
                                selectedSpotOrder = ts.orderFound
                                playheadMillis = ts.foundAtElapsedMillis
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Active Difference Callout Banner
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (activeHighlightTimestamp != null) RadiantAmber.copy(alpha = 0.55f) else Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeHighlightTimestamp != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Spot #${activeHighlightTimestamp.orderFound}: ${activeHighlightTimestamp.label} (${activeHighlightTimestamp.variationType.displayName})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RadiantAmber
                                )
                                Text(
                                    text = "Identified at ${formatMillisSec(activeHighlightTimestamp.foundAtElapsedMillis)} • Split time: +${formatMillisSec(activeHighlightTimestamp.splitDeltaMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        } else {
                            Text(
                                text = "Scrub or press Play to watch each difference get identified in chronological order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Timeline Scrubber & Transport Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playhead: ${formatMillisSec(playheadMillis)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${revealedTimestamps.size} / ${record.spotTimestamps.size} Differences Identified",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Total: ${formatMillisSec(totalDurationMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = playheadMillis.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                            onValueChange = { value ->
                                isPlaying = false
                                selectedSpotOrder = 0
                                playheadMillis = value.toLong()
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = RadiantAmber,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("replay_timeline_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step Previous Spot
                            IconButton(
                                onClick = {
                                    isPlaying = false
                                    val prev = record.spotTimestamps
                                        .lastOrNull { it.foundAtElapsedMillis < playheadMillis - 100L }
                                    if (prev != null) {
                                        selectedSpotOrder = prev.orderFound
                                        playheadMillis = prev.foundAtElapsedMillis
                                    } else {
                                        selectedSpotOrder = 0
                                        playheadMillis = 0L
                                    }
                                },
                                modifier = Modifier.testTag("replay_step_prev_button")
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Difference")
                            }

                            // Play / Pause Button
                            Button(
                                onClick = {
                                    selectedSpotOrder = 0
                                    if (playheadMillis >= totalDurationMs) {
                                        playheadMillis = 0L
                                        isPlaying = true
                                    } else {
                                        isPlaying = !isPlaying
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("replay_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isPlaying -> "Pause"
                                        playheadMillis >= totalDurationMs -> "Replay From 0.0s"
                                        else -> "Play"
                                    },
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Step Next Spot
                            IconButton(
                                onClick = {
                                    isPlaying = false
                                    val next = record.spotTimestamps
                                        .firstOrNull { it.foundAtElapsedMillis > playheadMillis + 50L }
                                        ?: record.spotTimestamps.lastOrNull()
                                    if (next != null) {
                                        selectedSpotOrder = next.orderFound
                                        playheadMillis = next.foundAtElapsedMillis
                                    }
                                },
                                modifier = Modifier.testTag("replay_step_next_button")
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Difference")
                            }

                            // Playback Speed Toggle (1x / 2x)
                            OutlinedButton(
                                onClick = {
                                    playbackSpeed = if (playbackSpeed >= 1.9f) 1f else 2f
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("replay_speed_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${playbackSpeed.toInt()}x",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Chronological Difference Timestamp Breakdown List
                Text(
                    text = "Chronological Difference Timestamp Log (Tap any row to jump)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                val fastestSplit = record.fastestSplitMillis
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    record.spotTimestamps.forEach { ts ->
                        val isRevealed = ts.foundAtElapsedMillis <= playheadMillis
                        val isSelected = activeHighlightTimestamp?.orderFound == ts.orderFound
                        val isFastest = ts.splitDeltaMillis == fastestSplit
                        val spotColor = parseHexColorOrDefault(ts.accentHex, Color(record.theme.primaryHex))

                        Surface(
                            color = when {
                                isSelected -> spotColor.copy(alpha = 0.22f)
                                isRevealed -> Color(0xFF1E293B).copy(alpha = 0.85f)
                                else -> Color(0xFF0F172A).copy(alpha = 0.65f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = when {
                                    isSelected -> RadiantAmber
                                    isRevealed -> spotColor.copy(alpha = 0.55f)
                                    else -> Color(0xFF334155)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    isPlaying = false
                                    selectedSpotOrder = ts.orderFound
                                    playheadMillis = ts.foundAtElapsedMillis
                                }
                                .testTag("replay_timestamp_row_${ts.orderFound}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(spotColor.copy(alpha = 0.25f))
                                            .border(1.5.dp, spotColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${ts.orderFound}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ts.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (isFastest) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = RadiantAmber.copy(alpha = 0.22f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "FASTEST SPLIT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = RadiantAmber,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${ts.variationType.displayName} • Coords (${(ts.normalizedX * 100).toInt()}%, ${(ts.normalizedY * 100).toInt()}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "@ ${formatMillisSec(ts.foundAtElapsedMillis)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isRevealed) EmeraldSuccess else Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "Split +${formatMillisSec(ts.splitDeltaMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RadiantAmber
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Bottom CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Gallery", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onPlayLevelNow(record) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF041019)
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("replay_theater_play_level_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Challenge Your Best Time",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayScenePanel(
    badgeLabel: String,
    bitmap: Bitmap,
    revealedTimestamps: List<ReplaySpotTimestamp>,
    activeTimestamp: ReplaySpotTimestamp?,
    pulseProgress: Float,
    onSpotTap: (ReplaySpotTimestamp) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = badgeLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            revealedTimestamps.forEach { ts ->
                val center = Offset(ts.normalizedX * size.width, ts.normalizedY * size.height)
                val baseRadius = (minDim * 0.085f).coerceAtLeast(16f)
                val isCurrentFocus = activeTimestamp?.orderFound == ts.orderFound
                val ringColor = if (isCurrentFocus) RadiantAmber else EmeraldSuccess

                if (isCurrentFocus) {
                    val sonarRadius = baseRadius * (1.15f + 0.65f * pulseProgress)
                    drawCircle(
                        color = RadiantAmber.copy(alpha = (1f - pulseProgress) * 0.65f),
                        radius = sonarRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Target crosshairs
                    drawLine(
                        color = RadiantAmber.copy(alpha = 0.85f),
                        start = Offset(center.x - baseRadius * 1.4f, center.y),
                        end = Offset(center.x + baseRadius * 1.4f, center.y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                    drawLine(
                        color = RadiantAmber.copy(alpha = 0.85f),
                        start = Offset(center.x, center.y - baseRadius * 1.4f),
                        end = Offset(center.x, center.y + baseRadius * 1.4f),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                drawCircle(
                    color = ringColor.copy(alpha = 0.24f),
                    radius = baseRadius,
                    center = center
                )
                drawCircle(
                    color = ringColor,
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = if (isCurrentFocus) 3.dp.toPx() else 2.dp.toPx())
                )
            }
        }

        Surface(
            color = Color(0xCC090E1A),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun formatMillisSec(millis: Long): String =
    String.format(Locale.US, "%.1fs", millis.coerceAtLeast(0L) / 1000.0)

private fun parseHexColorOrDefault(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
