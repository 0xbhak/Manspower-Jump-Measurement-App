package com.example.jumpmeasure.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jumpmeasure.theme.DeepSpace
import com.example.jumpmeasure.theme.JumpMeasureTheme
import com.example.jumpmeasure.theme.NeonCyan
import com.example.jumpmeasure.theme.NeonPurple
import com.example.jumpmeasure.ui.language.LanguageManager
import com.example.jumpmeasure.ui.language.LocalAppStrings

@Composable
fun MainScreen(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF111128), Color(0xFF0D0D1A))
                )
            )
    ) {
        // ── Language Toggle Button (top-right corner) ──────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .background(NeonCyan.copy(alpha = 0.08f))
                .clickable { LanguageManager.toggle() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = s.langToggleLabel,
                    color = NeonCyan,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // ── Main Content Column ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Name (branding stays the same in both languages)
            Text(
                text = "MANS",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 60.sp,
                    color = Color.White
                )
            )
            Text(
                text = "PORT",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 60.sp,
                    color = NeonPurple,
                    letterSpacing = 4.sp
                )
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = s.appSubtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = NeonCyan.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(Modifier.height(48.dp))

            // Vertical Jump Card
            JumpTypeCard(
                title       = s.verticalJumpTitle,
                subtitle    = s.verticalJumpSubtitle,
                description = s.verticalJumpDesc,
                emoji       = "⬆️",
                accentColor = NeonPurple,
                onClick     = { onNavigateToCategory("VERTICAL") }
            )

            Spacer(Modifier.height(18.dp))

            // Horizontal Jump Card
            JumpTypeCard(
                title       = s.horizontalJumpTitle,
                subtitle    = s.horizontalJumpSubtitle,
                description = s.horizontalJumpDesc,
                emoji       = "➡️",
                accentColor = NeonCyan,
                onClick     = { onNavigateToCategory("HORIZONTAL") }
            )

            Spacer(Modifier.height(24.dp))

            // History Button
            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color.LightGray)
                Spacer(Modifier.width(8.dp))
                Text(
                    s.viewHistory,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.LightGray
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
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.14f), Color(0xFF0D0D1A))
                )
            )
            .clickable {
                pressed = true
                onClick()
            }
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 44.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
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
