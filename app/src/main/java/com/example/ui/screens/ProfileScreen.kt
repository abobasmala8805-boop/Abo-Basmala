package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Storefront
import com.example.data.model.AppPreferences
import com.example.data.model.AvatarPreset
import com.example.ui.components.DailyLoginHubBanner
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.components.ProfileStatsDashboard
import com.example.ui.components.RewardCurrencyHubBanner
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.SpotDiffViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: SpotDiffViewModel,
    profileState: ProfileUiState,
    preferences: AppPreferences,
    isGuest: Boolean,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToReplayGallery: () -> Unit = {},
    onBackToPlay: () -> Unit
) {
    BackHandler(onBack = onBackToPlay)
    val isCustomAiAvatarActive = profileState.selectedAvatarData.startsWith("data:image/")
    val leaderboardUiState by viewModel.globalLeaderboardState.collectAsStateWithLifecycle()
    val userLeaderboardEntries = (leaderboardUiState as? UiState.Success)?.data
        ?.filter { it.userId == viewModel.currentUserId }
        .orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Player Profile & Statistics",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Track your level speed, differences found, and win–loss ratio, or customize your AI leaderboard avatar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!profileState.statusMessage.isNullOrBlank()) {
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profileState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldSuccess
                    )
                }
            }
        }

        if (!profileState.errorMessage.isNullOrBlank()) {
            Surface(
                color = CoralError.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_error_banner")
            ) {
                Text(
                    text = profileState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoralError,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Calendar-Based Daily Login Streak Banner
        DailyLoginHubBanner(
            preferences = preferences,
            onOpenCalendar = viewModel::openDailyLoginModal
        )

        // Starlight Shards & Avatar Frame Emporium Banner
        RewardCurrencyHubBanner(
            preferences = preferences,
            onOpenShop = { viewModel.openRewardShop(initialTab = 1) },
            buttonTestTag = "profile_open_reward_shop_button"
        )

        // 1. Live Leaderboard Identity Preview Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlayerAvatarBadge(
                        avatarData = profileState.selectedAvatarData,
                        size = 86.dp,
                        avatarFrame = preferences.selectedAvatarFrame,
                        modifier = Modifier.testTag("profile_current_avatar_preview")
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = if (isCustomAiAvatarActive) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                RadiantAmber.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isCustomAiAvatarActive) {
                                    "CUSTOM AI AVATAR ACTIVE"
                                } else {
                                    "PRESET: ${AvatarPreset.fromToken(profileState.selectedAvatarData).displayName.uppercase()}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCustomAiAvatarActive) MaterialTheme.colorScheme.primary else RadiantAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = profileState.displayNameInput.ifBlank { "Spotter Pro" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Offline Progress: ${preferences.completedOfflineLevels.size}/100 • ${if (isGuest) "Guest Session" else "Cloud Leaderboard Synced"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = profileState.displayNameInput,
                    onValueChange = viewModel::updateProfileDisplayName,
                    label = { Text("Leaderboard Display Name") },
                    placeholder = { Text("Enter your player name...") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = viewModel::saveUserProfileChanges,
                        enabled = !profileState.isSavingProfile && !profileState.isGeneratingAiAvatar,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (profileState.isSavingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile & Sync", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToLeaderboard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("profile_view_leaderboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = RadiantAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Leaderboard")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = viewModel::openSocialSnapshotForProfileOrSetup,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_share_snapshot_button")
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
                        onClick = { viewModel.openRewardShop(initialTab = 1) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_avatar_frames_shop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Avatar Frames",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToReplayGallery,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_open_replay_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = RadiantAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Offline Replay Gallery & Difference Timestamps",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Player Statistics & Visual Charts Dashboard
        ProfileStatsDashboard(
            preferences = preferences,
            offlineLevels = viewModel.offlinePracticeLevels,
            userLeaderboardEntries = userLeaderboardEntries
        )

        // 3. AI Custom Avatar Generator Studio (gemini-nano-banana-2.1)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Generate Custom Avatar with AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Describe your character portrait and Gemini Nano Banana 2.1 will synthesize a custom leaderboard avatar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Style Inspiration Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = profileState.selectedPresetStyle == preset,
                            onClick = { viewModel.applyPresetInspirationPrompt(preset) },
                            label = { Text("${preset.displayName} Idea") },
                            modifier = Modifier.testTag("ai_prompt_chip_${preset.id.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = profileState.aiPromptInput,
                    onValueChange = viewModel::updateAiAvatarPrompt,
                    label = { Text("Custom AI Avatar Prompt") },
                    placeholder = { Text("e.g., Futuristic cyber detective fox wearing glowing neon goggles...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_avatar_prompt_input")
                )

                Button(
                    onClick = viewModel::generateCustomAiAvatar,
                    enabled = !profileState.isGeneratingAiAvatar,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF041019)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_ai_avatar_button")
                ) {
                    if (profileState.isGeneratingAiAvatar) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF041019),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Synthesizing Custom AI Avatar (15–45s)...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Custom AI Avatar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Curated Stylized Avatar Presets Grid
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Or Select a Stylized Preset Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any emblem below to equip it immediately next to your name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    AvatarPreset.entries.forEach { preset ->
                        val isSelected = profileState.selectedAvatarData.equals(preset.token, ignoreCase = true)
                        val accent = Color(preset.primaryHex)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) accent.copy(alpha = 0.22f) else Color(0xFF111827),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else Color(0xFF334155),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.selectPresetAvatar(preset) }
                                .testTag("avatar_preset_${preset.id.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PlayerAvatarBadge(
                                    avatarData = preset.token,
                                    size = 46.dp,
                                    showAiBadge = false
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
