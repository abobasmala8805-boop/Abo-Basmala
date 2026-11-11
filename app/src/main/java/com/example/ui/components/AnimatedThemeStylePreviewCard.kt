package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GameTheme
import com.example.ui.theme.RadiantAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedThemeStylePreviewCard(
    selectedTheme: GameTheme,
    onSelectTheme: (GameTheme) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Visual Theme & Art Style Preview",
    unlockedThemeIds: Set<String> = GameTheme.entries.map { it.id }.toSet(),
    onOpenRewardShop: (() -> Unit)? = null
) {
    val animatedPrimary by animateColorAsState(
        targetValue = Color(selectedTheme.primaryHex),
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "preview_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = Color(selectedTheme.secondaryHex),
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "preview_secondary"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "theme_preview_ambient")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        animatedPrimary.copy(alpha = 0.75f),
                        animatedSecondary.copy(alpha = 0.75f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("theme_style_preview_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = animatedPrimary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Animated Dual Color Swatch Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(animatedPrimary)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(animatedSecondary)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }

            // Smooth AnimatedContent Art Style Preview Banner
            AnimatedContent(
                targetState = selectedTheme,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val slideDirection = if (forward) 1 else -1
                    (fadeIn(animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(480, easing = FastOutSlowInEasing)
                        ) +
                        slideInHorizontally(
                            animationSpec = tween(480, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> slideDirection * (fullWidth / 4) }
                        )).togetherWith(
                        fadeOut(animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                            scaleOut(
                                targetScale = 1.06f,
                                animationSpec = tween(360, easing = FastOutSlowInEasing)
                            ) +
                            slideOutHorizontally(
                                animationSpec = tween(360, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -slideDirection * (fullWidth / 4) }
                            )
                    ).using(SizeTransform(clip = true))
                },
                label = "theme_art_style_transition"
            ) { theme ->
                val themePrimary = Color(theme.primaryHex)
                val themeSecondary = Color(theme.secondaryHex)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = 1.dp,
                            color = themePrimary.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("animated_theme_preview_banner_${theme.id.lowercase()}")
                ) {
                    // Procedural Art Style Canvas tailored to each theme
                    ThemeArtStyleCanvas(
                        theme = theme,
                        primary = themePrimary,
                        secondary = themeSecondary,
                        ambientPhase = ambientPhase,
                        pulseGlow = pulseGlow,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Scrim & Art Style Metadata Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x22060B14),
                                        Color(0xDD060B14)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = themePrimary.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = themePrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ART STYLE PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = theme.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Interactive Theme Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    val isUnlocked = theme.unlockCostShards == 0 || theme.id in unlockedThemeIds
                    val chipAccent = Color(theme.primaryHex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) chipAccent.copy(alpha = 0.24f) else Color(0xFF111827),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) chipAccent else Color(0xFF334155),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .testTag("settings_theme_chip_${theme.id.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(chipAccent)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked Theme",
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (!isUnlocked) {
                                Surface(
                                    color = RadiantAmber.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Diamond,
                                            contentDescription = null,
                                            tint = RadiantAmber,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "${theme.unlockCostShards}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RadiantAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (onOpenRewardShop != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, RadiantAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onOpenRewardShop() }
                        .testTag("theme_card_open_shop_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = RadiantAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tap locked themes or open the Reward Shop to unlock exotic styles",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = "Shop →",
                            style = MaterialTheme.typography.labelMedium,
                            color = RadiantAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeArtStyleCanvas(
    theme: GameTheme,
    primary: Color,
    secondary: Color,
    ambientPhase: Float,
    pulseGlow: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Rich atmospheric backdrop
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF070C18),
                    primary.copy(alpha = 0.28f),
                    secondary.copy(alpha = 0.25f),
                    Color(0xFF090E1A)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        when (theme) {
            GameTheme.NATURE -> {
                // Bioluminescent forest sanctuary: glowing canopy orbs, floating lanterns & water ripples
                for (i in 0..5) {
                    val x = w * (0.14f + i * 0.15f)
                    val floatOffset = sin((ambientPhase * 2 * Math.PI + i).toFloat()) * 8f
                    val y = h * (0.32f + (i % 2) * 0.16f) + floatOffset
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.55f),
                                secondary.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = 36f * pulseGlow
                        ),
                        radius = 36f * pulseGlow,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color(0xFFA7F3D0),
                        radius = 5.5f,
                        center = Offset(x, y)
                    )
                }
                // Stylized pine / botanical silhouettes on right
                val treePath = Path().apply {
                    moveTo(w * 0.78f, h * 0.15f)
                    lineTo(w * 0.68f, h * 0.85f)
                    lineTo(w * 0.88f, h * 0.85f)
                    close()
                }
                drawPath(treePath, color = primary.copy(alpha = 0.26f))
            }

            GameTheme.SPACE -> {
                // Deep Space cosmic nebula: ringed exoplanet & orbital starlight rings
                val planetCenter = Offset(w * 0.72f, h * 0.44f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.5f),
                            primary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = planetCenter,
                        radius = 75f * pulseGlow
                    ),
                    radius = 75f * pulseGlow,
                    center = planetCenter
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(primary, secondary),
                        start = Offset(planetCenter.x - 28f, planetCenter.y - 28f),
                        end = Offset(planetCenter.x + 28f, planetCenter.y + 28f)
                    ),
                    radius = 30f,
                    center = planetCenter
                )
                rotate(degrees = -22f + ambientPhase * 12f, pivot = planetCenter) {
                    drawOval(
                        color = Color(0xFFFBBF24).copy(alpha = 0.65f),
                        topLeft = Offset(planetCenter.x - 64f, planetCenter.y - 16f),
                        size = Size(128f, 32f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                // Twinkling stars
                for (i in 0..7) {
                    val sx = w * (0.10f + (i * 0.11f))
                    val sy = h * (0.20f + ((i * 37) % 55) / 100f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = if (i % 2 == 0) 3.5f * pulseGlow else 2.2f,
                        center = Offset(sx, sy)
                    )
                }
            }

            GameTheme.GEOMETRIC -> {
                // Bauhaus / Escher isometric geometric polyhedrons & crisp symmetry
                val cx = w * 0.72f
                val cy = h * 0.46f
                rotate(degrees = ambientPhase * 360f, pivot = Offset(cx, cy)) {
                    val poly = Path()
                    val sides = 6
                    val r = 44f * pulseGlow
                    for (i in 0 until sides) {
                        val angle = (i * 2.0 * Math.PI / sides)
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) poly.moveTo(px, py) else poly.lineTo(px, py)
                    }
                    poly.close()
                    drawPath(poly, color = primary.copy(alpha = 0.35f))
                    drawPath(poly, color = primary, style = Stroke(width = 2.5.dp.toPx()))
                }
                drawRect(
                    color = secondary.copy(alpha = 0.32f),
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(54f, 54f)
                )
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.45f),
                    radius = 26f,
                    center = Offset(w * 0.42f, h * 0.38f)
                )
            }

            GameTheme.CYBERPUNK -> {
                // Neon Synthwave sunset & perspective cyber grid
                val sunCenter = Offset(w * 0.70f, h * 0.42f)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFBBF24), primary, secondary)
                    ),
                    radius = 38f * pulseGlow,
                    center = sunCenter
                )
                // Horizontal synthwave grid lines
                for (i in 1..4) {
                    val ly = h * (0.55f + i * 0.09f)
                    drawLine(
                        color = secondary.copy(alpha = 0.45f),
                        start = Offset(0f, ly),
                        end = Offset(w, ly),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            GameTheme.ANCIENT -> {
                // Clockwork Relics: rotating celestial brass gears & temple mandala
                val gearCenter = Offset(w * 0.72f, h * 0.45f)
                rotate(degrees = ambientPhase * 360f, pivot = gearCenter) {
                    drawCircle(
                        color = primary.copy(alpha = 0.7f),
                        radius = 36f,
                        center = gearCenter,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    for (tooth in 0 until 8) {
                        val angle = tooth * (Math.PI / 4.0)
                        val tx = gearCenter.x + (44f * cos(angle)).toFloat()
                        val ty = gearCenter.y + (44f * sin(angle)).toFloat()
                        drawCircle(
                            color = primary,
                            radius = 5.5f,
                            center = Offset(tx, ty)
                        )
                    }
                }
                drawCircle(
                    color = secondary.copy(alpha = 0.65f),
                    radius = 18f * pulseGlow,
                    center = gearCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            GameTheme.SAKURA_ZENITH -> {
                // Sakura Zenith: glowing lantern orb & drifting cherry blossom petals
                val lanternCenter = Offset(w * 0.74f, h * 0.42f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.6f), primary.copy(alpha = 0.25f), Color.Transparent),
                        center = lanternCenter,
                        radius = 60f * pulseGlow
                    ),
                    radius = 60f * pulseGlow,
                    center = lanternCenter
                )
                for (i in 0..6) {
                    val px = w * (0.12f + i * 0.13f)
                    val py = h * (0.25f + ((i * 29) % 50) / 100f) + sin((ambientPhase * 2 * Math.PI + i).toFloat()) * 10f
                    drawCircle(
                        color = primary.copy(alpha = 0.75f),
                        radius = (5f + (i % 3) * 2.5f) * pulseGlow,
                        center = Offset(px, py)
                    )
                }
            }

            GameTheme.ATLANTIS_CORAL -> {
                // Atlantis Coral: bioluminescent underwater domes & rising pearl bubbles
                val domeCenter = Offset(w * 0.72f, h * 0.50f)
                drawCircle(
                    color = primary.copy(alpha = 0.45f),
                    radius = 44f * pulseGlow,
                    center = domeCenter,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = secondary.copy(alpha = 0.35f),
                    radius = 28f,
                    center = domeCenter
                )
                for (i in 0..5) {
                    val bx = w * (0.16f + i * 0.14f)
                    val by = h * (0.65f - ((ambientPhase + i * 0.17f) % 1f) * 0.48f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 4f + (i % 2) * 3f,
                        center = Offset(bx, by),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            GameTheme.SOLARPUNK_OASIS -> {
                // Solarpunk Oasis: rotating solar wind-turbine & stained-glass botanical sun
                val turbineCenter = Offset(w * 0.72f, h * 0.44f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.55f), primary.copy(alpha = 0.2f), Color.Transparent),
                        center = turbineCenter,
                        radius = 64f * pulseGlow
                    ),
                    radius = 64f * pulseGlow,
                    center = turbineCenter
                )
                rotate(degrees = ambientPhase * 360f, pivot = turbineCenter) {
                    for (blade in 0 until 6) {
                        val angle = blade * (Math.PI / 3.0)
                        val bx = turbineCenter.x + (38f * cos(angle)).toFloat()
                        val by = turbineCenter.y + (38f * sin(angle)).toFloat()
                        drawLine(
                            color = secondary,
                            start = turbineCenter,
                            end = Offset(bx, by),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}
