package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AppPreferences
import com.example.ui.components.AnimatedThemeStylePreviewCard
import com.example.ui.components.PlayerAvatarBadge
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RadiantAmber
import com.example.ui.viewmodel.SpotDiffViewModel
import com.example.util.onGoogleSignInClicked
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun OptionsScreen(
    viewModel: SpotDiffViewModel,
    preferences: AppPreferences,
    isGuest: Boolean,
    onOpenProfile: () -> Unit = {},
    onSignOut: () -> Unit,
    onBackToPlay: () -> Unit
) {
    BackHandler(onBack = onBackToPlay)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser = Firebase.auth.currentUser
    val supportPhone = stringResource(id = R.string.creator_support_phone)
    val supportEmail = stringResource(id = R.string.creator_support_email)

    var suggestionText by rememberSaveable { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Options & Support",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Switch visual art styles with live animated previews, customize audio/haptics, and send direct suggestions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Visual Theme & Art Style Selector with Smooth Transition Effect
        AnimatedThemeStylePreviewCard(
            selectedTheme = preferences.selectedTheme,
            onSelectTheme = viewModel::setTheme,
            unlockedThemeIds = preferences.unlockedThemeIds,
            onOpenRewardShop = { viewModel.openRewardShop(initialTab = 0) }
        )

        // Audio, Haptics & Zoom Controls Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Audio, Haptics & Zoom Controls",
                    style = MaterialTheme.typography.titleMedium
                )

                OptionToggleRow(
                    icon = Icons.Default.MusicNote,
                    title = "Background Music",
                    subtitle = "Ambient synthesized soundtrack during gameplay",
                    checked = preferences.musicEnabled,
                    onCheckedChange = viewModel::setMusicEnabled,
                    testTag = "switch_music"
                )

                HorizontalDivider(color = Color(0xFF334155))

                OptionToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Sound Effects (SFX)",
                    subtitle = "Audio chimes when spotting differences or time alerts",
                    checked = preferences.soundEffectsEnabled,
                    onCheckedChange = viewModel::setSoundEffectsEnabled,
                    testTag = "switch_sfx"
                )

                HorizontalDivider(color = Color(0xFF334155))

                OptionToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Vibration (Android Vibrator Haptics)",
                    subtitle = "Haptic pulse when spotting a difference or running out of time",
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = viewModel::setVibrationEnabled,
                    testTag = "switch_vibration"
                )

                HorizontalDivider(color = Color(0xFF334155))

                OptionToggleRow(
                    icon = Icons.Default.ZoomIn,
                    title = "Zoom One Image at a Time",
                    subtitle = "Zoom & pan any single image independently while the other stays as-is",
                    checked = preferences.independentZoom,
                    onCheckedChange = viewModel::setIndependentZoom,
                    testTag = "switch_independent_zoom"
                )

                HorizontalDivider(color = Color(0xFF334155))

                OptionToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "High-Difficulty Countdown Screen-Shake",
                    subtitle = "Subtle screen tremor during countdown ticks & final seconds on High Difficulty levels",
                    checked = preferences.screenShakeEnabled,
                    onCheckedChange = viewModel::setScreenShakeEnabled,
                    testTag = "switch_screen_shake"
                )

                HorizontalDivider(color = Color(0xFF334155))

                OptionToggleRow(
                    icon = Icons.Default.ViewColumn,
                    title = "Side-by-Side Comparison",
                    subtitle = "Display AI images side-by-side instead of stacked",
                    checked = preferences.sideBySideMode,
                    onCheckedChange = viewModel::setSideBySideMode,
                    testTag = "switch_side_by_side"
                )
            }
        }

        // Direct Email Suggestions Card (abobasmala8805@gmail.com) + Designer Credit (01060407111)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = RadiantAmber.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("creator_support_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = RadiantAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = RadiantAmber
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Designed by Abo Basmala",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Direct Suggestions Mail & Support Line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RadiantAmber
                        )
                    }
                }

                // Direct Email Suggestions Section
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Send Direct Mail with Suggestions",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = supportEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = suggestionText,
                            onValueChange = { suggestionText = it },
                            label = { Text("Your Suggestion, Idea, or Feedback") },
                            placeholder = { Text("Write your suggestions for SpotDiff AI...") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggestion_email_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Support Email", supportEmail))
                                    Toast.makeText(context, "Copied $supportEmail to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("copy_email_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Mail")
                            }

                            Button(
                                onClick = {
                                    sendDirectSuggestionEmail(
                                        context = context,
                                        recipientEmail = supportEmail,
                                        suggestionBody = suggestionText
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF041019)
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("send_email_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Direct Mail", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Phone Support Line Section
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Direct Support & Feedback Line",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Designed by Abo Basmala 01060407111",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Support Phone", supportPhone))
                            Toast.makeText(context, "Copied $supportPhone to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_phone_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Number")
                    }

                    Button(
                        onClick = {
                            try {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                context.startActivity(dialIntent)
                            } catch (ignored: Exception) {
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadiantAmber,
                            contentColor = Color(0xFF1A1002)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_support_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call 01060407111", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account / Guest Session Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        PlayerAvatarBadge(
                            avatarData = preferences.playerAvatarData,
                            size = 44.dp,
                            avatarFrame = preferences.selectedAvatarFrame
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = preferences.playerDisplayName.ifBlank {
                                    if (isGuest) "Guest Player" else (currentUser?.displayName ?: "Signed-In Player")
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isGuest) {
                                    "Playing without Google Account (Offline & AI levels enabled)"
                                } else {
                                    currentUser?.email ?: "Google Account Connected"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = CoralError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isGuest) "Exit Guest" else "Sign Out", color = CoralError)
                    }
                }

                FilledTonalButton(
                    onClick = onOpenProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("options_open_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Customize Profile & AI Avatar", fontWeight = FontWeight.Bold)
                }

                if (isGuest) {
                    if (!authError.isNullOrBlank()) {
                        Text(
                            text = authError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralError
                        )
                    }
                    Button(
                        onClick = {
                            authError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {},
                                onAuthError = { msg -> authError = msg },
                                scope = scope
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_google_sign_in_button")
                    ) {
                        Text("Connect Google Account for Cloud Leaderboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun sendDirectSuggestionEmail(
    context: Context,
    recipientEmail: String,
    suggestionBody: String
) {
    val subject = "SpotDiff AI - Player Suggestion & Feedback"
    val body = if (suggestionBody.isBlank()) {
        "Hello Abo Basmala,\n\nHere are my suggestions for SpotDiff AI:\n"
    } else {
        suggestionBody
    }
    try {
        val mailtoUri = Uri.parse(
            "mailto:$recipientEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri)
        context.startActivity(emailIntent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Send email to $recipientEmail"))
        } catch (ignored: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Support Email", recipientEmail))
            Toast.makeText(context, "Email address $recipientEmail copied to clipboard", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun OptionToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
