package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.repository.AiLevelGeneratorRepository
import com.example.data.repository.OfflinePracticeRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.SpotDiffRepository
import com.example.ui.components.ChallengeLinkGeneratorDialog
import com.example.ui.components.DailyLoginCalendarModal
import com.example.ui.components.OfflineReplayTheaterModal
import com.example.ui.components.RewardShopModal
import com.example.ui.components.SocialSnapshotModal
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LevelDesignerScreen
import com.example.ui.screens.OptionsScreen
import com.example.ui.screens.PlayGameScreen
import com.example.ui.screens.PracticeLevelsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.SpotDiffTheme
import com.example.ui.viewmodel.SpotDiffViewModel
import com.example.util.FeedbackManager
import com.example.util.authStateFlow
import com.example.util.configureAppCheck
import com.example.util.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainDestination {
    PLAY,
    PRACTICE,
    DESIGNER,
    LEADERBOARD,
    PROFILE,
    OPTIONS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val initialDeepLink = intent?.dataString

        setContent {
            SpotDiffAppRoot(initialChallengeUri = initialDeepLink)
        }
    }
}

@Composable
fun SpotDiffAppRoot(
    auth: FirebaseAuth = Firebase.auth,
    initialChallengeUri: String? = null
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by rememberSaveable { mutableStateOf(false) }
    val user = currentUser

    if (user == null && !isGuestMode) {
        SpotDiffTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AuthScreen(
                    onAuthSuccess = {},
                    onContinueAsGuest = { isGuestMode = true }
                )
            }
        }
    } else {
        val sessionKey = user?.uid ?: "guest_local_player"
        val isGuest = (user == null)
        key(sessionKey, isGuest) {
            AuthenticatedMainScreen(
                currentUserId = sessionKey,
                isGuest = isGuest,
                initialChallengeUri = initialChallengeUri,
                onExitGuest = { isGuestMode = false }
            )
        }
    }
}

@Composable
fun AuthenticatedMainScreen(
    currentUserId: String,
    isGuest: Boolean = false,
    initialChallengeUri: String? = null,
    onExitGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: SpotDiffViewModel = viewModel(
        key = "$currentUserId-$isGuest",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SpotDiffViewModel(
                    spotDiffRepository = SpotDiffRepository(db),
                    preferencesRepository = PreferencesRepository(app),
                    aiRepository = AiLevelGeneratorRepository(),
                    offlinePracticeRepository = OfflinePracticeRepository(app),
                    feedbackManager = FeedbackManager(app),
                    currentUserId = currentUserId,
                    isGuest = isGuest
                )
            }
        }
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val gameSessionState by viewModel.gameSessionState.collectAsStateWithLifecycle()
    val designerState by viewModel.designerState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val challengeLinkState by viewModel.challengeLinkState.collectAsStateWithLifecycle()
    val activeSnapshotSpec by viewModel.activeSnapshotSpec.collectAsStateWithLifecycle()
    val rewardShopState by viewModel.rewardShopState.collectAsStateWithLifecycle()
    val replayTheaterState by viewModel.replayTheaterState.collectAsStateWithLifecycle()
    val dailyLoginModalState by viewModel.dailyLoginModalState.collectAsStateWithLifecycle()
    val sharedLevelsState by viewModel.sharedLevelsState.collectAsStateWithLifecycle()
    val myLevelsState by viewModel.myLevelsState.collectAsStateWithLifecycle()
    val globalLeaderboardState by viewModel.globalLeaderboardState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainDestination.PLAY) }
    var practiceInitialTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(initialChallengeUri) {
        if (!initialChallengeUri.isNullOrBlank()) {
            viewModel.handleIncomingChallengeUri(initialChallengeUri)
        }
    }

    SpotDiffTheme(gameTheme = preferences.selectedTheme) {
        if (dailyLoginModalState.isOpen && initialChallengeUri.isNullOrBlank()) {
            DailyLoginCalendarModal(
                preferences = preferences,
                latestReceipt = dailyLoginModalState.latestReceipt,
                statusMessage = dailyLoginModalState.statusMessage,
                onClaimToday = viewModel::claimDailyLoginReward,
                onSimulateNextDay = viewModel::simulateNextLoginDay,
                onOpenRewardShop = {
                    viewModel.closeDailyLoginModal()
                    viewModel.openRewardShop(initialTab = 0)
                },
                onDismiss = viewModel::closeDailyLoginModal
            )
        }

        if (rewardShopState.isOpen) {
            RewardShopModal(
                preferences = preferences,
                statusMessage = rewardShopState.statusMessage,
                onDismiss = viewModel::closeRewardShop,
                onPurchaseOrSelectTheme = viewModel::purchaseOrSelectTheme,
                onPurchaseOrEquipFrame = viewModel::purchaseOrEquipAvatarFrame,
                onPurchaseHintPack = viewModel::purchaseHintPack,
                initialTab = rewardShopState.initialTab
            )
        }

        val activeReplayRecord = replayTheaterState.record
        if (activeReplayRecord != null) {
            OfflineReplayTheaterModal(
                record = activeReplayRecord,
                originalBitmap = replayTheaterState.originalBitmap,
                modifiedBitmap = replayTheaterState.modifiedBitmap,
                onDismiss = viewModel::closeReplayTheater,
                onPlayLevelNow = { rec ->
                    viewModel.playFromReplayRecord(rec) {
                        currentTab = MainDestination.PLAY
                    }
                }
            )
        }

        val activeChallengeModal = challengeLinkState.activeModalPayload
        if (activeChallengeModal != null) {
            val standings = remember(activeChallengeModal.challengeCode, globalLeaderboardState, preferences.challengeTimeRecords) {
                viewModel.getChallengeStandings(activeChallengeModal.challengeCode)
            }
            ChallengeLinkGeneratorDialog(
                payload = activeChallengeModal,
                standings = standings,
                onDismiss = viewModel::dismissChallengeGeneratorModal,
                onPlayChallengeNow = { payload ->
                    viewModel.acceptAndLaunchChallenge(payload) {
                        currentTab = MainDestination.PLAY
                    }
                }
            )
        }

        val currentSnapshotSpec = activeSnapshotSpec
        if (currentSnapshotSpec != null) {
            SocialSnapshotModal(
                spec = currentSnapshotSpec,
                onDismiss = viewModel::dismissSocialSnapshotModal
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == MainDestination.PLAY,
                        onClick = { currentTab = MainDestination.PLAY },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_play)) },
                        modifier = Modifier.testTag("nav_tab_play")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainDestination.PRACTICE,
                        onClick = { currentTab = MainDestination.PRACTICE },
                        icon = { Icon(Icons.Default.OfflineBolt, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_practice)) },
                        modifier = Modifier.testTag("nav_tab_practice")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainDestination.DESIGNER,
                        onClick = { currentTab = MainDestination.DESIGNER },
                        icon = { Icon(Icons.Default.Extension, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_designer)) },
                        modifier = Modifier.testTag("nav_tab_designer")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainDestination.LEADERBOARD,
                        onClick = { currentTab = MainDestination.LEADERBOARD },
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_leaderboard)) },
                        modifier = Modifier.testTag("nav_tab_leaderboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainDestination.PROFILE,
                        onClick = { currentTab = MainDestination.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_profile)) },
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainDestination.OPTIONS,
                        onClick = { currentTab = MainDestination.OPTIONS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.nav_options)) },
                        modifier = Modifier.testTag("nav_tab_options")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainDestination.PLAY -> {
                        PlayGameScreen(
                            viewModel = viewModel,
                            preferences = preferences,
                            sessionState = gameSessionState,
                            onNavigateToPractice = {
                                practiceInitialTab = 0
                                currentTab = MainDestination.PRACTICE
                            },
                            onNavigateToReplayGallery = {
                                practiceInitialTab = 1
                                currentTab = MainDestination.PRACTICE
                            },
                            onNavigateToLeaderboard = { currentTab = MainDestination.LEADERBOARD },
                            onNavigateToDesigner = { currentTab = MainDestination.DESIGNER }
                        )
                    }

                    MainDestination.PRACTICE -> {
                        PracticeLevelsScreen(
                            levels = viewModel.offlinePracticeLevels,
                            completedLevels = preferences.completedOfflineLevels,
                            savedReplayRecords = preferences.offlineReplayRecords,
                            initialSectionTab = practiceInitialTab,
                            onSelectOfflineLevel = { levelNumber ->
                                currentTab = MainDestination.PLAY
                                viewModel.startOfflinePracticeLevel(levelNumber)
                            },
                            onWatchReplay = viewModel::openReplayTheater,
                            onPlayFromReplay = { rec ->
                                viewModel.playFromReplayRecord(rec) {
                                    currentTab = MainDestination.PLAY
                                }
                            },
                            onBackToPlay = { currentTab = MainDestination.PLAY }
                        )
                    }

                    MainDestination.DESIGNER -> {
                        LevelDesignerScreen(
                            viewModel = viewModel,
                            designerState = designerState,
                            sharedLevelsState = sharedLevelsState,
                            myLevelsState = myLevelsState,
                            onPlayCustomLevel = { customLevel ->
                                currentTab = MainDestination.PLAY
                                viewModel.startNewAiLevel(customLevel)
                            },
                            onBackToPlay = { currentTab = MainDestination.PLAY }
                        )
                    }

                    MainDestination.LEADERBOARD -> {
                        LeaderboardScreen(
                            leaderboardState = globalLeaderboardState,
                            localChallengeRecords = preferences.challengeTimeRecords,
                            currentUserId = currentUserId,
                            currentPlayerAvatarData = profileState.selectedAvatarData,
                            isGuest = isGuest,
                            onOpenProfile = { currentTab = MainDestination.PROFILE },
                            onGenerateChallengeForEntry = viewModel::openChallengeGeneratorForLeaderboardEntry,
                            onBackToPlay = { currentTab = MainDestination.PLAY }
                        )
                    }

                    MainDestination.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            profileState = profileState,
                            preferences = preferences,
                            isGuest = isGuest,
                            onNavigateToLeaderboard = { currentTab = MainDestination.LEADERBOARD },
                            onNavigateToReplayGallery = {
                                practiceInitialTab = 1
                                currentTab = MainDestination.PRACTICE
                            },
                            onBackToPlay = { currentTab = MainDestination.PLAY }
                        )
                    }

                    MainDestination.OPTIONS -> {
                        OptionsScreen(
                            viewModel = viewModel,
                            preferences = preferences,
                            isGuest = isGuest,
                            onOpenProfile = { currentTab = MainDestination.PROFILE },
                            onSignOut = {
                                if (isGuest) {
                                    onExitGuest()
                                } else {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            },
                            onBackToPlay = { currentTab = MainDestination.PLAY }
                        )
                    }
                }
            }
        }
    }
}
