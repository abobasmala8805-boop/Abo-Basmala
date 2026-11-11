package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChallengeLinkPayload
import com.example.data.model.ChallengeTimeRecord
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.ChallengeLinkUiState
import java.util.Locale

fun shareChallengePayloadViaIntent(context: Context, payload: ChallengeLinkPayload) {
    val shareMessage = payload.toShareMessage()
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            "SpotDiff AI Speed Challenge (#${payload.challengeCode}): ${payload.title}"
        )
        putExtra(Intent.EXTRA_TEXT, shareMessage)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Challenge Link with Friends"))
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun ChallengeLinkHubCard(
    challengeState: ChallengeLinkUiState,
    standingsForParsed: List<ChallengeTimeRecord>,
    onInputChange: (String) -> Unit,
    onGenerateSetupChallengeLink: () -> Unit,
    onAcceptChallenge: (ChallengeLinkPayload) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copiedBanner by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("challenge_link_hub_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Friend Challenge Link & Speed Duel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generate a Challenge Link for your puzzle layout or paste a friend's link to race for the shortest time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Generate Challenge Link button for current setup
            FilledTonalButton(
                onClick = onGenerateSetupChallengeLink,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_setup_challenge_link_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Challenge Link for Current Setup",
                    fontWeight = FontWeight.Bold
                )
            }

            // Input field to paste a friend's Challenge Link or 6-character code
            OutlinedTextField(
                value = challengeState.inputLinkOrCode,
                onValueChange = {
                    copiedBanner = null
                    onInputChange(it)
                },
                label = { Text("Paste Friend's Challenge Link or Code") },
                placeholder = { Text("https://spotdiff.aistudio.app/challenge?code=... or 6-char code") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    if (challengeState.inputLinkOrCode.isNotBlank()) {
                        IconButton(onClick = { onInputChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear challenge link")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenge_link_input")
            )

            if (!challengeState.parseErrorMessage.isNullOrBlank()) {
                Text(
                    text = challengeState.parseErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralError
                )
            }

            val parsed = challengeState.parsedChallenge
            if (parsed != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0B1120).copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(parsed.theme.primaryHex).copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("parsed_challenge_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = parsed.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${parsed.theme.displayName} • ${parsed.variationCount} Differences • ${parsed.patternComplexity.displayName} (${parsed.timeLimitSeconds}s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Surface(
                                color = RadiantAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "#${parsed.challengeCode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (parsed.challengerTimeMillis != null && parsed.challengerTimeMillis > 0L) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Challenger %s completed this in %.1fs — Can you beat it?",
                                            parsed.challengerName,
                                            parsed.challengerTimeMillis / 1000.0
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }

                        // Fastest times for this Challenge Code
                        ChallengeStandingsCompactList(
                            challengeCode = parsed.challengeCode,
                            standings = standingsForParsed
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    copyTextToClipboard(context, "Challenge Link", parsed.toChallengeUrl())
                                    copiedBanner = "Challenge Link copied to clipboard!"
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Link")
                            }

                            Button(
                                onClick = { onAcceptChallenge(parsed) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = Color(0xFF041019)
                                ),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .testTag("accept_challenge_link_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept & Race", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!copiedBanner.isNullOrBlank()) {
                            Text(
                                text = copiedBanner!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeStandingsCompactList(
    challengeCode: String,
    standings: List<ChallengeTimeRecord>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = RadiantAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Shortest Completion Times (#$challengeCode)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${standings.size} recorded",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }

        if (standings.isEmpty()) {
            Text(
                text = "No speed records for #$challengeCode yet — complete this puzzle to set the #1 fastest time!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        } else {
            val fastestMs = standings.first().completionTimeMillis
            standings.take(5).forEachIndexed { idx, rec ->
                val rank = idx + 1
                val deltaSec = (rec.completionTimeMillis - fastestMs) / 1000.0
                val rankColor = when (rank) {
                    1 -> Color(0xFFFBBF24)
                    2 -> Color(0xFFCBD5E1)
                    3 -> Color(0xFFD97706)
                    else -> Color(0xFF64748B)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (rec.isCurrentUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        Color(0xFF1E293B).copy(alpha = 0.7f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelLarge,
                                color = rankColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                            PlayerAvatarBadge(
                                avatarData = rec.playerAvatarData,
                                size = 24.dp,
                                showAiBadge = false
                            )
                            Text(
                                text = rec.playerName + if (rec.isCurrentUser) " (You)" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (rank > 1 && deltaSec > 0.05) {
                                Text(
                                    text = String.format(Locale.US, "+%.1fs", deltaSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralError
                                )
                            } else if (rank == 1) {
                                Text(
                                    text = "FASTEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1fs", rec.completionTimeMillis / 1000.0),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeLinkGeneratorDialog(
    payload: ChallengeLinkPayload,
    standings: List<ChallengeTimeRecord>,
    onDismiss: () -> Unit,
    onPlayChallengeNow: (ChallengeLinkPayload) -> Unit
) {
    val context = LocalContext.current
    var copiedConfirmation by remember { mutableStateOf<String?>(null) }
    val challengeUrl = remember(payload) { payload.toChallengeUrl() }
    val themeColor = Color(payload.theme.primaryHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(16.dp)
                .border(
                    width = 1.5.dp,
                    color = themeColor.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("challenge_link_generator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                            color = themeColor.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = themeColor
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Puzzle Challenge Link",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Share this exact puzzle layout & race for the shortest time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_challenge_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Challenge Link Dialog")
                    }
                }

                // Visual Layout Fingerprint & Metadata Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0B1120),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Mini Blueprint Radar showing the exact difference spot coordinates
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF0F172A),
                                            themeColor.copy(alpha = 0.28f),
                                            Color(0xFF090D16)
                                        )
                                    )
                                )
                                .border(1.dp, themeColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stepX = size.width / 4f
                                val stepY = size.height / 4f
                                for (i in 1..3) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(i * stepX, 0f),
                                        end = Offset(i * stepX, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(0f, i * stepY),
                                        end = Offset(size.width, i * stepY),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                payload.spots.forEach { spot ->
                                    val c = Offset(spot.normalizedX * size.width, spot.normalizedY * size.height)
                                    val r = (spot.normalizedRadius * size.minDimension * 1.1f).coerceAtLeast(4.dp.toPx())
                                    drawCircle(
                                        color = RadiantAmber.copy(alpha = 0.3f),
                                        radius = r,
                                        center = c
                                    )
                                    drawCircle(
                                        color = RadiantAmber,
                                        radius = r,
                                        center = c,
                                        style = Stroke(width = 1.8.dp.toPx())
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = RadiantAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "CODE: ${payload.challengeCode}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RadiantAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = payload.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${payload.theme.displayName} • ${payload.variationCount} Spots • ${payload.patternComplexity.displayName} • ${payload.timeLimitSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )

                            if (payload.challengerTimeMillis != null && payload.challengerTimeMillis > 0L) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Target Time to Beat: %.1fs (by %s)",
                                        payload.challengerTimeMillis / 1000.0,
                                        payload.challengerName
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Generated Challenge Link Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SHAREABLE CHALLENGE LINK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = challengeUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("generated_challenge_url_text")
                        )
                    }
                }

                if (!copiedConfirmation.isNullOrBlank()) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
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
                                text = copiedConfirmation!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                // Copy & Share Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyTextToClipboard(context, "SpotDiff Challenge Link", challengeUrl)
                            copiedConfirmation = "Challenge Link (#${payload.challengeCode}) copied!"
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_challenge_link_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }

                    Button(
                        onClick = { shareChallengePayloadViaIntent(context, payload) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("share_challenge_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Challenge Link", fontWeight = FontWeight.Bold)
                    }
                }

                // Live Shortest Completion Times for this specific Challenge Layout
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0B1120).copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        ChallengeStandingsCompactList(
                            challengeCode = payload.challengeCode,
                            standings = standings
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onPlayChallengeNow(payload) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("play_challenge_from_modal_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play This Exact Challenge Layout Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
