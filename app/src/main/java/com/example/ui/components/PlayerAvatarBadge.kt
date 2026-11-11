package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AvatarFrame
import com.example.data.model.AvatarPreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun splitAvatarDataAndFrame(rawAvatarData: String): Pair<String, AvatarFrame?> {
    val frameMarker = "#frame:"
    val markerIdx = rawAvatarData.lastIndexOf(frameMarker)
    if (markerIdx < 0) return rawAvatarData to null
    val cleanData = rawAvatarData.substring(0, markerIdx)
    val frameId = rawAvatarData.substring(markerIdx + frameMarker.length)
    return cleanData to AvatarFrame.fromId(frameId)
}

fun encodeAvatarWithFrame(avatarData: String, frame: AvatarFrame): String {
    val (clean, _) = splitAvatarDataAndFrame(avatarData)
    return if (frame == AvatarFrame.DEFAULT_RING) clean else "$clean#frame:${frame.name}"
}

fun decodeAvatarDataUri(avatarData: String): Bitmap? {
    val (cleanData, _) = splitAvatarDataAndFrame(avatarData)
    if (!cleanData.startsWith("data:image/")) return null
    val commaIndex = cleanData.indexOf(',')
    if (commaIndex < 0 || commaIndex + 1 >= cleanData.length) return null
    return try {
        val base64Part = cleanData.substring(commaIndex + 1)
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun PlayerAvatarBadge(
    avatarData: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showAiBadge: Boolean = true,
    avatarFrame: AvatarFrame? = null
) {
    val (cleanAvatarData, embeddedFrame) = remember(avatarData) {
        splitAvatarDataAndFrame(avatarData)
    }
    val effectiveFrame = avatarFrame ?: embeddedFrame ?: AvatarFrame.DEFAULT_RING

    val isCustomAi = cleanAvatarData.startsWith("data:image/")
    val decodedBitmap = remember(cleanAvatarData) {
        if (isCustomAi) decodeAvatarDataUri(cleanAvatarData) else null
    }
    val preset = remember(cleanAvatarData) {
        AvatarPreset.fromToken(cleanAvatarData)
    }

    val primaryColor = if (isCustomAi) Color(0xFF38BDF8) else Color(preset.primaryHex)
    val secondaryColor = if (isCustomAi) Color(0xFFEC4899) else Color(preset.secondaryHex)
    val framePrimary = Color(effectiveFrame.primaryHex)
    val frameSecondary = Color(effectiveFrame.secondaryHex)

    val hasOrnateFrame = effectiveFrame != AvatarFrame.DEFAULT_RING
    val innerAvatarFraction = if (hasOrnateFrame) 0.82f else 1.0f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (hasOrnateFrame) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxR = this.size.minDimension / 2f

                // Outer radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            framePrimary.copy(alpha = 0.36f),
                            frameSecondary.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxR
                    ),
                    radius = maxR,
                    center = center
                )

                when (effectiveFrame) {
                    AvatarFrame.DEFAULT_RING -> Unit
                    AvatarFrame.EMERALD_VINE -> {
                        // Double emerald ring + 4 cardinal leaf diamond gems
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(framePrimary, frameSecondary, framePrimary)
                            ),
                            radius = maxR * 0.92f,
                            center = center,
                            style = Stroke(width = (maxR * 0.10f).coerceAtLeast(2f))
                        )
                        drawCircle(
                            color = Color(0xFFA7F3D0),
                            radius = maxR * 0.83f,
                            center = center,
                            style = Stroke(
                                width = (maxR * 0.04f).coerceAtLeast(1f),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                        for (i in 0 until 4) {
                            val angle = i * (PI / 2.0)
                            val gx = center.x + cos(angle).toFloat() * (maxR * 0.92f)
                            val gy = center.y + sin(angle).toFloat() * (maxR * 0.92f)
                            drawCircle(
                                color = Color(0xFF064E3B),
                                radius = (maxR * 0.13f).coerceAtLeast(3f),
                                center = Offset(gx, gy)
                            )
                            drawCircle(
                                color = Color(0xFF34D399),
                                radius = (maxR * 0.08f).coerceAtLeast(2f),
                                center = Offset(gx, gy)
                            )
                        }
                    }

                    AvatarFrame.SOLAR_CROWN -> {
                        // 8-point golden sunburst rays + regal gold ring
                        for (i in 0 until 8) {
                            val angle = i * (PI / 4.0)
                            val inner = maxR * 0.78f
                            val outer = maxR * 0.98f
                            val sx = center.x + cos(angle).toFloat() * inner
                            val sy = center.y + sin(angle).toFloat() * inner
                            val ex = center.x + cos(angle).toFloat() * outer
                            val ey = center.y + sin(angle).toFloat() * outer
                            drawLine(
                                color = framePrimary,
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = (maxR * 0.11f).coerceAtLeast(2.2f)
                            )
                        }
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(framePrimary, frameSecondary, Color(0xFFFEF08A), framePrimary)
                            ),
                            radius = maxR * 0.86f,
                            center = center,
                            style = Stroke(width = (maxR * 0.11f).coerceAtLeast(2.2f))
                        )
                    }

                    AvatarFrame.CYBER_HEX -> {
                        // Neon octagon matrix + glowing vertices
                        val octPath = Path()
                        for (i in 0 until 8) {
                            val angle = i * (PI / 4.0) + (PI / 8.0)
                            val px = center.x + cos(angle).toFloat() * (maxR * 0.93f)
                            val py = center.y + sin(angle).toFloat() * (maxR * 0.93f)
                            if (i == 0) octPath.moveTo(px, py) else octPath.lineTo(px, py)
                        }
                        octPath.close()
                        drawPath(
                            path = octPath,
                            brush = Brush.sweepGradient(
                                listOf(framePrimary, frameSecondary, framePrimary)
                            ),
                            style = Stroke(width = (maxR * 0.11f).coerceAtLeast(2.2f))
                        )
                        for (i in 0 until 4) {
                            val angle = i * (PI / 2.0) + (PI / 4.0)
                            val vx = center.x + cos(angle).toFloat() * (maxR * 0.93f)
                            val vy = center.y + sin(angle).toFloat() * (maxR * 0.93f)
                            drawCircle(
                                color = frameSecondary,
                                radius = (maxR * 0.09f).coerceAtLeast(2f),
                                center = Offset(vx, vy)
                            )
                        }
                    }

                    AvatarFrame.CELESTIAL_ORBIT -> {
                        // Cosmic astrolabe double orbit + 3 golden satellite moons
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(framePrimary, Color(0xFF38BDF8), frameSecondary, framePrimary)
                            ),
                            radius = maxR * 0.90f,
                            center = center,
                            style = Stroke(width = (maxR * 0.09f).coerceAtLeast(2f))
                        )
                        for (i in 0 until 3) {
                            val angle = i * (2.0 * PI / 3.0) - PI / 2.0
                            val mx = center.x + cos(angle).toFloat() * (maxR * 0.90f)
                            val my = center.y + sin(angle).toFloat() * (maxR * 0.90f)
                            drawCircle(
                                color = Color(0xFF1E1B4B),
                                radius = (maxR * 0.14f).coerceAtLeast(3.2f),
                                center = Offset(mx, my)
                            )
                            drawCircle(
                                color = frameSecondary,
                                radius = (maxR * 0.09f).coerceAtLeast(2.2f),
                                center = Offset(mx, my)
                            )
                        }
                    }

                    AvatarFrame.GRANDMASTER_PRISM -> {
                        // Full rainbow prismatic crown + 6 starlight nodes
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF43F5E),
                                    Color(0xFFF59E0B),
                                    Color(0xFF10B981),
                                    Color(0xFF38BDF8),
                                    Color(0xFFA855F7),
                                    Color(0xFFF43F5E)
                                )
                            ),
                            radius = maxR * 0.92f,
                            center = center,
                            style = Stroke(width = (maxR * 0.13f).coerceAtLeast(2.5f))
                        )
                        for (i in 0 until 6) {
                            val angle = i * (PI / 3.0) - PI / 2.0
                            val sx = center.x + cos(angle).toFloat() * (maxR * 0.92f)
                            val sy = center.y + sin(angle).toFloat() * (maxR * 0.92f)
                            drawCircle(
                                color = Color.White,
                                radius = (maxR * 0.09f).coerceAtLeast(2f),
                                center = Offset(sx, sy)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(size * innerAvatarFraction)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.35f),
                            Color(0xFF0F172A),
                            secondaryColor.copy(alpha = 0.40f)
                        )
                    )
                )
                .border(
                    width = if (size >= 64.dp) 2.5.dp else 1.8.dp,
                    brush = Brush.sweepGradient(
                        if (hasOrnateFrame) {
                            listOf(framePrimary, frameSecondary, framePrimary)
                        } else {
                            listOf(primaryColor, secondaryColor, primaryColor)
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (decodedBitmap != null) {
                Image(
                    bitmap = remember(decodedBitmap) { decodedBitmap.asImageBitmap() },
                    contentDescription = "AI Generated Custom Player Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                // Stylized vector & geometric emblem for preset avatar
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = this.size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.42f),
                                secondaryColor.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(r, r),
                            radius = r
                        ),
                        radius = r
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = r * 0.74f,
                        style = Stroke(width = (r * 0.06f).coerceAtLeast(1.5f))
                    )
                }

                val iconVector = when (preset) {
                    AvatarPreset.CYBER_DETECTIVE -> Icons.Default.RemoveRedEye
                    AvatarPreset.FOREST_SPIRIT -> Icons.Default.Spa
                    AvatarPreset.COSMIC_VOYAGER -> Icons.Default.Public
                    AvatarPreset.BAUHAUS_PRISM -> Icons.Default.Explore
                    AvatarPreset.CLOCKWORK_OWL -> Icons.Default.Psychology
                    AvatarPreset.NEON_KITSUNE -> Icons.Default.Pets
                    AvatarPreset.SOLAR_PHOENIX -> Icons.Default.LocalFireDepartment
                    AvatarPreset.CRYSTAL_SPHINX -> Icons.Default.Shield
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = preset.displayName,
                    tint = Color.White,
                    modifier = Modifier.size(size * innerAvatarFraction * 0.52f)
                )
            }
        }

        if (isCustomAi && showAiBadge && size >= 38.dp) {
            Surface(
                color = Color(0xFF090E1A),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.34f).coerceIn(14.dp, 26.dp))
                    .border(1.dp, Color(0xFF38BDF8), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Avatar",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
