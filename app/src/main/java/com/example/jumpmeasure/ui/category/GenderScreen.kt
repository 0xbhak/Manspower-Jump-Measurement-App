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
import com.example.jumpmeasure.domain.model.JumpGender
import com.example.jumpmeasure.theme.DeepSpace
import com.example.jumpmeasure.theme.NeonCyan
import com.example.jumpmeasure.theme.NeonPurple
import com.example.jumpmeasure.ui.language.LocalAppStrings

@Composable
fun GenderScreen(
    jumpType: String,
    category: JumpCategory,
    onGenderSelected: (String, JumpCategory, JumpGender) -> Unit,
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
            // Breadcrumb
            Text(
                text = "$jumpLabel · ${s.categoryName(category)}",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = accentColor.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = s.selectGender,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.genderScreenSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.55f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Male button
            GenderButton(
                label   = s.genderMale,
                emoji   = "♂",
                accent  = NeonPurple,
                onClick = { onGenderSelected(jumpType, category, JumpGender.PUTRA) }
            )
            Spacer(Modifier.height(20.dp))
            // Female button
            GenderButton(
                label   = s.genderFemale,
                emoji   = "♀",
                accent  = NeonCyan,
                onClick = { onGenderSelected(jumpType, category, JumpGender.PUTRI) }
            )
        }
    }
}

@Composable
private fun GenderButton(
    label: String,
    emoji: String,
    accent: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(80),
        label = "gender_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, accent, RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.18f), Color.Transparent)
                )
            )
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 40.sp, color = accent)
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }
    }
}
