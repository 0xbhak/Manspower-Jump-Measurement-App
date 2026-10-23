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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
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
import com.manspower.jumpmeasure.domain.model.JumpGender
import com.manspower.jumpmeasure.theme.NeonCyan
import com.manspower.jumpmeasure.theme.SportCrimson
import com.manspower.jumpmeasure.theme.SportGreen
import com.manspower.jumpmeasure.theme.SportYellow
import com.manspower.jumpmeasure.ui.components.AthleticBackground
import com.manspower.jumpmeasure.ui.language.LocalAppStrings

import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex

@Composable
fun GenderScreen(
    jumpType: String,
    category: JumpCategory,
    onGenderSelected: (String, JumpCategory, JumpGender, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val jumpLabel   = if (jumpType == "VERTICAL") s.jumpLabelVertical else s.jumpLabelHorizontal
    val accentColor = if (jumpType == "VERTICAL") SportCrimson else SportGreen
    var participantName by remember { mutableStateOf("") }
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

            // Breadcrumb
            Text(
                text = "$jumpLabel · ${s.categoryName(category)}",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = SportYellow,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.selectGender,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.genderScreenSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Participant Name Input
            OutlinedTextField(
                value = participantName,
                onValueChange = { participantName = it },
                label = { Text(s.participantNameLabel, color = Color.White.copy(alpha = 0.85f)) },
                placeholder = { Text(s.participantNameHint, color = Color.White.copy(alpha = 0.45f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Participant",
                        tint = SportYellow
                    )
                },
                trailingIcon = {
                    if (participantName.isNotEmpty()) {
                        IconButton(onClick = { participantName = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SportYellow,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    focusedLabelColor = SportYellow,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                    cursorColor = SportYellow
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Male button (Putra)
            GenderButton(
                label   = s.genderMale,
                emoji   = "♂",
                accent  = SportCrimson,
                onClick = { onGenderSelected(jumpType, category, JumpGender.PUTRA, participantName) }
            )
            Spacer(Modifier.height(14.dp))
            // Female button (Putri)
            GenderButton(
                label   = s.genderFemale,
                emoji   = "♀",
                accent  = NeonCyan,
                onClick = { onGenderSelected(jumpType, category, JumpGender.PUTRI, participantName) }
            )

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
            .height(84.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(accent, SportYellow.copy(alpha = 0.5f))),
                RoundedCornerShape(22.dp)
            )
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xE612162A), Color(0xCC0E111F))
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
            Text(text = emoji, fontSize = 34.sp, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 24.sp
                )
            )
        }
    }
}
