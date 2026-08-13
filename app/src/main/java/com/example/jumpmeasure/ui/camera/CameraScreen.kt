package com.example.jumpmeasure.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jumpmeasure.core.vision.BodyLandmark
import com.example.jumpmeasure.domain.model.JumpCategory
import com.example.jumpmeasure.domain.model.JumpGender
import com.example.jumpmeasure.domain.model.NormaEvaluator
import com.example.jumpmeasure.theme.SkeletonGreen
import com.example.jumpmeasure.theme.NeonPurple
import com.example.jumpmeasure.theme.NeonCyan
import com.example.jumpmeasure.core.analytics.JumpPhase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.jumpmeasure.ui.language.LocalAppStrings
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Instruction Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InstructionDialog(
    jumpType: String,
    category: JumpCategory,
    gender: JumpGender,
    onDismiss: () -> Unit
) {
    val s           = LocalAppStrings.current
    val accentColor = if (jumpType == "VERTICAL") NeonPurple else NeonCyan
    val jumpLabel   = if (jumpType == "VERTICAL") s.jumpLabelVertical else s.jumpLabelHorizontal
    val instructions = if (jumpType == "VERTICAL") s.verticalInstructions else s.horizontalInstructions
    val scrollState = rememberScrollState()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = if (jumpType == "VERTICAL") "⬆️" else "➡️",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text  = "${s.guidancePrefix} $jumpLabel",
                            color = accentColor,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black
                            )
                        )
                        Text(
                            text  = "${s.categoryName(category)} · ${s.genderName(gender)}",
                            color = Color.Gray,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                instructions.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = accentColor.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text     = item.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                        )
                        Column {
                            Text(
                                text  = item.title,
                                color = accentColor,
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text       = item.desc,
                                color      = Color.White.copy(alpha = 0.85f),
                                style      = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Bottom tip
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A40), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text       = s.guideTipText,
                        color      = Color(0xFFFFD700),
                        style      = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        textAlign  = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors  = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor   = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    s.guideUnderstoodBtn,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        containerColor   = Color(0xFF16162A),
        textContentColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CameraScreen(
    jumpType: String = "VERTICAL",
    category: JumpCategory = JumpCategory.SMP,
    gender: JumpGender = JumpGender.PUTRA,
    cameraDistance: Float = 3.0f,
    modifier: Modifier = Modifier,
    viewModel: MeasurementViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val s       = LocalAppStrings.current
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var showInstructions by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(viewModel.saveMessage) {
        viewModel.saveMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreviewContent(
                viewModel, jumpType, category, gender, cameraDistance, onBack,
                Modifier.fillMaxSize()
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )

            // Instruction popup — shown before tracking starts
            if (showInstructions) {
                InstructionDialog(
                    jumpType = jumpType,
                    category = category,
                    gender   = gender,
                    onDismiss = { showInstructions = false }
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(text = s.cameraPermissionRequired, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraPreviewContent
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraPreviewContent(
    viewModel: MeasurementViewModel,
    jumpType: String,
    category: JumpCategory,
    gender: JumpGender,
    cameraDistance: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s              = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val poseState         by viewModel.poseState.collectAsStateWithLifecycle()
    val jumpPhase         by viewModel.jumpPhase.collectAsStateWithLifecycle()
    val jumpMetrics       by viewModel.jumpMetrics.collectAsStateWithLifecycle()
    val trackingState     by viewModel.trackingState.collectAsStateWithLifecycle()
    val countdownValue    by viewModel.countdownValue.collectAsStateWithLifecycle()
    val pendingJumpResult by viewModel.pendingJumpResult.collectAsStateWithLifecycle()

    val accentColor = if (jumpType == "VERTICAL") NeonPurple else NeonCyan

    Box(modifier = modifier.fillMaxSize()) {

        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                viewModel.startCamera(previewView.surfaceProvider, lifecycleOwner, jumpType, cameraDistance)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Skeleton Overlay
        if (trackingState == TrackingState.TRACKING) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val result    = poseState ?: return@Canvas
                val landmarks = result.landmarks

                val frameWidth  = result.imageWidth.toFloat()
                val frameHeight = result.imageHeight.toFloat()
                if (frameWidth <= 0f || frameHeight <= 0f) return@Canvas

                val scaleFactor  = maxOf(size.width / frameWidth, size.height / frameHeight)
                val scaledWidth  = frameWidth * scaleFactor
                val scaledHeight = frameHeight * scaleFactor
                val offsetX      = (size.width - scaledWidth) / 2f
                val offsetY      = (size.height - scaledHeight) / 2f

                fun normalizedToOffset(nx: Double, ny: Double) = Offset(
                    x = offsetX + (nx.toFloat() * scaledWidth),
                    y = offsetY + (ny.toFloat() * scaledHeight)
                )

                fun landmarkToOffset(lm: com.example.jumpmeasure.core.vision.PoseLandmarkSample) =
                    normalizedToOffset(lm.normalizedX, lm.normalizedY)

                fun drawBone(start: BodyLandmark, end: BodyLandmark) {
                    val s = landmarks[start] ?: return
                    val e = landmarks[end]   ?: return
                    if ((s.presence ?: 0.0) > 0.5 && (e.presence ?: 0.0) > 0.5) {
                        drawLine(SkeletonGreen, landmarkToOffset(s), landmarkToOffset(e), strokeWidth = 8f)
                        drawCircle(SkeletonGreen, radius = 6f, center = landmarkToOffset(s))
                        drawCircle(SkeletonGreen, radius = 6f, center = landmarkToOffset(e))
                    }
                }

                drawBone(BodyLandmark.LEFT_HIP,    BodyLandmark.LEFT_KNEE)
                drawBone(BodyLandmark.LEFT_KNEE,   BodyLandmark.LEFT_ANKLE)
                drawBone(BodyLandmark.LEFT_ANKLE,  BodyLandmark.LEFT_FOOT_INDEX)
                drawBone(BodyLandmark.RIGHT_HIP,   BodyLandmark.RIGHT_KNEE)
                drawBone(BodyLandmark.RIGHT_KNEE,  BodyLandmark.RIGHT_ANKLE)
                drawBone(BodyLandmark.RIGHT_ANKLE, BodyLandmark.RIGHT_FOOT_INDEX)
                drawBone(BodyLandmark.LEFT_HIP,    BodyLandmark.RIGHT_HIP)
                drawBone(BodyLandmark.LEFT_SHOULDER,  BodyLandmark.LEFT_HIP)
                drawBone(BodyLandmark.RIGHT_SHOULDER, BodyLandmark.RIGHT_HIP)
                drawBone(BodyLandmark.LEFT_SHOULDER,  BodyLandmark.RIGHT_SHOULDER)

                // Takeoff & Landing markers for horizontal jump
                if (jumpType == "HORIZONTAL") {
                    jumpMetrics?.let { metrics ->
                        val groundNY = landmarks[BodyLandmark.LEFT_HEEL]?.normalizedY ?: 0.9
                        if (metrics.takeoffNormalizedX > 0.0) {
                            drawCircle(
                                color  = NeonPurple.copy(alpha = 0.6f),
                                radius = 22f,
                                center = normalizedToOffset(metrics.takeoffNormalizedX, groundNY)
                            )
                        }
                        if (metrics.landingNormalizedX > 0.0) {
                            drawCircle(
                                color  = NeonCyan.copy(alpha = 0.6f),
                                radius = 22f,
                                center = normalizedToOffset(metrics.landingNormalizedX, groundNY)
                            )
                        }
                    }
                }
            }
        }

        // ── Top Metrics HUD ──
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(14.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = "${s.categoryName(category)} · ${s.genderName(gender)}",
                color = accentColor,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${s.phaseLabel} ${jumpPhase.name}",
                color = if (jumpPhase == JumpPhase.AIRBORNE) NeonPurple else NeonCyan,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
            )

            jumpMetrics?.let { metrics ->
                Spacer(Modifier.height(6.dp))
                if (jumpType == "VERTICAL") {
                    Text(
                        text  = "${s.durationLabel} ${metrics.flightTimeMs} ms",
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text  = String.format(Locale.US, "${s.heightLabel} %.2f cm", metrics.jumpHeightCm),
                        color = NeonPurple,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Text(
                        text  = "${s.durationLabel} ${metrics.flightTimeMs} ms",
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text  = String.format(Locale.US, "${s.distanceLabel} %.2f cm", metrics.horizontalDistanceCm),
                        color = NeonCyan,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    )
                    if (metrics.jumpAngleDegrees > 0.0) {
                        Text(
                            text  = String.format(Locale.US, "${s.angleLabel} %.1f°", metrics.jumpAngleDegrees),
                            color = Color.Yellow,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = String.format(Locale.US, "${s.confidenceLabel} %.1f%%", metrics.confidenceScore * 100),
                    color = if (metrics.confidenceScore > 0.8) SkeletonGreen else Color.Yellow,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Back Button ──
        androidx.compose.material3.IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // ── Action Buttons ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            when (trackingState) {
                TrackingState.IDLE -> {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.startTrackingWithCountdown() },
                        colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            s.startTracking,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                }
                TrackingState.COUNTDOWN -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = countdownValue.toString(),
                            style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                            color = NeonCyan
                        )
                    }
                }
                TrackingState.TRACKING -> {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.pauseTracking() },
                        colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow.copy(alpha = 0.85f))
                    ) {
                        Text(s.pauseBtn, color = Color.Black, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    }
                }
                TrackingState.PAUSED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.resumeTracking() },
                            colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text(s.resumeBtn, color = Color.Black, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        }
                        androidx.compose.material3.Button(
                            onClick = { viewModel.stopTracking() },
                            colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text(s.stopBtn, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // ── Trajectory / Parabola Overlay ──
        if (jumpType == "HORIZONTAL" && pendingJumpResult != null) {
            TrajectoryOverlay(metrics = pendingJumpResult!!)
        }

        // ── Result Dialog ──
        if (pendingJumpResult != null) {
            val metrics = pendingJumpResult!!
            val valueCm = if (jumpType == "VERTICAL") metrics.jumpHeightCm else metrics.horizontalDistanceCm
            val norma   = NormaEvaluator.evaluate(valueCm, jumpType, category, gender)
            val normaDisplayLabel = s.translateNorma(norma.label)

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { /* block */ },
                title = {
                    Column {
                        Text(
                            s.jumpDetectedTitle,
                            color = accentColor,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${s.categoryName(category)} · ${s.genderName(gender)}",
                            color = Color.Gray,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (jumpType == "VERTICAL") {
                            Text(
                                "${s.heightLabel} ${String.format(Locale.US, "%.2f", metrics.jumpHeightCm)} cm",
                                color = NeonPurple,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                "${s.distanceLabel} ${String.format(Locale.US, "%.2f", metrics.horizontalDistanceCm)} cm",
                                color = NeonCyan,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            if (metrics.jumpAngleDegrees > 0.0) {
                                Text(
                                    "${s.jumpAngleLabel} ${String.format(Locale.US, "%.1f", metrics.jumpAngleDegrees)}°",
                                    color = Color.Yellow,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Text(
                            "${s.flightDurationLabel} ${metrics.flightTimeMs} ms",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${s.confidenceLabel} ${String.format(Locale.US, "%.1f", metrics.confidenceScore * 100)}%",
                            color = Color.Gray,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )

                        // Norma result box
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = when (norma.label) {
                                        "Sangat Baik", "Elite / Internasional" -> NeonPurple.copy(alpha = 0.2f)
                                        "Baik", "Sangat Tinggi", "Tinggi (Nasional)" -> NeonCyan.copy(alpha = 0.2f)
                                        "Cukup", "Sedang" -> Color.Yellow.copy(alpha = 0.15f)
                                        else -> Color.Red.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(norma.emoji, fontSize = 28.sp)
                                Text(
                                    normaDisplayLabel,
                                    color = Color.White,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    s.normaAssessment,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.savePendingJump(jumpType, category, gender) }
                    ) {
                        Text(s.saveBtn, color = accentColor)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.discardPendingJump() }
                    ) {
                        Text(s.discardBtn, color = Color.Red)
                    }
                },
                containerColor   = Color(0xFF1C1C2E),
                textContentColor = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TrajectoryOverlay (Parabola Animation)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrajectoryOverlay(metrics: com.example.jumpmeasure.core.analytics.JumpMetrics) {
    // We only animate the path drawing from 0f to 1f over 1 second.
    val progress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1000, 
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "parabola_anim"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val startX = (metrics.takeoffNormalizedX * w).toFloat()
        val startY = (metrics.takeoffNormalizedY * h).toFloat()
        val endX   = (metrics.landingNormalizedX * w).toFloat()
        val endY   = (metrics.landingNormalizedY * h).toFloat()

        // To make it look like a parabola, the control point is halfway horizontally,
        // and raised vertically. We approximate the height visually.
        // A standard horizontal jump peaks halfway through.
        val controlX = (startX + endX) / 2f
        // Peak is above the start/end points. The higher the flight time, the higher it goes.
        // Or we can just use a fixed proportion of the distance (e.g. 30% of distance).
        val distanceX = Math.abs(endX - startX)
        val peakHeight = distanceX * 0.4f 
        
        // Base Y is the average of start and end Y (to account for slight tilt)
        val baseY = (startY + endY) / 2f
        val controlY = baseY - (peakHeight * 2f) // control point needs to be 2x the actual peak for quadratic bezier

        val path = Path().apply {
            moveTo(startX, startY)
            quadraticBezierTo(controlX, controlY, endX, endY)
        }

        // We use PathMeasure to draw only a portion of the path based on animation progress
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, forceClosed = false)
        val length = pathMeasure.length
        
        val animatedPath = Path()
        pathMeasure.getSegment(
            startDistance = 0f,
            stopDistance = length * progress.value,
            destination = animatedPath,
            startWithMoveTo = true
        )

        drawPath(
            path = animatedPath,
            color = Color.Yellow,
            style = Stroke(
                width = 8f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )

        // Draw start and current point circles
        drawCircle(
            color = Color.Yellow,
            radius = 12f,
            center = Offset(startX, startY)
        )

        if (progress.value > 0f) {
            // Get current point on path
            val pos = pathMeasure.getPosition(length * progress.value)
            if (pos != Offset.Unspecified) {
                drawCircle(
                    color = NeonCyan,
                    radius = 16f,
                    center = pos
                )
            }
        }
    }
}
