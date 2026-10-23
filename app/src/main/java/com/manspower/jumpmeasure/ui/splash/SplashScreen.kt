package com.manspower.jumpmeasure.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manspower.jumpmeasure.R
import com.manspower.jumpmeasure.theme.NeonCyan
import com.manspower.jumpmeasure.theme.SportYellow
import com.manspower.jumpmeasure.ui.components.AthleticBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(targetValue = 1.15f, animationSpec = tween(750))
        scale.animateTo(targetValue = 1f,    animationSpec = tween(350))
        delay(400)
        onNavigateToMain()
    }

    AthleticBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(scale.value)
            ) {
                // New App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Manspower Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(2.dp, SportYellow.copy(alpha = 0.8f), RoundedCornerShape(26.dp))
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "MANS",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                )
                Text(
                    text = "POWER",
                    style = TextStyle(
                        color = SportYellow,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Digital Jump Measurement",
                    style = TextStyle(
                        color = NeonCyan.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )
                )
            }
        }
    }
}
