package com.manspower.jumpmeasure.ui.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manspower.jumpmeasure.domain.model.JumpCategory
import com.manspower.jumpmeasure.theme.SportCrimson
import com.manspower.jumpmeasure.theme.SportGreen
import com.manspower.jumpmeasure.theme.SportYellow
import com.manspower.jumpmeasure.ui.components.AthleticBackground
import com.manspower.jumpmeasure.ui.language.LocalAppStrings

import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex

@Composable
fun CategoryScreen(
    jumpType: String,
    onCategorySelected: (String, JumpCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val jumpLabel   = if (jumpType == "VERTICAL") s.jumpLabelVertical else s.jumpLabelHorizontal
    val accentColor = if (jumpType == "VERTICAL") SportCrimson else SportGreen
    val scrollState = rememberScrollState()

    BackHandler(enabled = true) {
        onBack()
    }

    AthleticBackground(modifier = modifier) {
        // Main Column (Scrollable for landscape and portrait)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // Header
            Text(
                text = jumpLabel.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 3.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.selectCategory,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.categoryScreenSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // Category buttons — SMP, SMA, MAHASISWA, ATLET
            val categories = listOf(
                Triple(JumpCategory.SMP,       "🏫", s.categoryAgeSMP),
                Triple(JumpCategory.SMA,       "🏛️", s.categoryAgeSMA),
                Triple(JumpCategory.MAHASISWA, "🎓", s.categoryAgeMahasiswa),
                Triple(JumpCategory.ATLET,     "🏅", s.categoryAgeAtlet)
            )

            categories.forEach { (category, emoji, subtitle) ->
                CategoryButton(
                    label    = s.categoryName(category),
                    emoji    = emoji,
                    subtitle = subtitle,
                    accent   = accentColor,
                    onClick  = { onCategorySelected(jumpType, category) }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
        }

        // Back button (elevated on top of column, safely below notification bar)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
                .zIndex(20f)
                .background(Color(0xCC121626), shape = androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CategoryButton(
    label: String,
    emoji: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(80),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), SportYellow.copy(alpha = 0.4f))),
                RoundedCornerShape(20.dp)
            )
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xE612162A), Color(0xCC0E111F))
                )
            )
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SportYellow.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
