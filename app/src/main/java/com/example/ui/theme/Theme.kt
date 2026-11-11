package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.GameTheme

@Composable
fun SpotDiffTheme(
    gameTheme: GameTheme = GameTheme.NATURE,
    content: @Composable () -> Unit
) {
    val targetPrimary = Color(gameTheme.primaryHex)
    val targetSecondary = Color(gameTheme.secondaryHex)

    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "theme_primary_color"
    )
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "theme_secondary_color"
    )
    val primaryContainerColor by animateColorAsState(
        targetValue = targetPrimary.copy(alpha = 0.22f),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "theme_primary_container"
    )
    val secondaryContainerColor by animateColorAsState(
        targetValue = targetSecondary.copy(alpha = 0.22f),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "theme_secondary_container"
    )

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color(0xFF041019),
        primaryContainer = primaryContainerColor,
        onPrimaryContainer = TextPrimaryLight,
        secondary = secondaryColor,
        onSecondary = Color(0xFF041019),
        secondaryContainer = secondaryContainerColor,
        onSecondaryContainer = TextPrimaryLight,
        tertiary = RadiantAmber,
        onTertiary = Color(0xFF1A1002),
        background = ObsidianBackground,
        onBackground = TextPrimaryLight,
        surface = SlateSurface,
        onSurface = TextPrimaryLight,
        surfaceVariant = SlateSurfaceVariant,
        onSurfaceVariant = TextSecondaryLight,
        error = CoralError,
        onError = Color(0xFF28050B)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
