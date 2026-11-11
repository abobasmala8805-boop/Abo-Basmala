package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppPreferences
import com.example.data.model.DailyCalendarDayCell
import com.example.data.model.DailyLoginClaimReceipt
import com.example.data.model.DailyLoginRewardTier
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber

@Composable
fun DailyLoginCalendarModal(
    preferences: AppPreferences,
    latestReceipt: DailyLoginClaimReceipt?,
    statusMessage: String?,
    onClaimToday: () -> Unit,
    onSimulateNextDay: () -> Unit,
    onOpenRewardShop: () -> Unit,
    onDismiss: () -> Unit
) {
    val summary = remember(
        preferences.dailyLoginStreak,
        preferences.bestDailyLoginStreak,
        preferences.lastDailyClaimDate,
        preferences.dailyLoginDates,
        preferences.simulatedDayOffset
    ) {
        preferences.dailyLoginSummary()
    }

    val activeTier = summary.activeTier
    val infiniteTransition = rememberInfiniteTransition(label = "daily_calendar_pulse")
    val todayPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "today_pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            RadiantAmber.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            EmeraldSuccess.copy(alpha = 0.75f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .testTag("daily_login_calendar_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = RadiantAmber.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, RadiantAmber.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Daily Login Calendar",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Surface(
                                    color = RadiantAmber.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "WEEK ${summary.weekCycleNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RadiantAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Consecutive days of app usage grant progressively better rewards!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_daily_login_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Daily Login Calendar",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                // Streak Metrics Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF111C33),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = RadiantAmber,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = if (summary.isClaimedToday) {
                                        "${summary.currentStreak}-Day Active Streak"
                                    } else {
                                        "Claim for ${summary.targetClaimStreak}-Day Streak!"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Best Login Streak: ${summary.bestStreak}d • Checked In: ${summary.checkedInCountThisMonth}d this month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF451A03).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(1.dp, RadiantAmber.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .clickable { onOpenRewardShop() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${preferences.shardBalance}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Monthly Check-In Calendar Grid
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                        .testTag("daily_login_month_calendar_grid")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = summary.monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LegendDot(color = EmeraldSuccess, label = "Checked In")
                                LegendDot(color = RadiantAmber, label = "Today")
                            }
                        }

                        // Days of Week Header
                        val dayHeaders = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayHeaders.forEach { d ->
                                Text(
                                    text = d,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 6 rows x 7 columns
                        val rows = summary.cells.chunked(7)
                        rows.forEach { weekCells ->
                            // Only render trailing row if at least one day belongs to the current month or streak
                            if (weekCells.any { it.isCurrentMonth || it.isToday || it.isCheckedIn }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    weekCells.forEach { cell ->
                                        CalendarDayCellBox(
                                            cell = cell,
                                            pulseScale = if (cell.isToday && !cell.isCheckedIn) todayPulseScale else 1f,
                                            onClickToday = {
                                                if (cell.isToday && !cell.isCheckedIn) {
                                                    onClaimToday()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7-Day Progressive Escalating Reward Track
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Progressive Streak Rewards",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (summary.weekBonusPercent > 0) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+${summary.weekBonusPercent}% Weekly Cycle Boost",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.rewardTiers.forEach { tier ->
                            val isPastClaimedInCycle = tier.dayIndex < summary.activeTierDayIndex ||
                                (tier.dayIndex == summary.activeTierDayIndex && summary.isClaimedToday)
                            val isCurrentTarget = tier.dayIndex == summary.activeTierDayIndex && !summary.isClaimedToday

                            ProgressiveRewardTierCard(
                                tier = tier,
                                isClaimed = isPastClaimedInCycle,
                                isCurrentTarget = isCurrentTarget,
                                pulseScale = if (isCurrentTarget) todayPulseScale else 1f,
                                onClick = {
                                    if (isCurrentTarget) {
                                        onClaimToday()
                                    }
                                }
                            )
                        }
                    }
                }

                // Today's Spotlight Reward Summary Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (summary.isClaimedToday) {
                        EmeraldSuccess.copy(alpha = 0.14f)
                    } else {
                        RadiantAmber.copy(alpha = 0.14f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (summary.isClaimedToday) {
                                EmeraldSuccess.copy(alpha = 0.6f)
                            } else {
                                RadiantAmber.copy(alpha = 0.65f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    imageVector = if (summary.isClaimedToday) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.CardGiftcard
                                    },
                                    contentDescription = null,
                                    tint = if (summary.isClaimedToday) EmeraldSuccess else RadiantAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = activeTier.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeTier.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "+${activeTier.scaledShards} Shards",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RadiantAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• +${activeTier.bonusHints} Hints",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (activeTier.milestoneBonusLabel != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Milestone Bonus: ${activeTier.milestoneBonusLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Status / Claim Receipt Banner
                AnimatedVisibility(visible = !statusMessage.isNullOrBlank() || latestReceipt != null) {
                    val bannerText = statusMessage ?: latestReceipt?.let { rec ->
                        val extra = rec.specialUnlockTitle?.let { " • $it" }.orEmpty()
                        "Claimed Day ${rec.streakDayReached}: +${rec.shardsAwarded} Starlight Shards & +${rec.hintsAwarded} Bonus Hints$extra"
                    }.orEmpty()
                    if (bannerText.isNotBlank()) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .testTag("daily_login_status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Primary Claim Button & Simulate Next Consecutive Day Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (summary.isClaimedToday) {
                                onDismiss()
                            } else {
                                onClaimToday()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (summary.isClaimedToday) EmeraldSuccess else RadiantAmber,
                            contentColor = Color(0xFF041019)
                        ),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(52.dp)
                            .testTag("claim_daily_reward_button")
                    ) {
                        Icon(
                            imageVector = if (summary.isClaimedToday) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (summary.isClaimedToday) {
                                "Day ${summary.currentStreak} Claimed • Continue"
                            } else {
                                "Claim Day ${summary.targetClaimStreak} (+${activeTier.scaledShards} Shards)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onSimulateNextDay,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(52.dp)
                            .testTag("simulate_next_day_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Simulate Next Consecutive Day",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Next Day (+1d)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun CalendarDayCellBox(
    cell: DailyCalendarDayCell,
    pulseScale: Float,
    onClickToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        cell.isToday && cell.isCheckedIn -> EmeraldSuccess.copy(alpha = 0.28f)
        cell.isToday -> RadiantAmber.copy(alpha = 0.24f)
        cell.isCheckedIn -> EmeraldSuccess.copy(alpha = 0.16f)
        cell.isCurrentMonth -> Color(0xFF162038)
        else -> Color(0xFF0B1120)
    }

    val borderColor = when {
        cell.isToday && cell.isCheckedIn -> EmeraldSuccess
        cell.isToday -> RadiantAmber
        cell.isCheckedIn -> EmeraldSuccess.copy(alpha = 0.55f)
        cell.isCurrentMonth -> Color(0xFF1E293B)
        else -> Color.Transparent
    }

    val textColor = when {
        !cell.isCurrentMonth -> Color(0xFF475569)
        cell.isToday -> Color.White
        cell.isCheckedIn -> EmeraldSuccess
        else -> Color(0xFFCBD5E1)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(42.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (cell.isToday) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (cell.isToday && !cell.isCheckedIn) {
                    Modifier.clickable { onClickToday() }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${cell.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (cell.isToday || cell.isCheckedIn) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 11.sp
            )

            when {
                cell.isCheckedIn -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Checked in",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(12.dp)
                    )
                }
                cell.isToday -> {
                    Text(
                        text = "CLAIM",
                        color = RadiantAmber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                cell.streakDayBadge != null && cell.isCurrentMonth -> {
                    Text(
                        text = "D${cell.streakDayBadge}",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressiveRewardTierCard(
    tier: DailyLoginRewardTier,
    isClaimed: Boolean,
    isCurrentTarget: Boolean,
    pulseScale: Float,
    onClick: () -> Unit
) {
    val containerColor = when {
        isCurrentTarget -> RadiantAmber.copy(alpha = 0.20f)
        isClaimed -> EmeraldSuccess.copy(alpha = 0.14f)
        tier.isMilestone -> Color(0xFF172554).copy(alpha = 0.65f)
        else -> Color(0xFF111C33)
    }

    val borderColor = when {
        isCurrentTarget -> RadiantAmber
        isClaimed -> EmeraldSuccess.copy(alpha = 0.65f)
        tier.isMilestone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color(0xFF1E293B)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .width(108.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .border(
                width = if (isCurrentTarget) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isCurrentTarget) { onClick() }
            .testTag("daily_tier_card_day_${tier.dayIndex}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = when {
                    isCurrentTarget -> RadiantAmber
                    isClaimed -> EmeraldSuccess
                    else -> Color(0xFF1E293B)
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when {
                        isClaimed -> "DAY ${tier.dayIndex} ✓"
                        isCurrentTarget -> "DAY ${tier.dayIndex} NOW"
                        else -> "DAY ${tier.dayIndex}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentTarget || isClaimed) Color(0xFF041019) else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Icon(
                imageVector = when {
                    isClaimed -> Icons.Default.CheckCircle
                    tier.isMilestone -> Icons.Default.Star
                    else -> Icons.Default.Diamond
                },
                contentDescription = null,
                tint = when {
                    isClaimed -> EmeraldSuccess
                    isCurrentTarget || tier.isMilestone -> RadiantAmber
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = "+${tier.scaledShards}",
                style = MaterialTheme.typography.titleSmall,
                color = RadiantAmber,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "+${tier.bonusHints} Hint${if (tier.bonusHints > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold
                )
            }

            if (tier.milestoneBonusLabel != null) {
                Text(
                    text = tier.milestoneBonusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(11.dp))
            }
        }
    }
}

@Composable
fun DailyLoginHubBanner(
    preferences: AppPreferences,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(
        preferences.dailyLoginStreak,
        preferences.bestDailyLoginStreak,
        preferences.lastDailyClaimDate,
        preferences.dailyLoginDates,
        preferences.simulatedDayOffset
    ) {
        preferences.dailyLoginSummary()
    }
    val activeTier = summary.activeTier

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111C33)),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = if (!summary.isClaimedToday) RadiantAmber.copy(alpha = 0.75f) else EmeraldSuccess.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onOpenCalendar() }
            .testTag("daily_login_hub_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (!summary.isClaimedToday) RadiantAmber.copy(alpha = 0.22f) else EmeraldSuccess.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (!summary.isClaimedToday) RadiantAmber else EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (summary.isClaimedToday) {
                                "Daily Login Calendar • ${summary.currentStreak}d Streak"
                            } else {
                                "Daily Login Reward Ready! (Day ${summary.targetClaimStreak})"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (summary.isClaimedToday) {
                            "${summary.monthTitle}: ${summary.checkedInCountThisMonth} days checked in • Tap to view calendar"
                        } else {
                            "Claim +${activeTier.scaledShards} Shards & +${activeTier.bonusHints} Hints on the ${summary.monthTitle} calendar!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.isClaimedToday) Color(0xFF94A3B8) else RadiantAmber
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onOpenCalendar,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("open_daily_login_calendar_button")
            ) {
                Icon(
                    imageVector = if (summary.isClaimedToday) Icons.Default.CalendarMonth else Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (summary.isClaimedToday) EmeraldSuccess else RadiantAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (summary.isClaimedToday) "Calendar" else "Claim",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
