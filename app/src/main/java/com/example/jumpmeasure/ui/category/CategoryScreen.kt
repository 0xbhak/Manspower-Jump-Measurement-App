package com.example.jumpmeasure.ui.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.jumpmeasure.domain.model.JumpCategory
import com.example.jumpmeasure.theme.DeepSpace
import com.example.jumpmeasure.theme.NeonCyan
import com.example.jumpmeasure.theme.NeonPurple
import com.example.jumpmeasure.ui.language.LocalAppStrings

@Composable
fun CategoryScreen(
    jumpType: String,
    onCategorySelected: (String, JumpCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val jumpLabel   = if (jumpType == "VERTICAL") s.jumpLabelVertical else s.jumpLabelHorizontal
    val accentColor = if (jumpType == "VERTICAL") NeonPurple else NeonCyan

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = jumpLabel.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 3.sp
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.selectCategory,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.categoryScreenSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Category buttons — subtitles and labels from AppStrings
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
                Spacer(Modifier.height(14.dp))
            }
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
            .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.12f), Color.Transparent)
                )
            )
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accent.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}
