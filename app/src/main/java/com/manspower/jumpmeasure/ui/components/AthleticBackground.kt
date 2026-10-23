package com.manspower.jumpmeasure.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import com.manspower.jumpmeasure.theme.SportCrimson
import com.manspower.jumpmeasure.theme.SportGreen
import com.manspower.jumpmeasure.theme.SportYellow

/**
 * Athletic colorful background inspired by the dynamic sports ribbons (Red, Green, Yellow, Black/Dark)
 * from the reference image, rendered behind screens while maintaining excellent text contrast.
 */
@Composable
fun AthleticBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090B14))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Deep Athletic ambient gradients
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(SportCrimson.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.20f),
                    radius = w * 0.75f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(SportGreen.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.35f),
                    radius = w * 0.70f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(SportYellow.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.15f),
                    radius = w * 0.55f
                )
            )

            // 2. Dynamic Sport Crimson Red Ribbon (Left wing swoosh)
            val redPath = Path().apply {
                moveTo(-20f, h * 0.05f)
                cubicTo(
                    w * 0.05f, h * 0.22f,
                    w * 0.32f, h * 0.38f,
                    w * 0.52f, h * 0.34f
                )
                cubicTo(
                    w * 0.38f, h * 0.28f,
                    w * 0.15f, h * 0.16f,
                    w * 0.05f, -20f
                )
                close()
            }
            drawPath(
                path = redPath,
                brush = Brush.linearGradient(
                    colors = listOf(SportCrimson.copy(alpha = 0.85f), Color(0xFFFF4B6E).copy(alpha = 0.65f)),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.5f, h * 0.35f)
                ),
                style = Fill
            )

            // 3. Dynamic Golden Yellow Ribbon (Upper right swoosh)
            val yellowPath = Path().apply {
                moveTo(w * 0.55f, h * 0.02f)
                cubicTo(
                    w * 0.68f, h * 0.08f,
                    w * 0.88f, h * 0.02f,
                    w * 1.05f, -20f
                )
                lineTo(w * 1.05f, h * 0.14f)
                cubicTo(
                    w * 0.85f, h * 0.22f,
                    w * 0.65f, h * 0.18f,
                    w * 0.55f, h * 0.02f
                )
                close()
            }
            drawPath(
                path = yellowPath,
                brush = Brush.linearGradient(
                    colors = listOf(SportYellow.copy(alpha = 0.90f), Color(0xFFFF9500).copy(alpha = 0.75f)),
                    start = Offset(w * 0.55f, 0f),
                    end = Offset(w, h * 0.2f)
                ),
                style = Fill
            )

            // 4. Dynamic Sport Green Ribbon (Right ascending wing)
            val greenPath = Path().apply {
                moveTo(w * 0.50f, h * 0.35f)
                cubicTo(
                    w * 0.72f, h * 0.30f,
                    w * 0.92f, h * 0.18f,
                    w * 1.05f, h * 0.05f
                )
                lineTo(w * 1.05f, h * 0.28f)
                cubicTo(
                    w * 0.85f, h * 0.38f,
                    w * 0.65f, h * 0.42f,
                    w * 0.50f, h * 0.35f
                )
                close()
            }
            drawPath(
                path = greenPath,
                brush = Brush.linearGradient(
                    colors = listOf(SportGreen.copy(alpha = 0.88f), Color(0xFF10B981).copy(alpha = 0.70f)),
                    start = Offset(w * 0.5f, h * 0.35f),
                    end = Offset(w, h * 0.1f)
                ),
                style = Fill
            )

            // 5. Dynamic Athletic Carbon Swoosh (Central bridging sweep like in reference)
            val blackPath = Path().apply {
                moveTo(w * 0.08f, h * 0.36f)
                cubicTo(
                    w * 0.32f, h * 0.44f,
                    w * 0.60f, h * 0.48f,
                    w * 0.78f, h * 0.38f
                )
                cubicTo(
                    w * 0.58f, h * 0.54f,
                    w * 0.28f, h * 0.52f,
                    w * 0.02f, h * 0.42f
                )
                close()
            }
            drawPath(
                path = blackPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1D2E).copy(alpha = 0.95f), Color(0xFF0F111B).copy(alpha = 0.85f)),
                    start = Offset(0f, h * 0.35f),
                    end = Offset(w * 0.8f, h * 0.55f)
                ),
                style = Fill
            )

            // Bottom accent swoosh
            val bottomRibbon = Path().apply {
                moveTo(-20f, h * 0.88f)
                cubicTo(
                    w * 0.25f, h * 0.82f,
                    w * 0.65f, h * 0.90f,
                    w * 1.05f, h * 0.80f
                )
                lineTo(w * 1.05f, h + 20f)
                lineTo(-20f, h + 20f)
                close()
            }
            drawPath(
                path = bottomRibbon,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E1324).copy(alpha = 0.5f), Color(0xFF080B15).copy(alpha = 0.95f)),
                    startY = h * 0.8f,
                    endY = h
                ),
                style = Fill
            )

            // 6. Subtle dark vignette overlay to ensure pristine UI text readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x990A0D18),
                        Color(0x660A0D18),
                        Color(0xB3090B14)
                    )
                )
            )
        }

        // Screen content
        content()
    }
}
