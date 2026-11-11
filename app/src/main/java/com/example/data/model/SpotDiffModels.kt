package com.example.data.model

import android.util.Base64
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GameTheme(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val promptStyle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val unlockCostShards: Int = 0
) {
    NATURE(
        id = "NATURE",
        displayName = "Mystic Nature",
        subtitle = "Bioluminescent forests, botanical sanctuaries & alpine lakes",
        promptStyle = "Lush bioluminescent nature sanctuary with glowing lanterns, intricate botanical flora, crystal water reflections, vibrant emerald and cyan palette, high-detail digital illustration",
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF06B6D4,
        unlockCostShards = 0
    ),
    SPACE(
        id = "SPACE",
        displayName = "Deep Space",
        subtitle = "Cosmic nebulae, orbital observatories & starlit geometry",
        promptStyle = "Deep space cosmic nebula observatory with ringed exoplanets, glowing star clusters, golden astrolabe rings, and celestial satellites, rich midnight indigo and magenta palette, crisp sci-fi illustration",
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFFEC4899,
        unlockCostShards = 0
    ),
    GEOMETRIC(
        id = "GEOMETRIC",
        displayName = "Minimalist Geometric",
        subtitle = "Bauhaus architecture, isometric monuments & crisp symmetry",
        promptStyle = "Minimalist geometric isometric architecture inspired by Bauhaus and Escher, clean interlocking polyhedrons, warm terracotta, cobalt blue, and sunlit gold shapes, ultra-crisp vector art aesthetic",
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFF38BDF8,
        unlockCostShards = 0
    ),
    CYBERPUNK(
        id = "CYBERPUNK",
        displayName = "Neon Synthwave",
        subtitle = "Futuristic cyber alleys, hologram shrines & retro-tech",
        promptStyle = "Futuristic synthwave cyberpunk rooftop garden with neon holograms, floating drones, glowing circuit totems, electric cyan and coral pink lighting, detailed digital concept art",
        primaryHex = 0xFFEC4899,
        secondaryHex = 0xFF22D3EE,
        unlockCostShards = 0
    ),
    ANCIENT(
        id = "ANCIENT",
        displayName = "Clockwork Relics",
        subtitle = "Celestial brass mechanisms, temple mosaics & sun dials",
        promptStyle = "Intricate ancient clockwork temple library with brass gears, stained glass celestial domes, glowing runes, and marble pillars, warm amber and turquoise accents, detailed fantasy illustration",
        primaryHex = 0xFFD97706,
        secondaryHex = 0xFF14B8A6,
        unlockCostShards = 0
    ),
    SAKURA_ZENITH(
        id = "SAKURA_ZENITH",
        displayName = "Sakura Zenith",
        subtitle = "Floating lantern pagodas, cherry blossom mist & koi ponds",
        promptStyle = "Serene floating Japanese pagoda sanctuary surrounded by glowing sakura cherry blossoms, paper lanterns, arched vermilion bridges, and mirror koi ponds, rose quartz and golden dawn palette",
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFFFBBF24,
        unlockCostShards = 160
    ),
    ATLANTIS_CORAL(
        id = "ATLANTIS_CORAL",
        displayName = "Atlantis Coral",
        subtitle = "Sunken crystal domes, bioluminescent reefs & pearl arches",
        promptStyle = "Majestic underwater Atlantis crystal sanctuary with bioluminescent coral reefs, marble arches, golden tridents, and schooling jellyfish, electric aquamarine and violet palette",
        primaryHex = 0xFF06B6D4,
        secondaryHex = 0xFFA855F7,
        unlockCostShards = 240
    ),
    SOLARPUNK_OASIS(
        id = "SOLARPUNK_OASIS",
        displayName = "Solarpunk Oasis",
        subtitle = "Hanging sky-gardens, stained-glass wind sails & waterfalls",
        promptStyle = "Utopian solarpunk floating terrace with stained-glass solar sails, cascading waterfalls, brass botanical conservatories, and sunlit greenery, vibrant emerald and radiant gold palette",
        primaryHex = 0xFF34D399,
        secondaryHex = 0xFFF59E0B,
        unlockCostShards = 320
    );

    companion object {
        val defaultUnlockedIds: Set<String> = entries
            .filter { it.unlockCostShards == 0 }
            .map { it.id }
            .toSet()

        fun fromId(id: String): GameTheme = entries.firstOrNull { it.id == id } ?: NATURE
    }
}

enum class AvatarFrame(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val costShards: Int,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    DEFAULT_RING(
        id = "DEFAULT_RING",
        displayName = "Standard Neon Ring",
        subtitle = "Clean dual-tone gradient border",
        costShards = 0,
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFEC4899
    ),
    EMERALD_VINE(
        id = "EMERALD_VINE",
        displayName = "Bioluminescent Halo",
        subtitle = "Glowing emerald double-ring with diamond nodes",
        costShards = 120,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF06B6D4
    ),
    SOLAR_CROWN(
        id = "SOLAR_CROWN",
        displayName = "Solar Flare Crown",
        subtitle = "Radiant golden sun-crest with 8 starlight points",
        costShards = 180,
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFFFB7185
    ),
    CYBER_HEX(
        id = "CYBER_HEX",
        displayName = "Synthwave Hex Matrix",
        subtitle = "Futuristic octagonal neon circuit armor",
        costShards = 240,
        primaryHex = 0xFFEC4899,
        secondaryHex = 0xFF22D3EE
    ),
    CELESTIAL_ORBIT(
        id = "CELESTIAL_ORBIT",
        displayName = "Celestial Astrolabe",
        subtitle = "Deep-space orbital rings with satellite moons",
        costShards = 320,
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFFFBBF24
    ),
    GRANDMASTER_PRISM(
        id = "GRANDMASTER_PRISM",
        displayName = "Apex Prismatic Aura",
        subtitle = "Multi-spectrum diamond crown for master spotters",
        costShards = 450,
        primaryHex = 0xFFA855F7,
        secondaryHex = 0xFF34D399
    );

    companion object {
        val defaultUnlockedIds: Set<String> = setOf(DEFAULT_RING.id)

        fun fromId(id: String): AvatarFrame =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT_RING
    }
}

enum class HintShopPack(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val hintsGranted: Int,
    val costShards: Int,
    val badgeText: String
) {
    SINGLE_CHARGE(
        id = "SINGLE_CHARGE",
        displayName = "+1 Bonus Hint Charge",
        subtitle = "Add 1 extra reserve hint to your inventory for any level",
        hintsGranted = 1,
        costShards = 50,
        badgeText = "QUICK REFILL"
    ),
    DETECTIVE_BUNDLE(
        id = "DETECTIVE_BUNDLE",
        displayName = "+3 Detective Hint Pack",
        subtitle = "Stockpile 3 reserve hints for tricky Intricate puzzles",
        hintsGranted = 3,
        costShards = 125,
        badgeText = "SAVE 25 SHARDS"
    ),
    GRANDMASTER_VAULT(
        id = "GRANDMASTER_VAULT",
        displayName = "+8 Grandmaster Hint Vault",
        subtitle = "Massive reserve of 8 bonus hints for high-streak runs",
        hintsGranted = 8,
        costShards = 280,
        badgeText = "BEST VALUE"
    )
}

data class LevelRewardSummary(
    val baseShards: Int,
    val differencesBonusShards: Int,
    val speedBonusShards: Int,
    val streakBonusShards: Int,
    val totalShardsAwarded: Int
)

object RewardCalculator {
    fun calculateReward(
        isVictory: Boolean,
        spotsFound: Int,
        totalSpots: Int,
        complexity: PatternComplexity,
        remainingMillis: Long,
        timeLimitSeconds: Int,
        winStreak: Int
    ): LevelRewardSummary {
        if (!isVictory) {
            val consolation = (spotsFound.coerceAtLeast(0) * 6).coerceIn(5, 45)
            return LevelRewardSummary(
                baseShards = 0,
                differencesBonusShards = consolation,
                speedBonusShards = 0,
                streakBonusShards = 0,
                totalShardsAwarded = consolation
            )
        }
        val base = when (complexity) {
            PatternComplexity.SUBTLE -> 55
            PatternComplexity.MODERATE -> 75
            PatternComplexity.INTRICATE -> 105
        }
        val diffsBonus = totalSpots.coerceAtLeast(3) * 10
        val totalLimitMs = (timeLimitSeconds.coerceAtLeast(20) * 1000L).toFloat()
        val remainingRatio = (remainingMillis.toFloat() / totalLimitMs).coerceIn(0f, 1f)
        val speedBonus = when {
            remainingRatio >= 0.60f -> 45
            remainingRatio >= 0.35f -> 25
            remainingRatio >= 0.15f -> 10
            else -> 0
        }
        val streakBonus = (winStreak.coerceIn(0, 8) * 15)
        val total = base + diffsBonus + speedBonus + streakBonus
        return LevelRewardSummary(
            baseShards = base,
            differencesBonusShards = diffsBonus,
            speedBonusShards = speedBonus,
            streakBonusShards = streakBonus,
            totalShardsAwarded = total
        )
    }
}

enum class AvatarPreset(
    val id: String,
    val token: String,
    val displayName: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val aiPromptSuggestion: String
) {
    CYBER_DETECTIVE(
        id = "CYBER_DETECTIVE",
        token = "preset:cyber_detective",
        displayName = "Cyber Detective",
        subtitle = "Neon visor & holographic lens",
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFFEC4899,
        aiPromptSuggestion = "Futuristic neon cyberpunk detective portrait with glowing cyan visor goggles, stylized digital illustration avatar"
    ),
    FOREST_SPIRIT(
        id = "FOREST_SPIRIT",
        token = "preset:forest_spirit",
        displayName = "Forest Spirit",
        subtitle = "Bioluminescent guardian",
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF06B6D4,
        aiPromptSuggestion = "Mystical bioluminescent forest spirit guardian with glowing emerald antlers and fireflies, stylized fantasy portrait avatar"
    ),
    COSMIC_VOYAGER(
        id = "COSMIC_VOYAGER",
        token = "preset:cosmic_voyager",
        displayName = "Cosmic Voyager",
        subtitle = "Starlight helm & nebula crest",
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFFA855F7,
        aiPromptSuggestion = "Celestial deep space astronaut voyager with galaxy reflection in golden helmet visor, vibrant sci-fi portrait avatar"
    ),
    BAUHAUS_PRISM(
        id = "BAUHAUS_PRISM",
        token = "preset:bauhaus_prism",
        displayName = "Bauhaus Prism",
        subtitle = "Geometric crystalline mask",
        primaryHex = 0xFFF59E0B,
        secondaryHex = 0xFF38BDF8,
        aiPromptSuggestion = "Minimalist Bauhaus geometric crystal mask character made of interlocking gold and cobalt polyhedrons, crisp vector art avatar"
    ),
    CLOCKWORK_OWL(
        id = "CLOCKWORK_OWL",
        token = "preset:clockwork_owl",
        displayName = "Clockwork Owl",
        subtitle = "Brass gears & sharp eyes",
        primaryHex = 0xFFD97706,
        secondaryHex = 0xFF14B8A6,
        aiPromptSuggestion = "Ornate steampunk clockwork owl scholar with brass monocle and glowing turquoise eyes, detailed fantasy avatar"
    ),
    NEON_KITSUNE(
        id = "NEON_KITSUNE",
        token = "preset:neon_kitsune",
        displayName = "Neon Kitsune",
        subtitle = "Synthwave fox mask",
        primaryHex = 0xFFEC4899,
        secondaryHex = 0xFFFBBF24,
        aiPromptSuggestion = "Stylized synthwave kitsune fox spirit mask with glowing magenta and gold circuit markings, vibrant anime-inspired game avatar"
    ),
    SOLAR_PHOENIX(
        id = "SOLAR_PHOENIX",
        token = "preset:solar_phoenix",
        displayName = "Solar Phoenix",
        subtitle = "Radiant ember crest",
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFFF59E0B,
        aiPromptSuggestion = "Majestic solar phoenix emblem with radiant golden and coral feathers, high-contrast game profile avatar"
    ),
    CRYSTAL_SPHINX(
        id = "CRYSTAL_SPHINX",
        token = "preset:crystal_sphinx",
        displayName = "Crystal Sphinx",
        subtitle = "Ancient riddle master",
        primaryHex = 0xFF22D3EE,
        secondaryHex = 0xFF818CF8,
        aiPromptSuggestion = "Ancient crystal guardian sphinx with glowing sapphire runes and golden crown, stylized game character avatar"
    );

    companion object {
        fun fromToken(token: String): AvatarPreset =
            entries.firstOrNull { it.token.equals(token, ignoreCase = true) || it.id.equals(token, ignoreCase = true) }
                ?: CYBER_DETECTIVE
    }
}

enum class PatternComplexity(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultRadius: Float,
    val scoreMultiplier: Float,
    val promptDetailInstruction: String
) {
    SUBTLE(
        id = "SUBTLE",
        displayName = "Subtle & Clean",
        description = "Balanced composition with clear focal points and gentle variations",
        defaultRadius = 0.085f,
        scoreMultiplier = 1.0f,
        promptDetailInstruction = "clean composition with distinct focal objects and clear negative space"
    ),
    MODERATE(
        id = "MODERATE",
        displayName = "Moderate Detail",
        description = "Rich layered patterns with mid-sized chromatic and structural changes",
        defaultRadius = 0.070f,
        scoreMultiplier = 1.5f,
        promptDetailInstruction = "richly layered scene with multiple decorative motifs, repeating patterns, and secondary props"
    ),
    INTRICATE(
        id = "INTRICATE",
        displayName = "Intricate Master",
        description = "High-density visual tapestry with micro-variations for sharp eyes",
        defaultRadius = 0.058f,
        scoreMultiplier = 2.2f,
        promptDetailInstruction = "ultra-intricate, dense tapestry of fine geometric and organic micro-patterns with many small objects"
    );

    companion object {
        fun fromId(id: String): PatternComplexity = entries.firstOrNull { it.id == id } ?: MODERATE
    }
}

enum class VariationType(
    val id: String,
    val displayName: String
) {
    COLOR_SHIFT("COLOR_SHIFT", "Chromatic Hue Shift"),
    PATCH_MIRROR("PATCH_MIRROR", "Symmetry Flip"),
    LUMINANCE_PULSE("LUMINANCE_PULSE", "Glow & Contrast Anomaly"),
    DETAIL_CLONE("DETAIL_CLONE", "Vanishing Object"),
    PRISM_SHIFT("PRISM_SHIFT", "Prismatic Refraction");

    companion object {
        fun fromId(id: String): VariationType = entries.firstOrNull { it.id == id } ?: COLOR_SHIFT
    }
}

data class DifferenceSpot(
    val index: Int,
    val normalizedX: Float, // 0.12f .. 0.88f
    val normalizedY: Float, // 0.12f .. 0.88f
    val normalizedRadius: Float, // 0.05f .. 0.11f
    val label: String,
    val hint: String,
    val variationType: VariationType = VariationType.COLOR_SHIFT,
    val accentHex: String = "#38BDF8"
) {
    fun serialize(): String {
        val safeLabel = label.replace("|", " ").replace(",", " ").take(40).ifBlank { "Anomaly" }
        val safeHint = hint.replace("|", " ").replace(",", " ").take(80).ifBlank { "Look closely in this area" }
        return String.format(
            Locale.US,
            "%.3f,%.3f,%.3f,%s,%s,%s,%s",
            normalizedX.coerceIn(0.10f, 0.90f),
            normalizedY.coerceIn(0.10f, 0.90f),
            normalizedRadius.coerceIn(0.045f, 0.12f),
            safeLabel,
            variationType.id,
            accentHex,
            safeHint
        )
    }

    companion object {
        fun serializeList(spots: List<DifferenceSpot>): String {
            return spots.joinToString("|") { it.serialize() }
        }

        fun deserializeList(raw: String): List<DifferenceSpot> {
            if (raw.isBlank()) return emptyList()
            return raw.split("|").mapIndexedNotNull { idx, token ->
                val parts = token.split(",")
                if (parts.size < 4) return@mapIndexedNotNull null
                val x = parts[0].toFloatOrNull()?.coerceIn(0.10f, 0.90f) ?: return@mapIndexedNotNull null
                val y = parts[1].toFloatOrNull()?.coerceIn(0.10f, 0.90f) ?: return@mapIndexedNotNull null
                val r = parts[2].toFloatOrNull()?.coerceIn(0.045f, 0.12f) ?: 0.075f
                val label = parts[3].ifBlank { "Spot #${idx + 1}" }
                val type = if (parts.size > 4) VariationType.fromId(parts[4]) else VariationType.COLOR_SHIFT
                val hex = if (parts.size > 5 && parts[5].startsWith("#")) parts[5] else "#38BDF8"
                val hint = if (parts.size > 6) parts[6] else "Inspect the $label carefully."
                DifferenceSpot(
                    index = idx,
                    normalizedX = x,
                    normalizedY = y,
                    normalizedRadius = r,
                    label = label,
                    hint = hint,
                    variationType = type,
                    accentHex = hex
                )
            }
        }
    }
}

data class OfflinePracticeLevel(
    val levelNumber: Int, // 1..100
    val title: String,
    val subtitle: String,
    val theme: GameTheme,
    val variationCount: Int,
    val timeLimitSeconds: Int,
    val complexity: PatternComplexity,
    val spots: List<DifferenceSpot>
)

@IgnoreExtraProperties
data class CustomLevel(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val title: String = "",
    val scenePrompt: String = "",
    val theme: String = GameTheme.NATURE.id,
    val variationCount: Long = 5L,
    val timeLimitSeconds: Long = 90L,
    val patternComplexity: String = PatternComplexity.MODERATE.id,
    val visibility: String = "SHARED",
    val shareCode: String = "",
    val spotsData: String = "",
    val playsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val playerAvatarData: String = AvatarPreset.CYBER_DETECTIVE.token,
    val boardId: String = "GLOBAL",
    val theme: String = GameTheme.NATURE.id,
    val variationCount: Long = 5L,
    val patternComplexity: String = PatternComplexity.MODERATE.id,
    val completionTimeMillis: Long = 30000L,
    val score: Long = 1000L,
    val levelTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarData: String = AvatarPreset.CYBER_DETECTIVE.token,
    val avatarPrompt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LevelHistoryPoint(
    val label: String,
    val themeId: String,
    val complexityId: String,
    val durationSeconds: Float,
    val differencesFound: Int,
    val isWin: Boolean
) {
    fun serialize(): String = String.format(
        Locale.US,
        "%s,%s,%s,%.1f,%d,%d",
        label.replace(",", " ").replace("|", " ").take(24).ifBlank { "Level" },
        themeId,
        complexityId,
        durationSeconds.coerceIn(1f, 600f),
        differencesFound.coerceIn(0, 20),
        if (isWin) 1 else 0
    )

    companion object {
        fun serializeList(list: List<LevelHistoryPoint>): String =
            list.takeLast(24).joinToString("|") { it.serialize() }

        fun deserializeList(raw: String): List<LevelHistoryPoint> {
            if (raw.isBlank()) return emptyList()
            return raw.split("|").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size < 6) return@mapNotNull null
                val label = parts[0].ifBlank { "Level" }
                val themeId = parts[1].ifBlank { GameTheme.NATURE.id }
                val complexityId = parts[2].ifBlank { PatternComplexity.MODERATE.id }
                val durationSec = parts[3].toFloatOrNull()?.coerceIn(1f, 600f) ?: return@mapNotNull null
                val diffs = parts[4].toIntOrNull()?.coerceIn(0, 20) ?: 0
                val isWin = parts[5] == "1"
                LevelHistoryPoint(
                    label = label,
                    themeId = themeId,
                    complexityId = complexityId,
                    durationSeconds = durationSec,
                    differencesFound = diffs,
                    isWin = isWin
                )
            }
        }
    }
}

data class ChallengeTimeRecord(
    val challengeCode: String,
    val levelTitle: String,
    val playerName: String,
    val playerAvatarData: String,
    val completionTimeMillis: Long,
    val score: Int,
    val themeId: String,
    val variationCount: Int,
    val complexityId: String,
    val isCurrentUser: Boolean = true
) {
    fun serialize(): String = String.format(
        Locale.US,
        "%s,%s,%s,%d,%d,%s,%d,%s,%d",
        challengeCode.replace(",", "").replace("|", "").take(12).ifBlank { "DUEL01" },
        levelTitle.replace(",", " ").replace("|", " ").take(36).ifBlank { "Challenge" },
        playerName.replace(",", " ").replace("|", " ").take(28).ifBlank { "Spotter" },
        completionTimeMillis.coerceIn(1000L, 600000L),
        score.coerceAtLeast(0),
        themeId,
        variationCount.coerceIn(3, 10),
        complexityId,
        if (isCurrentUser) 1 else 0
    )

    companion object {
        fun serializeList(records: List<ChallengeTimeRecord>): String =
            records.takeLast(40).joinToString("|") { it.serialize() }

        fun deserializeList(raw: String, fallbackAvatar: String = AvatarPreset.CYBER_DETECTIVE.token): List<ChallengeTimeRecord> {
            if (raw.isBlank()) return emptyList()
            return raw.split("|").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size < 9) return@mapNotNull null
                val code = parts[0].ifBlank { return@mapNotNull null }
                val title = parts[1].ifBlank { "Challenge" }
                val player = parts[2].ifBlank { "Spotter" }
                val timeMs = parts[3].toLongOrNull()?.coerceIn(1000L, 600000L) ?: return@mapNotNull null
                val score = parts[4].toIntOrNull()?.coerceAtLeast(0) ?: 100
                val themeId = parts[5].ifBlank { GameTheme.NATURE.id }
                val variations = parts[6].toIntOrNull()?.coerceIn(3, 10) ?: 5
                val complexityId = parts[7].ifBlank { PatternComplexity.MODERATE.id }
                val isCurrentUser = parts[8] == "1"
                ChallengeTimeRecord(
                    challengeCode = code,
                    levelTitle = title,
                    playerName = player,
                    playerAvatarData = if (isCurrentUser) fallbackAvatar else AvatarPreset.COSMIC_VOYAGER.token,
                    completionTimeMillis = timeMs,
                    score = score,
                    themeId = themeId,
                    variationCount = variations,
                    complexityId = complexityId,
                    isCurrentUser = isCurrentUser
                )
            }
        }
    }
}

data class ChallengeLinkPayload(
    val challengeCode: String,
    val title: String,
    val scenePrompt: String,
    val theme: GameTheme,
    val variationCount: Int,
    val timeLimitSeconds: Int,
    val patternComplexity: PatternComplexity,
    val spots: List<DifferenceSpot>,
    val offlineLevelNumber: Int? = null,
    val challengerName: String = "Spotter Pro",
    val challengerTimeMillis: Long? = null
) {
    fun encodePayloadToken(): String {
        val cleanTitle = stripChallengePrefix(title).replace("~", " ").take(45).ifBlank { "${theme.displayName} Duel" }
        val cleanPrompt = scenePrompt.replace("~", " ").take(180).ifBlank { theme.promptStyle.take(160) }
        val cleanChallenger = challengerName.replace("~", " ").take(30).ifBlank { "Spotter Pro" }
        val spotsSerialized = DifferenceSpot.serializeList(spots)
        val raw = listOf(
            challengeCode.uppercase(Locale.US).take(10),
            cleanTitle,
            cleanPrompt,
            theme.id,
            variationCount.coerceIn(3, 10).toString(),
            timeLimitSeconds.coerceIn(20, 300).toString(),
            patternComplexity.id,
            (offlineLevelNumber ?: -1).toString(),
            cleanChallenger,
            (challengerTimeMillis ?: -1L).toString(),
            spotsSerialized
        ).joinToString("~")

        return Base64.encodeToString(
            raw.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun toChallengeUrl(): String {
        val token = encodePayloadToken()
        return "https://spotdiff.aistudio.app/challenge?code=${challengeCode.uppercase(Locale.US)}&data=$token"
    }

    fun toDeepLinkUri(): String {
        val token = encodePayloadToken()
        return "spotdiff://challenge?code=${challengeCode.uppercase(Locale.US)}&data=$token"
    }

    fun toShareMessage(): String {
        val cleanTitle = stripChallengePrefix(title)
        val url = toChallengeUrl()
        val timeChallengeLine = if (challengerTimeMillis != null && challengerTimeMillis > 0L) {
            String.format(
                Locale.US,
                "⚡ Target Time to Beat: %.1fs (by %s)\n",
                challengerTimeMillis / 1000.0,
                challengerName
            )
        } else {
            "⚡ Created by: $challengerName — Let's see who finishes in the shortest time!\n"
        }

        return buildString {
            append("🎯 SpotDiff AI Speed Challenge: \"$cleanTitle\"\n")
            append(timeChallengeLine)
            append("🎨 Theme: ${theme.displayName} • ${variationCount} Differences • ${patternComplexity.displayName} (${timeLimitSeconds}s limit)\n")
            append("🔑 Challenge Code: ${challengeCode.uppercase(Locale.US)}\n")
            append("🔗 Challenge Link:\n$url")
        }
    }

    companion object {
        private val PREFIX_REGEX = Regex("^\\[#([A-Za-z0-9_-]{3,12})]\\s*(.*)$")

        fun extractChallengeCodeFromTitle(rawTitle: String): String? {
            val match = PREFIX_REGEX.find(rawTitle.trim()) ?: return null
            return match.groupValues[1].uppercase(Locale.US)
        }

        fun stripChallengePrefix(rawTitle: String): String {
            val match = PREFIX_REGEX.find(rawTitle.trim()) ?: return rawTitle.trim()
            return match.groupValues[2].trim().ifBlank { rawTitle.trim() }
        }

        fun withChallengePrefix(code: String, cleanTitle: String): String {
            val safeCode = code.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9_-]"), "").take(10)
            val stripped = stripChallengePrefix(cleanTitle).ifBlank { "AI Speed Challenge" }
            if (safeCode.isBlank()) return stripped.take(80)
            return "[#$safeCode] $stripped".take(80)
        }

        fun computeDeterministicCode(
            title: String,
            theme: GameTheme,
            variationCount: Int,
            complexity: PatternComplexity,
            spots: List<DifferenceSpot>,
            offlineLevelNumber: Int? = null
        ): String {
            if (offlineLevelNumber != null) {
                return String.format(Locale.US, "OFL%03d", offlineLevelNumber.coerceIn(1, 100))
            }
            val existingCode = extractChallengeCodeFromTitle(title)
            if (!existingCode.isNullOrBlank()) return existingCode

            val seedStr = "${stripChallengePrefix(title)}|${theme.id}|$variationCount|${complexity.id}|" +
                spots.joinToString(";") {
                    String.format(Locale.US, "%.2f:%.2f:%s", it.normalizedX, it.normalizedY, it.variationType.id)
                }
            val hash = abs(seedStr.hashCode())
            val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            val sb = StringBuilder(6)
            var v = hash
            repeat(6) { idx ->
                sb.append(chars[(v + idx * 17) % chars.length])
                v /= chars.length
            }
            return sb.toString()
        }

        fun decodeFromPayloadToken(token: String): ChallengeLinkPayload? {
            return try {
                val cleanToken = token.trim()
                    .substringBefore(" ")
                    .substringBefore("\n")
                    .substringBefore("&")
                val decodedBytes = Base64.decode(cleanToken, Base64.URL_SAFE)
                val raw = String(decodedBytes, StandardCharsets.UTF_8)
                val parts = raw.split("~")
                if (parts.size < 11) return null
                val code = parts[0].trim().uppercase(Locale.US).ifBlank { "CHL001" }
                val title = parts[1].trim().ifBlank { "Puzzle Challenge" }
                val prompt = parts[2].trim()
                val theme = GameTheme.fromId(parts[3].trim())
                val variations = parts[4].toIntOrNull()?.coerceIn(3, 10) ?: 5
                val timeLimit = parts[5].toIntOrNull()?.coerceIn(20, 300) ?: 90
                val complexity = PatternComplexity.fromId(parts[6].trim())
                val offlineNum = parts[7].toIntOrNull()?.takeIf { it in 1..100 }
                val challenger = parts[8].trim().ifBlank { "Spotter Pro" }
                val challengerTimeMs = parts[9].toLongOrNull()?.takeIf { it > 0L }
                val spots = DifferenceSpot.deserializeList(parts[10])
                    .takeIf { it.isNotEmpty() }
                    ?: deterministicSpotsForSeed(code, variations, complexity)

                ChallengeLinkPayload(
                    challengeCode = code,
                    title = title,
                    scenePrompt = prompt.ifBlank { "${theme.promptStyle}, ${complexity.promptDetailInstruction}" },
                    theme = theme,
                    variationCount = spots.size,
                    timeLimitSeconds = timeLimit,
                    patternComplexity = complexity,
                    spots = spots,
                    offlineLevelNumber = offlineNum,
                    challengerName = challenger,
                    challengerTimeMillis = challengerTimeMs
                )
            } catch (_: Exception) {
                null
            }
        }

        fun parseFromInput(
            rawInput: String,
            knownCustomLevels: List<CustomLevel> = emptyList(),
            offlineLevels: List<OfflinePracticeLevel> = emptyList()
        ): ChallengeLinkPayload? {
            val trimmed = rawInput.trim()
            if (trimmed.isBlank()) return null

            // 1. Check if input contains `data=` query parameter from a Challenge Link URL or share message
            if (trimmed.contains("data=")) {
                val dataPart = trimmed.substringAfter("data=")
                    .substringBefore("&")
                    .substringBefore(" ")
                    .substringBefore("\n")
                    .trim()
                decodeFromPayloadToken(dataPart)?.let { return it }
            }

            // 2. Extract code from `code=` query parameter or `[#CODE]` or direct 4-10 char input
            val extractedCode = when {
                trimmed.contains("code=", ignoreCase = true) -> {
                    trimmed.substringAfter("code=", "")
                        .substringBefore("&")
                        .substringBefore(" ")
                        .substringBefore("\n")
                        .trim()
                        .uppercase(Locale.US)
                }
                trimmed.startsWith("#") || trimmed.startsWith("[#") -> {
                    trimmed.removePrefix("[#").removePrefix("#").substringBefore("]").trim().uppercase(Locale.US)
                }
                else -> {
                    // Check if the user pasted a multi-line message containing "Challenge Code: XXXXXX"
                    val codeLineMatch = Regex("(?:Challenge Code|Share Code|CODE)\\s*:\\s*([A-Za-z0-9_-]{4,12})", RegexOption.IGNORE_CASE)
                        .find(trimmed)
                    codeLineMatch?.groupValues?.get(1)?.uppercase(Locale.US)
                        ?: trimmed.uppercase(Locale.US).replace(Regex("[^A-Z0-9_-]"), "").take(10)
                }
            }

            if (extractedCode.length < 3) return null

            // 3. Check if it's an Offline Level code like OFL001..OFL100
            if (extractedCode.startsWith("OFL")) {
                val num = extractedCode.removePrefix("OFL").toIntOrNull()
                if (num != null) {
                    val offlineSpec = offlineLevels.firstOrNull { it.levelNumber == num }
                    if (offlineSpec != null) {
                        return ChallengeLinkPayload(
                            challengeCode = extractedCode,
                            title = offlineSpec.title,
                            scenePrompt = offlineSpec.subtitle,
                            theme = offlineSpec.theme,
                            variationCount = offlineSpec.variationCount,
                            timeLimitSeconds = offlineSpec.timeLimitSeconds,
                            patternComplexity = offlineSpec.complexity,
                            spots = offlineSpec.spots,
                            offlineLevelNumber = offlineSpec.levelNumber,
                            challengerName = "Offline Campaign"
                        )
                    }
                }
            }

            // 4. Check if it matches a known CustomLevel in Firestore by shareCode
            val matchedLevel = knownCustomLevels.firstOrNull {
                it.shareCode.equals(extractedCode, ignoreCase = true) ||
                    extractChallengeCodeFromTitle(it.title)?.equals(extractedCode, ignoreCase = true) == true
            }
            if (matchedLevel != null) {
                val theme = GameTheme.fromId(matchedLevel.theme)
                val complexity = PatternComplexity.fromId(matchedLevel.patternComplexity)
                val spots = DifferenceSpot.deserializeList(matchedLevel.spotsData)
                    .takeIf { it.isNotEmpty() }
                    ?: deterministicSpotsForSeed(extractedCode, matchedLevel.variationCount.toInt(), complexity)
                return ChallengeLinkPayload(
                    challengeCode = matchedLevel.shareCode.ifBlank { extractedCode }.uppercase(Locale.US),
                    title = stripChallengePrefix(matchedLevel.title),
                    scenePrompt = matchedLevel.scenePrompt,
                    theme = theme,
                    variationCount = spots.size,
                    timeLimitSeconds = matchedLevel.timeLimitSeconds.toInt().coerceIn(20, 300),
                    patternComplexity = complexity,
                    spots = spots,
                    offlineLevelNumber = null,
                    challengerName = matchedLevel.authorName.ifBlank { "Puzzle Architect" }
                )
            }

            // 5. Otherwise construct a deterministic Challenge Layout from the code seed so all friends with the same code play the exact same layout!
            val hash = abs(extractedCode.hashCode())
            val themes = GameTheme.entries
            val complexities = PatternComplexity.entries
            val theme = themes[hash % themes.size]
            val complexity = complexities[(hash / 7) % complexities.size]
            val variationCount = 4 + (hash % 5) // 4..8 spots
            val timeLimit = listOf(60, 75, 90, 120)[(hash / 13) % 4]
            val spots = deterministicSpotsForSeed(extractedCode, variationCount, complexity)

            return ChallengeLinkPayload(
                challengeCode = extractedCode,
                title = "${theme.displayName} Duel #$extractedCode",
                scenePrompt = "${theme.promptStyle}, ${complexity.promptDetailInstruction}",
                theme = theme,
                variationCount = spots.size,
                timeLimitSeconds = timeLimit,
                patternComplexity = complexity,
                spots = spots,
                offlineLevelNumber = null,
                challengerName = "Challenge Host"
            )
        }

        fun deterministicSpotsForSeed(
            seedCode: String,
            count: Int,
            complexity: PatternComplexity
        ): List<DifferenceSpot> {
            val anchorCandidates = listOf(
                0.22f to 0.24f, 0.76f to 0.22f, 0.50f to 0.48f,
                0.24f to 0.76f, 0.78f to 0.74f, 0.50f to 0.20f,
                0.20f to 0.50f, 0.80f to 0.50f, 0.50f to 0.80f,
                0.36f to 0.34f, 0.64f to 0.36f, 0.36f to 0.64f
            )
            val hash = abs(seedCode.hashCode())
            val types = VariationType.entries
            val colors = listOf("#38BDF8", "#F59E0B", "#EC4899", "#10B981", "#A855F7")
            val safeCount = count.coerceIn(3, 10)
            return (0 until safeCount).map { idx ->
                val candidateIdx = (idx * 3 + hash) % anchorCandidates.size
                val (baseX, baseY) = anchorCandidates[candidateIdx]
                val jitterX = (((hash + idx * 31) % 7) - 3) * 0.012f
                val jitterY = (((hash / 3 + idx * 19) % 7) - 3) * 0.012f
                val x = (baseX + jitterX).coerceIn(0.14f, 0.86f)
                val y = (baseY + jitterY).coerceIn(0.14f, 0.86f)
                val vType = types[(hash + idx) % types.size]
                DifferenceSpot(
                    index = idx,
                    normalizedX = x,
                    normalizedY = y,
                    normalizedRadius = complexity.defaultRadius,
                    label = "Anomaly #${idx + 1} (${vType.displayName.substringBefore(" ")})",
                    hint = "Inspect the ${vType.displayName.lowercase()} in this region",
                    variationType = vType,
                    accentHex = colors[idx % colors.size]
                )
            }
        }
    }
}

data class StreakEscalationProfile(
    val currentStreak: Int,
    val tierIndex: Int,
    val tierBadge: String,
    val tierDescription: String,
    val baseComplexity: PatternComplexity,
    val effectiveComplexity: PatternComplexity,
    val baseVariationCount: Int,
    val effectiveVariationCount: Int,
    val bonusVariations: Int,
    val anomalyRadiusScale: Float,
    val anomalySubtletyScale: Float,
    val scoreBonusMultiplier: Float,
    val aiPromptModifier: String,
    val nextTierStreakTarget: Int?
) {
    val isComplexityEscalated: Boolean
        get() = effectiveComplexity != baseComplexity || bonusVariations > 0 || tierIndex > 0
}

object StreakComplexityScaler {
    fun resolve(
        winStreak: Int,
        baseComplexity: PatternComplexity,
        baseVariationCount: Int
    ): StreakEscalationProfile {
        val safeStreak = winStreak.coerceAtLeast(0)
        val safeBaseCount = baseVariationCount.coerceIn(3, 10)

        return when {
            safeStreak >= 6 -> {
                val effectiveCount = (safeBaseCount + 3).coerceIn(3, 10)
                StreakEscalationProfile(
                    currentStreak = safeStreak,
                    tierIndex = 3,
                    tierBadge = "Grandmaster Overdrive",
                    tierDescription = "Max AI pattern density • Intricate micro-anomalies • +${effectiveCount - safeBaseCount} extra variations",
                    baseComplexity = baseComplexity,
                    effectiveComplexity = PatternComplexity.INTRICATE,
                    baseVariationCount = safeBaseCount,
                    effectiveVariationCount = effectiveCount,
                    bonusVariations = effectiveCount - safeBaseCount,
                    anomalyRadiusScale = 0.82f,
                    anomalySubtletyScale = 0.84f,
                    scoreBonusMultiplier = 1.50f,
                    aiPromptModifier = "STREAK OVERDRIVE (Streak $safeStreak): Weave an ultra-dense, interlocking tapestry of micro-architecture, camouflaged secondary ornaments, and fine repeating symmetries for an expert eye.",
                    nextTierStreakTarget = null
                )
            }
            safeStreak >= 4 -> {
                val effectiveCount = (safeBaseCount + 2).coerceIn(3, 10)
                StreakEscalationProfile(
                    currentStreak = safeStreak,
                    tierIndex = 2,
                    tierBadge = "Blazing Streak",
                    tierDescription = "Escalated to Intricate AI patterns • Tighter targets • +${effectiveCount - safeBaseCount} extra variations",
                    baseComplexity = baseComplexity,
                    effectiveComplexity = PatternComplexity.INTRICATE,
                    baseVariationCount = safeBaseCount,
                    effectiveVariationCount = effectiveCount,
                    bonusVariations = effectiveCount - safeBaseCount,
                    anomalyRadiusScale = 0.88f,
                    anomalySubtletyScale = 0.90f,
                    scoreBonusMultiplier = 1.30f,
                    aiPromptModifier = "HIGH WIN STREAK (Streak $safeStreak): Increase visual pattern complexity with layered decorative motifs, intricate background geometry, and subtle chromatic transitions.",
                    nextTierStreakTarget = 6
                )
            }
            safeStreak >= 2 -> {
                val escalatedComp = when (baseComplexity) {
                    PatternComplexity.SUBTLE -> PatternComplexity.MODERATE
                    PatternComplexity.MODERATE -> PatternComplexity.INTRICATE
                    PatternComplexity.INTRICATE -> PatternComplexity.INTRICATE
                }
                val effectiveCount = (safeBaseCount + 1).coerceIn(3, 10)
                StreakEscalationProfile(
                    currentStreak = safeStreak,
                    tierIndex = 1,
                    tierBadge = "Heating Up",
                    tierDescription = "AI complexity boosted to ${escalatedComp.displayName} • +${effectiveCount - safeBaseCount} bonus variation",
                    baseComplexity = baseComplexity,
                    effectiveComplexity = escalatedComp,
                    baseVariationCount = safeBaseCount,
                    effectiveVariationCount = effectiveCount,
                    bonusVariations = effectiveCount - safeBaseCount,
                    anomalyRadiusScale = 0.94f,
                    anomalySubtletyScale = 0.95f,
                    scoreBonusMultiplier = 1.15f,
                    aiPromptModifier = "RISING WIN STREAK (Streak $safeStreak): Add secondary visual patterns, balanced symmetry details, and richer ambient textures to challenge the player.",
                    nextTierStreakTarget = 4
                )
            }
            else -> {
                StreakEscalationProfile(
                    currentStreak = safeStreak,
                    tierIndex = 0,
                    tierBadge = if (safeStreak == 1) "First Win Spark" else "Base Complexity",
                    tierDescription = "Win ${2 - safeStreak} more consecutive level${if (2 - safeStreak == 1) "" else "s"} to unlock Tier 1 AI Complexity Boost",
                    baseComplexity = baseComplexity,
                    effectiveComplexity = baseComplexity,
                    baseVariationCount = safeBaseCount,
                    effectiveVariationCount = safeBaseCount,
                    bonusVariations = 0,
                    anomalyRadiusScale = 1.0f,
                    anomalySubtletyScale = 1.0f,
                    scoreBonusMultiplier = if (safeStreak == 1) 1.05f else 1.0f,
                    aiPromptModifier = "",
                    nextTierStreakTarget = 2
                )
            }
        }
    }
}

data class ReplaySpotTimestamp(
    val spotIndex: Int,
    val orderFound: Int,
    val foundAtElapsedMillis: Long,
    val splitDeltaMillis: Long,
    val label: String,
    val variationTypeId: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val accentHex: String
) {
    val variationType: VariationType
        get() = VariationType.fromId(variationTypeId)

    companion object {
        fun serializeList(list: List<ReplaySpotTimestamp>): String =
            list.joinToString("^") { item ->
                val safeLabel = item.label.replace(":", " ").replace("^", " ").replace(";", " ")
                String.format(
                    Locale.US,
                    "%d:%d:%d:%d:%s:%s:%.4f:%.4f:%s",
                    item.spotIndex,
                    item.orderFound,
                    item.foundAtElapsedMillis,
                    item.splitDeltaMillis,
                    safeLabel,
                    item.variationTypeId,
                    item.normalizedX,
                    item.normalizedY,
                    item.accentHex.replace(":", "")
                )
            }

        fun deserializeList(raw: String): List<ReplaySpotTimestamp> {
            if (raw.isBlank()) return emptyList()
            return raw.split("^").mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size < 9) return@mapNotNull null
                try {
                    ReplaySpotTimestamp(
                        spotIndex = parts[0].toInt(),
                        orderFound = parts[1].toInt(),
                        foundAtElapsedMillis = parts[2].toLong(),
                        splitDeltaMillis = parts[3].toLong(),
                        label = parts[4],
                        variationTypeId = parts[5],
                        normalizedX = parts[6].toFloat(),
                        normalizedY = parts[7].toFloat(),
                        accentHex = parts[8]
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.orderFound }
        }
    }
}

data class OfflineReplayRecord(
    val replayId: String,
    val levelTitle: String,
    val offlineLevelNumber: Int?,
    val challengeCode: String,
    val themeId: String,
    val complexityId: String,
    val variationCount: Int,
    val timeLimitSeconds: Int,
    val completionTimeMillis: Long,
    val score: Int,
    val mistakesCount: Int,
    val winStreakAtCompletion: Int,
    val recordedAtEpochMillis: Long = System.currentTimeMillis(),
    val spotsData: String,
    val spotTimestamps: List<ReplaySpotTimestamp>
) {
    val theme: GameTheme
        get() = GameTheme.fromId(themeId)

    val complexity: PatternComplexity
        get() = PatternComplexity.fromId(complexityId)

    val spots: List<DifferenceSpot>
        get() = DifferenceSpot.deserializeList(spotsData)

    val fastestSplitMillis: Long
        get() = spotTimestamps.minOfOrNull { it.splitDeltaMillis } ?: completionTimeMillis

    val averageSplitMillis: Long
        get() = if (spotTimestamps.isNotEmpty()) {
            completionTimeMillis / spotTimestamps.size
        } else {
            completionTimeMillis
        }

    fun toOfflinePracticeLevelSpec(): OfflinePracticeLevel {
        val parsedSpots = spots.ifEmpty {
            ChallengeLinkPayload.deterministicSpotsForSeed(
                seedCode = challengeCode.ifBlank { levelTitle },
                count = variationCount.coerceIn(3, 10),
                complexity = complexity
            )
        }
        val seedLevelNumber = offlineLevelNumber ?: ((abs(replayId.hashCode()) % 100) + 1)
        return OfflinePracticeLevel(
            levelNumber = seedLevelNumber,
            title = levelTitle,
            subtitle = "${theme.displayName} • ${parsedSpots.size} Differences • ${complexity.displayName}",
            theme = theme,
            variationCount = parsedSpots.size,
            timeLimitSeconds = timeLimitSeconds.coerceIn(20, 300),
            complexity = complexity,
            spots = parsedSpots
        )
    }

    companion object {
        fun serializeList(list: List<OfflineReplayRecord>): String =
            list.take(60).joinToString("\n") { rec ->
                val safeId = rec.replayId.replace("\t", " ").replace("\n", " ")
                val safeTitle = rec.levelTitle.replace("\t", " ").replace("\n", " ")
                val safeCode = rec.challengeCode.replace("\t", " ").replace("\n", " ")
                val safeSpots = rec.spotsData.replace("\t", " ").replace("\n", " ")
                val safeTimestamps = ReplaySpotTimestamp.serializeList(rec.spotTimestamps)
                listOf(
                    safeId,
                    safeTitle,
                    rec.offlineLevelNumber?.toString() ?: "",
                    safeCode,
                    rec.themeId,
                    rec.complexityId,
                    rec.variationCount.toString(),
                    rec.timeLimitSeconds.toString(),
                    rec.completionTimeMillis.toString(),
                    rec.score.toString(),
                    rec.mistakesCount.toString(),
                    rec.winStreakAtCompletion.toString(),
                    rec.recordedAtEpochMillis.toString(),
                    safeSpots,
                    safeTimestamps
                ).joinToString("\t")
            }

        fun deserializeList(raw: String): List<OfflineReplayRecord> {
            if (raw.isBlank()) return emptyList()
            return raw.split("\n").mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size < 15) return@mapNotNull null
                try {
                    OfflineReplayRecord(
                        replayId = parts[0],
                        levelTitle = parts[1],
                        offlineLevelNumber = parts[2].toIntOrNull(),
                        challengeCode = parts[3],
                        themeId = parts[4],
                        complexityId = parts[5],
                        variationCount = parts[6].toInt().coerceIn(3, 10),
                        timeLimitSeconds = parts[7].toInt().coerceIn(20, 300),
                        completionTimeMillis = parts[8].toLong().coerceAtLeast(500L),
                        score = parts[9].toInt(),
                        mistakesCount = parts[10].toInt(),
                        winStreakAtCompletion = parts[11].toInt(),
                        recordedAtEpochMillis = parts[12].toLong(),
                        spotsData = parts[13],
                        spotTimestamps = ReplaySpotTimestamp.deserializeList(parts[14])
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.completionTimeMillis }
        }

        fun createSyntheticReplayForOfflineLevel(
            spec: OfflinePracticeLevel,
            completionTimeMillis: Long = 16_400L + (spec.levelNumber * 190L) % 9_500L,
            winStreak: Int = 2,
            recordedAtEpochMillis: Long = System.currentTimeMillis() - (spec.levelNumber * 120_000L)
        ): OfflineReplayRecord {
            val count = spec.spots.size.coerceAtLeast(1)
            val weights = spec.spots.mapIndexed { idx, _ ->
                0.78f + ((spec.levelNumber * 7 + idx * 13) % 7) * 0.08f
            }
            val totalWeight = weights.sum().coerceAtLeast(0.1f)
            var cumulativeMs = 0L
            val timestamps = spec.spots.mapIndexed { idx, spot ->
                val split = if (idx == count - 1) {
                    (completionTimeMillis - cumulativeMs).coerceAtLeast(900L)
                } else {
                    ((completionTimeMillis * (weights[idx] / totalWeight)).toLong()).coerceAtLeast(900L)
                }
                cumulativeMs = if (idx == count - 1) completionTimeMillis else (cumulativeMs + split)
                ReplaySpotTimestamp(
                    spotIndex = spot.index,
                    orderFound = idx + 1,
                    foundAtElapsedMillis = cumulativeMs,
                    splitDeltaMillis = split,
                    label = spot.label,
                    variationTypeId = spot.variationType.id,
                    normalizedX = spot.normalizedX,
                    normalizedY = spot.normalizedY,
                    accentHex = spot.accentHex
                )
            }
            val code = ChallengeLinkPayload.computeDeterministicCode(
                title = spec.title,
                theme = spec.theme,
                variationCount = spec.spots.size,
                complexity = spec.complexity,
                spots = spec.spots,
                offlineLevelNumber = spec.levelNumber
            )
            val estScore = (spec.spots.size * 340 * spec.complexity.scoreMultiplier + 850).toInt()
            return OfflineReplayRecord(
                replayId = "offline_${spec.levelNumber}",
                levelTitle = spec.title,
                offlineLevelNumber = spec.levelNumber,
                challengeCode = code,
                themeId = spec.theme.id,
                complexityId = spec.complexity.id,
                variationCount = spec.spots.size,
                timeLimitSeconds = spec.timeLimitSeconds,
                completionTimeMillis = completionTimeMillis,
                score = estScore,
                mistakesCount = 0,
                winStreakAtCompletion = winStreak,
                recordedAtEpochMillis = recordedAtEpochMillis,
                spotsData = DifferenceSpot.serializeList(spec.spots),
                spotTimestamps = timestamps
            )
        }
    }
}

data class DifficultyTensionProfile(
    val isHighDifficulty: Boolean,
    val difficultyScore: Int,
    val tierBadge: String,
    val description: String,
    val baseShakeScale: Float
)

object DifficultyTensionCalculator {
    fun evaluate(
        complexity: PatternComplexity,
        variationCount: Int,
        timeLimitSeconds: Int,
        activeWinStreak: Int = 0
    ): DifficultyTensionProfile {
        val complexityPts = when (complexity) {
            PatternComplexity.SUBTLE -> 0
            PatternComplexity.MODERATE -> 1
            PatternComplexity.INTRICATE -> 3
        }
        val variationPts = when {
            variationCount >= 8 -> 3
            variationCount >= 6 -> 2
            variationCount >= 5 -> 1
            else -> 0
        }
        val timerPts = when {
            timeLimitSeconds <= 45 -> 3
            timeLimitSeconds <= 60 -> 2
            timeLimitSeconds <= 75 -> 1
            else -> 0
        }
        val streakPts = when {
            activeWinStreak >= 4 -> 2
            activeWinStreak >= 2 -> 1
            else -> 0
        }
        val totalScore = complexityPts + variationPts + timerPts + streakPts
        val isHigh = complexity == PatternComplexity.INTRICATE ||
            variationCount >= 6 ||
            timeLimitSeconds <= 60 ||
            activeWinStreak >= 2 ||
            totalScore >= 3

        return when {
            totalScore >= 6 -> DifficultyTensionProfile(
                isHighDifficulty = true,
                difficultyScore = totalScore,
                tierBadge = "EXTREME TENSION",
                description = "Maximum countdown pressure — subtle screen-shake intensifies through the countdown phase.",
                baseShakeScale = 1.0f
            )
            isHigh -> DifficultyTensionProfile(
                isHighDifficulty = true,
                difficultyScore = totalScore.coerceAtLeast(3),
                tierBadge = "HIGH DIFFICULTY TENSION",
                description = "High difficulty active — subtle countdown tremor & final-seconds screen-shake enabled.",
                baseShakeScale = 0.78f
            )
            else -> DifficultyTensionProfile(
                isHighDifficulty = false,
                difficultyScore = totalScore,
                tierBadge = "STANDARD CALM",
                description = "Select 6+ variations, <=60s timer, or Intricate patterns to trigger high-difficulty countdown shake.",
                baseShakeScale = 0.35f
            )
        }
    }

    /**
     * Computes the normalized screen-shake intensity [0f..1f] during the countdown phase.
     * - On High-Difficulty levels, produces:
     *   1) A subtle opening countdown lock-in tremor during the first 3.0s of the level.
     *   2) A rhythmic second-tick pulse once the timer enters the second half of the countdown or <= 45s.
     *   3) A continuous, escalating subtle screen-shake during the final 30 seconds (peaking in the final 10s).
     */
    fun computeCountdownShakeIntensity(
        profile: DifficultyTensionProfile,
        remainingMillis: Long,
        elapsedMillis: Long,
        timeLimitSeconds: Int,
        screenShakeEnabled: Boolean
    ): Float {
        if (!screenShakeEnabled) return 0f

        val totalMillis = (timeLimitSeconds.coerceAtLeast(20) * 1000L).toFloat()
        val remainingFraction = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
        val secondsLeft = remainingMillis / 1000f

        // Tick pulse at each second boundary (0..220ms into each second)
        val millisInSecond = (remainingMillis % 1000L).toFloat()
        val tickBeat = if (millisInSecond <= 240f) {
            (1f - millisInSecond / 240f).coerceIn(0f, 1f)
        } else {
            0.22f
        }

        if (!profile.isHighDifficulty) {
            // Even on standard levels, give a very gentle warning pulse in the final 10 seconds if shake is enabled
            return if (secondsLeft <= 10f && remainingMillis > 0L) {
                ((1f - secondsLeft / 10f).coerceIn(0.15f, 0.5f) * 0.35f * (0.5f + 0.5f * tickBeat))
            } else {
                0f
            }
        }

        // 1. Opening 3-second countdown lock-in tremor when high-difficulty level starts
        val openingTremor = if (elapsedMillis in 1L..3_000L) {
            val fade = 1f - (elapsedMillis / 3000f)
            0.36f * fade * profile.baseShakeScale
        } else {
            0f
        }

        // 2. Countdown phase tension escalation
        val countdownEscalation = when {
            remainingMillis <= 0L -> 0f
            secondsLeft <= 10f -> {
                val urgency = (1f - (secondsLeft / 10f)).coerceIn(0f, 1f)
                (0.62f + 0.38f * urgency) * (0.65f + 0.35f * tickBeat)
            }
            secondsLeft <= 25f || remainingFraction <= 0.35f -> {
                val urgency = (1f - (secondsLeft / 25f).coerceIn(0f, 1f))
                (0.34f + 0.28f * urgency) * (0.55f + 0.45f * tickBeat)
            }
            secondsLeft <= 45f || remainingFraction <= 0.60f -> {
                0.22f * tickBeat
            }
            else -> {
                // Subtle heartbeat pulse on every 5-second mark even early in a high-difficulty level
                if ((remainingMillis / 1000L) % 5L == 0L && millisInSecond <= 220f) 0.18f * tickBeat else 0f
            }
        } * profile.baseShakeScale

        return maxOf(openingTremor, countdownEscalation).coerceIn(0f, 1f)
    }
}

data class DailyLoginRewardTier(
    val dayIndex: Int, // 1..7
    val title: String,
    val subtitle: String,
    val baseShards: Int,
    val scaledShards: Int,
    val bonusHints: Int,
    val milestoneBonusLabel: String? = null,
    val unlockAvatarFrame: AvatarFrame? = null,
    val unlockTheme: GameTheme? = null,
    val isMilestone: Boolean = false
)

data class DailyCalendarDayCell(
    val dateKey: String, // YYYY-MM-DD
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isCheckedIn: Boolean,
    val isFuture: Boolean,
    val streakDayBadge: Int? = null
)

data class DailyLoginClaimReceipt(
    val claimedDateKey: String,
    val streakDayReached: Int,
    val tierDayIndex: Int,
    val shardsAwarded: Int,
    val hintsAwarded: Int,
    val specialUnlockTitle: String? = null
)

data class DailyLoginCalendarSummary(
    val monthTitle: String,
    val todayDateKey: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val targetClaimStreak: Int,
    val activeTierDayIndex: Int, // 1..7
    val weekCycleNumber: Int,
    val weekBonusPercent: Int,
    val isClaimedToday: Boolean,
    val checkedInCountThisMonth: Int,
    val cells: List<DailyCalendarDayCell>,
    val rewardTiers: List<DailyLoginRewardTier>
) {
    val activeTier: DailyLoginRewardTier
        get() = rewardTiers.firstOrNull { it.dayIndex == activeTierDayIndex } ?: rewardTiers.first()
}

object DailyLoginStreakManager {
    private val baseTierSpecs = listOf(
        DailyLoginRewardTier(
            dayIndex = 1,
            title = "Day 1 • Welcome Spark",
            subtitle = "Kickstart your daily spotter streak",
            baseShards = 60,
            scaledShards = 60,
            bonusHints = 1,
            isMilestone = false
        ),
        DailyLoginRewardTier(
            dayIndex = 2,
            title = "Day 2 • Keen Eye",
            subtitle = "Consecutive day momentum boost",
            baseShards = 90,
            scaledShards = 90,
            bonusHints = 1,
            isMilestone = false
        ),
        DailyLoginRewardTier(
            dayIndex = 3,
            title = "Day 3 • Midweek Prism",
            subtitle = "Double Hint Supply + Prism Shards",
            baseShards = 140,
            scaledShards = 140,
            bonusHints = 2,
            milestoneBonusLabel = "Prism Chest",
            isMilestone = true
        ),
        DailyLoginRewardTier(
            dayIndex = 4,
            title = "Day 4 • Temporal Focus",
            subtitle = "Escalating Shard & Hint cache",
            baseShards = 185,
            scaledShards = 185,
            bonusHints = 2,
            isMilestone = false
        ),
        DailyLoginRewardTier(
            dayIndex = 5,
            title = "Day 5 • Vanguard Surge",
            subtitle = "Triple Hints + Synthwave Hex Frame",
            baseShards = 250,
            scaledShards = 250,
            bonusHints = 3,
            milestoneBonusLabel = "Synthwave Hex Frame",
            unlockAvatarFrame = AvatarFrame.CYBER_HEX,
            isMilestone = true
        ),
        DailyLoginRewardTier(
            dayIndex = 6,
            title = "Day 6 • Astral Cache",
            subtitle = "High-tier Starlight Shard bounty",
            baseShards = 340,
            scaledShards = 340,
            bonusHints = 3,
            isMilestone = false
        ),
        DailyLoginRewardTier(
            dayIndex = 7,
            title = "Day 7 • Grand Zenith Vault",
            subtitle = "Mega Shard Jackpot + 5 Hints + Celestial Orbit",
            baseShards = 500,
            scaledShards = 500,
            bonusHints = 5,
            milestoneBonusLabel = "Celestial Orbit + Sakura Theme",
            unlockAvatarFrame = AvatarFrame.CELESTIAL_ORBIT,
            unlockTheme = GameTheme.SAKURA_ZENITH,
            isMilestone = true
        )
    )

    fun dateKeyForOffset(dayOffset: Int = 0): String {
        val cal = Calendar.getInstance()
        if (dayOffset != 0) {
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        return formatCalendarDateKey(cal)
    }

    fun previousDateKey(dateKey: String, daysBack: Int = 1): String {
        val cal = parseDateKeyToCalendar(dateKey)
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        return formatCalendarDateKey(cal)
    }

    fun formatCalendarDateKey(cal: Calendar): String {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun parseDateKeyToCalendar(dateKey: String): Calendar {
        val cal = Calendar.getInstance()
        val parts = dateKey.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, (m - 1).coerceIn(0, 11))
                cal.set(Calendar.DAY_OF_MONTH, d.coerceIn(1, 31))
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun buildScaledTiers(streakCount: Int): List<DailyLoginRewardTier> {
        val safeStreak = streakCount.coerceAtLeast(1)
        val weekCycleIndex = (safeStreak - 1) / 7 // 0 for Week 1, 1 for Week 2, etc.
        val multiplier = 1f + (weekCycleIndex * 0.25f).coerceAtMost(1.5f)
        return baseTierSpecs.map { spec ->
            spec.copy(
                scaledShards = (spec.baseShards * multiplier).roundToInt()
            )
        }
    }

    fun buildCalendarSummary(
        dailyLoginStreak: Int,
        bestDailyLoginStreak: Int,
        lastDailyClaimDate: String,
        dailyLoginDates: Set<String>,
        simulatedDayOffset: Int = 0
    ): DailyLoginCalendarSummary {
        val todayKey = dateKeyForOffset(simulatedDayOffset)
        val yesterdayKey = previousDateKey(todayKey, 1)
        val twoDaysAgoKey = previousDateKey(todayKey, 2)

        // Pre-seed 2 prior consecutive check-ins on first install so the calendar immediately
        // showcases an active multi-day streak trajectory ready for Day 3 check-in!
        val effectiveCheckedDates = if (dailyLoginDates.isEmpty() && lastDailyClaimDate.isBlank()) {
            setOf(twoDaysAgoKey, yesterdayKey)
        } else {
            dailyLoginDates
        }
        val effectiveLastClaim = if (lastDailyClaimDate.isBlank() && dailyLoginDates.isEmpty()) {
            yesterdayKey
        } else {
            lastDailyClaimDate
        }
        val effectiveCurrentStreak = when {
            dailyLoginStreak > 0 -> dailyLoginStreak
            dailyLoginDates.isEmpty() && lastDailyClaimDate.isBlank() -> 2
            else -> 0
        }

        val isClaimedToday = effectiveLastClaim == todayKey || todayKey in effectiveCheckedDates
        val isContinuingYesterday = effectiveLastClaim == yesterdayKey || yesterdayKey in effectiveCheckedDates

        val targetClaimStreak = when {
            isClaimedToday -> effectiveCurrentStreak.coerceAtLeast(1)
            isContinuingYesterday -> effectiveCurrentStreak + 1
            else -> 1
        }

        val activeTierDayIndex = ((targetClaimStreak - 1) % 7) + 1
        val weekCycleNumber = ((targetClaimStreak - 1) / 7) + 1
        val weekBonusPercent = ((weekCycleNumber - 1) * 25).coerceAtMost(150)
        val tiers = buildScaledTiers(targetClaimStreak)

        val todayCal = parseDateKeyToCalendar(todayKey)
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val monthTitle = monthFormat.format(todayCal.time)
        val targetMonth = todayCal.get(Calendar.MONTH)
        val targetYear = todayCal.get(Calendar.YEAR)

        // Start from the 1st of the active month, then back up to Sunday for a 6x7 (42-cell) calendar grid
        val gridCal = (todayCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
        val leadingOffset = (firstDayOfWeek - Calendar.SUNDAY).coerceAtLeast(0)
        gridCal.add(Calendar.DAY_OF_YEAR, -leadingOffset)

        val cells = ArrayList<DailyCalendarDayCell>(42)
        var checkedInThisMonth = 0

        for (i in 0 until 42) {
            val cellKey = formatCalendarDateKey(gridCal)
            val isCurrMonth = gridCal.get(Calendar.MONTH) == targetMonth &&
                gridCal.get(Calendar.YEAR) == targetYear
            val isTodayCell = cellKey == todayKey
            val isChecked = cellKey in effectiveCheckedDates
            val isFutureCell = cellKey > todayKey

            if (isCurrMonth && isChecked) {
                checkedInThisMonth++
            }

            // Compute optional streak day badge for today and upcoming milestone days in the 7-day cycle
            val daysFromToday = ((gridCal.timeInMillis - todayCal.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
            val streakBadge = when {
                isTodayCell -> targetClaimStreak
                daysFromToday in 1..6 -> {
                    val futureStreak = if (isClaimedToday) effectiveCurrentStreak + daysFromToday else targetClaimStreak + daysFromToday
                    futureStreak
                }
                else -> null
            }

            cells.add(
                DailyCalendarDayCell(
                    dateKey = cellKey,
                    dayOfMonth = gridCal.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = isCurrMonth,
                    isToday = isTodayCell,
                    isCheckedIn = isChecked,
                    isFuture = isFutureCell,
                    streakDayBadge = streakBadge
                )
            )
            gridCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return DailyLoginCalendarSummary(
            monthTitle = monthTitle,
            todayDateKey = todayKey,
            currentStreak = if (isClaimedToday) effectiveCurrentStreak.coerceAtLeast(1) else if (isContinuingYesterday) effectiveCurrentStreak else 0,
            bestStreak = maxOf(bestDailyLoginStreak, effectiveCurrentStreak),
            targetClaimStreak = targetClaimStreak,
            activeTierDayIndex = activeTierDayIndex,
            weekCycleNumber = weekCycleNumber,
            weekBonusPercent = weekBonusPercent,
            isClaimedToday = isClaimedToday,
            checkedInCountThisMonth = checkedInThisMonth,
            cells = cells,
            rewardTiers = tiers
        )
    }
}

data class AppPreferences(
    val musicEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val screenShakeEnabled: Boolean = true,
    val selectedTheme: GameTheme = GameTheme.NATURE,
    val variationCount: Int = 5,
    val timeLimitSeconds: Int = 90,
    val patternComplexity: PatternComplexity = PatternComplexity.MODERATE,
    val sideBySideMode: Boolean = true,
    val independentZoom: Boolean = true,
    val completedOfflineLevels: Set<Int> = emptySet(),
    val playerDisplayName: String = "",
    val playerAvatarData: String = AvatarPreset.CYBER_DETECTIVE.token,
    val lastAvatarPrompt: String = "",
    val totalDifferencesFound: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalWinTimeMillis: Long = 0L,
    val bestWinTimeMillis: Long = 0L,
    val recentLevelHistory: List<LevelHistoryPoint> = emptyList(),
    val challengeTimeRecords: List<ChallengeTimeRecord> = emptyList(),
    val offlineReplayRecords: List<OfflineReplayRecord> = emptyList(),
    val shardBalance: Int = 250,
    val totalShardsEarned: Int = 250,
    val unlockedThemeIds: Set<String> = GameTheme.defaultUnlockedIds,
    val unlockedAvatarFrameIds: Set<String> = AvatarFrame.defaultUnlockedIds,
    val selectedAvatarFrameId: String = AvatarFrame.DEFAULT_RING.id,
    val bonusHintsInventory: Int = 0,
    val dailyLoginStreak: Int = 0,
    val bestDailyLoginStreak: Int = 0,
    val lastDailyClaimDate: String = "",
    val dailyLoginDates: Set<String> = emptySet(),
    val simulatedDayOffset: Int = 0
) {
    val selectedAvatarFrame: AvatarFrame
        get() = AvatarFrame.fromId(selectedAvatarFrameId)

    fun isThemeUnlocked(theme: GameTheme): Boolean =
        theme.unlockCostShards == 0 || theme.id in unlockedThemeIds

    fun isAvatarFrameUnlocked(frame: AvatarFrame): Boolean =
        frame.costShards == 0 || frame.id in unlockedAvatarFrameIds

    fun currentStreakProfile(): StreakEscalationProfile =
        StreakComplexityScaler.resolve(
            winStreak = currentWinStreak,
            baseComplexity = patternComplexity,
            baseVariationCount = variationCount
        )

    fun currentDifficultyTensionProfile(): DifficultyTensionProfile {
        val streakProfile = currentStreakProfile()
        return DifficultyTensionCalculator.evaluate(
            complexity = streakProfile.effectiveComplexity,
            variationCount = streakProfile.effectiveVariationCount,
            timeLimitSeconds = timeLimitSeconds,
            activeWinStreak = currentWinStreak
        )
    }

    fun dailyLoginSummary(): DailyLoginCalendarSummary =
        DailyLoginStreakManager.buildCalendarSummary(
            dailyLoginStreak = dailyLoginStreak,
            bestDailyLoginStreak = bestDailyLoginStreak,
            lastDailyClaimDate = lastDailyClaimDate,
            dailyLoginDates = dailyLoginDates,
            simulatedDayOffset = simulatedDayOffset
        )
}





