package com.manspower.jumpmeasure.ui.camera

import androidx.camera.core.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manspower.jumpmeasure.core.camera.CameraConfiguration
import com.manspower.jumpmeasure.core.camera.CameraFrameSource
import com.manspower.jumpmeasure.core.vision.PoseEstimator
import com.manspower.jumpmeasure.core.vision.PoseEstimatorConfiguration
import com.manspower.jumpmeasure.core.vision.PoseFrameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.manspower.jumpmeasure.core.geometry.GeometryCalibrator
import com.manspower.jumpmeasure.core.geometry.CoordinateTransformer
import com.manspower.jumpmeasure.domain.model.Meters
import com.manspower.jumpmeasure.core.geometry.CalibrationResult
import com.manspower.jumpmeasure.data.local.dao.JumpDao
import com.manspower.jumpmeasure.data.local.entity.JumpEntity
import com.manspower.jumpmeasure.domain.model.JumpCategory
import com.manspower.jumpmeasure.domain.model.JumpGender
import com.manspower.jumpmeasure.domain.model.NormaEvaluator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.manspower.jumpmeasure.core.analytics.JumpTracker
import com.manspower.jumpmeasure.core.analytics.JumpPhase
import com.manspower.jumpmeasure.core.analytics.JumpMetrics
import com.manspower.jumpmeasure.ui.language.AppLanguage
import com.manspower.jumpmeasure.ui.language.LanguageManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import android.util.Log

enum class TrackingState {
    IDLE, COUNTDOWN, TRACKING, PAUSED
}

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraFrameSource: CameraFrameSource,
    private val poseEstimator: PoseEstimator,
    private val jumpDao: JumpDao
) : ViewModel() {

    private val _poseState = MutableStateFlow<PoseFrameResult?>(null)
    val poseState: StateFlow<PoseFrameResult?> = _poseState.asStateFlow()

    private val jumpTracker = JumpTracker()

    private val _jumpPhase = MutableStateFlow<JumpPhase>(JumpPhase.IDLE)
    val jumpPhase: StateFlow<JumpPhase> = _jumpPhase.asStateFlow()

    private val _jumpMetrics = MutableStateFlow<JumpMetrics?>(null)
    val jumpMetrics: StateFlow<JumpMetrics?> = _jumpMetrics.asStateFlow()

    private val _saveMessage = MutableSharedFlow<String>()
    val saveMessage: SharedFlow<String> = _saveMessage.asSharedFlow()

    private val geometryCalibrator = GeometryCalibrator(context)
    private var coordinateTransformer: CoordinateTransformer? = null

    private var isJumpSaved = false

    private val _trackingState = MutableStateFlow(TrackingState.IDLE)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _countdownValue = MutableStateFlow(3)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _pendingJumpResult = MutableStateFlow<JumpMetrics?>(null)
    val pendingJumpResult: StateFlow<JumpMetrics?> = _pendingJumpResult.asStateFlow()

    private val _cameraRotation = MutableStateFlow(90)
    val cameraRotation: StateFlow<Int> = _cameraRotation.asStateFlow()

    private val _participantName = MutableStateFlow("")
    val participantName: StateFlow<String> = _participantName.asStateFlow()

    private val _transformerState = MutableStateFlow<CoordinateTransformer?>(null)
    val transformerState: StateFlow<CoordinateTransformer?> = _transformerState.asStateFlow()

    private val _groundBaselineYState = MutableStateFlow(0.85)
    val groundBaselineYState: StateFlow<Double> = _groundBaselineYState.asStateFlow()

    private val _takeoffXState = MutableStateFlow(0.18)
    val takeoffXState: StateFlow<Double> = _takeoffXState.asStateFlow()

    fun updateParticipantName(name: String) {
        _participantName.value = name
    }

    fun startTrackingWithCountdown() {
        viewModelScope.launch {
            _trackingState.value = TrackingState.COUNTDOWN
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                com.manspower.jumpmeasure.core.sound.SoundManager.playCountdownBeep()
                delay(1000)
            }
            com.manspower.jumpmeasure.core.sound.SoundManager.playStartTone()
            _jumpPhase.value = JumpPhase.IDLE
            jumpTracker.reset()
            _trackingState.value = TrackingState.TRACKING
        }
    }

    fun pauseTracking()  { _trackingState.value = TrackingState.PAUSED }
    fun resumeTracking() { _trackingState.value = TrackingState.TRACKING }

    fun stopTracking() {
        _trackingState.value = TrackingState.IDLE
        _jumpPhase.value = JumpPhase.IDLE
        jumpTracker.reset()
    }

    fun savePendingJump(
        participantName: String,
        jumpType: String,
        category: JumpCategory,
        gender: JumpGender
    ) {
        val metrics = _pendingJumpResult.value ?: return
        val valueCm = if (jumpType == "VERTICAL") metrics.jumpHeightCm else metrics.horizontalDistanceCm
        val norma   = NormaEvaluator.evaluate(valueCm, jumpType, category, gender)

        // Build bilingual save confirmation message
        val strings         = LanguageManager.currentStrings()
        val normaDisplay    = strings.translateNorma(norma.label)
        val savedText       = if (strings.language == com.manspower.jumpmeasure.ui.language.AppLanguage.ENGLISH)
            "✅ Saved" else "✅ Tersimpan"

        val effectiveName = if (participantName.isNotBlank()) participantName.trim() else "Peserta"

        viewModelScope.launch {
            jumpDao.insertJump(
                JumpEntity(
                    jumpType             = jumpType,
                    jumpHeightCm         = metrics.jumpHeightCm,
                    horizontalDistanceCm = metrics.horizontalDistanceCm,
                    jumpAngleDegrees     = metrics.jumpAngleDegrees,
                    flightTimeMs         = metrics.flightTimeMs,
                    confidenceScore      = metrics.confidenceScore,
                    category             = category.name,
                    gender               = gender.name,
                    normaLabel           = norma.label,   // always stored in Indonesian for DB consistency
                    participantName      = effectiveName
                )
            )
            _saveMessage.emit("$savedText — $effectiveName (${norma.emoji} $normaDisplay)")
            _pendingJumpResult.value = null
        }
    }

    fun discardPendingJump() {
        _pendingJumpResult.value = null
        _jumpPhase.value = JumpPhase.IDLE
        jumpTracker.reset()
    }

    private var frameProcessingJob: kotlinx.coroutines.Job? = null
    private var lastCalibratedRotation: Int = -1

    fun startCamera(
        surfaceProvider: Preview.SurfaceProvider,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        jumpType: String = "VERTICAL",
        cameraDistanceMeters: Float = 3.0f
    ) {
        // Configure tracker thresholds for the specific jump type
        jumpTracker.configure(jumpType)

        if (frameProcessingJob == null || frameProcessingJob?.isActive != true) {
            frameProcessingJob = viewModelScope.launch {
                poseEstimator.initialize(PoseEstimatorConfiguration())

                cameraFrameSource.frames.collect { frame ->
                    // Re-kalibrasi geometri jika transformer belum ada atau rotasi HP berubah (portrait <-> landscape)
                    if (coordinateTransformer == null || frame.rotationDegrees != lastCalibratedRotation) {
                        lastCalibratedRotation = frame.rotationDegrees
                        val isRotated     = frame.rotationDegrees == 90 || frame.rotationDegrees == 270
                        val logicalWidth  = if (isRotated) frame.height else frame.width
                        val logicalHeight = if (isRotated) frame.width  else frame.height

                        Log.d("MeasurementVM", "Calibrating: rotation=${frame.rotationDegrees}, " +
                            "raw=${frame.width}x${frame.height}, " +
                            "logical=${logicalWidth}x${logicalHeight}")

                        val result = geometryCalibrator.calibrate(
                            cameraDistanceMeters = Meters(cameraDistanceMeters.toDouble()),
                            athleteHeightMeters  = Meters(1.7),
                            frameWidthPx         = logicalWidth,
                            frameHeightPx        = logicalHeight
                        )
                        val transformer = when (result) {
                            is CalibrationResult.Success -> {
                                Log.d("MeasurementVM", "Calibration OK: " +
                                    "mppX=${result.transformer.metersPerPixelX}, " +
                                    "mppY=${result.transformer.metersPerPixelY}")
                                result.transformer
                            }
                            is CalibrationResult.Error -> {
                                Log.w("MeasurementVM", "Calibration failed, using fallback transformer")
                                com.manspower.jumpmeasure.core.geometry.PinholeCameraTransformer(
                                    frameWidthPixels      = logicalWidth,
                                    frameHeightPixels     = logicalHeight,
                                    cameraDistanceMeters  = cameraDistanceMeters.toDouble(),
                                    fovHorizontalDegrees  = 65.0,
                                    fovVerticalDegrees    = 45.0
                                )
                            }
                        }
                        coordinateTransformer = transformer
                        _transformerState.value = transformer
                    }

                    _cameraRotation.value = frame.rotationDegrees

                    if (_trackingState.value == TrackingState.TRACKING) {
                        val result = poseEstimator.estimate(frame)
                        _poseState.value = result

                        if (result.status == com.manspower.jumpmeasure.core.vision.PoseFrameStatus.SUCCESS) {
                            val newPhase = jumpTracker.processFrame(result, jumpType, coordinateTransformer)
                            _jumpPhase.value  = newPhase
                            _jumpMetrics.value = jumpTracker.currentMetrics

                            if (jumpTracker.groundBaselineY in 0.1..0.98) {
                                _groundBaselineYState.value = jumpTracker.groundBaselineY
                            }
                            if (jumpTracker.takeoffNormalizedX in 0.05..0.95) {
                                _takeoffXState.value = jumpTracker.takeoffNormalizedX
                            }

                            if (newPhase == JumpPhase.LANDING && !isJumpSaved) {
                                isJumpSaved = true
                                com.manspower.jumpmeasure.core.sound.SoundManager.playLandingSuccessTone()
                                jumpTracker.currentMetrics?.let { metrics ->
                                    _pendingJumpResult.value = metrics
                                    _trackingState.value = TrackingState.PAUSED
                                }
                            } else if (newPhase == JumpPhase.IDLE || newPhase == JumpPhase.STANDING) {
                                isJumpSaved = false
                            }
                        }
                    } else {
                        _poseState.value = null
                    }

                    frame.image.close()
                }
            }
        }

        // Hubungkan CameraX ke surfaceProvider & lifecycleOwner yang aktif
        viewModelScope.launch {
            val config = CameraConfiguration(
                surfaceProvider = surfaceProvider,
                lifecycleOwner  = lifecycleOwner
            )
            cameraFrameSource.start(config)
        }
    }

    fun stopCamera() {
        viewModelScope.launch {
            cameraFrameSource.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            cameraFrameSource.stop()
            poseEstimator.close()
        }
    }
}
