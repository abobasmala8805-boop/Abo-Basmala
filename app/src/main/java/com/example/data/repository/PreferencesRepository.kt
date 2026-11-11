package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.AvatarFrame
import com.example.data.model.AvatarPreset
import com.example.data.model.ChallengeTimeRecord
import com.example.data.model.DailyLoginClaimReceipt
import com.example.data.model.DailyLoginStreakManager
import com.example.data.model.GameTheme
import com.example.data.model.HintShopPack
import com.example.data.model.LevelHistoryPoint
import com.example.data.model.OfflineReplayRecord
import com.example.data.model.PatternComplexity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spotdiff_settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SCREEN_SHAKE_ENABLED = booleanPreferencesKey("screen_shake_enabled")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val VARIATION_COUNT = intPreferencesKey("variation_count")
        val TIME_LIMIT_SECONDS = intPreferencesKey("time_limit_seconds")
        val PATTERN_COMPLEXITY = stringPreferencesKey("pattern_complexity")
        val SIDE_BY_SIDE_MODE = booleanPreferencesKey("side_by_side_mode")
        val INDEPENDENT_ZOOM = booleanPreferencesKey("independent_zoom")
        val COMPLETED_OFFLINE_LEVELS = stringSetPreferencesKey("completed_offline_levels")
        val PLAYER_DISPLAY_NAME = stringPreferencesKey("player_display_name")
        val PLAYER_AVATAR_DATA = stringPreferencesKey("player_avatar_data")
        val LAST_AVATAR_PROMPT = stringPreferencesKey("last_avatar_prompt")
        val TOTAL_DIFFERENCES_FOUND = intPreferencesKey("total_differences_found")
        val GAMES_WON = intPreferencesKey("games_won")
        val GAMES_LOST = intPreferencesKey("games_lost")
        val CURRENT_WIN_STREAK = intPreferencesKey("current_win_streak")
        val BEST_WIN_STREAK = intPreferencesKey("best_win_streak")
        val TOTAL_WIN_TIME_MILLIS = longPreferencesKey("total_win_time_millis")
        val BEST_WIN_TIME_MILLIS = longPreferencesKey("best_win_time_millis")
        val RECENT_LEVEL_HISTORY = stringPreferencesKey("recent_level_history")
        val CHALLENGE_TIME_RECORDS = stringPreferencesKey("challenge_time_records")
        val OFFLINE_REPLAY_RECORDS = stringPreferencesKey("offline_replay_records")
        val SHARD_BALANCE = intPreferencesKey("shard_balance")
        val TOTAL_SHARDS_EARNED = intPreferencesKey("total_shards_earned")
        val UNLOCKED_THEME_IDS = stringSetPreferencesKey("unlocked_theme_ids")
        val UNLOCKED_AVATAR_FRAME_IDS = stringSetPreferencesKey("unlocked_avatar_frame_ids")
        val SELECTED_AVATAR_FRAME_ID = stringPreferencesKey("selected_avatar_frame_id")
        val BONUS_HINTS_INVENTORY = intPreferencesKey("bonus_hints_inventory")
        val DAILY_LOGIN_STREAK = intPreferencesKey("daily_login_streak")
        val BEST_DAILY_LOGIN_STREAK = intPreferencesKey("best_daily_login_streak")
        val LAST_DAILY_CLAIM_DATE = stringPreferencesKey("last_daily_claim_date")
        val DAILY_LOGIN_DATES = stringSetPreferencesKey("daily_login_dates")
        val SIMULATED_DAY_OFFSET = intPreferencesKey("simulated_day_offset")
    }

    val preferencesFlow: Flow<AppPreferences> = context.applicationContext.dataStore.data.map { prefs ->
        val completedSet = prefs[Keys.COMPLETED_OFFLINE_LEVELS]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

        val avatarToken = prefs[Keys.PLAYER_AVATAR_DATA]?.takeIf { it.isNotBlank() }
            ?: AvatarPreset.CYBER_DETECTIVE.token
        val history = LevelHistoryPoint.deserializeList(prefs[Keys.RECENT_LEVEL_HISTORY] ?: "")
        val challengeRecords = ChallengeTimeRecord.deserializeList(
            raw = prefs[Keys.CHALLENGE_TIME_RECORDS] ?: "",
            fallbackAvatar = avatarToken
        )
        val replayRecords = OfflineReplayRecord.deserializeList(
            raw = prefs[Keys.OFFLINE_REPLAY_RECORDS] ?: ""
        )

        val unlockedThemes = GameTheme.defaultUnlockedIds + (prefs[Keys.UNLOCKED_THEME_IDS] ?: emptySet())
        val unlockedFrames = AvatarFrame.defaultUnlockedIds + (prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] ?: emptySet())
        val selectedFrameId = prefs[Keys.SELECTED_AVATAR_FRAME_ID]?.takeIf { it in unlockedFrames }
            ?: AvatarFrame.DEFAULT_RING.id

        AppPreferences(
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            soundEffectsEnabled = prefs[Keys.SFX_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            screenShakeEnabled = prefs[Keys.SCREEN_SHAKE_ENABLED] ?: true,
            selectedTheme = GameTheme.fromId(prefs[Keys.SELECTED_THEME] ?: GameTheme.NATURE.id),
            variationCount = (prefs[Keys.VARIATION_COUNT] ?: 5).coerceIn(3, 10),
            timeLimitSeconds = (prefs[Keys.TIME_LIMIT_SECONDS] ?: 90).coerceIn(20, 300),
            patternComplexity = PatternComplexity.fromId(
                prefs[Keys.PATTERN_COMPLEXITY] ?: PatternComplexity.MODERATE.id
            ),
            sideBySideMode = prefs[Keys.SIDE_BY_SIDE_MODE] ?: true,
            independentZoom = prefs[Keys.INDEPENDENT_ZOOM] ?: true,
            completedOfflineLevels = completedSet,
            playerDisplayName = prefs[Keys.PLAYER_DISPLAY_NAME] ?: "",
            playerAvatarData = avatarToken,
            lastAvatarPrompt = prefs[Keys.LAST_AVATAR_PROMPT] ?: "",
            totalDifferencesFound = (prefs[Keys.TOTAL_DIFFERENCES_FOUND] ?: 0).coerceAtLeast(0),
            gamesWon = (prefs[Keys.GAMES_WON] ?: 0).coerceAtLeast(0),
            gamesLost = (prefs[Keys.GAMES_LOST] ?: 0).coerceAtLeast(0),
            currentWinStreak = (prefs[Keys.CURRENT_WIN_STREAK] ?: 0).coerceAtLeast(0),
            bestWinStreak = (prefs[Keys.BEST_WIN_STREAK] ?: 0).coerceAtLeast(0),
            totalWinTimeMillis = (prefs[Keys.TOTAL_WIN_TIME_MILLIS] ?: 0L).coerceAtLeast(0L),
            bestWinTimeMillis = (prefs[Keys.BEST_WIN_TIME_MILLIS] ?: 0L).coerceAtLeast(0L),
            recentLevelHistory = history,
            challengeTimeRecords = challengeRecords,
            offlineReplayRecords = replayRecords,
            shardBalance = (prefs[Keys.SHARD_BALANCE] ?: 250).coerceAtLeast(0),
            totalShardsEarned = (prefs[Keys.TOTAL_SHARDS_EARNED] ?: 250).coerceAtLeast(0),
            unlockedThemeIds = unlockedThemes,
            unlockedAvatarFrameIds = unlockedFrames,
            selectedAvatarFrameId = selectedFrameId,
            bonusHintsInventory = (prefs[Keys.BONUS_HINTS_INVENTORY] ?: 0).coerceAtLeast(0),
            dailyLoginStreak = (prefs[Keys.DAILY_LOGIN_STREAK] ?: 0).coerceAtLeast(0),
            bestDailyLoginStreak = (prefs[Keys.BEST_DAILY_LOGIN_STREAK] ?: 0).coerceAtLeast(0),
            lastDailyClaimDate = prefs[Keys.LAST_DAILY_CLAIM_DATE] ?: "",
            dailyLoginDates = prefs[Keys.DAILY_LOGIN_DATES] ?: emptySet(),
            simulatedDayOffset = (prefs[Keys.SIMULATED_DAY_OFFSET] ?: 0).coerceIn(0, 365)
        )
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.applicationContext.dataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.applicationContext.dataStore.edit { it[Keys.SFX_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.applicationContext.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setScreenShakeEnabled(enabled: Boolean) {
        context.applicationContext.dataStore.edit { it[Keys.SCREEN_SHAKE_ENABLED] = enabled }
    }

    suspend fun setSelectedTheme(theme: GameTheme) {
        context.applicationContext.dataStore.edit { it[Keys.SELECTED_THEME] = theme.id }
    }

    suspend fun setVariationCount(count: Int) {
        context.applicationContext.dataStore.edit {
            it[Keys.VARIATION_COUNT] = count.coerceIn(3, 10)
        }
    }

    suspend fun setTimeLimitSeconds(seconds: Int) {
        context.applicationContext.dataStore.edit {
            it[Keys.TIME_LIMIT_SECONDS] = seconds.coerceIn(20, 300)
        }
    }

    suspend fun setPatternComplexity(complexity: PatternComplexity) {
        context.applicationContext.dataStore.edit {
            it[Keys.PATTERN_COMPLEXITY] = complexity.id
        }
    }

    suspend fun setSideBySideMode(enabled: Boolean) {
        context.applicationContext.dataStore.edit {
            it[Keys.SIDE_BY_SIDE_MODE] = enabled
        }
    }

    suspend fun setIndependentZoom(enabled: Boolean) {
        context.applicationContext.dataStore.edit {
            it[Keys.INDEPENDENT_ZOOM] = enabled
        }
    }

    suspend fun markOfflineLevelCompleted(levelNumber: Int) {
        context.applicationContext.dataStore.edit { prefs ->
            val existing = prefs[Keys.COMPLETED_OFFLINE_LEVELS] ?: emptySet()
            prefs[Keys.COMPLETED_OFFLINE_LEVELS] = existing + levelNumber.toString()
        }
    }

    suspend fun incrementDifferencesFound(delta: Int = 1) {
        if (delta <= 0) return
        context.applicationContext.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_DIFFERENCES_FOUND] ?: 0
            prefs[Keys.TOTAL_DIFFERENCES_FOUND] = current + delta
        }
    }

    suspend fun recordGameOutcome(
        levelTitle: String,
        theme: GameTheme,
        complexity: PatternComplexity,
        durationMillis: Long,
        differencesFound: Int,
        isWin: Boolean,
        shardsEarned: Int = 0
    ) {
        val safeDurationMs = durationMillis.coerceAtLeast(1000L)
        context.applicationContext.dataStore.edit { prefs ->
            if (isWin) {
                val currentWins = prefs[Keys.GAMES_WON] ?: 0
                prefs[Keys.GAMES_WON] = currentWins + 1
                val nextStreak = (prefs[Keys.CURRENT_WIN_STREAK] ?: 0) + 1
                prefs[Keys.CURRENT_WIN_STREAK] = nextStreak
                val prevBestStreak = prefs[Keys.BEST_WIN_STREAK] ?: 0
                if (nextStreak > prevBestStreak) {
                    prefs[Keys.BEST_WIN_STREAK] = nextStreak
                }
                val currentTotalTime = prefs[Keys.TOTAL_WIN_TIME_MILLIS] ?: 0L
                prefs[Keys.TOTAL_WIN_TIME_MILLIS] = currentTotalTime + safeDurationMs
                val currentBest = prefs[Keys.BEST_WIN_TIME_MILLIS] ?: 0L
                if (currentBest == 0L || safeDurationMs < currentBest) {
                    prefs[Keys.BEST_WIN_TIME_MILLIS] = safeDurationMs
                }
            } else {
                val currentLosses = prefs[Keys.GAMES_LOST] ?: 0
                prefs[Keys.GAMES_LOST] = currentLosses + 1
                prefs[Keys.CURRENT_WIN_STREAK] = 0
            }

            if (shardsEarned > 0) {
                val currentBalance = prefs[Keys.SHARD_BALANCE] ?: 250
                val currentTotalEarned = prefs[Keys.TOTAL_SHARDS_EARNED] ?: 250
                prefs[Keys.SHARD_BALANCE] = currentBalance + shardsEarned
                prefs[Keys.TOTAL_SHARDS_EARNED] = currentTotalEarned + shardsEarned
            }

            val existingHistory = LevelHistoryPoint.deserializeList(
                prefs[Keys.RECENT_LEVEL_HISTORY] ?: ""
            )
            val newPoint = LevelHistoryPoint(
                label = levelTitle.take(24).ifBlank { "${theme.displayName} Level" },
                themeId = theme.id,
                complexityId = complexity.id,
                durationSeconds = safeDurationMs / 1000f,
                differencesFound = differencesFound.coerceAtLeast(0),
                isWin = isWin
            )
            prefs[Keys.RECENT_LEVEL_HISTORY] = LevelHistoryPoint.serializeList(existingHistory + newPoint)
        }
    }

    suspend fun unlockAndSelectThemeWithShards(theme: GameTheme): Boolean {
        var success = false
        context.applicationContext.dataStore.edit { prefs ->
            val unlocked = GameTheme.defaultUnlockedIds + (prefs[Keys.UNLOCKED_THEME_IDS] ?: emptySet())
            if (theme.unlockCostShards == 0 || theme.id in unlocked) {
                prefs[Keys.SELECTED_THEME] = theme.id
                success = true
                return@edit
            }
            val balance = prefs[Keys.SHARD_BALANCE] ?: 250
            if (balance >= theme.unlockCostShards) {
                prefs[Keys.SHARD_BALANCE] = balance - theme.unlockCostShards
                prefs[Keys.UNLOCKED_THEME_IDS] = unlocked + theme.id
                prefs[Keys.SELECTED_THEME] = theme.id
                success = true
            }
        }
        return success
    }

    suspend fun unlockAndEquipAvatarFrameWithShards(frame: AvatarFrame): Boolean {
        var success = false
        context.applicationContext.dataStore.edit { prefs ->
            val unlocked = AvatarFrame.defaultUnlockedIds + (prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] ?: emptySet())
            if (frame.costShards == 0 || frame.id in unlocked) {
                prefs[Keys.SELECTED_AVATAR_FRAME_ID] = frame.id
                success = true
                return@edit
            }
            val balance = prefs[Keys.SHARD_BALANCE] ?: 250
            if (balance >= frame.costShards) {
                prefs[Keys.SHARD_BALANCE] = balance - frame.costShards
                prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] = unlocked + frame.id
                prefs[Keys.SELECTED_AVATAR_FRAME_ID] = frame.id
                success = true
            }
        }
        return success
    }

    suspend fun selectAvatarFrame(frame: AvatarFrame) {
        context.applicationContext.dataStore.edit { prefs ->
            val unlocked = AvatarFrame.defaultUnlockedIds + (prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] ?: emptySet())
            if (frame.costShards == 0 || frame.id in unlocked) {
                prefs[Keys.SELECTED_AVATAR_FRAME_ID] = frame.id
            }
        }
    }

    suspend fun purchaseHintPackWithShards(pack: HintShopPack): Boolean {
        var success = false
        context.applicationContext.dataStore.edit { prefs ->
            val balance = prefs[Keys.SHARD_BALANCE] ?: 250
            if (balance >= pack.costShards) {
                val currentHints = prefs[Keys.BONUS_HINTS_INVENTORY] ?: 0
                prefs[Keys.SHARD_BALANCE] = balance - pack.costShards
                prefs[Keys.BONUS_HINTS_INVENTORY] = currentHints + pack.hintsGranted
                success = true
            }
        }
        return success
    }

    suspend fun consumeBonusHintIfAvailable() {
        context.applicationContext.dataStore.edit { prefs ->
            val currentHints = prefs[Keys.BONUS_HINTS_INVENTORY] ?: 0
            if (currentHints > 0) {
                prefs[Keys.BONUS_HINTS_INVENTORY] = currentHints - 1
            }
        }
    }

    suspend fun recordChallengeCompletion(record: ChallengeTimeRecord) {
        context.applicationContext.dataStore.edit { prefs ->
            val existing = ChallengeTimeRecord.deserializeList(
                raw = prefs[Keys.CHALLENGE_TIME_RECORDS] ?: ""
            )
            prefs[Keys.CHALLENGE_TIME_RECORDS] = ChallengeTimeRecord.serializeList(existing + record)
        }
    }

    suspend fun recordOfflineReplayPerformance(record: OfflineReplayRecord) {
        context.applicationContext.dataStore.edit { prefs ->
            val existing = OfflineReplayRecord.deserializeList(
                raw = prefs[Keys.OFFLINE_REPLAY_RECORDS] ?: ""
            )
            val prevForLevel = existing.firstOrNull { it.replayId.equals(record.replayId, ignoreCase = true) }
            val shouldSave = prevForLevel == null ||
                record.completionTimeMillis < prevForLevel.completionTimeMillis ||
                (record.completionTimeMillis == prevForLevel.completionTimeMillis && record.score >= prevForLevel.score)

            if (shouldSave) {
                val filtered = existing.filterNot { it.replayId.equals(record.replayId, ignoreCase = true) }
                val updated = (filtered + record).sortedBy { it.completionTimeMillis }
                prefs[Keys.OFFLINE_REPLAY_RECORDS] = OfflineReplayRecord.serializeList(updated)
            }
        }
    }

    suspend fun savePlayerProfile(displayName: String, avatarData: String, avatarPrompt: String = "") {
        context.applicationContext.dataStore.edit { prefs ->
            prefs[Keys.PLAYER_DISPLAY_NAME] = displayName.trim().take(60)
            prefs[Keys.PLAYER_AVATAR_DATA] = avatarData.trim().take(28_000).ifBlank {
                AvatarPreset.CYBER_DETECTIVE.token
            }
            if (avatarPrompt.isNotBlank()) {
                prefs[Keys.LAST_AVATAR_PROMPT] = avatarPrompt.trim().take(280)
            }
        }
    }

    suspend fun claimDailyLoginReward(): DailyLoginClaimReceipt? {
        var receipt: DailyLoginClaimReceipt? = null
        context.applicationContext.dataStore.edit { prefs ->
            val dayOffset = (prefs[Keys.SIMULATED_DAY_OFFSET] ?: 0).coerceIn(0, 365)
            val todayKey = DailyLoginStreakManager.dateKeyForOffset(dayOffset)
            val yesterdayKey = DailyLoginStreakManager.previousDateKey(todayKey, 1)
            val twoDaysAgoKey = DailyLoginStreakManager.previousDateKey(todayKey, 2)

            val rawDates = prefs[Keys.DAILY_LOGIN_DATES] ?: emptySet()
            val rawLastClaim = prefs[Keys.LAST_DAILY_CLAIM_DATE] ?: ""
            val rawStreak = prefs[Keys.DAILY_LOGIN_STREAK] ?: 0

            val existingDates = if (rawDates.isEmpty() && rawLastClaim.isBlank()) {
                setOf(twoDaysAgoKey, yesterdayKey)
            } else {
                rawDates
            }
            val effectiveLastClaim = if (rawLastClaim.isBlank() && rawDates.isEmpty()) {
                yesterdayKey
            } else {
                rawLastClaim
            }
            val effectiveStreak = when {
                rawStreak > 0 -> rawStreak
                rawDates.isEmpty() && rawLastClaim.isBlank() -> 2
                else -> 0
            }

            if (effectiveLastClaim == todayKey || todayKey in existingDates) {
                return@edit
            }

            val nextStreak = if (effectiveLastClaim == yesterdayKey || yesterdayKey in existingDates) {
                effectiveStreak + 1
            } else {
                1
            }

            val tiers = DailyLoginStreakManager.buildScaledTiers(nextStreak)
            val tierDayIndex = ((nextStreak - 1) % 7) + 1
            val rewardTier = tiers.firstOrNull { it.dayIndex == tierDayIndex } ?: tiers.first()

            var shardsToGrant = rewardTier.scaledShards
            val hintsToGrant = rewardTier.bonusHints
            var unlockedBonusLabel: String? = null

            val unlockedFrames = AvatarFrame.defaultUnlockedIds + (prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] ?: emptySet())
            val unlockedThemes = GameTheme.defaultUnlockedIds + (prefs[Keys.UNLOCKED_THEME_IDS] ?: emptySet())

            val frameBonus = rewardTier.unlockAvatarFrame
            val themeBonus = rewardTier.unlockTheme

            if (frameBonus != null && frameBonus.id !in unlockedFrames) {
                prefs[Keys.UNLOCKED_AVATAR_FRAME_IDS] = unlockedFrames + frameBonus.id
                prefs[Keys.SELECTED_AVATAR_FRAME_ID] = frameBonus.id
                unlockedBonusLabel = "Unlocked & Equipped ${frameBonus.displayName} Avatar Frame!"
            } else if (themeBonus != null && themeBonus.id !in unlockedThemes) {
                prefs[Keys.UNLOCKED_THEME_IDS] = unlockedThemes + themeBonus.id
                unlockedBonusLabel = "Unlocked ${themeBonus.displayName} Art Theme!"
            } else if (rewardTier.isMilestone) {
                shardsToGrant += 75
                unlockedBonusLabel = "${rewardTier.milestoneBonusLabel ?: "Milestone Chest"} (+75 Bonus Shards)"
            }

            val currentBalance = prefs[Keys.SHARD_BALANCE] ?: 250
            val currentTotalEarned = prefs[Keys.TOTAL_SHARDS_EARNED] ?: 250
            val currentHints = prefs[Keys.BONUS_HINTS_INVENTORY] ?: 0
            val prevBest = prefs[Keys.BEST_DAILY_LOGIN_STREAK] ?: 0

            prefs[Keys.SHARD_BALANCE] = currentBalance + shardsToGrant
            prefs[Keys.TOTAL_SHARDS_EARNED] = currentTotalEarned + shardsToGrant
            prefs[Keys.BONUS_HINTS_INVENTORY] = currentHints + hintsToGrant
            prefs[Keys.DAILY_LOGIN_STREAK] = nextStreak
            prefs[Keys.BEST_DAILY_LOGIN_STREAK] = maxOf(prevBest, nextStreak)
            prefs[Keys.LAST_DAILY_CLAIM_DATE] = todayKey
            prefs[Keys.DAILY_LOGIN_DATES] = existingDates + todayKey

            receipt = DailyLoginClaimReceipt(
                claimedDateKey = todayKey,
                streakDayReached = nextStreak,
                tierDayIndex = tierDayIndex,
                shardsAwarded = shardsToGrant,
                hintsAwarded = hintsToGrant,
                specialUnlockTitle = unlockedBonusLabel
            )
        }
        return receipt
    }

    suspend fun advanceSimulatedLoginDay() {
        context.applicationContext.dataStore.edit { prefs ->
            val currentOffset = (prefs[Keys.SIMULATED_DAY_OFFSET] ?: 0).coerceIn(0, 365)
            val todayKey = DailyLoginStreakManager.dateKeyForOffset(currentOffset)
            val yesterdayKey = DailyLoginStreakManager.previousDateKey(todayKey, 1)
            val twoDaysAgoKey = DailyLoginStreakManager.previousDateKey(todayKey, 2)
            val existingDates = prefs[Keys.DAILY_LOGIN_DATES] ?: emptySet()
            val lastClaim = prefs[Keys.LAST_DAILY_CLAIM_DATE] ?: ""

            // Ensure current day is checked in before advancing to tomorrow so consecutive streak continues cleanly
            if (existingDates.isEmpty() && lastClaim.isBlank()) {
                prefs[Keys.DAILY_LOGIN_DATES] = setOf(twoDaysAgoKey, yesterdayKey, todayKey)
                prefs[Keys.LAST_DAILY_CLAIM_DATE] = todayKey
                prefs[Keys.DAILY_LOGIN_STREAK] = 3
                prefs[Keys.BEST_DAILY_LOGIN_STREAK] = maxOf(prefs[Keys.BEST_DAILY_LOGIN_STREAK] ?: 0, 3)
            }
            prefs[Keys.SIMULATED_DAY_OFFSET] = (currentOffset + 1).coerceAtMost(365)
        }
    }
}

