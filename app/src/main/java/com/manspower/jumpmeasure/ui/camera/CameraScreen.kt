package com.manspower.jumpmeasure.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manspower.jumpmeasure.core.vision.BodyLandmark
import com.manspower.jumpmeasure.domain.model.JumpCategory
import com.manspower.jumpmeasure.domain.model.JumpGender
import com.manspower.jumpmeasure.domain.model.NormaEvaluator
import com.manspower.jumpmeasure.domain.model.NormaThreshold
import com.manspower.jumpmeasure.core.geometry.CoordinateTransformer
import com.manspower.jumpmeasure.theme.SkeletonGreen
import com.manspower.jumpmeasure.theme.NeonPurple
import com.manspower.jumpmeasure.theme.NeonCyan
import com.manspower.jumpmeasure.core.analytics.JumpPhase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.manspower.jumpmeasure.ui.language.LocalAppStrings
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
                    .heightIn(max = if (isLandscape) 180.dp else 400.dp)
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
    initialParticipantName: String = "Peserta",
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

    LaunchedEffect(initialParticipantName) {
        viewModel.updateParticipantName(initialParticipantName)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(viewModel.saveMessage) {
        viewModel.saveMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = true) {
        viewModel.stopCamera()
        onBack()
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
    val configuration  = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape    = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val poseState              by viewModel.poseState.collectAsStateWithLifecycle()
    val jumpPhase              by viewModel.jumpPhase.collectAsStateWithLifecycle()
    val jumpMetrics            by viewModel.jumpMetrics.collectAsStateWithLifecycle()
    val trackingState          by viewModel.trackingState.collectAsStateWithLifecycle()
    val countdownValue         by viewModel.countdownValue.collectAsStateWithLifecycle()
    val pendingJumpResult      by viewModel.pendingJumpResult.collectAsStateWithLifecycle()
    val currentParticipantName by viewModel.participantName.collectAsStateWithLifecycle()
    val transformer            by viewModel.transformerState.collectAsStateWithLifecycle()
    val groundBaselineY        by viewModel.groundBaselineYState.collectAsStateWithLifecycle()
    val takeoffX               by viewModel.takeoffXState.collectAsStateWithLifecycle()

    var showNormaGuides     by remember { mutableStateOf(true) }
    var showEditNameDialog  by remember { mutableStateOf(false) }
    var editNameInput       by remember { mutableStateOf("") }

    val accentColor = if (jumpType == "VERTICAL") NeonPurple else NeonCyan

    DisposableEffect(lifecycleOwner) {
        onDispose {
            viewModel.stopCamera()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                viewModel.startCamera(previewView.surfaceProvider, lifecycleOwner, jumpType, cameraDistance)
                previewView
            },
            update = { previewView ->
                // Memastikan stream kamera tetap berjalan dan terikat saat orientasi layar berputar
                viewModel.startCamera(previewView.surfaceProvider, lifecycleOwner, jumpType, cameraDistance)
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Norma Guides Overlay (Meter/CM visual patokan) ──
        if (showNormaGuides) {
            val currentVal = if (jumpType == "VERTICAL") jumpMetrics?.jumpHeightCm else jumpMetrics?.horizontalDistanceCm
            NormaGuideOverlay(
                jumpType = jumpType,
                category = category,
                gender = gender,
                groundBaselineY = groundBaselineY,
                takeoffX = takeoffX,
                transformer = transformer,
                cameraDistance = cameraDistance,
                currentJumpValueCm = currentVal,
                poseState = poseState
            )
        }

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

                fun landmarkToOffset(lm: com.manspower.jumpmeasure.core.vision.PoseLandmarkSample) =
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

        // ── Top / Side Metrics HUD ──
        if (isLandscape) {
            // ── LANDSCAPE MODE: Thin sidebar on the right side of screen ──
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
                    .width(175.dp)
                    .background(Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Participant name & edit button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "👤 $currentParticipantName",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    androidx.compose.material3.IconButton(
                        onClick = {
                            editNameInput = currentParticipantName
                            showEditNameDialog = true
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = "${s.categoryName(category)} · ${s.genderName(gender)}",
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(6.dp))

                // Status Fase Box (Thin for landscape, doesn't block screen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = when (jumpPhase) {
                                JumpPhase.AIRBORNE -> NeonPurple.copy(alpha = 0.35f)
                                JumpPhase.PREPARATION -> Color.Yellow.copy(alpha = 0.3f)
                                JumpPhase.LANDING -> SkeletonGreen.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.12f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${s.phaseLabel} ${jumpPhase.name}",
                        color = when (jumpPhase) {
                            JumpPhase.AIRBORNE -> NeonPurple
                            JumpPhase.PREPARATION -> Color.Yellow
                            JumpPhase.LANDING -> SkeletonGreen
                            else -> NeonCyan
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                jumpMetrics?.let { metrics ->
                    Spacer(Modifier.height(6.dp))
                    if (jumpType == "VERTICAL") {
                        Text(
                            text = String.format(Locale.US, "%.1f cm", metrics.jumpHeightCm),
                            color = NeonPurple,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Text(
                            text = String.format(Locale.US, "%.1f cm", metrics.horizontalDistanceCm),
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "${metrics.flightTimeMs} ms",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "${s.confidenceLabel} %.1f%%", metrics.confidenceScore * 100),
                        color = if (metrics.confidenceScore > 0.8) SkeletonGreen else Color.Yellow,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Action buttons integrated neatly inside right panel in landscape mode!
                when (trackingState) {
                    TrackingState.IDLE -> {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.startTrackingWithCountdown() },
                            colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        ) {
                            Text(
                                s.startTracking,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    TrackingState.COUNTDOWN -> {
                        Text(
                            text = countdownValue.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan
                        )
                    }
                    TrackingState.TRACKING -> {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.pauseTracking() },
                            colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Yellow.copy(alpha = 0.85f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        ) {
                            Text(s.pauseBtn, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TrackingState.PAUSED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Button(
                                onClick = { viewModel.resumeTracking() },
                                colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(30.dp)
                            ) {
                                Text(s.resumeBtn, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            androidx.compose.material3.Button(
                                onClick = { viewModel.stopTracking() },
                                colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(30.dp)
                            ) {
                                Text(s.stopBtn, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // ── PORTRAIT MODE: Top Center HUD (safe from notification bar) ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .padding(horizontal = 64.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Participant name & quick edit button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text  = "👤 $currentParticipantName",
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.IconButton(
                        onClick = {
                            editNameInput = currentParticipantName
                            showEditNameDialog = true
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "${s.categoryName(category)} · ${s.genderName(gender)}",
                    color = accentColor.copy(alpha = 0.85f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
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

            // Action Buttons for Portrait
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 64.dp)
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
        }

        // ── Back Button (safe from notification bar) ──
        androidx.compose.material3.IconButton(
            onClick = {
                viewModel.stopCamera()
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
                .zIndex(20f)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // ── Guide Lines Toggle Button (safe from notification bar) ──
        androidx.compose.material3.IconButton(
            onClick = { showNormaGuides = !showNormaGuides },
            modifier = Modifier
                .align(if (isLandscape) Alignment.TopStart else Alignment.TopEnd)
                .statusBarsPadding()
                .padding(
                    start = if (isLandscape) 68.dp else 0.dp,
                    end = if (isLandscape) 0.dp else 16.dp,
                    top = 16.dp
                )
                .zIndex(20f)
                .background(
                    if (showNormaGuides) accentColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.55f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .border(
                    1.dp,
                    if (showNormaGuides) accentColor else Color.White.copy(alpha = 0.3f),
                    androidx.compose.foundation.shape.CircleShape
                )
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (showNormaGuides) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = s.normaGuideToggleLabel,
                tint = if (showNormaGuides) accentColor else Color.White.copy(alpha = 0.6f)
            )
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
                modifier = Modifier
                    .widthIn(max = if (isLandscape) 420.dp else 520.dp)
                    .heightIn(max = if (isLandscape) 290.dp else 580.dp),
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isLandscape) 170.dp else 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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

                        // Participant Name display
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "👤 ${s.participantNameLabel}: ",
                                color = Color.LightGray,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                            Text(
                                currentParticipantName,
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.savePendingJump(currentParticipantName, jumpType, category, gender) }
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

        // ── Quick Edit Participant Dialog ──
        if (showEditNameDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = {
                    Text(
                        s.participantNameDialogTitle,
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text(s.participantNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = accentColor,
                            cursorColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val newName = if (editNameInput.isNotBlank()) editNameInput.trim() else "Peserta"
                            viewModel.updateParticipantName(newName)
                            showEditNameDialog = false
                        }
                    ) {
                        Text("OK", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showEditNameDialog = false }) {
                        Text(s.cancelBtn, color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2E),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NormaGuideOverlay (Garis Titik Start Loncatan Abu-Abu Tipis)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NormaGuideOverlay(
    jumpType: String,
    category: JumpCategory,
    gender: JumpGender,
    groundBaselineY: Double,
    takeoffX: Double,
    transformer: CoordinateTransformer?,
    cameraDistance: Float,
    currentJumpValueCm: Double?,
    poseState: com.manspower.jumpmeasure.core.vision.PoseFrameResult?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx  = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        if (widthPx <= 0f || heightPx <= 0f) return@BoxWithConstraints

        val frameWidth  = poseState?.imageWidth?.toFloat()  ?: (if (jumpType == "VERTICAL") 720f else 1280f)
        val frameHeight = poseState?.imageHeight?.toFloat() ?: (if (jumpType == "VERTICAL") 1280f else 720f)
        val scaleFactor  = maxOf(widthPx / frameWidth, heightPx / frameHeight)
        val scaledWidth  = frameWidth * scaleFactor
        val scaledHeight = frameHeight * scaleFactor
        val offsetX      = (widthPx - scaledWidth) / 2f
        val offsetY      = (heightPx - scaledHeight) / 2f

        if (jumpType == "VERTICAL") {
            // LONCAT TEGAK: Hanya garis abu-abu tipis untuk titik start / lantai
            val baseYScreen = offsetY + (groundBaselineY.toFloat() * scaledHeight)
            val clampedBaseY = baseYScreen.coerceIn(heightPx * 0.5f, heightPx * 0.95f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Garis abu-abu tipis untuk titik start loncatan
                drawLine(
                    color = Color(0xFFAAAAAA).copy(alpha = 0.55f),
                    start = Offset(0f, clampedBaseY),
                    end   = Offset(widthPx, clampedBaseY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // Label diskret kecil penanda titik start lantai
            val badgeY = (clampedBaseY - with(density) { 18.dp.toPx() }).toInt().coerceAtLeast(0)
            val badgeX = with(density) { 16.dp.toPx() }.toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(badgeX, badgeY) }
                    .background(Color(0x991E1E2E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "START / LANTAI",
                    color = Color(0xFFCCCCCC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // LOMPAT JAUH: Hanya garis abu-abu tipis untuk titik start / tolakan
            val startXScreen  = offsetX + (takeoffX.toFloat() * scaledWidth)
            val clampedStartX = startXScreen.coerceIn(widthPx * 0.05f, widthPx * 0.45f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Garis abu-abu tipis untuk titik start tolakan
                drawLine(
                    color = Color(0xFFAAAAAA).copy(alpha = 0.55f),
                    start = Offset(clampedStartX, 0f),
                    end   = Offset(clampedStartX, heightPx),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // Label diskret kecil penanda titik start tolakan
            val badgeX = (clampedStartX + with(density) { 6.dp.toPx() }).toInt()
            val badgeY = (heightPx - with(density) { 32.dp.toPx() }).toInt().coerceAtLeast(0)

            Box(
                modifier = Modifier
                    .offset { IntOffset(badgeX, badgeY) }
                    .background(Color(0x991E1E2E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "START / TOLAKAN",
                    color = Color(0xFFCCCCCC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TrajectoryOverlay (Parabola Animation)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrajectoryOverlay(metrics: com.manspower.jumpmeasure.core.analytics.JumpMetrics) {
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

        val controlX = (startX + endX) / 2f
        val distanceX = Math.abs(endX - startX)
        val peakHeight = distanceX * 0.4f 
        
        val baseY = (startY + endY) / 2f
        val controlY = baseY - (peakHeight * 2f)

        val path = Path().apply {
            moveTo(startX, startY)
            quadraticTo(controlX, controlY, endX, endY)
        }

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

