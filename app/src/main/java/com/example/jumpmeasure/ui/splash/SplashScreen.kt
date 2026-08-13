package com.example.jumpmeasure.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.jumpmeasure.theme.NeonCyan
import com.example.jumpmeasure.theme.NeonPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(targetValue = 1.2f, animationSpec = tween(800))
        scale.animateTo(targetValue = 1f,   animationSpec = tween(400))
        delay(300)
        onNavigateToMain()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Text(
                text = "MANS",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp
                )
            )
            Text(
                text = "PORT",
                style = TextStyle(
                    color = NeonPurple,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp
                )
            )
            Text(
                text = "AI Jump Measurement",
                style = TextStyle(
                    color = NeonCyan.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
            )
        }
    }
}

