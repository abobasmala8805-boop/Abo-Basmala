package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotDiffRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCustomLevel_validPayload_succeedsAndEmitsInSharedLevels() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SpotDiffRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCustomLevel(
                title = "Crystal Forest",
                scenePrompt = "Glowing crystal trees and floating lanterns",
                theme = "NATURE",
                variationCount = 5,
                timeLimitSeconds = 90,
                patternComplexity = "MODERATE",
                visibility = "SHARED",
                spotsData = "0.250,0.350,0.075,Lantern,COLOR_SHIFT,#38BDF8,Check top left"
            )
        }
        assertTrue(createResult.isSuccess)
        val createdLevel = createResult.getOrThrow()
        assertTrue(createdLevel.id.isNotBlank())

        val sharedLevels = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSharedLevels().first { list -> list.any { it.id == createdLevel.id } }
        }
        assertTrue(sharedLevels.any { it.id == createdLevel.id })
    }

    @Test
    fun getCustomLevelById_crossUserPrivateLevel_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = SpotDiffRepository(firestore)
        val privateLevel = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCustomLevel(
                title = "Secret Vault",
                scenePrompt = "Private geometric chamber",
                theme = "GEOMETRIC",
                variationCount = 4,
                timeLimitSeconds = 60,
                patternComplexity = "SUBTLE",
                visibility = "PRIVATE",
                spotsData = "0.500,0.500,0.080,Cube,PATCH_MIRROR,#F59E0B,Center cube"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SpotDiffRepository(firestore)
        val fetchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCustomLevelById(privateLevel.id)
        }
        assertTrue(fetchResult.isFailure)
        val firestoreEx = checkNotNull(fetchResult.exceptionOrNull()?.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun submitLeaderboardScore_andObserveGlobalLeaderboard_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SpotDiffRepository(firestore)

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitLeaderboardScore(
                theme = "SPACE",
                variationCount = 6,
                patternComplexity = "INTRICATE",
                completionTimeMillis = 19500L,
                score = 2400,
                levelTitle = "Nebula Core"
            )
        }
        assertTrue(submitResult.isSuccess)
        val entryId = submitResult.getOrThrow()

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGlobalLeaderboard().first { list -> list.any { it.id == entryId } }
        }
        assertTrue(entries.any { it.id == entryId })
    }

    @Test
    fun observeSharedLevels_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SpotDiffRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSharedLevels().first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        val ex = checkNotNull(caughtException) { "Expected exception for unauthenticated flow" }
        val firestoreException = checkNotNull(ex.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $ex"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun saveUserProfile_andSyncLeaderboardAvatar_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SpotDiffRepository(firestore)

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitLeaderboardScore(
                theme = "CYBERPUNK",
                variationCount = 5,
                patternComplexity = "MODERATE",
                completionTimeMillis = 22000L,
                score = 2100,
                levelTitle = "Synthwave Rooftop",
                playerName = "Alice",
                playerAvatarData = "preset:cyber_detective"
            )
        }
        assertTrue(submitResult.isSuccess)
        val scoreId = submitResult.getOrThrow()

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(
                displayName = "Alice AI",
                avatarData = "preset:neon_kitsune",
                avatarPrompt = "Neon kitsune mask"
            )
        }
        assertTrue(profileResult.isSuccess)

        val updatedEntries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGlobalLeaderboard().first { list ->
                list.any { it.id == scoreId && it.playerAvatarData == "preset:neon_kitsune" && it.playerName == "Alice AI" }
            }
        }
        assertTrue(updatedEntries.any { it.id == scoreId && it.playerAvatarData == "preset:neon_kitsune" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice@spotdiff.test"
        const val BOB_EMAIL = "bob@spotdiff.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
