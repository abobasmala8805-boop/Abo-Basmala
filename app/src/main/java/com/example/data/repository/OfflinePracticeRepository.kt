package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.R
import com.example.data.model.DifferenceSpot
import com.example.data.model.GameTheme
import com.example.data.model.OfflinePracticeLevel
import com.example.data.model.PatternComplexity
import com.example.data.model.VariationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class OfflinePracticeRepository(private val context: Context) {

    private val natureTitles = listOf(
        "Whispering Canopy", "Emerald Lagoon", "Bioluminescent Grove", "Lotus Sanctuary", "Misty Waterfall",
        "Firefly Meadow", "Crystal Fern Valley", "Moonlit Mangrove", "Orchid Conservatory", "Amber Bamboo Trail",
        "Starlight Lichen Cave", "Golden Moss Bridge", "Sapphire Creek", "Dewdrop Terraces", "Sunlit Sequoia",
        "Coral Reef Shallows", "Alpine Wildflower Pass", "Jade Bonsai Courtyard", "Twilight Reed Marsh", "Aurora Pine Ridge"
    )

    private val spaceTitles = listOf(
        "Nebula Observatory", "Ringed Exoplanet", "Orbital Dock Alpha", "Pulsar Beacon", "Lunar Glass Dome",
        "Binary Star Forge", "Comet Tail Harvester", "Deep Space Relay", "Supernova Remnant", "Titan Methane Sea",
        "Celestial Astrolabe", "Lagrange Point Station", "Ion Sails Armada", "Cryo Crater Base", "Quasar Horizon",
        "Magnetar Shield Array", "Stardust Refinery", "Eclipse Research Lab", "Dyson Ring Sector", "Galactic Core Gate"
    )

    private val geometricTitles = listOf(
        "Bauhaus Pavilion", "Isometric Citadel", "Tessellation Plaza", "Prism Courtyard", "Golden Ratio Spiral",
        "Origami Monolith", "Hexagonal Atrium", "Polyhedron Gallery", "Mobius Promenade", "Vector Sanctuary",
        "Cubic Zen Garden", "Cobalt Archways", "Terracotta Steps", "Symmetry Obelisk", "Platonic Solids Hall",
        "Fibonacci Terrace", "Minimalist Sundial", "Chromatic Tangram", "Faceted Solarium", "Concentric Amphitheater"
    )

    private val cyberpunkTitles = listOf(
        "Neon Rooftop Shrine", "Synthwave Overpass", "Hologram Night Market", "Cybernetic Pagoda", "Laser Rain Alley",
        "Quantum Server Vault", "Chromatic Skybridge", "Retro Arcade Nexus", "Drone Delivery Pier", "Electric Koi Pond",
        "Plasma Tower Vista", "Glitch Lotus Plaza", "Magenta Monorail", "Subwoofer Zen Deck", "Circuitry Botanical",
        "Cyber Samurai Dojo", "Vaporwave Sunset Bay", "Optical Fiber Canyon", "Neon Ramen Harbor", "Mainframe Spire"
    )

    private val ancientTitles = listOf(
        "Brass Astrolabe Hall", "Clockwork Sun Temple", "Celestial Gear Vault", "Runic Mosaic Chamber", "Alabaster Sundial",
        "Pendulum Sanctuary", "Gilded Orrery Dome", "Steampunk Library", "Turquoise Zodiac Arch", "Chrono Belfry",
        "Obsidian Water Clock", "Byzantine Star Vault", "Mechanical Sphinx", "Amber Hourglass Keep", "Aether Compass Room",
        "Copper Planetarium", "Temple of Whispers", "Emerald Gyroscope", "Sunken Automaton", "Eternal Solarium"
    )

    val allLevels: List<OfflinePracticeLevel> by lazy {
        (1..100).map { num -> buildOfflineLevelSpec(num) }
    }

    fun getLevel(levelNumber: Int): OfflinePracticeLevel {
        return allLevels[(levelNumber - 1).coerceIn(0, 99)]
    }

    private fun buildOfflineLevelSpec(levelNumber: Int): OfflinePracticeLevel {
        val theme = when (levelNumber) {
            in 1..20 -> GameTheme.NATURE
            in 21..40 -> GameTheme.SPACE
            in 41..60 -> GameTheme.GEOMETRIC
            in 61..80 -> GameTheme.CYBERPUNK
            else -> GameTheme.ANCIENT
        }
        val indexInChapter = (levelNumber - 1) % 20
        val baseTitle = when (theme) {
            GameTheme.NATURE -> natureTitles[indexInChapter]
            GameTheme.SPACE -> spaceTitles[indexInChapter]
            GameTheme.GEOMETRIC -> geometricTitles[indexInChapter]
            GameTheme.CYBERPUNK -> cyberpunkTitles[indexInChapter]
            GameTheme.ANCIENT -> ancientTitles[indexInChapter]
            GameTheme.SAKURA_ZENITH -> natureTitles[indexInChapter]
            GameTheme.ATLANTIS_CORAL -> spaceTitles[indexInChapter]
            GameTheme.SOLARPUNK_OASIS -> geometricTitles[indexInChapter]
        }

        val variationCount = when {
            indexInChapter < 4 -> 3
            indexInChapter < 8 -> 4
            indexInChapter < 12 -> 5
            indexInChapter < 16 -> 6
            indexInChapter < 18 -> 7
            else -> 8
        }

        val complexity = when {
            indexInChapter < 6 -> PatternComplexity.SUBTLE
            indexInChapter < 14 -> PatternComplexity.MODERATE
            else -> PatternComplexity.INTRICATE
        }

        val timeLimitSeconds = when (complexity) {
            PatternComplexity.SUBTLE -> 120
            PatternComplexity.MODERATE -> 95
            PatternComplexity.INTRICATE -> 75
        }

        val spots = generateDeterministicSpots(
            levelNumber = levelNumber,
            count = variationCount,
            complexity = complexity
        )

        return OfflinePracticeLevel(
            levelNumber = levelNumber,
            title = "Practice #$levelNumber: $baseTitle",
            subtitle = "${theme.displayName} • $variationCount Differences • ${complexity.displayName}",
            theme = theme,
            variationCount = variationCount,
            timeLimitSeconds = timeLimitSeconds,
            complexity = complexity,
            spots = spots
        )
    }

    private fun generateDeterministicSpots(
        levelNumber: Int,
        count: Int,
        complexity: PatternComplexity
    ): List<DifferenceSpot> {
        val rng = Random(levelNumber * 9973L + 42L)
        val anchorCandidates = listOf(
            0.22f to 0.22f, 0.50f to 0.20f, 0.78f to 0.22f,
            0.22f to 0.50f, 0.50f to 0.50f, 0.78f to 0.50f,
            0.22f to 0.78f, 0.50f to 0.78f, 0.78f to 0.78f,
            0.35f to 0.35f, 0.65f to 0.35f, 0.35f to 0.65f, 0.65f to 0.65f
        ).shuffled(rng)

        val labels = listOf(
            "Lantern Core", "Star Emblem", "Crystal Facet", "Neon Orb",
            "Brass Gear", "Crown Motif", "Floating Spire", "Prism Ring",
            "Radiant Blossom", "Celestial Dial"
        )
        val colors = listOf(
            "#38BDF8", "#F59E0B", "#EC4899", "#10B981",
            "#A855F7", "#FB7185", "#22D3EE", "#FBBF24"
        )
        val types = VariationType.entries

        val chosen = mutableListOf<DifferenceSpot>()
        for ((ax, ay) in anchorCandidates) {
            if (chosen.size >= count) break
            val jitterX = (ax + (rng.nextFloat() - 0.5f) * 0.06f).coerceIn(0.16f, 0.84f)
            val jitterY = (ay + (rng.nextFloat() - 0.5f) * 0.06f).coerceIn(0.16f, 0.84f)
            val tooClose = chosen.any { hypot(it.normalizedX - jitterX, it.normalizedY - jitterY) < 0.15f }
            if (!tooClose) {
                val idx = chosen.size
                val vType = types[(levelNumber + idx) % types.size]
                val label = labels[(levelNumber + idx * 3) % labels.size]
                chosen.add(
                    DifferenceSpot(
                        index = idx,
                        normalizedX = jitterX,
                        normalizedY = jitterY,
                        normalizedRadius = complexity.defaultRadius,
                        label = label,
                        hint = "Inspect the $label (${vType.displayName.lowercase()})",
                        variationType = vType,
                        accentHex = colors[(levelNumber + idx * 2) % colors.size]
                    )
                )
            }
        }
        return chosen
    }

    /**
     * Generates the pre-loaded offline level image pair completely on-device without internet.
     * Combines the bundled artwork drawable with rich thematic vector motifs and exact
     * spot-specific visual differences so every difference spot is crisp, visible, and accurate.
     */
    suspend fun generateOfflineLevelPair(level: OfflinePracticeLevel): GeneratedLevelPair =
        withContext(Dispatchers.Default) {
            val sizePx = 800
            val original = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val modified = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

            renderOfflineScene(
                target = original,
                level = level,
                isModifiedVariant = false
            )
            renderOfflineScene(
                target = modified,
                level = level,
                isModifiedVariant = true
            )

            GeneratedLevelPair(
                levelTitle = level.title,
                scenePrompt = level.subtitle,
                originalBitmap = original,
                modifiedBitmap = modified,
                spots = level.spots
            )
        }

    private fun renderOfflineScene(
        target: Bitmap,
        level: OfflinePracticeLevel,
        isModifiedVariant: Boolean
    ) {
        val canvas = Canvas(target)
        val w = target.width.toFloat()
        val h = target.height.toFloat()
        val rng = Random(level.levelNumber * 13337L + 7L)

        // 1. Draw pre-loaded bundled base illustration crop tinted to the level's theme
        val baseDrawableBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_hero_banner_1794391386542)
        } catch (e: Exception) {
            null
        }

        if (baseDrawableBitmap != null) {
            val srcW = baseDrawableBitmap.width
            val srcH = baseDrawableBitmap.height
            val cropSize = minOf(srcW, srcH)
            val maxOffsetX = (srcW - cropSize).coerceAtLeast(1)
            val offsetX = ((level.levelNumber * 37) % maxOffsetX).coerceIn(0, maxOffsetX)
            val cropped = Bitmap.createBitmap(baseDrawableBitmap, offsetX, 0, cropSize, cropSize)
            val destRect = RectF(0f, 0f, w, h)
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 185
            }
            canvas.drawBitmap(cropped, null, destRect, basePaint)
        }

        // 2. Theme-specific atmospheric gradient overlay
        val (bgTop, bgBottom, accentPrimary, accentSecondary) = when (level.theme) {
            GameTheme.NATURE -> listOf(
                Color.argb(145, 6, 44, 34),
                Color.argb(165, 11, 22, 44),
                Color.rgb(16, 185, 129),
                Color.rgb(56, 189, 248)
            )
            GameTheme.SPACE -> listOf(
                Color.argb(150, 15, 12, 48),
                Color.argb(165, 30, 10, 42),
                Color.rgb(129, 140, 248),
                Color.rgb(244, 114, 182)
            )
            GameTheme.GEOMETRIC -> listOf(
                Color.argb(155, 23, 30, 52),
                Color.argb(165, 42, 26, 20),
                Color.rgb(245, 158, 11),
                Color.rgb(56, 189, 248)
            )
            GameTheme.CYBERPUNK -> listOf(
                Color.argb(150, 28, 10, 46),
                Color.argb(165, 8, 32, 48),
                Color.rgb(236, 72, 153),
                Color.rgb(34, 211, 238)
            )
            GameTheme.ANCIENT -> listOf(
                Color.argb(150, 44, 26, 12),
                Color.argb(165, 12, 36, 38),
                Color.rgb(217, 119, 6),
                Color.rgb(20, 184, 166)
            )
            GameTheme.SAKURA_ZENITH -> listOf(
                Color.argb(150, 46, 14, 28),
                Color.argb(165, 34, 18, 38),
                Color.rgb(251, 113, 133),
                Color.rgb(251, 191, 36)
            )
            GameTheme.ATLANTIS_CORAL -> listOf(
                Color.argb(150, 8, 36, 52),
                Color.argb(165, 24, 14, 48),
                Color.rgb(6, 182, 212),
                Color.rgb(168, 85, 247)
            )
            GameTheme.SOLARPUNK_OASIS -> listOf(
                Color.argb(150, 10, 42, 30),
                Color.argb(165, 38, 28, 12),
                Color.rgb(52, 211, 153),
                Color.rgb(245, 158, 11)
            )
        }

        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, bgTop, bgBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, gradPaint)

        // 3. Draw background decorative pattern grid based on complexity
        val decorCount = when (level.complexity) {
            PatternComplexity.SUBTLE -> 16
            PatternComplexity.MODERATE -> 28
            PatternComplexity.INTRICATE -> 42
        }
        val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until decorCount) {
            val dx = rng.nextFloat() * w
            val dy = rng.nextFloat() * h
            val dr = 8f + rng.nextFloat() * 22f
            decorPaint.color = if (i % 2 == 0) accentPrimary else accentSecondary
            decorPaint.alpha = 55 + rng.nextInt(55)
            decorPaint.style = if (i % 3 == 0) Paint.Style.STROKE else Paint.Style.FILL
            decorPaint.strokeWidth = 2.5f
            canvas.drawCircle(dx, dy, dr, decorPaint)
        }

        // 4. Draw rich focal ornaments at every DifferenceSpot coordinate
        // In the original image, each ornament is drawn in its canonical form;
        // in the modified image, each ornament has a clear, unmistakable visual variation
        // (color shift, rotated geometry, extra/missing inner core, or scale/shape change)
        for (spot in level.spots) {
            val cx = spot.normalizedX * w
            val cy = spot.normalizedY * h
            val r = spot.normalizedRadius * w * 0.78f

            val baseColor = try {
                Color.parseColor(spot.accentHex)
            } catch (e: Exception) {
                accentPrimary
            }
            val altColor = when (spot.index % 4) {
                0 -> Color.rgb(251, 113, 133) // Coral
                1 -> Color.rgb(52, 211, 153)  // Mint
                2 -> Color.rgb(250, 204, 21)  // Gold
                else -> Color.rgb(167, 139, 250) // Lavender
            }

            val activeColor = if (isModifiedVariant && spot.variationType == VariationType.COLOR_SHIFT) {
                altColor
            } else {
                baseColor
            }

            // Outer ambient glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    r * 1.15f,
                    intArrayOf(
                        Color.argb(175, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)),
                        Color.argb(0, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
                    ),
                    floatArrayOf(0.2f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, r * 1.15f, glowPaint)

            when (spot.variationType) {
                VariationType.COLOR_SHIFT -> {
                    // Rich multi-ring medallion whose color shifts between Original and Modified
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = activeColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, r * 0.62f, fillPaint)
                    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                    }
                    canvas.drawCircle(cx, cy, r * 0.42f, ringPaint)
                }

                VariationType.PATCH_MIRROR -> {
                    // Crescent / Asymmetric star that flips orientation in Modified
                    val angleOffset = if (isModifiedVariant) Math.PI else 0.0
                    val polyPath = Path()
                    val points = 5
                    for (i in 0 until points * 2) {
                        val rad = if (i % 2 == 0) r * 0.68f else r * 0.30f
                        val a = angleOffset + i * Math.PI / points
                        val px = cx + (rad * cos(a)).toFloat()
                        val py = cy + (rad * sin(a)).toFloat()
                        if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                    }
                    polyPath.close()
                    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = activeColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(polyPath, starPaint)
                }

                VariationType.LUMINANCE_PULSE -> {
                    // Concentric lantern: in Modified, has a bright golden diamond core
                    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = activeColor
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                    }
                    canvas.drawCircle(cx, cy, r * 0.60f, outerPaint)
                    if (!isModifiedVariant) {
                        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(cx, cy, r * 0.32f, innerPaint)
                    } else {
                        val diamond = Path().apply {
                            moveTo(cx, cy - r * 0.48f)
                            lineTo(cx + r * 0.48f, cy)
                            lineTo(cx, cy + r * 0.48f)
                            lineTo(cx - r * 0.48f, cy)
                            close()
                        }
                        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = altColor
                            style = Paint.Style.FILL
                        }
                        canvas.drawPath(diamond, diamondPaint)
                    }
                }

                VariationType.DETAIL_CLONE -> {
                    // Vanishing object: present in Original, absent in Modified (leaving only subtle ring)
                    if (!isModifiedVariant) {
                        val hexPath = Path()
                        for (i in 0 until 6) {
                            val a = i * Math.PI / 3.0
                            val px = cx + (r * 0.58f * cos(a)).toFloat()
                            val py = cy + (r * 0.58f * sin(a)).toFloat()
                            if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                        }
                        hexPath.close()
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = activeColor
                            style = Paint.Style.FILL
                        }
                        canvas.drawPath(hexPath, p)
                        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(cx, cy, r * 0.2f, dotPaint)
                    }
                }

                VariationType.PRISM_SHIFT -> {
                    // Geometric triangle vs hexagon shift
                    val sides = if (isModifiedVariant) 3 else 6
                    val poly = Path()
                    for (i in 0 until sides) {
                        val a = (i * 2.0 * Math.PI / sides) - Math.PI / 2.0
                        val px = cx + (r * 0.62f * cos(a)).toFloat()
                        val py = cy + (r * 0.62f * sin(a)).toFloat()
                        if (i == 0) poly.moveTo(px, py) else poly.lineTo(px, py)
                    }
                    poly.close()
                    val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isModifiedVariant) altColor else activeColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(poly, fillP)
                }
            }
        }
    }
}
