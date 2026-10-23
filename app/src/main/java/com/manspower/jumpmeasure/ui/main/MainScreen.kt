package com.manspower.jumpmeasure.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manspower.jumpmeasure.R
import com.manspower.jumpmeasure.theme.JumpMeasureTheme
import com.manspower.jumpmeasure.theme.NeonCyan
import com.manspower.jumpmeasure.theme.SportCrimson
import com.manspower.jumpmeasure.theme.SportGreen
import com.manspower.jumpmeasure.theme.SportYellow
import com.manspower.jumpmeasure.ui.components.AthleticBackground
import com.manspower.jumpmeasure.ui.language.LanguageManager
import com.manspower.jumpmeasure.ui.language.LocalAppStrings

import androidx.compose.ui.zIndex

@Composable
fun MainScreen(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val scrollState = rememberScrollState()

    AthleticBackground(modifier = modifier) {
        // ── Main Content Column (Scrollable for Portrait & Landscape) ────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Manspower Logo",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SportYellow.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            )

            Spacer(Modifier.height(12.dp))

            // App Name Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MANS",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        color = Color.White
                    )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "POWER",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        color = SportYellow,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = s.appSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NeonCyan.copy(alpha = 0.9f),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(Modifier.height(32.dp))

            // Vertical Jump Card (Loncat Tegak)
            JumpTypeCard(
                title       = s.verticalJumpTitle,
                subtitle    = s.verticalJumpSubtitle,
                description = s.verticalJumpDesc,
                emoji       = "⬆️",
                accentColor = SportCrimson,
                secondaryAccent = SportYellow,
                onClick     = { onNavigateToCategory("VERTICAL") }
            )

            Spacer(Modifier.height(16.dp))

            // Horizontal Jump Card (Lompat Jauh)
            JumpTypeCard(
                title       = s.horizontalJumpTitle,
                subtitle    = s.horizontalJumpSubtitle,
                description = s.horizontalJumpDesc,
                emoji       = "➡️",
                accentColor = SportGreen,
                secondaryAccent = NeonCyan,
                onClick     = { onNavigateToCategory("HORIZONTAL") }
            )

            Spacer(Modifier.height(20.dp))

            // History Button
            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x9911162B),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = SportYellow)
                Spacer(Modifier.width(8.dp))
                Text(
                    s.viewHistory,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Language Toggle Button (elevated top-right corner, safe from notification bar) ──
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .zIndex(20f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SportYellow.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .background(Color(0xE6101424))
                .clickable { LanguageManager.toggle() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = s.langToggleLabel,
                    color = SportYellow,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun JumpTypeCard(
    title: String,
    subtitle: String,
    description: String,
    emoji: String,
    accentColor: Color,
    secondaryAccent: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(80),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(accentColor, secondaryAccent.copy(alpha = 0.5f))),
                RoundedCornerShape(22.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.18f),
                        Color(0xE6101426)
                    )
                )
            )
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = secondaryAccent,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    JumpMeasureTheme {
        MainScreen(onNavigateToCategory = {}, onNavigateToHistory = {})
    }
}
