package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChallengeLinkPayload
import com.example.data.model.ChallengeTimeRecord
import com.example.data.model.GameTheme
import com.example.data.model.LeaderboardEntry
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.UiState
import java.util.Locale

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    localChallengeRecords: List<ChallengeTimeRecord> = emptyList(),
    currentUserId: String,
    currentPlayerAvatarData: String,
    isGuest: Boolean = false,
    onOpenProfile: () -> Unit = {},
    onGenerateChallengeForEntry: (LeaderboardEntry) -> Unit = {},
    onBackToPlay: () -> Unit
) {
    BackHandler(onBack = onBackToPlay)
    var selectedThemeFilter by remember { mutableStateOf<String?>(null) }
    var challengeCodeFilter by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = RadiantAmber,
                    modifier = Modifier.size(30.dp)
                )
                Column {
                    Text(
                        text = "Global & Challenge Leaderboard",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Track who completes each puzzle layout in the shortest time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onOpenProfile,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("leaderboard_open_profile_button")
            ) {
                PlayerAvatarBadge(
                    avatarData = currentPlayerAvatarData,
                    size = 24.dp,
                    showAiBadge = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("My Avatar")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter by Specific Challenge Link / Code to see who completed that layout in the shortest time
        OutlinedTextField(
            value = challengeCodeFilter,
            onValueChange = { raw ->
                // If user pastes a full Challenge Link URL, extract its code automatically
                val extracted = if (raw.contains("code=", ignoreCase = true)) {
                    raw.substringAfter("code=", "").substringBefore("&").substringBefore(" ").trim()
                } else {
                    raw.removePrefix("#").trim()
                }
                challengeCodeFilter = extracted.uppercase(Locale.US).take(16)
            },
            label = { Text("Filter by Challenge Code or Paste Challenge Link") },
            placeholder = { Text("e.g., 8F4A2C or OFL001 to view puzzle fastest times") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            trailingIcon = {
                if (challengeCodeFilter.isNotBlank()) {
                    IconButton(onClick = { challengeCodeFilter = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear challenge filter")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("leaderboard_challenge_code_filter_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedThemeFilter == null,
                onClick = { selectedThemeFilter = null },
                label = { Text("All Themes") },
                modifier = Modifier.testTag("leaderboard_filter_all")
            )
            GameTheme.entries.forEach { theme ->
                FilterChip(
                    selected = selectedThemeFilter == theme.id,
                    onClick = { selectedThemeFilter = theme.id },
                    label = { Text(theme.displayName) },
                    modifier = Modifier.testTag("leaderboard_filter_${theme.id.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (leaderboardState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = leaderboardState.message,
                        color = CoralError,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is UiState.Success -> {
                // Combine cloud entries with any local challenge records (especially useful in Guest mode or for newly accepted challenges)
                val mergedEntries = remember(leaderboardState.data, localChallengeRecords, currentUserId, currentPlayerAvatarData) {
                    val cloudList = leaderboardState.data
                    val localConverted = localChallengeRecords.mapIndexed { idx, rec ->
                        LeaderboardEntry(
                            id = "local_${rec.challengeCode}_${rec.completionTimeMillis}_$idx",
                            userId = if (rec.isCurrentUser) currentUserId else "challenger_${rec.playerName}",
                            playerName = rec.playerName,
                            playerAvatarData = if (rec.isCurrentUser && currentPlayerAvatarData.isNotBlank()) {
                                currentPlayerAvatarData
                            } else {
                                rec.playerAvatarData
                            },
                            boardId = "GLOBAL",
                            theme = rec.themeId,
                            variationCount = rec.variationCount.toLong(),
                            patternComplexity = rec.complexityId,
                            completionTimeMillis = rec.completionTimeMillis,
                            score = rec.score.toLong(),
                            levelTitle = ChallengeLinkPayload.withChallengePrefix(rec.challengeCode, rec.levelTitle)
                        )
                    }
                    (cloudList + localConverted)
                        .distinctBy { "${it.playerName.lowercase()}_${it.completionTimeMillis}_${ChallengeLinkPayload.stripChallengePrefix(it.levelTitle).lowercase()}" }
                        .sortedWith(
                            compareBy<LeaderboardEntry> { it.completionTimeMillis }
                                .thenByDescending { it.score }
                        )
                }

                // Discover active Challenge Codes so user can tap a chip to filter by a specific puzzle layout
                val discoveredChallengeCodes = remember(mergedEntries) {
                    mergedEntries.mapNotNull { ChallengeLinkPayload.extractChallengeCodeFromTitle(it.levelTitle) }
                        .distinct()
                        .take(8)
                }

                if (discoveredChallengeCodes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Puzzles:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        discoveredChallengeCodes.forEach { code ->
                            val isSelected = challengeCodeFilter.equals(code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    challengeCodeFilter = if (isSelected) "" else code
                                },
                                label = { Text("#$code") },
                                modifier = Modifier.testTag("leaderboard_challenge_chip_${code.lowercase()}")
                            )
                        }
                    }
                }

                val filteredEntries = remember(mergedEntries, selectedThemeFilter, challengeCodeFilter) {
                    mergedEntries.filter { entry ->
                        val matchesTheme = selectedThemeFilter == null || entry.theme == selectedThemeFilter
                        val codeInTitle = ChallengeLinkPayload.extractChallengeCodeFromTitle(entry.levelTitle)
                        val q = challengeCodeFilter.trim()
                        val matchesChallenge = q.isBlank() ||
                            (codeInTitle != null && codeInTitle.contains(q, ignoreCase = true)) ||
                            entry.levelTitle.contains(q, ignoreCase = true)
                        matchesTheme && matchesChallenge
                    }
                }

                if (filteredEntries.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = RadiantAmber,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (challengeCodeFilter.isNotBlank()) {
                                    "No Completions Yet for Challenge #$challengeCodeFilter"
                                } else if (isGuest) {
                                    "Guest Mode Active"
                                } else {
                                    "No Speed Records Yet"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (challengeCodeFilter.isNotBlank()) {
                                    "Be the first among your friends to complete puzzle layout #$challengeCodeFilter and claim the #1 shortest time!"
                                } else if (isGuest) {
                                    "Complete any AI or Offline level to track your local Challenge Link speed records, or connect Google in Options to sync globally!"
                                } else {
                                    "Complete an AI Spot the Difference level or share a Challenge Link with friends to race for the #1 shortest completion time!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val fastestTimeMillis = filteredEntries.first().completionTimeMillis

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filteredEntries, key = { _, item -> item.id }) { index, entry ->
                            val rank = index + 1
                            val isCurrentUser = entry.userId == currentUserId
                            val medalColor = when (rank) {
                                1 -> Color(0xFFFBBF24) // Gold
                                2 -> Color(0xFFCBD5E1) // Silver
                                3 -> Color(0xFFD97706) // Bronze
                                else -> Color(0xFF334155)
                            }
                            val timeSeconds = entry.completionTimeMillis / 1000.0
                            val deltaSeconds = (entry.completionTimeMillis - fastestTimeMillis) / 1000.0
                            val theme = GameTheme.fromId(entry.theme)
                            val extractedCode = ChallengeLinkPayload.extractChallengeCodeFromTitle(entry.levelTitle)
                            val cleanTitle = ChallengeLinkPayload.stripChallengePrefix(entry.levelTitle)

                            val avatarToDisplay = if (isCurrentUser && currentPlayerAvatarData.isNotBlank()) {
                                currentPlayerAvatarData
                            } else {
                                entry.playerAvatarData
                            }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentUser) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (rank <= 3 || isCurrentUser) 1.5.dp else 0.dp,
                                        color = if (rank <= 3) medalColor else if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Rank Badge
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(medalColor.copy(alpha = if (rank <= 3) 0.28f else 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#$rank",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (rank <= 3) medalColor else Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        PlayerAvatarBadge(
                                            avatarData = avatarToDisplay,
                                            size = 44.dp,
                                            modifier = Modifier.testTag("leaderboard_avatar_$rank")
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = entry.playerName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isCurrentUser) {
                                                    Surface(
                                                        color = EmeraldSuccess.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = "YOU",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = EmeraldSuccess,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (!extractedCode.isNullOrBlank()) {
                                                    Surface(
                                                        color = RadiantAmber.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = "#$extractedCode",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = RadiantAmber,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "$cleanTitle • ${theme.displayName} (${entry.variationCount} spots)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                color = EmeraldSuccess.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "%.1fs", timeSeconds),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = EmeraldSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (rank == 1) {
                                                    "SHORTEST TIME"
                                                } else {
                                                    String.format(Locale.US, "+%.1fs • %d pts", deltaSeconds, entry.score)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (rank == 1) RadiantAmber else Color(0xFF94A3B8)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onGenerateChallengeForEntry(entry) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("leaderboard_challenge_entry_$rank")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Generate Challenge Link for this puzzle",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
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
    }
}
