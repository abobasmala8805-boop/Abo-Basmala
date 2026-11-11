package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppPreferences
import com.example.data.model.GameTheme
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LevelHistoryPoint
import com.example.data.model.OfflinePracticeLevel
import com.example.data.model.PatternComplexity
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ThemeTimeStat(
    val theme: GameTheme,
    val averageSeconds: Float,
    val levelsCount: Int,
    val differencesCount: Int
)

data class ComplexityDiffStat(
    val complexity: PatternComplexity,
    val differencesFound: Int,
    val fraction: Float,
    val color: Color
)

data class PlayerComputedStats(
    val totalDifferencesFound: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val totalGamesPlayed: Int,
    val winRatePercent: Float,
    val winLossRatioLabel: String,
    val averageTimeSeconds: Float,
    val bestTimeSeconds: Float,
    val spotterRankTitle: String,
    val nextMilestoneTarget: Int,
    val themeTimeStats: List<ThemeTimeStat>,
    val complexityDiffStats: List<ComplexityDiffStat>,
    val recentHistory: List<LevelHistoryPoint>
)

private fun computePlayerAnalytics(
    preferences: AppPreferences,
    offlineLevels: List<OfflinePracticeLevel>,
    userLeaderboardEntries: List<LeaderboardEntry>
): PlayerComputedStats {
    // Synthesize any completed offline levels or cloud entries if history doesn't already cover them
    val offlineLevelMap = offlineLevels.associateBy { it.levelNumber }
    val derivedFromOffline = preferences.completedOfflineLevels.sorted().mapNotNull { num ->
        val spec = offlineLevelMap[num] ?: return@mapNotNull null
        LevelHistoryPoint(
            label = "Practice #$num",
            themeId = spec.theme.id,
            complexityId = spec.complexity.id,
            durationSeconds = (spec.timeLimitSeconds * 0.48f).coerceIn(18f, 95f),
            differencesFound = spec.variationCount,
            isWin = true
        )
    }

    val derivedFromCloud = userLeaderboardEntries.map { entry ->
        LevelHistoryPoint(
            label = entry.levelTitle.ifBlank { "${entry.theme} Run" },
            themeId = entry.theme,
            complexityId = entry.patternComplexity,
            durationSeconds = (entry.completionTimeMillis / 1000f).coerceIn(5f, 300f),
            differencesFound = entry.variationCount.toInt().coerceIn(3, 10),
            isWin = true
        )
    }

    val combinedHistory = buildList {
        addAll(preferences.recentLevelHistory)
        if (preferences.recentLevelHistory.size < derivedFromOffline.size) {
            addAll(derivedFromOffline.takeLast(12))
        }
        if (preferences.recentLevelHistory.isEmpty() && derivedFromCloud.isNotEmpty()) {
            addAll(derivedFromCloud.take(10))
        }
    }.takeLast(24)

    val winsFromHistoryOrPrefs = max(
        preferences.gamesWon,
        max(preferences.completedOfflineLevels.size, userLeaderboardEntries.size)
    )
    val lossesFromPrefs = preferences.gamesLost
    val totalGames = winsFromHistoryOrPrefs + lossesFromPrefs

    val derivedDiffs = combinedHistory.sumOf { it.differencesFound }
    val totalDiffs = max(preferences.totalDifferencesFound, derivedDiffs)

    val winRatePercent = if (totalGames > 0) {
        (winsFromHistoryOrPrefs.toFloat() / totalGames.toFloat()) * 100f
    } else {
        0f
    }

    val winLossRatioLabel = when {
        totalGames == 0 -> "0 : 0"
        lossesFromPrefs == 0 -> String.format(Locale.US, "%d.0 : 0", winsFromHistoryOrPrefs)
        else -> String.format(
            Locale.US,
            "%.2f : 1",
            winsFromHistoryOrPrefs.toFloat() / lossesFromPrefs.toFloat()
        )
    }

    val winHistory = combinedHistory.filter { it.isWin }
    val avgTimeSec = when {
        preferences.gamesWon > 0 && preferences.totalWinTimeMillis > 0L -> {
            (preferences.totalWinTimeMillis.toFloat() / preferences.gamesWon.toFloat()) / 1000f
        }
        winHistory.isNotEmpty() -> {
            winHistory.map { it.durationSeconds }.average().toFloat()
        }
        else -> 0f
    }

    val bestTimeSec = when {
        preferences.bestWinTimeMillis > 0L -> preferences.bestWinTimeMillis / 1000f
        winHistory.isNotEmpty() -> winHistory.minOf { it.durationSeconds }
        else -> 0f
    }

    val rankAndMilestone = when {
        totalDiffs >= 250 -> "Apex Chrono Observer" to 500
        totalDiffs >= 100 -> "Grandmaster Detective" to 250
        totalDiffs >= 40 -> "Eagle-Eyed Spotter" to 100
        totalDiffs >= 10 -> "Field Investigator" to 40
        else -> "Rookie Spotter" to 10
    }

    val themeStats = GameTheme.entries.map { theme ->
        val matching = combinedHistory.filter { it.themeId.equals(theme.id, ignoreCase = true) }
        val avgForTheme = if (matching.isNotEmpty()) {
            matching.map { it.durationSeconds }.average().toFloat()
        } else {
            0f
        }
        val diffsForTheme = matching.sumOf { it.differencesFound }
        ThemeTimeStat(
            theme = theme,
            averageSeconds = avgForTheme,
            levelsCount = matching.size,
            differencesCount = diffsForTheme
        )
    }

    val complexityColors = mapOf(
        PatternComplexity.SUBTLE to Color(0xFF10B981),
        PatternComplexity.MODERATE to Color(0xFF38BDF8),
        PatternComplexity.INTRICATE to Color(0xFFF59E0B)
    )

    val rawComplexityCounts = PatternComplexity.entries.associateWith { comp ->
        val historyCount = combinedHistory
            .filter { it.complexityId.equals(comp.id, ignoreCase = true) }
            .sumOf { it.differencesFound }
        if (historyCount > 0) {
            historyCount
        } else if (totalDiffs > 0 && preferences.patternComplexity == comp) {
            totalDiffs
        } else {
            0
        }
    }

    val complexitySum = rawComplexityCounts.values.sum().coerceAtLeast(1)
    val complexityStats = PatternComplexity.entries.map { comp ->
        val count = rawComplexityCounts[comp] ?: 0
        ComplexityDiffStat(
            complexity = comp,
            differencesFound = count,
            fraction = if (rawComplexityCounts.values.sum() > 0) {
                (count.toFloat() / complexitySum.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            },
            color = complexityColors[comp] ?: Color(0xFF38BDF8)
        )
    }

    return PlayerComputedStats(
        totalDifferencesFound = totalDiffs,
        gamesWon = winsFromHistoryOrPrefs,
        gamesLost = lossesFromPrefs,
        totalGamesPlayed = totalGames,
        winRatePercent = winRatePercent,
        winLossRatioLabel = winLossRatioLabel,
        averageTimeSeconds = avgTimeSec,
        bestTimeSeconds = bestTimeSec,
        spotterRankTitle = rankAndMilestone.first,
        nextMilestoneTarget = rankAndMilestone.second,
        themeTimeStats = themeStats,
        complexityDiffStats = complexityStats,
        recentHistory = combinedHistory
    )
}

@Composable
fun ProfileStatsDashboard(
    preferences: AppPreferences,
    offlineLevels: List<OfflinePracticeLevel>,
    userLeaderboardEntries: List<LeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    val stats = remember(preferences, offlineLevels, userLeaderboardEntries) {
        computePlayerAnalytics(
            preferences = preferences,
            offlineLevels = offlineLevels,
            userLeaderboardEntries = userLeaderboardEntries
        )
    }

    var timeChartMode by rememberSaveable { mutableStateOf("THEME") } // "THEME" or "TREND"

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "statsChartAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_stats_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column {
                    Text(
                        text = "Player Analytics & Performance Charts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rank: ${stats.spotterRankTitle} • ${stats.totalGamesPlayed} sessions tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Top 3 KPI Summary Cards (Total Differences Found, Average Time / Level, Win-Loss Ratio)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_stats_kpi_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatCard(
                title = "Differences",
                value = stats.totalDifferencesFound.toString(),
                subtitle = "Total Spotted",
                accentColor = MaterialTheme.colorScheme.primary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_total_differences")
            )

            KpiStatCard(
                title = "Avg Time",
                value = if (stats.averageTimeSeconds > 0f) {
                    String.format(Locale.US, "%.1fs", stats.averageTimeSeconds)
                } else {
                    "--"
                },
                subtitle = if (stats.bestTimeSeconds > 0f) {
                    String.format(Locale.US, "Best: %.1fs", stats.bestTimeSeconds)
                } else {
                    "Per Level"
                },
                accentColor = RadiantAmber,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = RadiantAmber,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_avg_time")
            )

            KpiStatCard(
                title = "Win / Loss",
                value = stats.winLossRatioLabel,
                subtitle = String.format(Locale.US, "%.0f%% Win Rate", stats.winRatePercent),
                accentColor = EmeraldSuccess,
                icon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_win_loss")
            )
        }

        // 2. Win-Loss Ratio Donut Chart Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chart_win_loss_ratio")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Win–Loss Ratio & Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Visualizing cleared levels vs. time-out defeats across AI & Practice modes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Radial Donut Chart
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WinLossDonutCanvas(
                            wins = stats.gamesWon,
                            losses = stats.gamesLost,
                            animProgress = animProgress,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.0f%%", stats.winRatePercent),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "WIN RATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Legend & Breakdown Metrics
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LegendMetricRow(
                            dotColor = EmeraldSuccess,
                            label = "Levels Cleared (Wins)",
                            value = "${stats.gamesWon}"
                        )
                        LegendMetricRow(
                            dotColor = CoralError,
                            label = "Time-Outs (Losses)",
                            value = "${stats.gamesLost}"
                        )
                        LegendMetricRow(
                            dotColor = RadiantAmber,
                            label = "Win–Loss Ratio",
                            value = stats.winLossRatioLabel
                        )
                        LegendMetricRow(
                            dotColor = Color(0xFFEC4899),
                            label = "Win Streak (Now / Best)",
                            value = "${preferences.currentWinStreak} / ${max(preferences.bestWinStreak, preferences.currentWinStreak)}"
                        )
                        LegendMetricRow(
                            dotColor = MaterialTheme.colorScheme.primary,
                            label = "Offline Campaign",
                            value = "${preferences.completedOfflineLevels.size}/100"
                        )
                    }
                }
            }
        }

        // 3. Average Time per Level Chart (Switchable: Bar Chart by Theme vs. Recent Trend Sparkline)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chart_avg_time_per_level")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        Icon(
                            imageVector = if (timeChartMode == "THEME") Icons.Default.BarChart else Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = RadiantAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Average Time per Level",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (stats.averageTimeSeconds > 0f) {
                                    String.format(
                                        Locale.US,
                                        "Overall Average: %.1fs • Best: %.1fs",
                                        stats.averageTimeSeconds,
                                        stats.bestTimeSeconds
                                    )
                                } else {
                                    "Complete a level to benchmark your speed across art styles"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chart Mode FilterChips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = timeChartMode == "THEME",
                        onClick = { timeChartMode = "THEME" },
                        label = { Text("By Theme (Bar Chart)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("avg_time_mode_theme_chip")
                    )
                    FilterChip(
                        selected = timeChartMode == "TREND",
                        onClick = { timeChartMode = "TREND" },
                        label = { Text("Recent Runs (Trend)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("avg_time_mode_trend_chip")
                    )
                }

                if (timeChartMode == "THEME") {
                    ThemeAverageTimeBarChart(
                        themeStats = stats.themeTimeStats,
                        overallAvgSeconds = stats.averageTimeSeconds,
                        animProgress = animProgress
                    )
                } else {
                    RecentLevelsSparklineChart(
                        history = stats.recentHistory,
                        overallAvgSeconds = stats.averageTimeSeconds,
                        accentColor = MaterialTheme.colorScheme.primary,
                        animProgress = animProgress
                    )
                }
            }
        }

        // 4. Total Differences Found Distribution Chart (By Complexity & Milestone Gauge)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chart_total_differences_found")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Differences Found Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Distribution of ${stats.totalDifferencesFound} spotted anomalies by pattern complexity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stacked Horizontal Proportion Bar Canvas
                DifferencesProportionBarCanvas(
                    complexityStats = stats.complexityDiffStats,
                    animProgress = animProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(50))
                )

                // Individual Complexity Bars
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.complexityDiffStats.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Text(
                                        text = item.complexity.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${item.differencesFound} spotted (${String.format(Locale.US, "%.0f%%", item.fraction * 100f)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = item.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (item.fraction * animProgress).coerceIn(0.04f, 1f) },
                                color = item.color,
                                trackColor = Color(0xFF0F172A),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }

                // Next Rank Milestone Progress Footer
                val milestoneProgress = (stats.totalDifferencesFound.toFloat() / stats.nextMilestoneTarget.toFloat())
                    .coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "Next Rank Milestone: ${stats.spotterRankTitle}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${stats.totalDifferencesFound} / ${stats.nextMilestoneTarget} diffs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { milestoneProgress * animProgress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFF1E293B),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111827).copy(alpha = 0.88f),
        modifier = modifier.border(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.38f),
            shape = RoundedCornerShape(18.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendMetricRow(
    dotColor: Color,
    label: String,
    value: String
) {
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
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun WinLossDonutCanvas(
    wins: Int,
    losses: Int,
    animProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val diameter = size.minDimension - strokeWidth * 1.3f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Base track circle
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        val total = wins + losses
        if (total <= 0) {
            // Subtle idle preview ring
            drawArc(
                color = EmeraldSuccess.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f * animProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth * 0.65f, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        val winFraction = wins.toFloat() / total.toFloat()
        val lossFraction = losses.toFloat() / total.toFloat()
        val gapDegrees = if (wins > 0 && losses > 0) 10f else 0f
        val availableSweep = (360f - gapDegrees * 2f).coerceAtLeast(300f)

        val winSweep = availableSweep * winFraction * animProgress
        val lossSweep = availableSweep * lossFraction * animProgress

        if (lossSweep > 0.5f) {
            drawArc(
                color = CoralError,
                startAngle = -90f + winSweep + gapDegrees,
                sweepAngle = lossSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (winSweep > 0.5f) {
            drawArc(
                color = EmeraldSuccess,
                startAngle = -90f,
                sweepAngle = winSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun ThemeAverageTimeBarChart(
    themeStats: List<ThemeTimeStat>,
    overallAvgSeconds: Float,
    animProgress: Float
) {
    val maxTime = max(
        themeStats.maxOfOrNull { it.averageSeconds } ?: 0f,
        max(overallAvgSeconds, 60f)
    ) * 1.15f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1120))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Horizontal guide lines
                val steps = 3
                for (i in 0..steps) {
                    val y = h * (i.toFloat() / steps.toFloat())
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Dashed benchmark line for overall average time
                if (overallAvgSeconds > 0f) {
                    val avgY = h - (overallAvgSeconds / maxTime).coerceIn(0.08f, 0.92f) * h
                    drawLine(
                        color = RadiantAmber.copy(alpha = 0.85f),
                        start = Offset(0f, avgY),
                        end = Offset(w, avgY),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }

                // Bars for each GameTheme
                val count = themeStats.size.coerceAtLeast(1)
                val slotWidth = w / count.toFloat()
                val barWidth = min(slotWidth * 0.52f, 38.dp.toPx())

                themeStats.forEachIndexed { idx, stat ->
                    val centerX = slotWidth * idx + slotWidth * 0.5f
                    val primaryColor = Color(stat.theme.primaryHex)
                    val secondaryColor = Color(stat.theme.secondaryHex)

                    val normalizedHeight = if (stat.averageSeconds > 0f) {
                        ((stat.averageSeconds / maxTime).coerceIn(0.12f, 0.95f)) * animProgress
                    } else {
                        0.08f * animProgress
                    }
                    val barHeight = h * normalizedHeight
                    val topY = h - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (stat.averageSeconds > 0f) {
                                listOf(primaryColor, secondaryColor.copy(alpha = 0.75f))
                            } else {
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            },
                            startY = topY,
                            endY = h
                        ),
                        topLeft = Offset(centerX - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        // X-Axis Labels & Time Readouts under each bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            themeStats.forEach { stat ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (stat.averageSeconds > 0f) {
                            String.format(Locale.US, "%.1fs", stat.averageSeconds)
                        } else {
                            "--"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stat.averageSeconds > 0f) Color(stat.theme.primaryHex) else Color(0xFF64748B)
                    )
                    Text(
                        text = stat.theme.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentLevelsSparklineChart(
    history: List<LevelHistoryPoint>,
    overallAvgSeconds: Float,
    accentColor: Color,
    animProgress: Float
) {
    val points = history.takeLast(10)
    val maxSec = max(points.maxOfOrNull { it.durationSeconds } ?: 60f, 45f) * 1.15f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1120))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (points.isEmpty()) {
                Text(
                    text = "Play any AI or Practice level to populate your level-by-level speed trend line.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (overallAvgSeconds > 0f) {
                        val avgY = h - (overallAvgSeconds / maxSec).coerceIn(0.1f, 0.9f) * h
                        drawLine(
                            color = RadiantAmber.copy(alpha = 0.75f),
                            start = Offset(0f, avgY),
                            end = Offset(w, avgY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    }

                    val coords = points.mapIndexed { idx, pt ->
                        val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1).toFloat()) * w
                        val normalizedY = (pt.durationSeconds / maxSec).coerceIn(0.08f, 0.94f) * animProgress
                        val y = h - normalizedY * h
                        Offset(x, y)
                    }

                    if (coords.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val prev = coords[i - 1]
                                val curr = coords[i]
                                val ctrlX = (prev.x + curr.x) / 2f
                                cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, h)
                            lineTo(coords.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = accentColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    coords.forEachIndexed { idx, offset ->
                        val isWin = points[idx].isWin
                        val dotColor = if (isWin) EmeraldSuccess else CoralError
                        drawCircle(
                            color = Color(0xFF0B1120),
                            radius = 6.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4.2.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }
        }

        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Oldest: ${points.first().label} (${String.format(Locale.US, "%.1fs", points.first().durationSeconds)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Latest: ${points.last().label} (${String.format(Locale.US, "%.1fs", points.last().durationSeconds)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DifferencesProportionBarCanvas(
    complexityStats: List<ComplexityDiffStat>,
    animProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF0B1120), size = size)

        val totalFraction = complexityStats.sumOf { it.fraction.toDouble() }.toFloat()
        if (totalFraction <= 0.001f) {
            // Equal placeholder segments when 0 differences found yet
            val segW = w / complexityStats.size.coerceAtLeast(1)
            complexityStats.forEachIndexed { idx, item ->
                drawRect(
                    color = item.color.copy(alpha = 0.28f),
                    topLeft = Offset(idx * segW, 0f),
                    size = Size(segW - 2.dp.toPx(), h)
                )
            }
            return@Canvas
        }

        var currentX = 0f
        complexityStats.forEach { item ->
            val segWidth = (w * (item.fraction / totalFraction) * animProgress).coerceAtLeast(0f)
            if (segWidth > 0.5f) {
                drawRect(
                    color = item.color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(segWidth, h)
                )
                currentX += segWidth
            }
        }
    }
}
