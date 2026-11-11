package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AvatarPreset
import com.example.data.model.CustomLevel
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SpotDiffRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun createCustomLevel(
        title: String,
        scenePrompt: String,
        theme: String,
        variationCount: Int,
        timeLimitSeconds: Int,
        patternComplexity: String,
        visibility: String,
        spotsData: String,
        authorName: String = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Puzzle Architect"
    ): Result<CustomLevel> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "lvl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val shareCode = UUID.randomUUID().toString().replace("-", "").uppercase().take(6)
        val docRef = db.collection(COLLECTION_CUSTOM_LEVELS).document(docId)

        val safeTitle = title.trim().take(60).let { if (it.length < 3) "$it Level" else it }
        val safePrompt = scenePrompt.trim().take(400).let { if (it.length < 3) "Vibrant puzzle scene" else it }
        val safeAuthor = authorName.trim().take(60).ifBlank { "Puzzle Architect" }
        val safeSpots = spotsData.take(4000).ifBlank { "0.250,0.350,0.075,Glowing Orb,COLOR_SHIFT,#38BDF8,Look near the top left" }

        val payload = mapOf(
            "userId" to uid,
            "authorName" to safeAuthor,
            "title" to safeTitle,
            "scenePrompt" to safePrompt,
            "theme" to theme,
            "variationCount" to variationCount.coerceIn(3, 10).toLong(),
            "timeLimitSeconds" to timeLimitSeconds.coerceIn(20, 300).toLong(),
            "patternComplexity" to patternComplexity,
            "visibility" to if (visibility == "PRIVATE") "PRIVATE" else "SHARED",
            "shareCode" to shareCode,
            "spotsData" to safeSpots,
            "playsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val created = CustomLevel(
                id = docId,
                userId = uid,
                authorName = safeAuthor,
                title = safeTitle,
                scenePrompt = safePrompt,
                theme = theme,
                variationCount = variationCount.coerceIn(3, 10).toLong(),
                timeLimitSeconds = timeLimitSeconds.coerceIn(20, 300).toLong(),
                patternComplexity = patternComplexity,
                visibility = if (visibility == "PRIVATE") "PRIVATE" else "SHARED",
                shareCode = shareCode,
                spotsData = safeSpots,
                playsCount = 0L
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCustomLevelById(levelId: String): Result<CustomLevel> {
        val docRef = db.collection(COLLECTION_CUSTOM_LEVELS).document(levelId)
        return try {
            val snap = docRef.get().await()
            val level = snap.toObject(
                CustomLevel::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Level $levelId not found")
            Result.success(level)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSharedLevels(): Flow<List<CustomLevel>> {
        val path = COLLECTION_CUSTOM_LEVELS
        return db.collection(COLLECTION_CUSTOM_LEVELS)
            .whereEqualTo("visibility", "SHARED")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomLevel::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeMyLevels(userId: String): Flow<List<CustomLevel>> {
        val path = COLLECTION_CUSTOM_LEVELS
        return db.collection(COLLECTION_CUSTOM_LEVELS)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomLevel::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun deleteCustomLevel(levelId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_CUSTOM_LEVELS).document(levelId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitLeaderboardScore(
        theme: String,
        variationCount: Int,
        patternComplexity: String,
        completionTimeMillis: Long,
        score: Int,
        levelTitle: String,
        boardId: String = "GLOBAL",
        playerName: String = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Spotter Pro",
        playerAvatarData: String = AvatarPreset.CYBER_DETECTIVE.token
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "score_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_LEADERBOARD).document(docId)

        val safePlayer = playerName.trim().take(60).ifBlank { "Spotter Pro" }
        val safeAvatar = playerAvatarData.trim().take(28_000).ifBlank { AvatarPreset.CYBER_DETECTIVE.token }
        val safeTitle = levelTitle.trim().take(80).ifBlank { "AI Spot Challenge" }

        val payload = mapOf(
            "userId" to uid,
            "playerName" to safePlayer,
            "playerAvatarData" to safeAvatar,
            "boardId" to if (boardId == "PRIVATE") "PRIVATE" else "GLOBAL",
            "theme" to theme,
            "variationCount" to variationCount.coerceIn(3, 10).toLong(),
            "patternComplexity" to patternComplexity,
            "completionTimeMillis" to completionTimeMillis.coerceIn(1000L, 600000L),
            "score" to score.coerceIn(0, 1000000).toLong(),
            "levelTitle" to safeTitle,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGlobalLeaderboard(): Flow<List<LeaderboardEntry>> {
        val path = COLLECTION_LEADERBOARD
        return db.collection(COLLECTION_LEADERBOARD)
            .whereEqualTo("boardId", "GLOBAL")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<LeaderboardEntry> { it.completionTimeMillis }
                        .thenByDescending { it.score }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeUserLeaderboard(userId: String): Flow<List<LeaderboardEntry>> {
        val path = COLLECTION_LEADERBOARD
        return db.collection(COLLECTION_LEADERBOARD)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.completionTimeMillis }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveUserProfile(
        displayName: String,
        avatarData: String,
        avatarPrompt: String = ""
    ): Result<UserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USER_PROFILES).document(uid)
        val safeName = displayName.trim().take(60).ifBlank {
            auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Spotter Pro"
        }
        val safeAvatar = avatarData.trim().take(28_000).ifBlank { AvatarPreset.CYBER_DETECTIVE.token }
        val safePrompt = avatarPrompt.trim().take(280)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "avatarData" to safeAvatar,
                        "avatarPrompt" to safePrompt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "avatarData" to safeAvatar,
                        "avatarPrompt" to safePrompt,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Also propagate the updated displayName and avatarData to any existing leaderboard entries owned by this user
            val myScoresSnap = db.collection(COLLECTION_LEADERBOARD)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (scoreDoc in myScoresSnap.documents) {
                scoreDoc.reference.update(
                    mapOf(
                        "playerName" to safeName,
                        "playerAvatarData" to safeAvatar,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = safeName,
                    avatarData = safeAvatar,
                    avatarPrompt = safePrompt
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection(COLLECTION_USER_PROFILES).document(userId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    companion object {
        const val COLLECTION_CUSTOM_LEVELS = "custom_levels"
        const val COLLECTION_LEADERBOARD = "leaderboard_entries"
        const val COLLECTION_USER_PROFILES = "user_profiles"
    }
}
