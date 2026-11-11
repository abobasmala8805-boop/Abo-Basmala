package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import com.example.data.model.DifferenceSpot
import com.example.data.model.GameTheme
import com.example.data.model.PatternComplexity
import com.example.data.model.StreakComplexityScaler
import com.example.data.model.VariationType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private const val TAG = "AiLevelGeneratorRepo"

data class GeneratedLevelPair(
    val levelTitle: String,
    val scenePrompt: String,
    val originalBitmap: Bitmap,
    val modifiedBitmap: Bitmap,
    val spots: List<DifferenceSpot>
)

class AiLevelGeneratorRepository {

    private val levelMetadataSchema = Schema.obj(
        properties = mapOf(
            "levelTitle" to Schema.string(description = "Creative 2 to 4 word title for the level"),
            "scenePrompt" to Schema.string(description = "Vivid visual prompt for the AI image generator"),
            "spots" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "normalizedX" to Schema.double(description = "X coordinate between 0.15 and 0.85"),
                        "normalizedY" to Schema.double(description = "Y coordinate between 0.15 and 0.85"),
                        "label" to Schema.string(description = "Short 2-3 word name of the subtle visual variation"),
                        "hint" to Schema.string(description = "Helpful clue telling the player what subtle difference to look for"),
                        "variationType" to Schema.enumeration(
                            values = listOf("COLOR_SHIFT", "PATCH_MIRROR", "LUMINANCE_PULSE", "DETAIL_CLONE", "PRISM_SHIFT"),
                            description = "Visual variation style"
                        ),
                        "accentHex" to Schema.string(description = "Hex color code like #38BDF8 or #F59E0B")
                    )
                ),
                description = "List of non-overlapping difference spots"
            )
        )
    )

    // Lazy initialization per Firebase AI Logic guidelines
    private val metadataModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = levelMetadataSchema
            }
        )
    }

    private val textHelperModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateCustomAvatarDataUri(
        userPrompt: String,
        presetStyle: com.example.data.model.AvatarPreset
    ): Pair<Bitmap, String> {
        val cleanPrompt = userPrompt.trim().ifBlank { presetStyle.aiPromptSuggestion }
        val fullAvatarPrompt = buildString {
            append("Stylized game profile character avatar portrait: ")
            append(cleanPrompt)
            append(". Centered head-and-shoulders character icon, vibrant glowing neon and jewel tones, clean dark background, high contrast digital art, no text or watermarks.")
        }

        val generatedBitmap = withTimeout(60_000L) {
            val response = imageModel.generateContent(fullAvatarPrompt)
            response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image
        } ?: throw IllegalStateException("No avatar image was returned by gemini-nano-banana-2.1.")

        val encodedDataUri = withContext(Dispatchers.Default) {
            val scaled = Bitmap.createScaledBitmap(generatedBitmap, 112, 112, true)
            val baos = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 74, baos)
            val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        }

        return generatedBitmap to encodedDataUri
    }

    suspend fun enhanceCustomPrompt(theme: GameTheme, complexity: PatternComplexity, rawIdea: String): String {
        val prompt = """
            Create a vivid, richly detailed 1-sentence image generation prompt for a Spot the Difference game.
            Theme: ${theme.displayName} (${theme.promptStyle}).
            Pattern Complexity: ${complexity.displayName} (${complexity.promptDetailInstruction}).
            User's concept: ${rawIdea.ifBlank { theme.subtitle }}.
            Output only the enhanced visual description without quotes.
        """.trimIndent()

        val response = withTimeout(45_000L) {
            textHelperModel.generateContent(prompt)
        }
        return response.text?.trim()?.take(350)?.ifBlank { null }
            ?: "${theme.promptStyle}, ${complexity.promptDetailInstruction}"
    }

    suspend fun generateAiLevel(
        theme: GameTheme,
        variationCount: Int,
        complexity: PatternComplexity,
        customTitle: String? = null,
        customScenePrompt: String? = null,
        predefinedSpots: List<DifferenceSpot>? = null,
        winStreak: Int = 0
    ): GeneratedLevelPair {
        val count = variationCount.coerceIn(3, 10)
        val streakProfile = StreakComplexityScaler.resolve(
            winStreak = winStreak,
            baseComplexity = complexity,
            baseVariationCount = count
        )

        // Step 1: Obtain or generate level title, visual prompt, and non-overlapping spots via Gemini 3.8 Flash
        val (resolvedTitle, resolvedPrompt, resolvedSpots) = if (
            !customTitle.isNullOrBlank() &&
            !customScenePrompt.isNullOrBlank() &&
            !predefinedSpots.isNullOrEmpty()
        ) {
            Triple(customTitle, customScenePrompt, predefinedSpots.take(count))
        } else {
            generateMetadataAndSpots(
                theme = theme,
                variationCount = count,
                complexity = complexity,
                customTitle = customTitle,
                customScenePrompt = customScenePrompt,
                winStreak = winStreak
            )
        }

        // Step 2: Generate the base scene Bitmap using Firebase AI Logic gemini-nano-banana-2.1
        // Strict 60s timeout per Firebase AI Logic image generation guidelines
        val fullImagePrompt = buildString {
            append(resolvedPrompt)
            append(". Style: ")
            append(theme.promptStyle)
            append(", ")
            append(complexity.promptDetailInstruction)
            if (streakProfile.aiPromptModifier.isNotBlank()) {
                append(". ")
                append(streakProfile.aiPromptModifier)
            }
            append(". Full-frame richly detailed illustration, crisp edges, vibrant colors, no text or watermarks.")
        }

        val generatedBitmap = withTimeout(60_000L) {
            val response = imageModel.generateContent(fullImagePrompt)
            response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image
        } ?: throw IllegalStateException("No image was returned by gemini-nano-banana-2.1.")

        // Step 3: Synthesize the side-by-side comparison Bitmap (Image B) by applying subtle,
        // complexity-calibrated & streak-scaled visual variations at the exact (normalizedX, normalizedY) coordinates
        val modifiedBitmap = withContext(Dispatchers.Default) {
            applySubtleVariationsToBitmap(
                source = generatedBitmap,
                spots = resolvedSpots,
                complexity = complexity,
                winStreak = winStreak
            )
        }

        return GeneratedLevelPair(
            levelTitle = resolvedTitle,
            scenePrompt = resolvedPrompt,
            originalBitmap = generatedBitmap,
            modifiedBitmap = modifiedBitmap,
            spots = resolvedSpots
        )
    }

    private suspend fun generateMetadataAndSpots(
        theme: GameTheme,
        variationCount: Int,
        complexity: PatternComplexity,
        customTitle: String?,
        customScenePrompt: String?,
        winStreak: Int = 0
    ): Triple<String, String, List<DifferenceSpot>> {
        val streakProfile = StreakComplexityScaler.resolve(
            winStreak = winStreak,
            baseComplexity = complexity,
            baseVariationCount = variationCount
        )
        val effectiveRadius = (complexity.defaultRadius * streakProfile.anomalyRadiusScale).coerceIn(0.048f, 0.10f)

        try {
            val metadataPrompt = """
                Design a Spot the Difference puzzle level.
                Theme: ${theme.displayName} (${theme.promptStyle})
                Pattern Complexity: ${complexity.displayName} (${complexity.promptDetailInstruction})
                Consecutive Win Streak: $winStreak (${streakProfile.tierBadge})
                ${if (streakProfile.aiPromptModifier.isNotBlank()) "Streak Complexity Instruction: ${streakProfile.aiPromptModifier}" else ""}
                Required number of variations: $variationCount
                ${if (!customTitle.isNullOrBlank()) "Preferred title: $customTitle" else ""}
                ${if (!customScenePrompt.isNullOrBlank()) "Scene description: $customScenePrompt" else ""}
                Ensure the $variationCount difference spots are well spaced out across the canvas (normalizedX and normalizedY between 0.16 and 0.84, at least 0.18 apart from each other).
            """.trimIndent()

            val response = withTimeout(45_000L) {
                metadataModel.generateContent(metadataPrompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty metadata JSON")
            val root = JSONObject(jsonText)
            val title = customTitle?.takeIf { it.isNotBlank() }
                ?: root.optString("levelTitle").takeIf { it.isNotBlank() }
                ?: "${theme.displayName} Challenge"
            val scenePrompt = customScenePrompt?.takeIf { it.isNotBlank() }
                ?: root.optString("scenePrompt").takeIf { it.isNotBlank() }
                ?: "${theme.promptStyle}, ${complexity.promptDetailInstruction}"

            val spotsArray = root.optJSONArray("spots")
            val parsedSpots = mutableListOf<DifferenceSpot>()
            if (spotsArray != null) {
                for (i in 0 until min(spotsArray.length(), variationCount)) {
                    val obj = spotsArray.getJSONObject(i)
                    val rawX = obj.optDouble("normalizedX", 0.25 + (i % 3) * 0.25).toFloat().coerceIn(0.15f, 0.85f)
                    val rawY = obj.optDouble("normalizedY", 0.25 + (i / 3) * 0.25).toFloat().coerceIn(0.15f, 0.85f)
                    val label = obj.optString("label", "Subtle Anomaly #${i + 1}")
                    val hint = obj.optString("hint", "Look closely around $label")
                    val type = VariationType.fromId(obj.optString("variationType", "COLOR_SHIFT"))
                    val hex = obj.optString("accentHex", "#38BDF8").let {
                        if (it.startsWith("#") && (it.length == 7 || it.length == 9)) it else "#38BDF8"
                    }
                    parsedSpots.add(
                        DifferenceSpot(
                            index = i,
                            normalizedX = rawX,
                            normalizedY = rawY,
                            normalizedRadius = effectiveRadius,
                            label = label,
                            hint = hint,
                            variationType = type,
                            accentHex = hex
                        )
                    )
                }
            }

            val finalSpots = ensureWellSpacedSpots(parsedSpots, variationCount, complexity, effectiveRadius)
            return Triple(title, scenePrompt, finalSpots)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to deterministic spot placement while preserving AI image generation", e)
            val fallbackTitle = customTitle?.takeIf { it.isNotBlank() } ?: "${theme.displayName} Odyssey"
            val fallbackPrompt = customScenePrompt?.takeIf { it.isNotBlank() }
                ?: "${theme.promptStyle}, ${complexity.promptDetailInstruction}"
            val spots = ensureWellSpacedSpots(emptyList(), variationCount, complexity, effectiveRadius)
            return Triple(fallbackTitle, fallbackPrompt, spots)
        }
    }

    fun ensureWellSpacedSpots(
        initial: List<DifferenceSpot>,
        targetCount: Int,
        complexity: PatternComplexity,
        radiusOverride: Float? = null
    ): List<DifferenceSpot> {
        val resolvedRadius = radiusOverride ?: complexity.defaultRadius
        val result = mutableListOf<DifferenceSpot>()
        val candidateGrid = listOf(
            0.22f to 0.22f, 0.50f to 0.20f, 0.78f to 0.24f,
            0.26f to 0.50f, 0.52f to 0.48f, 0.76f to 0.52f,
            0.22f to 0.78f, 0.48f to 0.76f, 0.78f to 0.78f,
            0.36f to 0.34f, 0.64f to 0.36f, 0.36f to 0.64f
        )
        val defaultLabels = listOf(
            "Chromatic Shift", "Symmetry Flip", "Radiant Detail", "Vanished Motif",
            "Prismatic Edge", "Shifted Ornament", "Glowing Core", "Inverted Facet",
            "Harmonic Tint", "Shadow Anomaly"
        )
        val defaultHexes = listOf(
            "#38BDF8", "#F59E0B", "#EC4899", "#10B981", "#A855F7",
            "#FB7185", "#22D3EE", "#FBBF24", "#34D399", "#818CF8"
        )
        val types = VariationType.entries

        for (spot in initial) {
            if (result.size >= targetCount) break
            val tooClose = result.any { existing ->
                hypot(existing.normalizedX - spot.normalizedX, existing.normalizedY - spot.normalizedY) < 0.14f
            }
            if (!tooClose) {
                result.add(spot.copy(index = result.size, normalizedRadius = resolvedRadius))
            }
        }

        var gridIdx = 0
        while (result.size < targetCount && gridIdx < candidateGrid.size) {
            val (gx, gy) = candidateGrid[gridIdx]
            gridIdx++
            val tooClose = result.any { existing ->
                hypot(existing.normalizedX - gx, existing.normalizedY - gy) < 0.14f
            }
            if (!tooClose) {
                val idx = result.size
                val label = defaultLabels[idx % defaultLabels.size]
                val vType = types[idx % types.size]
                result.add(
                    DifferenceSpot(
                        index = idx,
                        normalizedX = gx,
                        normalizedY = gy,
                        normalizedRadius = resolvedRadius,
                        label = label,
                        hint = "Spot the ${vType.displayName.lowercase()} in this region.",
                        variationType = vType,
                        accentHex = defaultHexes[idx % defaultHexes.size]
                    )
                )
            }
        }
        return result
    }

    fun applySubtleVariationsToBitmap(
        source: Bitmap,
        spots: List<DifferenceSpot>,
        complexity: PatternComplexity,
        winStreak: Int = 0
    ): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val minDim = min(width, height).toFloat()

        val streakProfile = StreakComplexityScaler.resolve(
            winStreak = winStreak,
            baseComplexity = complexity,
            baseVariationCount = spots.size.coerceIn(3, 10)
        )

        // Intensity scales subtly with both PatternComplexity and consecutive Win Streak so higher streaks require sharper observation
        val baseIntensity = when (complexity) {
            PatternComplexity.SUBTLE -> 1.0f
            PatternComplexity.MODERATE -> 0.82f
            PatternComplexity.INTRICATE -> 0.68f
        }
        val intensityFactor = (baseIntensity * streakProfile.anomalySubtletyScale).coerceIn(0.55f, 1.0f)

        for (spot in spots) {
            val cx = (spot.normalizedX * width).coerceIn(16f, width - 16f)
            val cy = (spot.normalizedY * height).coerceIn(16f, height - 16f)
            val radiusPx = (spot.normalizedRadius * minDim * 0.72f).coerceIn(16f, minDim * 0.14f)

            val left = (cx - radiusPx).toInt().coerceIn(0, width - 2)
            val top = (cy - radiusPx).toInt().coerceIn(0, height - 2)
            val patchW = (radiusPx * 2).toInt().coerceIn(2, width - left)
            val patchH = (radiusPx * 2).toInt().coerceIn(2, height - top)

            canvas.save()
            val clipPath = Path().apply {
                addCircle(cx, cy, radiusPx, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            when (spot.variationType) {
                VariationType.COLOR_SHIFT -> {
                    val patch = Bitmap.createBitmap(source, left, top, patchW, patchH)
                    // Rotate RGB channels subtly inside the soft circular region
                    val shiftMatrix = ColorMatrix(
                        floatArrayOf(
                            0.35f, 0.55f * intensityFactor, 0.10f, 0f, 18f * intensityFactor,
                            0.15f, 0.35f, 0.50f * intensityFactor, 0f, -12f * intensityFactor,
                            0.50f * intensityFactor, 0.15f, 0.35f, 0f, 22f * intensityFactor,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        colorFilter = ColorMatrixColorFilter(shiftMatrix)
                    }
                    canvas.drawBitmap(patch, left.toFloat(), top.toFloat(), paint)
                }

                VariationType.PATCH_MIRROR -> {
                    val patch = Bitmap.createBitmap(source, left, top, patchW, patchH)
                    val matrix = Matrix().apply {
                        postScale(-1f, 1f, patchW / 2f, patchH / 2f)
                        postRotate(90f, patchW / 2f, patchH / 2f)
                        postTranslate(left.toFloat(), top.toFloat())
                    }
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(patch, matrix, paint)
                }

                VariationType.LUMINANCE_PULSE -> {
                    val accentColor = try {
                        Color.parseColor(spot.accentHex)
                    } catch (e: Exception) {
                        Color.rgb(56, 189, 248)
                    }
                    val alpha = (155 * intensityFactor).toInt().coerceIn(85, 190)
                    val centerColor = Color.argb(
                        alpha,
                        Color.red(accentColor),
                        Color.green(accentColor),
                        Color.blue(accentColor)
                    )
                    val edgeColor = Color.argb(
                        0,
                        Color.red(accentColor),
                        Color.green(accentColor),
                        Color.blue(accentColor)
                    )
                    val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx,
                            cy,
                            radiusPx,
                            intArrayOf(centerColor, centerColor, edgeColor),
                            floatArrayOf(0f, 0.45f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, radiusPx, radialPaint)
                    // Subtle inner star/motif
                    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((190 * intensityFactor).toInt(), 255, 255, 255)
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, radiusPx * 0.22f, corePaint)
                }

                VariationType.DETAIL_CLONE -> {
                    // Clone an adjacent background region over this spot to make an object "vanish"
                    val offsetX = if (left + patchW * 2 < width) left + patchW else (left - patchW).coerceAtLeast(0)
                    val cloneW = patchW.coerceAtMost(width - offsetX)
                    if (cloneW > 2 && patchH > 2) {
                        val neighborPatch = Bitmap.createBitmap(source, offsetX, top, cloneW, patchH)
                        val destRect = RectF(left.toFloat(), top.toFloat(), (left + patchW).toFloat(), (top + patchH).toFloat())
                        canvas.drawBitmap(neighborPatch, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG))
                    }
                }

                VariationType.PRISM_SHIFT -> {
                    val accentColor = try {
                        Color.parseColor(spot.accentHex)
                    } catch (e: Exception) {
                        Color.rgb(245, 158, 11)
                    }
                    val polyPath = Path()
                    val sides = 6
                    val innerR = radiusPx * 0.62f
                    for (i in 0 until sides) {
                        val angle = (i * 2.0 * Math.PI / sides) + (spot.index * 0.4)
                        val px = cx + (innerR * cos(angle)).toFloat()
                        val py = cy + (innerR * sin(angle)).toFloat()
                        if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                    }
                    polyPath.close()
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(
                            (145 * intensityFactor).toInt().coerceIn(80, 175),
                            Color.red(accentColor),
                            Color.green(accentColor),
                            Color.blue(accentColor)
                        )
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(polyPath, fillPaint)
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((170 * intensityFactor).toInt(), 255, 255, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = (radiusPx * 0.09f).coerceAtLeast(2f)
                    }
                    canvas.drawPath(polyPath, strokePaint)
                }
            }
            canvas.restore()
        }

        return output
    }
}
