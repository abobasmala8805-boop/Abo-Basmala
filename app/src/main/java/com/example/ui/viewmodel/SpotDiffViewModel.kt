package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.AppPreferences
import com.example.data.model.AvatarFrame
import com.example.data.model.ChallengeLinkPayload
import com.example.data.model.ChallengeTimeRecord
import com.example.data.model.CustomLevel
import com.example.data.model.DailyLoginClaimReceipt
import com.example.data.model.DifferenceSpot
import com.example.data.model.GameTheme
import com.example.data.model.HintShopPack
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LevelRewardSummary
import com.example.data.model.OfflinePracticeLevel
import com.example.data.model.OfflineReplayRecord
import com.example.data.model.PatternComplexity
import com.example.data.model.ReplaySpotTimestamp
import com.example.data.model.RewardCalculator
import com.example.data.model.VariationType
import com.example.data.repository.AiLevelGeneratorRepository
import com.example.data.repository.OfflinePracticeRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.SpotDiffRepository
import com.example.ui.components.encodeAvatarWithFrame
import com.example.util.FeedbackManager
import com.example.util.SocialSnapshotSpec
import com.google.firebase.Firebase
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private const val TAG = "SpotDiffViewModel"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TapFeedback(
    val normalizedX: Float,
    val normalizedY: Float,
    val isHit: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface GameSessionUiState {
    data object Idle : GameSessionUiState
    data class GeneratingAiLevel(
        val theme: GameTheme,
        val variationCount: Int,
        val complexity: PatternComplexity,
        val customTitle: String? = null,
        val activeWinStreak: Int = 0,
        val streakTierBadge: String = "",
        val isStreakEscalated: Boolean = false
    ) : GameSessionUiState

    data class Playing(
        val levelId: String? = null,
        val offlineLevelNumber: Int? = null,
        val challengeCode: String = "",
        val challengerName: String? = null,
        val challengerTimeMillis: Long? = null,
        val levelTitle: String,
        val scenePrompt: String,
        val theme: GameTheme,
        val variationCount: Int,
        val patternComplexity: PatternComplexity,
        val timeLimitSeconds: Int,
        val originalBitmap: Bitmap,
        val modifiedBitmap: Bitmap,
        val spots: List<DifferenceSpot>,
        val foundSpotIndices: Set<Int> = emptySet(),
        val spotTimestamps: List<ReplaySpotTimestamp> = emptyList(),
        val activeHintIndex: Int? = null,
        val hintOffsetNormalizedX: Float = 0f,
        val hintOffsetNormalizedY: Float = 0f,
        val hintsRemaining: Int = 3,
        val hintCooldownMillis: Long = 0L,
        val hintTotalCooldownMillis: Long = 15_000L,
        val remainingMillis: Long,
        val elapsedMillis: Long = 0L,
        val comboCount: Int = 0,
        val score: Int = 0,
        val mistakesCount: Int = 0,
        val activeWinStreak: Int = 0,
        val streakTierBadge: String = "",
        val isStreakEscalated: Boolean = false,
        val lastTapFeedback: TapFeedback? = null,
        val rewardSummary: LevelRewardSummary? = null,
        val latestReplayRecord: OfflineReplayRecord? = null,
        val isVictory: Boolean = false,
        val isTimeOut: Boolean = false,
        val isSubmittingScore: Boolean = false,
        val scoreSubmitted: Boolean = false
    ) : GameSessionUiState

    data class Error(
        val message: String,
        val theme: GameTheme,
        val variationCount: Int,
        val complexity: PatternComplexity
    ) : GameSessionUiState
}

data class ChallengeLinkUiState(
    val inputLinkOrCode: String = "",
    val parsedChallenge: ChallengeLinkPayload? = null,
    val parseErrorMessage: String? = null,
    val activeModalPayload: ChallengeLinkPayload? = null
)

data class RewardShopUiState(
    val isOpen: Boolean = false,
    val initialTab: Int = 0,
    val statusMessage: String? = null
)

data class OfflineReplayTheaterUiState(
    val record: OfflineReplayRecord? = null,
    val originalBitmap: Bitmap? = null,
    val modifiedBitmap: Bitmap? = null
)

data class DailyLoginModalUiState(
    val isOpen: Boolean = true,
    val latestReceipt: DailyLoginClaimReceipt? = null,
    val statusMessage: String? = null
)

data class CustomDesignerUiState(
    val title: String = "",
    val scenePrompt: String = "",
    val theme: GameTheme = GameTheme.NATURE,
    val variationCount: Int = 5,
    val timeLimitSeconds: Int = 90,
    val patternComplexity: PatternComplexity = PatternComplexity.MODERATE,
    val visibility: String = "SHARED",
    val spots: List<DifferenceSpot> = defaultDesignerSpots(5, PatternComplexity.MODERATE),
    val selectedSpotIndex: Int = 0,
    val isEnhancingPrompt: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val lastCreatedShareCode: String? = null,
    val shareCodeSearchQuery: String = ""
)

data class ProfileUiState(
    val displayNameInput: String = "",
    val selectedAvatarData: String = com.example.data.model.AvatarPreset.CYBER_DETECTIVE.token,
    val aiPromptInput: String = com.example.data.model.AvatarPreset.CYBER_DETECTIVE.aiPromptSuggestion,
    val selectedPresetStyle: com.example.data.model.AvatarPreset = com.example.data.model.AvatarPreset.CYBER_DETECTIVE,
    val isGeneratingAiAvatar: Boolean = false,
    val isSavingProfile: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultDesignerSpots(count: Int, complexity: PatternComplexity): List<DifferenceSpot> {
    val grid = listOf(
        0.24f to 0.26f, 0.74f to 0.24f, 0.50f to 0.48f,
        0.26f to 0.74f, 0.76f to 0.72f, 0.50f to 0.22f,
        0.22f to 0.50f, 0.78f to 0.50f, 0.50f to 0.78f,
        0.38f to 0.36f
    )
    val types = VariationType.entries
    val colors = listOf("#38BDF8", "#F59E0B", "#EC4899", "#10B981", "#A855F7")
    return (0 until count.coerceIn(3, 10)).map { idx ->
        val (x, y) = grid[idx % grid.size]
        val vType = types[idx % types.size]
        DifferenceSpot(
            index = idx,
            normalizedX = x,
            normalizedY = y,
            normalizedRadius = complexity.defaultRadius,
            label = "Spot #${idx + 1} (${vType.displayName.substringBefore(" ")})",
            hint = "Look for the ${vType.displayName.lowercase()} in this zone",
            variationType = vType,
            accentHex = colors[idx % colors.size]
        )
    }
}

class SpotDiffViewModel(
    private val spotDiffRepository: SpotDiffRepository,
    private val preferencesRepository: PreferencesRepository,
    private val aiRepository: AiLevelGeneratorRepository,
    private val offlinePracticeRepository: OfflinePracticeRepository,
    private val feedbackManager: FeedbackManager,
    val currentUserId: String,
    val isGuest: Boolean = false
) : ViewModel() {

    val offlinePracticeLevels: List<OfflinePracticeLevel> = offlinePracticeRepository.allLevels

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppPreferences()
        )

    // Auth-gated Firestore flows: never attach snapshot listeners in Guest mode (when auth.currentUser == null)
    val sharedLevelsState: StateFlow<UiState<List<CustomLevel>>> = (
        if (isGuest) {
            flowOf<UiState<List<CustomLevel>>>(UiState.Success(emptyList()))
        } else {
            spotDiffRepository.observeSharedLevels()
                .map<List<CustomLevel>, UiState<List<CustomLevel>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing shared levels", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load shared levels"))
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = if (isGuest) UiState.Success(emptyList()) else UiState.Loading
    )

    val myLevelsState: StateFlow<UiState<List<CustomLevel>>> = (
        if (isGuest) {
            flowOf<UiState<List<CustomLevel>>>(UiState.Success(emptyList()))
        } else {
            spotDiffRepository.observeMyLevels(currentUserId)
                .map<List<CustomLevel>, UiState<List<CustomLevel>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing user levels", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load your custom levels"))
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = if (isGuest) UiState.Success(emptyList()) else UiState.Loading
    )

    val globalLeaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = (
        if (isGuest) {
            flowOf<UiState<List<LeaderboardEntry>>>(UiState.Success(emptyList()))
        } else {
            spotDiffRepository.observeGlobalLeaderboard()
                .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing global leaderboard", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load global leaderboard"))
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = if (isGuest) UiState.Success(emptyList()) else UiState.Loading
    )

    private val _gameSessionState = MutableStateFlow<GameSessionUiState>(GameSessionUiState.Idle)
    val gameSessionState: StateFlow<GameSessionUiState> = _gameSessionState.asStateFlow()

    private val _designerState = MutableStateFlow(CustomDesignerUiState())
    val designerState: StateFlow<CustomDesignerUiState> = _designerState.asStateFlow()

    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _challengeLinkState = MutableStateFlow(ChallengeLinkUiState())
    val challengeLinkState: StateFlow<ChallengeLinkUiState> = _challengeLinkState.asStateFlow()

    private val _activeSnapshotSpec = MutableStateFlow<SocialSnapshotSpec?>(null)
    val activeSnapshotSpec: StateFlow<SocialSnapshotSpec?> = _activeSnapshotSpec.asStateFlow()

    private val _rewardShopState = MutableStateFlow(RewardShopUiState())
    val rewardShopState: StateFlow<RewardShopUiState> = _rewardShopState.asStateFlow()

    private val _replayTheaterState = MutableStateFlow(OfflineReplayTheaterUiState())
    val replayTheaterState: StateFlow<OfflineReplayTheaterUiState> = _replayTheaterState.asStateFlow()

    private val _dailyLoginModalState = MutableStateFlow(DailyLoginModalUiState(isOpen = true))
    val dailyLoginModalState: StateFlow<DailyLoginModalUiState> = _dailyLoginModalState.asStateFlow()

    private var timerJob: Job? = null
    private var generationJob: Job? = null
    private var profileInitialized = false

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                feedbackManager.updateSettings(
                    music = prefs.musicEnabled,
                    sfx = prefs.soundEffectsEnabled,
                    vibration = prefs.vibrationEnabled
                )
                if (!profileInitialized) {
                    profileInitialized = true
                    val fallbackName = Firebase.auth.currentUser?.displayName
                        ?.takeIf { it.isNotBlank() } ?: if (isGuest) "Guest Spotter" else "Spotter Pro"
                    _profileState.update { state ->
                        state.copy(
                            displayNameInput = prefs.playerDisplayName.ifBlank { fallbackName },
                            selectedAvatarData = prefs.playerAvatarData,
                            aiPromptInput = prefs.lastAvatarPrompt.ifBlank {
                                com.example.data.model.AvatarPreset.CYBER_DETECTIVE.aiPromptSuggestion
                            }
                        )
                    }
                }
            }
        }
        if (!isGuest) {
            viewModelScope.launch {
                spotDiffRepository.observeUserProfile(currentUserId)
                    .catch { e -> Log.w(TAG, "Error observing user profile", e) }
                    .collect { cloudProfile ->
                        if (cloudProfile != null) {
                            preferencesRepository.savePlayerProfile(
                                displayName = cloudProfile.displayName,
                                avatarData = cloudProfile.avatarData,
                                avatarPrompt = cloudProfile.avatarPrompt
                            )
                            _profileState.update { state ->
                                state.copy(
                                    displayNameInput = cloudProfile.displayName.ifBlank { state.displayNameInput },
                                    selectedAvatarData = cloudProfile.avatarData.ifBlank { state.selectedAvatarData },
                                    aiPromptInput = cloudProfile.avatarPrompt.ifBlank { state.aiPromptInput }
                                )
                            }
                        }
                    }
            }
        }
    }

    // --- Preferences, Difficulty, Daily Login Calendar & Reward Shop Controls ---

    fun openDailyLoginModal() {
        feedbackManager.playClickFeedback()
        _dailyLoginModalState.update { it.copy(isOpen = true) }
    }

    fun closeDailyLoginModal() {
        feedbackManager.playClickFeedback()
        _dailyLoginModalState.update { it.copy(isOpen = false, statusMessage = null) }
    }

    fun claimDailyLoginReward() {
        viewModelScope.launch {
            val receipt = preferencesRepository.claimDailyLoginReward()
            if (receipt != null) {
                feedbackManager.playSpotFoundFeedback()
                val bonusPart = receipt.specialUnlockTitle?.let { " • $it" }.orEmpty()
                _dailyLoginModalState.update {
                    it.copy(
                        latestReceipt = receipt,
                        statusMessage = "Day ${receipt.streakDayReached} Claimed! +${receipt.shardsAwarded} Shards & +${receipt.hintsAwarded} Hints$bonusPart"
                    )
                }
            } else {
                feedbackManager.playClickFeedback()
                _dailyLoginModalState.update {
                    it.copy(
                        statusMessage = "Today's daily streak reward is already claimed! Tap 'Simulate Next Day (+1d)' to test tomorrow's tier."
                    )
                }
            }
        }
    }

    fun simulateNextLoginDay() {
        feedbackManager.playClickFeedback()
        viewModelScope.launch {
            preferencesRepository.advanceSimulatedLoginDay()
            _dailyLoginModalState.update {
                it.copy(
                    isOpen = true,
                    statusMessage = "Advanced calendar by +1 consecutive day! Claim your escalated daily reward now."
                )
            }
        }
    }

    fun setTheme(theme: GameTheme) {
        val prefs = preferences.value
        if (prefs.isThemeUnlocked(theme)) {
            feedbackManager.playClickFeedback()
            viewModelScope.launch { preferencesRepository.setSelectedTheme(theme) }
        } else {
            openRewardShop(
                initialTab = 0,
                message = "Unlock '${theme.displayName}' for ${theme.unlockCostShards} Starlight Shards!"
            )
        }
    }

    fun openRewardShop(initialTab: Int = 0, message: String? = null) {
        feedbackManager.playClickFeedback()
        _rewardShopState.value = RewardShopUiState(
            isOpen = true,
            initialTab = initialTab.coerceIn(0, 2),
            statusMessage = message
        )
    }

    fun closeRewardShop() {
        _rewardShopState.update { it.copy(isOpen = false, statusMessage = null) }
    }

    fun openReplayTheater(record: OfflineReplayRecord) {
        feedbackManager.playClickFeedback()
        _replayTheaterState.value = OfflineReplayTheaterUiState(
            record = record,
            originalBitmap = null,
            modifiedBitmap = null
        )
        viewModelScope.launch {
            val spec = record.toOfflinePracticeLevelSpec()
            val generated = offlinePracticeRepository.generateOfflineLevelPair(spec)
            _replayTheaterState.update { state ->
                if (state.record?.replayId == record.replayId) {
                    state.copy(
                        originalBitmap = generated.originalBitmap,
                        modifiedBitmap = generated.modifiedBitmap
                    )
                } else state
            }
        }
    }

    fun openReplayTheaterForCurrentVictory() {
        val current = _gameSessionState.value as? GameSessionUiState.Playing ?: return
        val record = current.latestReplayRecord ?: return
        feedbackManager.playClickFeedback()
        _replayTheaterState.value = OfflineReplayTheaterUiState(
            record = record,
            originalBitmap = current.originalBitmap,
            modifiedBitmap = current.modifiedBitmap
        )
    }

    fun closeReplayTheater() {
        _replayTheaterState.value = OfflineReplayTheaterUiState()
    }

    fun playFromReplayRecord(record: OfflineReplayRecord, onNavigateToPlay: () -> Unit = {}) {
        closeReplayTheater()
        onNavigateToPlay()
        if (record.offlineLevelNumber != null) {
            startOfflinePracticeLevel(
                levelNumber = record.offlineLevelNumber,
                challengeCodeOverride = record.challengeCode,
                challengerName = "Your Best Replay",
                challengerTimeMillis = record.completionTimeMillis
            )
        } else {
            val customLevel = CustomLevel(
                id = record.replayId,
                userId = currentUserId,
                authorName = currentActivePlayerName(),
                title = ChallengeLinkPayload.stripChallengePrefix(record.levelTitle),
                scenePrompt = "${record.theme.promptStyle}, ${record.complexity.promptDetailInstruction}",
                theme = record.themeId,
                variationCount = record.variationCount.toLong(),
                timeLimitSeconds = record.timeLimitSeconds.toLong(),
                patternComplexity = record.complexityId,
                visibility = "SHARED",
                shareCode = record.challengeCode,
                spotsData = record.spotsData
            )
            startNewAiLevel(
                customLevel = customLevel,
                challengeCodeOverride = record.challengeCode,
                challengerName = "Your Best Replay",
                challengerTimeMillis = record.completionTimeMillis
            )
        }
    }

    fun purchaseOrSelectTheme(theme: GameTheme) {
        val prefs = preferences.value
        if (prefs.isThemeUnlocked(theme)) {
            feedbackManager.playClickFeedback()
            viewModelScope.launch {
                preferencesRepository.setSelectedTheme(theme)
                _rewardShopState.update {
                    it.copy(statusMessage = "Applied '${theme.displayName}' visual theme!")
                }
            }
            return
        }
        viewModelScope.launch {
            val unlocked = preferencesRepository.unlockAndSelectThemeWithShards(theme)
            if (unlocked) {
                feedbackManager.playSpotFoundFeedback()
                _rewardShopState.update {
                    it.copy(statusMessage = "Unlocked & applied '${theme.displayName}' (-${theme.unlockCostShards} Shards)!")
                }
            } else {
                feedbackManager.playMissPenaltyFeedback()
                _rewardShopState.update {
                    it.copy(statusMessage = "Need ${theme.unlockCostShards} Shards to unlock '${theme.displayName}'. Complete levels to earn more!")
                }
            }
        }
    }

    fun purchaseOrEquipAvatarFrame(frame: AvatarFrame) {
        val prefs = preferences.value
        if (prefs.isAvatarFrameUnlocked(frame)) {
            feedbackManager.playClickFeedback()
            viewModelScope.launch {
                preferencesRepository.selectAvatarFrame(frame)
                _rewardShopState.update {
                    it.copy(statusMessage = "Equipped '${frame.displayName}' avatar frame!")
                }
            }
            return
        }
        viewModelScope.launch {
            val unlocked = preferencesRepository.unlockAndEquipAvatarFrameWithShards(frame)
            if (unlocked) {
                feedbackManager.playSpotFoundFeedback()
                _rewardShopState.update {
                    it.copy(statusMessage = "Unlocked & equipped '${frame.displayName}' (-${frame.costShards} Shards)!")
                }
            } else {
                feedbackManager.playMissPenaltyFeedback()
                _rewardShopState.update {
                    it.copy(statusMessage = "Need ${frame.costShards} Shards to unlock '${frame.displayName}'.")
                }
            }
        }
    }

    fun purchaseHintPack(pack: HintShopPack) {
        viewModelScope.launch {
            val purchased = preferencesRepository.purchaseHintPackWithShards(pack)
            if (purchased) {
                feedbackManager.playSpotFoundFeedback()
                // Also immediately add the purchased hints to an active game session if one is in progress
                _gameSessionState.update { state ->
                    if (state is GameSessionUiState.Playing && !state.isVictory && !state.isTimeOut) {
                        state.copy(hintsRemaining = state.hintsRemaining + pack.hintsGranted)
                    } else state
                }
                _rewardShopState.update {
                    it.copy(statusMessage = "Added +${pack.hintsGranted} Bonus Hint${if (pack.hintsGranted == 1) "" else "s"} to your reserve (-${pack.costShards} Shards)!")
                }
            } else {
                feedbackManager.playMissPenaltyFeedback()
                _rewardShopState.update {
                    it.copy(statusMessage = "Not enough Shards for ${pack.displayName} (${pack.costShards} Shards required).")
                }
            }
        }
    }

    fun setVariationCount(count: Int) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setVariationCount(count.coerceIn(3, 10)) }
    }

    fun setTimeLimitSeconds(seconds: Int) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setTimeLimitSeconds(seconds.coerceIn(20, 300)) }
    }

    fun setPatternComplexity(complexity: PatternComplexity) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setPatternComplexity(complexity) }
    }

    fun setSideBySideMode(enabled: Boolean) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setSideBySideMode(enabled) }
    }

    fun setIndependentZoom(enabled: Boolean) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setIndependentZoom(enabled) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMusicEnabled(enabled) }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEffectsEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun setScreenShakeEnabled(enabled: Boolean) {
        feedbackManager.playClickFeedback()
        viewModelScope.launch { preferencesRepository.setScreenShakeEnabled(enabled) }
    }

    private fun hintCooldownFor(complexity: PatternComplexity): Long = when (complexity) {
        PatternComplexity.SUBTLE -> 16_000L
        PatternComplexity.MODERATE -> 13_000L
        PatternComplexity.INTRICATE -> 10_000L
    }

    private fun initialHintsFor(variationCount: Int): Int = if (variationCount >= 7) 4 else 3

    private fun currentActivePlayerName(): String {
        val currentPrefs = preferences.value
        return _profileState.value.displayNameInput.trim()
            .ifBlank { currentPrefs.playerDisplayName }
            .ifBlank {
                Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: if (isGuest) "Guest Spotter" else "Spotter Pro"
            }
    }

    private fun currentActivePlayerAvatar(): String {
        val currentPrefs = preferences.value
        val rawAvatar = _profileState.value.selectedAvatarData.ifBlank { currentPrefs.playerAvatarData }
        return encodeAvatarWithFrame(rawAvatar, currentPrefs.selectedAvatarFrame)
    }

    // --- Challenge Link Generation, Parsing & Fastest Time Tracking ---

    fun updateChallengeLinkInput(rawInput: String) {
        val shared = (sharedLevelsState.value as? UiState.Success)?.data.orEmpty()
        val mine = (myLevelsState.value as? UiState.Success)?.data.orEmpty()
        val knownLevels = (mine + shared).distinctBy { it.id }

        if (rawInput.isBlank()) {
            _challengeLinkState.update {
                it.copy(
                    inputLinkOrCode = "",
                    parsedChallenge = null,
                    parseErrorMessage = null
                )
            }
            return
        }

        val parsed = ChallengeLinkPayload.parseFromInput(
            rawInput = rawInput,
            knownCustomLevels = knownLevels,
            offlineLevels = offlinePracticeLevels
        )

        _challengeLinkState.update {
            it.copy(
                inputLinkOrCode = rawInput,
                parsedChallenge = parsed,
                parseErrorMessage = if (parsed == null && rawInput.trim().length >= 3) {
                    "Could not recognize Challenge Link or Code. Paste a full Challenge Link or 6-character code."
                } else null
            )
        }
    }

    fun handleIncomingChallengeUri(uriString: String) {
        if (uriString.isBlank()) return
        updateChallengeLinkInput(uriString)
        val parsed = _challengeLinkState.value.parsedChallenge
        if (parsed != null) {
            _challengeLinkState.update { it.copy(activeModalPayload = parsed) }
        }
    }

    fun openChallengeGeneratorForPlaying() {
        val current = _gameSessionState.value as? GameSessionUiState.Playing ?: return
        feedbackManager.playClickFeedback()
        val code = current.challengeCode.ifBlank {
            ChallengeLinkPayload.computeDeterministicCode(
                title = current.levelTitle,
                theme = current.theme,
                variationCount = current.variationCount,
                complexity = current.patternComplexity,
                spots = current.spots,
                offlineLevelNumber = current.offlineLevelNumber
            )
        }
        val payload = ChallengeLinkPayload(
            challengeCode = code,
            title = ChallengeLinkPayload.stripChallengePrefix(current.levelTitle),
            scenePrompt = current.scenePrompt,
            theme = current.theme,
            variationCount = current.variationCount,
            timeLimitSeconds = current.timeLimitSeconds,
            patternComplexity = current.patternComplexity,
            spots = current.spots,
            offlineLevelNumber = current.offlineLevelNumber,
            challengerName = currentActivePlayerName(),
            challengerTimeMillis = if (current.isVictory) current.elapsedMillis.coerceAtLeast(1000L) else current.challengerTimeMillis
        )
        _challengeLinkState.update { it.copy(activeModalPayload = payload) }
    }

    fun openChallengeGeneratorForCustomLevel(level: CustomLevel) {
        feedbackManager.playClickFeedback()
        val theme = GameTheme.fromId(level.theme)
        val complexity = PatternComplexity.fromId(level.patternComplexity)
        val spots = DifferenceSpot.deserializeList(level.spotsData).takeIf { it.isNotEmpty() }
            ?: ChallengeLinkPayload.deterministicSpotsForSeed(
                level.shareCode.ifBlank { "LVL001" },
                level.variationCount.toInt(),
                complexity
            )
        val code = level.shareCode.ifBlank {
            ChallengeLinkPayload.computeDeterministicCode(
                title = level.title,
                theme = theme,
                variationCount = spots.size,
                complexity = complexity,
                spots = spots
            )
        }
        // Check if current player or leaderboard has a best time for this level's challenge code
        val standings = getChallengeStandings(code)
        val bestTimeMs = standings.minByOrNull { it.completionTimeMillis }?.completionTimeMillis

        val payload = ChallengeLinkPayload(
            challengeCode = code,
            title = ChallengeLinkPayload.stripChallengePrefix(level.title),
            scenePrompt = level.scenePrompt,
            theme = theme,
            variationCount = spots.size,
            timeLimitSeconds = level.timeLimitSeconds.toInt().coerceIn(20, 300),
            patternComplexity = complexity,
            spots = spots,
            offlineLevelNumber = null,
            challengerName = level.authorName.ifBlank { currentActivePlayerName() },
            challengerTimeMillis = bestTimeMs
        )
        _challengeLinkState.update { it.copy(activeModalPayload = payload) }
    }

    fun openChallengeGeneratorForSetup() {
        feedbackManager.playClickFeedback()
        val prefs = preferences.value
        val seedTitle = "${prefs.selectedTheme.displayName} Speed Duel"
        val spots = defaultDesignerSpots(prefs.variationCount, prefs.patternComplexity)
        val code = ChallengeLinkPayload.computeDeterministicCode(
            title = seedTitle,
            theme = prefs.selectedTheme,
            variationCount = prefs.variationCount,
            complexity = prefs.patternComplexity,
            spots = spots
        )
        val standings = getChallengeStandings(code)
        val bestTimeMs = standings.minByOrNull { it.completionTimeMillis }?.completionTimeMillis

        val payload = ChallengeLinkPayload(
            challengeCode = code,
            title = seedTitle,
            scenePrompt = "${prefs.selectedTheme.promptStyle}, ${prefs.patternComplexity.promptDetailInstruction}",
            theme = prefs.selectedTheme,
            variationCount = prefs.variationCount,
            timeLimitSeconds = prefs.timeLimitSeconds,
            patternComplexity = prefs.patternComplexity,
            spots = spots,
            offlineLevelNumber = null,
            challengerName = currentActivePlayerName(),
            challengerTimeMillis = bestTimeMs
        )
        _challengeLinkState.update { it.copy(activeModalPayload = payload) }
    }

    fun openChallengeGeneratorForLeaderboardEntry(entry: LeaderboardEntry) {
        feedbackManager.playClickFeedback()
        val theme = GameTheme.fromId(entry.theme)
        val complexity = PatternComplexity.fromId(entry.patternComplexity)
        val extractedCode = ChallengeLinkPayload.extractChallengeCodeFromTitle(entry.levelTitle)
        val cleanTitle = ChallengeLinkPayload.stripChallengePrefix(entry.levelTitle)
        val spots = ChallengeLinkPayload.deterministicSpotsForSeed(
            seedCode = extractedCode ?: cleanTitle,
            count = entry.variationCount.toInt().coerceIn(3, 10),
            complexity = complexity
        )
        val code = extractedCode ?: ChallengeLinkPayload.computeDeterministicCode(
            title = cleanTitle,
            theme = theme,
            variationCount = spots.size,
            complexity = complexity,
            spots = spots
        )
        val payload = ChallengeLinkPayload(
            challengeCode = code,
            title = cleanTitle,
            scenePrompt = "${theme.promptStyle}, ${complexity.promptDetailInstruction}",
            theme = theme,
            variationCount = spots.size,
            timeLimitSeconds = 90,
            patternComplexity = complexity,
            spots = spots,
            offlineLevelNumber = null,
            challengerName = entry.playerName.ifBlank { "Spotter Pro" },
            challengerTimeMillis = entry.completionTimeMillis
        )
        _challengeLinkState.update { it.copy(activeModalPayload = payload) }
    }

    fun dismissChallengeGeneratorModal() {
        _challengeLinkState.update { it.copy(activeModalPayload = null) }
    }

    fun openSocialSnapshotForPlaying() {
        val current = _gameSessionState.value as? GameSessionUiState.Playing ?: return
        feedbackManager.playClickFeedback()
        val prefs = preferences.value
        val code = current.challengeCode.ifBlank {
            ChallengeLinkPayload.computeDeterministicCode(
                title = current.levelTitle,
                theme = current.theme,
                variationCount = current.variationCount,
                complexity = current.patternComplexity,
                spots = current.spots,
                offlineLevelNumber = current.offlineLevelNumber
            )
        }
        val bestStreak = max(prefs.bestWinStreak, current.activeWinStreak)
        _activeSnapshotSpec.value = SocialSnapshotSpec(
            playerName = currentActivePlayerName(),
            playerAvatarData = currentActivePlayerAvatar(),
            levelTitle = ChallengeLinkPayload.stripChallengePrefix(current.levelTitle),
            theme = current.theme,
            complexity = current.patternComplexity,
            variationCount = current.spots.size,
            spotsFoundCount = current.foundSpotIndices.size,
            finalTimeMillis = current.elapsedMillis.coerceAtLeast(1000L),
            score = current.score,
            currentWinStreak = current.activeWinStreak,
            bestWinStreak = bestStreak,
            streakTierBadge = current.streakTierBadge.ifBlank { prefs.currentStreakProfile().tierBadge },
            challengeCode = code,
            isVictory = !current.isTimeOut,
            originalBitmap = current.originalBitmap,
            modifiedBitmap = current.modifiedBitmap,
            spots = current.spots,
            foundSpotIndices = current.foundSpotIndices
        )
    }

    fun openSocialSnapshotForProfileOrSetup() {
        feedbackManager.playClickFeedback()
        val prefs = preferences.value
        val streakProfile = prefs.currentStreakProfile()
        val bestTimeMs = when {
            prefs.bestWinTimeMillis > 0L -> prefs.bestWinTimeMillis
            prefs.gamesWon > 0 && prefs.totalWinTimeMillis > 0L -> prefs.totalWinTimeMillis / prefs.gamesWon
            prefs.recentLevelHistory.isNotEmpty() -> (prefs.recentLevelHistory.last().durationSeconds * 1000f).toLong()
            else -> 0L
        }
        val lastLevelTitle = prefs.recentLevelHistory.lastOrNull()?.label
            ?: "${prefs.selectedTheme.displayName} Career Record"
        _activeSnapshotSpec.value = SocialSnapshotSpec(
            playerName = currentActivePlayerName(),
            playerAvatarData = currentActivePlayerAvatar(),
            levelTitle = lastLevelTitle,
            theme = prefs.selectedTheme,
            complexity = streakProfile.effectiveComplexity,
            variationCount = streakProfile.effectiveVariationCount,
            spotsFoundCount = streakProfile.effectiveVariationCount,
            finalTimeMillis = bestTimeMs,
            score = (prefs.totalDifferencesFound * 250).coerceAtLeast(1000),
            currentWinStreak = prefs.currentWinStreak,
            bestWinStreak = max(prefs.bestWinStreak, prefs.currentWinStreak),
            streakTierBadge = streakProfile.tierBadge,
            challengeCode = "",
            isVictory = true,
            originalBitmap = null,
            modifiedBitmap = null
        )
    }

    fun dismissSocialSnapshotModal() {
        _activeSnapshotSpec.value = null
    }

    fun acceptAndLaunchChallenge(payload: ChallengeLinkPayload, onNavigateToPlay: () -> Unit = {}) {
        feedbackManager.playClickFeedback()
        _challengeLinkState.update { it.copy(activeModalPayload = null) }

        // If the challenge link includes a challenger's target time, record it locally so it appears on the speed standings immediately
        if (payload.challengerTimeMillis != null && payload.challengerTimeMillis > 0L) {
            viewModelScope.launch {
                preferencesRepository.recordChallengeCompletion(
                    ChallengeTimeRecord(
                        challengeCode = payload.challengeCode.uppercase(),
                        levelTitle = ChallengeLinkPayload.stripChallengePrefix(payload.title),
                        playerName = payload.challengerName,
                        playerAvatarData = com.example.data.model.AvatarPreset.COSMIC_VOYAGER.token,
                        completionTimeMillis = payload.challengerTimeMillis,
                        score = (1800 - (payload.challengerTimeMillis / 100L).toInt()).coerceAtLeast(250),
                        themeId = payload.theme.id,
                        variationCount = payload.variationCount,
                        complexityId = payload.patternComplexity.id,
                        isCurrentUser = false
                    )
                )
            }
        }

        onNavigateToPlay()

        if (payload.offlineLevelNumber != null) {
            startOfflinePracticeLevel(
                levelNumber = payload.offlineLevelNumber,
                challengeCodeOverride = payload.challengeCode,
                challengerName = payload.challengerName,
                challengerTimeMillis = payload.challengerTimeMillis
            )
        } else {
            val challengeLevel = CustomLevel(
                id = "challenge_${payload.challengeCode.lowercase()}",
                userId = currentUserId,
                authorName = payload.challengerName,
                title = ChallengeLinkPayload.stripChallengePrefix(payload.title),
                scenePrompt = payload.scenePrompt,
                theme = payload.theme.id,
                variationCount = payload.variationCount.toLong(),
                timeLimitSeconds = payload.timeLimitSeconds.toLong(),
                patternComplexity = payload.patternComplexity.id,
                visibility = "SHARED",
                shareCode = payload.challengeCode.uppercase(),
                spotsData = DifferenceSpot.serializeList(payload.spots)
            )
            startNewAiLevel(
                customLevel = challengeLevel,
                challengeCodeOverride = payload.challengeCode.uppercase(),
                challengerName = payload.challengerName,
                challengerTimeMillis = payload.challengerTimeMillis
            )
        }
    }

    fun getChallengeStandings(challengeCode: String): List<ChallengeTimeRecord> {
        val normCode = challengeCode.trim().uppercase()
        if (normCode.isBlank()) return emptyList()

        val cloudEntries = (globalLeaderboardState.value as? UiState.Success)?.data.orEmpty()
        val fromCloud = cloudEntries.mapNotNull { entry ->
            val entryCode = ChallengeLinkPayload.extractChallengeCodeFromTitle(entry.levelTitle)
            if (entryCode != null && entryCode.equals(normCode, ignoreCase = true)) {
                ChallengeTimeRecord(
                    challengeCode = normCode,
                    levelTitle = ChallengeLinkPayload.stripChallengePrefix(entry.levelTitle),
                    playerName = entry.playerName,
                    playerAvatarData = entry.playerAvatarData,
                    completionTimeMillis = entry.completionTimeMillis,
                    score = entry.score.toInt(),
                    themeId = entry.theme,
                    variationCount = entry.variationCount.toInt(),
                    complexityId = entry.patternComplexity,
                    isCurrentUser = entry.userId == currentUserId
                )
            } else null
        }

        val fromLocal = preferences.value.challengeTimeRecords.filter {
            it.challengeCode.equals(normCode, ignoreCase = true)
        }

        return (fromCloud + fromLocal)
            .distinctBy { "${it.playerName.lowercase()}_${it.completionTimeMillis}" }
            .sortedBy { it.completionTimeMillis }
    }

    // --- 100 Offline Practice Levels (Zero Internet Required) ---

    fun startOfflinePracticeLevel(
        levelNumber: Int,
        challengeCodeOverride: String? = null,
        challengerName: String? = null,
        challengerTimeMillis: Long? = null
    ) {
        generationJob?.cancel()
        timerJob?.cancel()
        feedbackManager.playClickFeedback()

        val currentPrefs = preferences.value
        val streakProfile = currentPrefs.currentStreakProfile()
        val spec = offlinePracticeRepository.getLevel(levelNumber)
        generationJob = viewModelScope.launch {
            val generated = offlinePracticeRepository.generateOfflineLevelPair(spec)
            val cooldownMs = hintCooldownFor(spec.complexity)
            val challengeCode = challengeCodeOverride?.takeIf { it.isNotBlank() }
                ?: ChallengeLinkPayload.computeDeterministicCode(
                    title = generated.levelTitle,
                    theme = spec.theme,
                    variationCount = generated.spots.size,
                    complexity = spec.complexity,
                    spots = generated.spots,
                    offlineLevelNumber = spec.levelNumber
                )
            val playingState = GameSessionUiState.Playing(
                levelId = "offline_${spec.levelNumber}",
                offlineLevelNumber = spec.levelNumber,
                challengeCode = challengeCode,
                challengerName = challengerName,
                challengerTimeMillis = challengerTimeMillis,
                levelTitle = generated.levelTitle,
                scenePrompt = generated.scenePrompt,
                theme = spec.theme,
                variationCount = generated.spots.size,
                patternComplexity = spec.complexity,
                timeLimitSeconds = spec.timeLimitSeconds,
                originalBitmap = generated.originalBitmap,
                modifiedBitmap = generated.modifiedBitmap,
                spots = generated.spots,
                hintsRemaining = initialHintsFor(generated.spots.size) + currentPrefs.bonusHintsInventory,
                hintCooldownMillis = 0L,
                hintTotalCooldownMillis = cooldownMs,
                remainingMillis = spec.timeLimitSeconds * 1000L,
                activeWinStreak = currentPrefs.currentWinStreak,
                streakTierBadge = streakProfile.tierBadge,
                isStreakEscalated = false
            )
            _gameSessionState.value = playingState
            startCountdownTimer()
        }
    }

    // --- AI Level Generation & Gameplay ---

    fun startNewAiLevel(
        customLevel: CustomLevel? = null,
        challengeCodeOverride: String? = null,
        challengerName: String? = null,
        challengerTimeMillis: Long? = null
    ) {
        generationJob?.cancel()
        timerJob?.cancel()

        val currentPrefs = preferences.value
        val streakProfile = currentPrefs.currentStreakProfile()
        val isDynamicAiSession = customLevel == null
        val activeStreak = currentPrefs.currentWinStreak

        val theme = customLevel?.let { GameTheme.fromId(it.theme) } ?: currentPrefs.selectedTheme
        val variationCount = customLevel?.variationCount?.toInt()?.coerceIn(3, 10)
            ?: streakProfile.effectiveVariationCount
        val timeLimitSec = customLevel?.timeLimitSeconds?.toInt()?.coerceIn(20, 300) ?: currentPrefs.timeLimitSeconds
        val complexity = customLevel?.let { PatternComplexity.fromId(it.patternComplexity) }
            ?: streakProfile.effectiveComplexity
        val customSpots = customLevel?.spotsData?.let { DifferenceSpot.deserializeList(it) }?.takeIf { it.isNotEmpty() }

        generationJob = viewModelScope.launch {
            _gameSessionState.value = GameSessionUiState.GeneratingAiLevel(
                theme = theme,
                variationCount = variationCount,
                complexity = complexity,
                customTitle = customLevel?.title?.let { ChallengeLinkPayload.stripChallengePrefix(it) },
                activeWinStreak = activeStreak,
                streakTierBadge = streakProfile.tierBadge,
                isStreakEscalated = isDynamicAiSession && streakProfile.isComplexityEscalated
            )
            try {
                val generated = aiRepository.generateAiLevel(
                    theme = theme,
                    variationCount = variationCount,
                    complexity = complexity,
                    customTitle = customLevel?.title?.let { ChallengeLinkPayload.stripChallengePrefix(it) },
                    customScenePrompt = customLevel?.scenePrompt,
                    predefinedSpots = customSpots,
                    winStreak = if (isDynamicAiSession) activeStreak else 0
                )

                val cooldownMs = hintCooldownFor(complexity)
                val resolvedChallengeCode = challengeCodeOverride?.takeIf { it.isNotBlank() }
                    ?: customLevel?.shareCode?.takeIf { it.isNotBlank() && it != "PREVIEW" }
                    ?: ChallengeLinkPayload.computeDeterministicCode(
                        title = generated.levelTitle,
                        theme = theme,
                        variationCount = generated.spots.size,
                        complexity = complexity,
                        spots = generated.spots
                    )
                val playingState = GameSessionUiState.Playing(
                    levelId = customLevel?.id,
                    offlineLevelNumber = null,
                    challengeCode = resolvedChallengeCode,
                    challengerName = challengerName,
                    challengerTimeMillis = challengerTimeMillis,
                    levelTitle = ChallengeLinkPayload.stripChallengePrefix(generated.levelTitle),
                    scenePrompt = generated.scenePrompt,
                    theme = theme,
                    variationCount = generated.spots.size,
                    patternComplexity = complexity,
                    timeLimitSeconds = timeLimitSec,
                    originalBitmap = generated.originalBitmap,
                    modifiedBitmap = generated.modifiedBitmap,
                    spots = generated.spots,
                    hintsRemaining = initialHintsFor(generated.spots.size) + currentPrefs.bonusHintsInventory,
                    hintCooldownMillis = 0L,
                    hintTotalCooldownMillis = cooldownMs,
                    remainingMillis = timeLimitSec * 1000L,
                    activeWinStreak = activeStreak,
                    streakTierBadge = streakProfile.tierBadge,
                    isStreakEscalated = isDynamicAiSession && streakProfile.isComplexityEscalated
                )
                _gameSessionState.value = playingState
                startCountdownTimer()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI level generation timed out", e)
                _gameSessionState.value = GameSessionUiState.Error(
                    message = "AI image generation timed out after 60 seconds. Please tap Retry.",
                    theme = theme,
                    variationCount = variationCount,
                    complexity = complexity
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI quota exceeded", e)
                _gameSessionState.value = GameSessionUiState.Error(
                    message = "Rate limit or quota exceeded for AI image generation. Please try again shortly.",
                    theme = theme,
                    variationCount = variationCount,
                    complexity = complexity
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate AI level", e)
                _gameSessionState.value = GameSessionUiState.Error(
                    message = e.localizedMessage ?: "Failed to generate AI level images.",
                    theme = theme,
                    variationCount = variationCount,
                    complexity = complexity
                )
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val tickMs = 100L
            while (true) {
                delay(tickMs)
                val current = _gameSessionState.value as? GameSessionUiState.Playing ?: break
                if (current.isVictory || current.isTimeOut) break

                val nextRemaining = (current.remainingMillis - tickMs).coerceAtLeast(0L)
                val nextElapsed = current.elapsedMillis + tickMs
                val nextHintCooldown = (current.hintCooldownMillis - tickMs).coerceAtLeast(0L)
                if (nextRemaining == 0L) {
                    // Trigger Android Vibrator haptic alert when player runs out of time
                    feedbackManager.playTimeOutFeedback()
                    val reward = RewardCalculator.calculateReward(
                        isVictory = false,
                        spotsFound = current.foundSpotIndices.size,
                        totalSpots = current.spots.size,
                        complexity = current.patternComplexity,
                        remainingMillis = 0L,
                        timeLimitSeconds = current.timeLimitSeconds,
                        winStreak = 0
                    )
                    viewModelScope.launch {
                        preferencesRepository.recordGameOutcome(
                            levelTitle = current.levelTitle,
                            theme = current.theme,
                            complexity = current.patternComplexity,
                            durationMillis = nextElapsed,
                            differencesFound = current.foundSpotIndices.size,
                            isWin = false,
                            shardsEarned = reward.totalShardsAwarded
                        )
                    }
                    _gameSessionState.value = current.copy(
                        remainingMillis = 0L,
                        elapsedMillis = nextElapsed,
                        hintCooldownMillis = 0L,
                        activeWinStreak = 0,
                        streakTierBadge = "Base Complexity",
                        isStreakEscalated = false,
                        rewardSummary = reward,
                        isTimeOut = true
                    )
                    break
                } else {
                    _gameSessionState.value = current.copy(
                        remainingMillis = nextRemaining,
                        elapsedMillis = nextElapsed,
                        hintCooldownMillis = nextHintCooldown
                    )
                }
            }
        }
    }

    fun onImageTapped(normalizedX: Float, normalizedY: Float) {
        val current = _gameSessionState.value as? GameSessionUiState.Playing ?: return
        if (current.isVictory || current.isTimeOut) return

        val hitSpot = current.spots.firstOrNull { spot ->
            spot.index !in current.foundSpotIndices &&
                hypot(spot.normalizedX - normalizedX, spot.normalizedY - normalizedY) <= (spot.normalizedRadius * 1.45f).coerceAtLeast(0.085f)
        }

        if (hitSpot != null) {
            val newFound = current.foundSpotIndices + hitSpot.index
            val newCombo = current.comboCount + 1
            val foundAtMs = current.elapsedMillis.coerceAtLeast(100L)
            val prevFoundMs = current.spotTimestamps.lastOrNull()?.foundAtElapsedMillis ?: 0L
            val splitDeltaMs = (foundAtMs - prevFoundMs).coerceAtLeast(100L)
            val newSpotTimestamp = ReplaySpotTimestamp(
                spotIndex = hitSpot.index,
                orderFound = current.spotTimestamps.size + 1,
                foundAtElapsedMillis = foundAtMs,
                splitDeltaMillis = splitDeltaMs,
                label = hitSpot.label,
                variationTypeId = hitSpot.variationType.id,
                normalizedX = hitSpot.normalizedX,
                normalizedY = hitSpot.normalizedY,
                accentHex = hitSpot.accentHex
            )
            val newTimestamps = current.spotTimestamps + newSpotTimestamp

            val streakMultiplier = com.example.data.model.StreakComplexityScaler.resolve(
                winStreak = current.activeWinStreak,
                baseComplexity = current.patternComplexity,
                baseVariationCount = current.variationCount
            ).scoreBonusMultiplier
            val basePoints = (250 * current.patternComplexity.scoreMultiplier * streakMultiplier).toInt()
            val comboBonus = newCombo * 75
            val isAllFound = newFound.size >= current.spots.size

            val timeBonus = if (isAllFound) ((current.remainingMillis / 1000L).toInt() * 15) else 0
            val newScore = current.score + basePoints + comboBonus + timeBonus

            // Always trigger spot-found haptic vibration via Android Vibrator when a player identifies a difference
            feedbackManager.playSpotFoundFeedback()
            viewModelScope.launch {
                preferencesRepository.incrementDifferencesFound(1)
            }

            val nextWinStreak = if (isAllFound) current.activeWinStreak + 1 else current.activeWinStreak
            val nextStreakProfile = com.example.data.model.StreakComplexityScaler.resolve(
                winStreak = nextWinStreak,
                baseComplexity = preferences.value.patternComplexity,
                baseVariationCount = preferences.value.variationCount
            )

            var victoryReward: LevelRewardSummary? = null
            var completedReplayRecord: OfflineReplayRecord? = null

            if (isAllFound) {
                timerJob?.cancel()
                feedbackManager.playVictoryFeedback()
                val completionMs = current.elapsedMillis.coerceAtLeast(1000L)
                val code = current.challengeCode.ifBlank {
                    ChallengeLinkPayload.computeDeterministicCode(
                        title = current.levelTitle,
                        theme = current.theme,
                        variationCount = current.variationCount,
                        complexity = current.patternComplexity,
                        spots = current.spots,
                        offlineLevelNumber = current.offlineLevelNumber
                    )
                }
                val playerName = currentActivePlayerName()
                val playerAvatar = currentActivePlayerAvatar()
                val reward = RewardCalculator.calculateReward(
                    isVictory = true,
                    spotsFound = newFound.size,
                    totalSpots = current.spots.size,
                    complexity = current.patternComplexity,
                    remainingMillis = current.remainingMillis,
                    timeLimitSeconds = current.timeLimitSeconds,
                    winStreak = nextWinStreak
                )
                victoryReward = reward

                val replayId = if (current.offlineLevelNumber != null) {
                    "offline_${current.offlineLevelNumber}"
                } else {
                    "level_${code.lowercase()}"
                }
                val replayRecord = OfflineReplayRecord(
                    replayId = replayId,
                    levelTitle = ChallengeLinkPayload.stripChallengePrefix(current.levelTitle),
                    offlineLevelNumber = current.offlineLevelNumber,
                    challengeCode = code,
                    themeId = current.theme.id,
                    complexityId = current.patternComplexity.id,
                    variationCount = current.spots.size,
                    timeLimitSeconds = current.timeLimitSeconds,
                    completionTimeMillis = completionMs,
                    score = newScore,
                    mistakesCount = current.mistakesCount,
                    winStreakAtCompletion = nextWinStreak,
                    recordedAtEpochMillis = System.currentTimeMillis(),
                    spotsData = DifferenceSpot.serializeList(current.spots),
                    spotTimestamps = newTimestamps
                )
                completedReplayRecord = replayRecord

                viewModelScope.launch {
                    if (current.offlineLevelNumber != null) {
                        preferencesRepository.markOfflineLevelCompleted(current.offlineLevelNumber)
                    }
                    preferencesRepository.recordOfflineReplayPerformance(replayRecord)
                    preferencesRepository.recordGameOutcome(
                        levelTitle = current.levelTitle,
                        theme = current.theme,
                        complexity = current.patternComplexity,
                        durationMillis = completionMs,
                        differencesFound = newFound.size,
                        isWin = true,
                        shardsEarned = reward.totalShardsAwarded
                    )
                    preferencesRepository.recordChallengeCompletion(
                        ChallengeTimeRecord(
                            challengeCode = code,
                            levelTitle = ChallengeLinkPayload.stripChallengePrefix(current.levelTitle),
                            playerName = playerName,
                            playerAvatarData = playerAvatar,
                            completionTimeMillis = completionMs,
                            score = newScore,
                            themeId = current.theme.id,
                            variationCount = current.variationCount,
                            complexityId = current.patternComplexity.id,
                            isCurrentUser = true
                        )
                    )
                }
            }

            val clearingHint = current.activeHintIndex == hitSpot.index
            val updatedState = current.copy(
                foundSpotIndices = newFound,
                spotTimestamps = newTimestamps,
                activeHintIndex = if (clearingHint) null else current.activeHintIndex,
                hintOffsetNormalizedX = if (clearingHint) 0f else current.hintOffsetNormalizedX,
                hintOffsetNormalizedY = if (clearingHint) 0f else current.hintOffsetNormalizedY,
                comboCount = newCombo,
                score = newScore,
                activeWinStreak = nextWinStreak,
                streakTierBadge = nextStreakProfile.tierBadge,
                isStreakEscalated = nextStreakProfile.isComplexityEscalated,
                lastTapFeedback = TapFeedback(hitSpot.normalizedX, hitSpot.normalizedY, isHit = true),
                rewardSummary = victoryReward ?: current.rewardSummary,
                latestReplayRecord = completedReplayRecord ?: current.latestReplayRecord,
                isVictory = isAllFound
            )
            _gameSessionState.value = updatedState

            if (isAllFound && !isGuest) {
                submitVictoryScore(updatedState)
            }
        } else {
            val penaltyMillis = 3000L
            val nextRemaining = (current.remainingMillis - penaltyMillis).coerceAtLeast(0L)
            val timedOut = nextRemaining == 0L
            var timeoutReward: LevelRewardSummary? = null
            if (timedOut) {
                timerJob?.cancel()
                feedbackManager.playTimeOutFeedback()
                val reward = RewardCalculator.calculateReward(
                    isVictory = false,
                    spotsFound = current.foundSpotIndices.size,
                    totalSpots = current.spots.size,
                    complexity = current.patternComplexity,
                    remainingMillis = 0L,
                    timeLimitSeconds = current.timeLimitSeconds,
                    winStreak = 0
                )
                timeoutReward = reward
                viewModelScope.launch {
                    preferencesRepository.recordGameOutcome(
                        levelTitle = current.levelTitle,
                        theme = current.theme,
                        complexity = current.patternComplexity,
                        durationMillis = current.elapsedMillis.coerceAtLeast(1000L),
                        differencesFound = current.foundSpotIndices.size,
                        isWin = false,
                        shardsEarned = reward.totalShardsAwarded
                    )
                }
            } else {
                feedbackManager.playMissPenaltyFeedback()
            }

            _gameSessionState.value = current.copy(
                remainingMillis = nextRemaining,
                comboCount = 0,
                mistakesCount = current.mistakesCount + 1,
                score = max(0, current.score - 40),
                activeWinStreak = if (timedOut) 0 else current.activeWinStreak,
                streakTierBadge = if (timedOut) "Base Complexity" else current.streakTierBadge,
                isStreakEscalated = if (timedOut) false else current.isStreakEscalated,
                lastTapFeedback = TapFeedback(normalizedX, normalizedY, isHit = false),
                rewardSummary = timeoutReward ?: current.rewardSummary,
                isTimeOut = timedOut
            )
        }
    }

    fun useHint() {
        val current = _gameSessionState.value as? GameSessionUiState.Playing ?: return
        if (current.isVictory || current.isTimeOut || current.hintsRemaining <= 0 || current.hintCooldownMillis > 0L) return

        val unfoundSpots = current.spots.filter { it.index !in current.foundSpotIndices }
        if (unfoundSpots.isEmpty()) return

        // Prefer a different unfound spot if one is already hinted, or pick the first unfound spot
        val nextUnfound = unfoundSpots.firstOrNull { it.index != current.activeHintIndex } ?: unfoundSpots.first()

        // Slight deterministic offset so the faint circle encloses the general area around the hidden difference
        // without giving away the exact center pixel, keeping the hint fair and rewarding.
        val jitterSeed = (nextUnfound.index + 1) * 73
        val offsetX = (((jitterSeed % 5) - 2) * 0.011f).coerceIn(-0.024f, 0.024f)
        val offsetY = ((((jitterSeed / 5) % 5) - 2) * 0.011f).coerceIn(-0.024f, 0.024f)
        val cooldownMs = hintCooldownFor(current.patternComplexity)

        val bonusReserve = preferences.value.bonusHintsInventory
        if (bonusReserve > 0 && current.hintsRemaining <= bonusReserve) {
            viewModelScope.launch {
                preferencesRepository.consumeBonusHintIfAvailable()
            }
        }

        feedbackManager.playHintFeedback()
        _gameSessionState.value = current.copy(
            activeHintIndex = nextUnfound.index,
            hintOffsetNormalizedX = offsetX,
            hintOffsetNormalizedY = offsetY,
            hintsRemaining = current.hintsRemaining - 1,
            hintCooldownMillis = cooldownMs,
            hintTotalCooldownMillis = cooldownMs
        )
    }

    fun submitVictoryScore(playingState: GameSessionUiState.Playing? = null) {
        if (isGuest) return
        val current = playingState ?: (_gameSessionState.value as? GameSessionUiState.Playing) ?: return
        if (current.scoreSubmitted || current.isSubmittingScore) return

        val activeName = currentActivePlayerName()
        val activeAvatar = currentActivePlayerAvatar()
        val taggedLevelTitle = ChallengeLinkPayload.withChallengePrefix(
            code = current.challengeCode,
            cleanTitle = current.levelTitle
        )

        viewModelScope.launch {
            _gameSessionState.update { state ->
                if (state is GameSessionUiState.Playing) state.copy(isSubmittingScore = true) else state
            }
            val completionMs = current.elapsedMillis.coerceAtLeast(1000L)
            val result = spotDiffRepository.submitLeaderboardScore(
                theme = current.theme.id,
                variationCount = current.variationCount,
                patternComplexity = current.patternComplexity.id,
                completionTimeMillis = completionMs,
                score = current.score.coerceAtLeast(100),
                levelTitle = taggedLevelTitle,
                playerName = activeName,
                playerAvatarData = activeAvatar
            )
            _gameSessionState.update { state ->
                if (state is GameSessionUiState.Playing) {
                    state.copy(
                        isSubmittingScore = false,
                        scoreSubmitted = result.isSuccess
                    )
                } else state
            }
        }
    }

    fun exitCurrentGame() {
        timerJob?.cancel()
        generationJob?.cancel()
        _gameSessionState.value = GameSessionUiState.Idle
    }

    // --- User Profile & AI Avatar Studio Actions ---

    fun updateProfileDisplayName(name: String) {
        _profileState.update {
            it.copy(
                displayNameInput = name.take(40),
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun updateAiAvatarPrompt(prompt: String) {
        _profileState.update {
            it.copy(
                aiPromptInput = prompt.take(260),
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun selectPresetAvatar(preset: com.example.data.model.AvatarPreset) {
        feedbackManager.playClickFeedback()
        _profileState.update {
            it.copy(
                selectedAvatarData = preset.token,
                selectedPresetStyle = preset,
                statusMessage = "Selected '${preset.displayName}' avatar emblem.",
                errorMessage = null
            )
        }
        persistProfileStateInternal(
            displayName = _profileState.value.displayNameInput,
            avatarData = preset.token,
            avatarPrompt = _profileState.value.aiPromptInput,
            silent = true
        )
    }

    fun applyPresetInspirationPrompt(preset: com.example.data.model.AvatarPreset) {
        feedbackManager.playClickFeedback()
        _profileState.update {
            it.copy(
                selectedPresetStyle = preset,
                aiPromptInput = preset.aiPromptSuggestion,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun generateCustomAiAvatar() {
        val current = _profileState.value
        if (current.isGeneratingAiAvatar) return
        feedbackManager.playClickFeedback()

        viewModelScope.launch {
            _profileState.update {
                it.copy(
                    isGeneratingAiAvatar = true,
                    statusMessage = null,
                    errorMessage = null
                )
            }
            try {
                val (_, dataUri) = aiRepository.generateCustomAvatarDataUri(
                    userPrompt = current.aiPromptInput,
                    presetStyle = current.selectedPresetStyle
                )
                feedbackManager.playSpotFoundFeedback()
                _profileState.update {
                    it.copy(
                        selectedAvatarData = dataUri,
                        isGeneratingAiAvatar = false,
                        statusMessage = "Custom AI Avatar generated and synced to your profile!",
                        errorMessage = null
                    )
                }
                persistProfileStateInternal(
                    displayName = _profileState.value.displayNameInput,
                    avatarData = dataUri,
                    avatarPrompt = current.aiPromptInput,
                    silent = true
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI avatar generation timed out", e)
                _profileState.update {
                    it.copy(
                        isGeneratingAiAvatar = false,
                        errorMessage = "AI avatar generation timed out after 60s. Tap Generate again to retry."
                    )
                }
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI avatar quota exceeded", e)
                _profileState.update {
                    it.copy(
                        isGeneratingAiAvatar = false,
                        errorMessage = "Rate limit or quota reached for AI avatar generation. Try again shortly or pick a preset below."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate custom AI avatar", e)
                _profileState.update {
                    it.copy(
                        isGeneratingAiAvatar = false,
                        errorMessage = e.localizedMessage ?: "Failed to generate custom AI avatar."
                    )
                }
            }
        }
    }

    fun saveUserProfileChanges() {
        val current = _profileState.value
        if (current.isSavingProfile) return
        feedbackManager.playClickFeedback()
        persistProfileStateInternal(
            displayName = current.displayNameInput,
            avatarData = current.selectedAvatarData,
            avatarPrompt = current.aiPromptInput,
            silent = false
        )
    }

    private fun persistProfileStateInternal(
        displayName: String,
        avatarData: String,
        avatarPrompt: String,
        silent: Boolean
    ) {
        val cleanName = displayName.trim().ifBlank {
            Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: if (isGuest) "Guest Spotter" else "Spotter Pro"
        }
        val cleanAvatar = avatarData.trim().ifBlank {
            com.example.data.model.AvatarPreset.CYBER_DETECTIVE.token
        }

        viewModelScope.launch {
            if (!silent) {
                _profileState.update { it.copy(isSavingProfile = true, statusMessage = null, errorMessage = null) }
            }
            preferencesRepository.savePlayerProfile(
                displayName = cleanName,
                avatarData = cleanAvatar,
                avatarPrompt = avatarPrompt
            )
            if (!isGuest) {
                val result = spotDiffRepository.saveUserProfile(
                    displayName = cleanName,
                    avatarData = cleanAvatar,
                    avatarPrompt = avatarPrompt
                )
                if (!silent) {
                    result.fold(
                        onSuccess = {
                            feedbackManager.playSpotFoundFeedback()
                            _profileState.update {
                                it.copy(
                                    isSavingProfile = false,
                                    displayNameInput = cleanName,
                                    statusMessage = "Profile & Avatar synced to Global Leaderboard!"
                                )
                            }
                        },
                        onFailure = { err ->
                            _profileState.update {
                                it.copy(
                                    isSavingProfile = false,
                                    errorMessage = "Saved locally, cloud sync failed: ${err.localizedMessage ?: "Error"}"
                                )
                            }
                        }
                    )
                }
            } else if (!silent) {
                feedbackManager.playSpotFoundFeedback()
                _profileState.update {
                    it.copy(
                        isSavingProfile = false,
                        displayNameInput = cleanName,
                        statusMessage = "Profile & Avatar saved! Connect Google in Options to sync with Global Leaderboard."
                    )
                }
            }
        }
    }

    // --- Custom Level Designer Actions ---

    fun updateDesignerTitle(title: String) {
        _designerState.update { it.copy(title = title.take(60), statusMessage = null) }
    }

    fun updateDesignerPrompt(prompt: String) {
        _designerState.update { it.copy(scenePrompt = prompt.take(380), statusMessage = null) }
    }

    fun updateDesignerTheme(theme: GameTheme) {
        feedbackManager.playClickFeedback()
        _designerState.update { it.copy(theme = theme, statusMessage = null) }
    }

    fun updateDesignerVariationCount(count: Int) {
        val coerced = count.coerceIn(3, 10)
        _designerState.update { state ->
            val currentSpots = state.spots
            val updatedSpots = if (coerced > currentSpots.size) {
                val defaults = defaultDesignerSpots(coerced, state.patternComplexity)
                (currentSpots + defaults.drop(currentSpots.size)).mapIndexed { idx, s -> s.copy(index = idx) }
            } else {
                currentSpots.take(coerced).mapIndexed { idx, s -> s.copy(index = idx) }
            }
            state.copy(
                variationCount = coerced,
                spots = updatedSpots,
                selectedSpotIndex = state.selectedSpotIndex.coerceIn(0, coerced - 1),
                statusMessage = null
            )
        }
    }

    fun updateDesignerTimeLimit(seconds: Int) {
        _designerState.update { it.copy(timeLimitSeconds = seconds.coerceIn(20, 300), statusMessage = null) }
    }

    fun updateDesignerComplexity(complexity: PatternComplexity) {
        feedbackManager.playClickFeedback()
        _designerState.update { state ->
            state.copy(
                patternComplexity = complexity,
                spots = state.spots.map { it.copy(normalizedRadius = complexity.defaultRadius) },
                statusMessage = null
            )
        }
    }

    fun updateDesignerVisibility(visibility: String) {
        _designerState.update { it.copy(visibility = visibility, statusMessage = null) }
    }

    fun selectDesignerSpot(index: Int) {
        _designerState.update { state ->
            state.copy(selectedSpotIndex = index.coerceIn(0, (state.spots.size - 1).coerceAtLeast(0)))
        }
    }

    fun moveSelectedDesignerSpot(normalizedX: Float, normalizedY: Float) {
        val x = normalizedX.coerceIn(0.12f, 0.88f)
        val y = normalizedY.coerceIn(0.12f, 0.88f)
        feedbackManager.playClickFeedback()
        _designerState.update { state ->
            val updated = state.spots.mapIndexed { idx, spot ->
                if (idx == state.selectedSpotIndex) spot.copy(normalizedX = x, normalizedY = y) else spot
            }
            state.copy(spots = updated)
        }
    }

    fun updateSelectedDesignerSpotType(type: VariationType) {
        feedbackManager.playClickFeedback()
        _designerState.update { state ->
            val updated = state.spots.mapIndexed { idx, spot ->
                if (idx == state.selectedSpotIndex) {
                    spot.copy(
                        variationType = type,
                        label = "Spot #${idx + 1} (${type.displayName.substringBefore(" ")})",
                        hint = "Look for the ${type.displayName.lowercase()} here"
                    )
                } else spot
            }
            state.copy(spots = updated)
        }
    }

    fun updateShareCodeSearch(query: String) {
        _designerState.update { it.copy(shareCodeSearchQuery = query.uppercase().take(16)) }
    }

    fun enhanceDesignerPromptWithAi() {
        val current = _designerState.value
        if (current.isEnhancingPrompt) return

        viewModelScope.launch {
            _designerState.update { it.copy(isEnhancingPrompt = true, statusMessage = null) }
            try {
                val enhanced = aiRepository.enhanceCustomPrompt(
                    theme = current.theme,
                    complexity = current.patternComplexity,
                    rawIdea = current.scenePrompt.ifBlank { current.title }
                )
                _designerState.update {
                    it.copy(
                        scenePrompt = enhanced.take(380),
                        isEnhancingPrompt = false,
                        statusMessage = "AI enhanced your scene description!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enhance prompt with AI", e)
                _designerState.update {
                    it.copy(
                        isEnhancingPrompt = false,
                        statusMessage = "AI Enhance error: ${e.localizedMessage ?: "Failed"}"
                    )
                }
            }
        }
    }

    fun publishCustomLevel(onPublished: (CustomLevel) -> Unit = {}) {
        if (isGuest) {
            _designerState.update {
                it.copy(statusMessage = "Sign in with Google to publish and share custom levels online, or tap 'Test Play' to play locally!")
            }
            return
        }
        val current = _designerState.value
        if (current.isSaving) return

        val title = current.title.trim().ifBlank { "${current.theme.displayName} Custom" }
        val prompt = current.scenePrompt.trim().ifBlank {
            "${current.theme.promptStyle}, ${current.patternComplexity.promptDetailInstruction}"
        }
        val spotsSerialized = DifferenceSpot.serializeList(current.spots)
        val authorName = _profileState.value.displayNameInput.trim().ifBlank {
            preferences.value.playerDisplayName
        }.ifBlank {
            Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Puzzle Architect"
        }

        viewModelScope.launch {
            _designerState.update { it.copy(isSaving = true, statusMessage = null) }
            val result = spotDiffRepository.createCustomLevel(
                title = title,
                scenePrompt = prompt,
                theme = current.theme.id,
                variationCount = current.variationCount,
                timeLimitSeconds = current.timeLimitSeconds,
                patternComplexity = current.patternComplexity.id,
                visibility = current.visibility,
                spotsData = spotsSerialized,
                authorName = authorName
            )
            result.fold(
                onSuccess = { created ->
                    feedbackManager.playSpotFoundFeedback()
                    _designerState.update {
                        it.copy(
                            isSaving = false,
                            title = "",
                            scenePrompt = "",
                            lastCreatedShareCode = created.shareCode,
                            statusMessage = "Published '${created.title}'! Share Code: ${created.shareCode}"
                        )
                    }
                    onPublished(created)
                },
                onFailure = { err ->
                    _designerState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Failed to save level: ${err.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun deleteCustomLevel(levelId: String) {
        if (isGuest) return
        viewModelScope.launch {
            spotDiffRepository.deleteCustomLevel(levelId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        generationJob?.cancel()
        feedbackManager.stopAmbientMusic()
    }
}
