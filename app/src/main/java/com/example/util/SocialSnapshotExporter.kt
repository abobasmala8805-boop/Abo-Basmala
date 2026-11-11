package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.AvatarPreset
import com.example.data.model.DifferenceSpot
import com.example.data.model.GameTheme
import com.example.data.model.PatternComplexity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class SocialSnapshotFormat(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val widthPx: Int,
    val heightPx: Int
) {
    SQUARE_POST(
        id = "SQUARE_POST",
        displayName = "Square Post (1:1)",
        subtitle = "1080×1080 • Feed, X, WhatsApp",
        widthPx = 1080,
        heightPx = 1080
    ),
    VERTICAL_STORY(
        id = "VERTICAL_STORY",
        displayName = "Vertical Card (4:5)",
        subtitle = "1080×1350 • Stories & Status",
        widthPx = 1080,
        heightPx = 1350
    )
}

data class SocialSnapshotSpec(
    val playerName: String,
    val playerAvatarData: String,
    val levelTitle: String,
    val theme: GameTheme,
    val complexity: PatternComplexity,
    val variationCount: Int,
    val spotsFoundCount: Int,
    val finalTimeMillis: Long,
    val score: Int,
    val currentWinStreak: Int,
    val bestWinStreak: Int,
    val streakTierBadge: String,
    val challengeCode: String = "",
    val isVictory: Boolean = true,
    val originalBitmap: Bitmap? = null,
    val modifiedBitmap: Bitmap? = null,
    val spots: List<DifferenceSpot> = emptyList(),
    val foundSpotIndices: Set<Int> = emptySet()
)

object SocialSnapshotExporter {

    fun renderSnapshotBitmap(
        spec: SocialSnapshotSpec,
        format: SocialSnapshotFormat = SocialSnapshotFormat.SQUARE_POST,
        showSpottedRings: Boolean = true
    ): Bitmap {
        val w = format.widthPx
        val h = format.heightPx
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val primaryColor = (spec.theme.primaryHex or 0xFF000000L).toInt()
        val secondaryColor = (spec.theme.secondaryHex or 0xFF000000L).toInt()
        val emeraldInt = Color.rgb(16, 185, 129)
        val amberInt = Color.rgb(245, 158, 11)
        val coralInt = Color.rgb(244, 63, 94)

        // 1. Deep multi-stop background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                intArrayOf(
                    Color.rgb(6, 11, 22),
                    Color.rgb(13, 21, 38),
                    Color.rgb(8, 13, 26)
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // 2. Ambient radial theme glows
        val topGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.18f,
                h * 0.14f,
                w * 0.55f,
                intArrayOf(withAlpha(primaryColor, 72), withAlpha(primaryColor, 18), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), topGlowPaint)

        val bottomGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.84f,
                h * 0.82f,
                w * 0.58f,
                intArrayOf(withAlpha(secondaryColor, 68), withAlpha(amberInt, 20), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bottomGlowPaint)

        // Subtle geometric tech grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(16, 148, 163, 184)
            strokeWidth = 1.5f
        }
        val step = 90
        for (x in 0..w step step) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), h.toFloat(), gridPaint)
        }
        for (y in 0..h step step) {
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), gridPaint)
        }

        // Outer framing border
        val outerRect = RectF(24f, 24f, w - 24f, h - 24f)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                intArrayOf(withAlpha(primaryColor, 180), withAlpha(amberInt, 160), withAlpha(secondaryColor, 180)),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(outerRect, 42f, 42f, borderPaint)

        // 3. Top Header Row (Brand Badge + Player Profile Pill)
        val padX = 56f
        var cursorY = 64f

        // Brand & Status Pill
        val badgeRect = RectF(padX, cursorY, padX + 430f, cursorY + 52f)
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(primaryColor, 42)
        }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = withAlpha(primaryColor, 165)
        }
        canvas.drawRoundRect(badgeRect, 26f, 26f, badgeFill)
        canvas.drawRoundRect(badgeRect, 26f, 26f, badgeStroke)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText(
            if (spec.isVictory) "★ SPOTDIFF AI • VICTORY SNAPSHOT" else "⚡ SPOTDIFF AI • RUN SNAPSHOT",
            badgeRect.left + 22f,
            badgeRect.centerY() + 8f,
            badgeTextPaint
        )

        // Challenge code pill on the top-right
        if (spec.challengeCode.isNotBlank()) {
            val codeText = "CHALLENGE #${spec.challengeCode.uppercase(Locale.US)}"
            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = amberInt
                textSize = 22f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val codeWidth = codePaint.measureText(codeText) + 44f
            val codeRect = RectF(w - padX - codeWidth, cursorY, w - padX, cursorY + 52f)
            val codeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 15, 23, 42)
            }
            val codeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = withAlpha(amberInt, 150)
            }
            canvas.drawRoundRect(codeRect, 26f, 26f, codeBg)
            canvas.drawRoundRect(codeRect, 26f, 26f, codeBorder)
            canvas.drawText(
                codeText,
                codeRect.left + 22f,
                codeRect.centerY() + 8f,
                codePaint
            )
        }

        cursorY += 78f

        // Player Row + Level Title
        drawPlayerBadgeAndTitle(
            canvas = canvas,
            spec = spec,
            left = padX,
            top = cursorY,
            right = w - padX,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor
        )

        cursorY += 108f

        // 4. Hero Metrics Section: FINAL TIME & CONSECUTIVE WIN STREAK (Side-by-Side Hero Cards)
        val heroCardHeight = if (format == SocialSnapshotFormat.VERTICAL_STORY) 250f else 224f
        val gap = 24f
        val cardW = (w - padX * 2 - gap) / 2f

        val timeCardRect = RectF(padX, cursorY, padX + cardW, cursorY + heroCardHeight)
        val streakCardRect = RectF(padX + cardW + gap, cursorY, w - padX, cursorY + heroCardHeight)

        drawFinalTimeHeroCard(
            canvas = canvas,
            rect = timeCardRect,
            spec = spec,
            emeraldColor = emeraldInt,
            primaryColor = primaryColor,
            coralColor = coralInt
        )

        drawWinStreakHeroCard(
            canvas = canvas,
            rect = streakCardRect,
            spec = spec,
            amberColor = amberInt,
            secondaryColor = secondaryColor
        )

        cursorY += heroCardHeight + 26f

        // 5. Visual Puzzle Pair Showcase (or Thematic Showcase Card if no puzzle bitmaps provided)
        val footerReservedHeight = 118f
        val availableShowcaseHeight = (h - cursorY - footerReservedHeight).coerceAtLeast(240f)
        val showcaseRect = RectF(padX, cursorY, w - padX, cursorY + availableShowcaseHeight)

        if (spec.originalBitmap != null && spec.modifiedBitmap != null) {
            drawSideBySidePuzzleShowcase(
                canvas = canvas,
                rect = showcaseRect,
                spec = spec,
                primaryColor = primaryColor,
                emeraldColor = emeraldInt,
                showSpottedRings = showSpottedRings
            )
        } else {
            drawThemeStreakEmblemShowcase(
                canvas = canvas,
                rect = showcaseRect,
                spec = spec,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                amberColor = amberInt
            )
        }

        // 6. Footer Bar
        val footerTop = h - 86f
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val leftFooter = if (spec.challengeCode.isNotBlank()) {
            "Beat my time! Enter Challenge Code #${spec.challengeCode.uppercase(Locale.US)} in SpotDiff AI"
        } else {
            "Can you beat my Final Time & Win Streak in SpotDiff AI?"
        }
        canvas.drawText(leftFooter, padX, footerTop + 26f, footerTextPaint)

        val rightFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("SpotDiff AI • Abo Basmala", w - padX, footerTop + 26f, rightFooterPaint)

        return output
    }

    private fun drawPlayerBadgeAndTitle(
        canvas: Canvas,
        spec: SocialSnapshotSpec,
        left: Float,
        top: Float,
        right: Float,
        primaryColor: Int,
        secondaryColor: Int
    ) {
        val avatarSize = 84f
        val avatarCx = left + avatarSize / 2f
        val avatarCy = top + avatarSize / 2f

        // Decode custom AI avatar if data URI, otherwise render vibrant preset crest
        val decodedAvatar = decodeAvatarBitmapOrNull(spec.playerAvatarData)
        if (decodedAvatar != null) {
            canvas.save()
            val clipPath = Path().apply {
                addCircle(avatarCx, avatarCy, avatarSize / 2f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val dest = RectF(left, top, left + avatarSize, top + avatarSize)
            canvas.drawBitmap(decodedAvatar, null, dest, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } else {
            val preset = AvatarPreset.fromToken(spec.playerAvatarData)
            val p1 = (preset.primaryHex or 0xFF000000L).toInt()
            val p2 = (preset.secondaryHex or 0xFF000000L).toInt()
            val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(left, top, left + avatarSize, top + avatarSize, p1, p2, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(avatarCx, avatarCy, avatarSize / 2f, avatarPaint)

            val initials = spec.playerName.trim()
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "SP" }
            val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initials, avatarCx, avatarCy + 11f, initPaint)
        }

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = primaryColor
        }
        canvas.drawCircle(avatarCx, avatarCy, avatarSize / 2f, ringPaint)

        // Level Title & Player Name
        val textLeft = left + avatarSize + 22f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cleanTitle = spec.levelTitle.take(32).ifBlank { "${spec.theme.displayName} Challenge" }
        canvas.drawText(cleanTitle, textLeft, top + 38f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val subtitleLine = "Spotter: ${spec.playerName.ifBlank { "Spotter Pro" }}  •  ${spec.theme.displayName} (${spec.complexity.displayName})"
        canvas.drawText(subtitleLine.take(58), textLeft, top + 74f, subPaint)
    }

    private fun drawFinalTimeHeroCard(
        canvas: Canvas,
        rect: RectF,
        spec: SocialSnapshotSpec,
        emeraldColor: Int,
        primaryColor: Int,
        coralColor: Int
    ) {
        val accent = if (spec.isVictory) emeraldColor else coralColor
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(Color.argb(220, 15, 23, 42), withAlpha(accent, 38)),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = withAlpha(accent, 175)
        }
        canvas.drawRoundRect(rect, 30f, 30f, fillPaint)
        canvas.drawRoundRect(rect, 30f, 30f, strokePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(
            if (spec.isVictory) "⚡ FINAL COMPLETION TIME" else "⏱ ELAPSED RUN TIME",
            rect.left + 28f,
            rect.top + 44f,
            labelPaint
        )

        val formattedTime = formatTimeReadout(spec.finalTimeMillis)
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(formattedTime, rect.left + 28f, rect.top + 132f, valuePaint)

        // Sub-metrics pill row inside Final Time Card
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val spotsLine = "Spotted: ${spec.spotsFoundCount}/${spec.variationCount}  •  ${spec.score} pts"
        canvas.drawText(spotsLine, rect.left + 28f, rect.bottom - 30f, detailPaint)
    }

    private fun drawWinStreakHeroCard(
        canvas: Canvas,
        rect: RectF,
        spec: SocialSnapshotSpec,
        amberColor: Int,
        secondaryColor: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(Color.argb(220, 15, 23, 42), withAlpha(amberColor, 46)),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = withAlpha(amberColor, 185)
        }
        canvas.drawRoundRect(rect, 30f, 30f, fillPaint)
        canvas.drawRoundRect(rect, 30f, 30f, strokePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amberColor
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(
            "🔥 CONSECUTIVE WIN STREAK",
            rect.left + 28f,
            rect.top + 44f,
            labelPaint
        )

        val streakText = "${spec.currentWinStreak} ${if (spec.currentWinStreak == 1) "WIN" else "WINS"}"
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(streakText, rect.left + 28f, rect.top + 130f, valuePaint)

        val bestStreak = max(spec.bestWinStreak, spec.currentWinStreak)
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amberColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tierText = spec.streakTierBadge.ifBlank { "Base Complexity" }
        canvas.drawText(
            "$tierText  •  Best: $bestStreak",
            rect.left + 28f,
            rect.bottom - 30f,
            detailPaint
        )
    }

    private fun drawSideBySidePuzzleShowcase(
        canvas: Canvas,
        rect: RectF,
        spec: SocialSnapshotSpec,
        primaryColor: Int,
        emeraldColor: Int,
        showSpottedRings: Boolean
    ) {
        val containerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 11, 17, 32)
        }
        val containerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(110, 51, 65, 85)
        }
        canvas.drawRoundRect(rect, 30f, 30f, containerFill)
        canvas.drawRoundRect(rect, 30f, 30f, containerBorder)

        val innerPad = 22f
        val headerH = 42f
        val gap = 18f
        val availW = rect.width() - innerPad * 2 - gap
        val availH = rect.height() - innerPad * 2 - headerH
        val panelSide = min(availW / 2f, availH)

        val totalRowW = panelSide * 2f + gap
        val startX = rect.left + (rect.width() - totalRowW) / 2f
        val startY = rect.top + innerPad + headerH + (availH - panelSide) / 2f

        // Section Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText(
            "AI-GENERATED SIDE-BY-SIDE PUZZLE COMPARISON",
            startX,
            rect.top + innerPad + 24f,
            headerPaint
        )

        val leftPanel = RectF(startX, startY, startX + panelSide, startY + panelSide)
        val rightPanel = RectF(startX + panelSide + gap, startY, startX + panelSide * 2f + gap, startY + panelSide)

        // Draw Original Bitmap
        spec.originalBitmap?.let { bmp ->
            drawRoundedBitmapPanel(
                canvas = canvas,
                bitmap = bmp,
                panelRect = leftPanel,
                badgeLabel = "ORIGINAL SCENE",
                borderColor = primaryColor,
                spots = emptyList(),
                ringColor = emeraldColor
            )
        }

        // Draw Modified Bitmap with optional spotted rings
        spec.modifiedBitmap?.let { bmp ->
            val spotsToHighlight = if (showSpottedRings) {
                if (spec.foundSpotIndices.isNotEmpty()) {
                    spec.spots.filter { it.index in spec.foundSpotIndices }
                } else {
                    spec.spots
                }
            } else {
                emptyList()
            }
            drawRoundedBitmapPanel(
                canvas = canvas,
                bitmap = bmp,
                panelRect = rightPanel,
                badgeLabel = if (showSpottedRings) {
                    "SPOTTED (${spec.spotsFoundCount}/${spec.variationCount})"
                } else {
                    "MODIFIED SCENE"
                },
                borderColor = emeraldColor,
                spots = spotsToHighlight,
                ringColor = emeraldColor
            )
        }
    }

    private fun drawRoundedBitmapPanel(
        canvas: Canvas,
        bitmap: Bitmap,
        panelRect: RectF,
        badgeLabel: String,
        borderColor: Int,
        spots: List<DifferenceSpot>,
        ringColor: Int
    ) {
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(panelRect, 22f, 22f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, panelRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        // Draw spotted difference rings if enabled
        val minDim = min(panelRect.width(), panelRect.height())
        val ringFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = withAlpha(ringColor, 62)
        }
        val ringStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = ringColor
        }
        for (spot in spots) {
            val cx = panelRect.left + spot.normalizedX * panelRect.width()
            val cy = panelRect.top + spot.normalizedY * panelRect.height()
            val r = (spot.normalizedRadius * minDim).coerceAtLeast(18f)
            canvas.drawCircle(cx, cy, r, ringFill)
            canvas.drawCircle(cx, cy, r, ringStroke)
        }

        // Top-left label pill
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pillW = labelPaint.measureText(badgeLabel) + 28f
        val pillRect = RectF(panelRect.left + 12f, panelRect.top + 12f, panelRect.left + 12f + pillW, panelRect.top + 46f)
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 9, 14, 26)
        }
        canvas.drawRoundRect(pillRect, 14f, 14f, pillBg)
        canvas.drawText(badgeLabel, pillRect.left + 14f, pillRect.centerY() + 6f, labelPaint)

        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(borderColor, 195)
        }
        canvas.drawRoundRect(panelRect, 22f, 22f, borderPaint)
    }

    private fun drawThemeStreakEmblemShowcase(
        canvas: Canvas,
        rect: RectF,
        spec: SocialSnapshotSpec,
        primaryColor: Int,
        secondaryColor: Int,
        amberColor: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    Color.argb(215, 11, 17, 32),
                    withAlpha(primaryColor, 48),
                    withAlpha(secondaryColor, 48)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, 30f, 30f, fillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = withAlpha(primaryColor, 140)
        }
        canvas.drawRoundRect(rect, 30f, 30f, borderPaint)

        // Draw concentric geometric rings & star polygon in center
        val cx = rect.centerX()
        val cy = rect.centerY() - 18f
        val maxR = min(rect.width(), rect.height()) * 0.28f

        val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(amberColor, 145)
            pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
        }
        canvas.drawCircle(cx, cy, maxR, dashedPaint)

        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                maxR * 0.85f,
                intArrayOf(withAlpha(primaryColor, 110), withAlpha(secondaryColor, 45), Color.TRANSPARENT),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, maxR * 0.85f, innerCirclePaint)

        val polyPath = Path()
        val points = 8
        for (i in 0 until points) {
            val angle = i * 2.0 * Math.PI / points - Math.PI / 2.0
            val r = if (i % 2 == 0) maxR * 0.65f else maxR * 0.36f
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
        }
        polyPath.close()
        val polyStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }
        canvas.drawPath(polyPath, polyStroke)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "AI Complexity Tier: ${spec.streakTierBadge.ifBlank { "Base Complexity" }}",
            cx,
            rect.bottom - 72f,
            captionPaint
        )

        val subCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "${spec.theme.displayName} • ${spec.complexity.displayName} • ${spec.variationCount} Variations",
            cx,
            rect.bottom - 34f,
            subCaptionPaint
        )
    }

    fun formatTimeReadout(millis: Long): String {
        if (millis <= 0L) return "--.-s"
        val totalSeconds = millis / 1000.0
        return if (totalSeconds < 60.0) {
            String.format(Locale.US, "%.1fs", totalSeconds)
        } else {
            val mins = (millis / 60000L).toInt()
            val secs = ((millis % 60000L) / 1000.0)
            String.format(Locale.US, "%d:%04.1f", mins, secs)
        }
    }

    fun buildShareCaption(spec: SocialSnapshotSpec): String {
        val timeStr = formatTimeReadout(spec.finalTimeMillis)
        val bestStreak = max(spec.bestWinStreak, spec.currentWinStreak)
        return buildString {
            append("🔥 SpotDiff AI Snapshot — ${spec.playerName.ifBlank { "Spotter Pro" }}\n")
            append("⚡ Final Time: $timeStr (${spec.spotsFoundCount}/${spec.variationCount} differences • ${spec.score} pts)\n")
            append("🏆 Consecutive Win Streak: ${spec.currentWinStreak} (${spec.streakTierBadge.ifBlank { "Base Complexity" }} • Best: $bestStreak)\n")
            append("🎨 Theme: ${spec.theme.displayName} • ${spec.complexity.displayName}\n")
            if (spec.challengeCode.isNotBlank()) {
                append("🔑 Challenge Code: #${spec.challengeCode.uppercase(Locale.US)} — Can you beat my time?")
            }
        }
    }

    fun shareSnapshotToSocialMedia(
        context: Context,
        bitmap: Bitmap,
        spec: SocialSnapshotSpec
    ) {
        val sharedDir = File(context.cacheDir, "shared_snapshots").apply {
            if (!exists()) mkdirs()
        }
        // Clean up older temporary snapshots to keep cache tidy
        sharedDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("spotdiff_snapshot_")) {
                file.delete()
            }
        }

        val safeCode = spec.challengeCode.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "run" }
        val imageFile = File(sharedDir, "spotdiff_snapshot_${safeCode}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val caption = buildShareCaption(spec)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "My SpotDiff AI Time & Win Streak Snapshot")
            clipData = ClipData.newRawUri("SpotDiff AI Snapshot", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Time & Streak Snapshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun decodeAvatarBitmapOrNull(avatarData: String): Bitmap? {
        if (!avatarData.startsWith("data:image")) return null
        return try {
            val base64Part = avatarData.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
