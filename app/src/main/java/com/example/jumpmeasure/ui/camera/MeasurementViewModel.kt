package com.example.jumpmeasure.ui.camera

import androidx.camera.core.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jumpmeasure.core.camera.CameraConfiguration
import com.example.jumpmeasure.core.camera.CameraFrameSource
import com.example.jumpmeasure.core.vision.PoseEstimator
import com.example.jumpmeasure.core.vision.PoseEstimatorConfiguration
import com.example.jumpmeasure.core.vision.PoseFrameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.jumpmeasure.core.geometry.GeometryCalibrator
import com.example.jumpmeasure.core.geometry.CoordinateTransformer
import com.example.jumpmeasure.domain.model.Meters
import com.example.jumpmeasure.core.geometry.CalibrationResult
import com.example.jumpmeasure.data.local.dao.JumpDao
import com.example.jumpmeasure.data.local.entity.JumpEntity
import com.example.jumpmeasure.domain.model.JumpCategory
import com.example.jumpmeasure.domain.model.JumpGender
import com.example.jumpmeasure.domain.model.NormaEvaluator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.jumpmeasure.core.analytics.JumpTracker
import com.example.jumpmeasure.core.analytics.JumpPhase
import com.example.jumpmeasure.core.analytics.JumpMetrics
import com.example.jumpmeasure.ui.language.AppLanguage
import com.example.jumpmeasure.ui.language.LanguageManager
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

    private var isStarted = false
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

    fun startTrackingWithCountdown() {
        viewModelScope.launch {
            _trackingState.value = TrackingState.COUNTDOWN
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
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

    fun savePendingJump(jumpType: String, category: JumpCategory, gender: JumpGender) {
        val metrics = _pendingJumpResult.value ?: return
        val valueCm = if (jumpType == "VERTICAL") metrics.jumpHeightCm else metrics.horizontalDistanceCm
        val norma   = NormaEvaluator.evaluate(valueCm, jumpType, category, gender)

        // Build bilingual save confirmation message
        val strings         = LanguageManager.currentStrings()
        val normaDisplay    = strings.translateNorma(norma.label)
        val savedText       = if (strings.language == com.example.jumpmeasure.ui.language.AppLanguage.ENGLISH)
            "✅ Saved" else "✅ Tersimpan"

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
                    normaLabel           = norma.label   // always stored in Indonesian for DB consistency
                )
            )
            _saveMessage.emit("$savedText — ${norma.emoji} $normaDisplay")
            _pendingJumpResult.value = null
        }
    }

    fun discardPendingJump() {
        _pendingJumpResult.value = null
        _jumpPhase.value = JumpPhase.IDLE
        jumpTracker.reset()
    }

    fun startCamera(
        surfaceProvider: Preview.SurfaceProvider,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        jumpType: String = "VERTICAL",
        cameraDistanceMeters: Float = 3.0f
    ) {
        if (isStarted) return
        isStarted = true

        // Configure tracker thresholds for the specific jump type (HORIZONTAL needs
        // more lenient thresholds for side-view detection)
        jumpTracker.configure(jumpType)

        viewModelScope.launch {
            poseEstimator.initialize(PoseEstimatorConfiguration())

            launch {
                cameraFrameSource.frames.collect { frame ->

                    // Kalibrasi geometri (sekali saja, pakai logical dimensions agar konsisten
                    // dengan koordinat landmark dari MediaPipe)
                    if (jumpType == "HORIZONTAL" && coordinateTransformer == null) {
                        // [FIX v3] Gunakan frame.width / frame.height (sudah merupakan dimensi
                        // buffer ImageProxy), lalu tukar jika kamera dirotasi.
                        // JANGAN gunakan frame.image.width/height langsung — pada ImageProxy
                        // dengan format RGBA_8888, lebar/tinggi mungkin berbeda dari yang diharapkan.
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
                        coordinateTransformer = when (result) {
                            is CalibrationResult.Success -> {
                                Log.d("MeasurementVM", "Calibration OK: " +
                                    "mppX=${result.transformer.metersPerPixelX}, " +
                                    "mppY=${result.transformer.metersPerPixelY}")
                                result.transformer
                            }
                            is CalibrationResult.Error -> {
                                // [FIX v3] Jangan biarkan null — buat fallback transformer dengan
                                // FOV standar 65° horizontal agar Lompat Jauh tetap bisa diukur.
                                Log.w("MeasurementVM", "Calibration failed, using fallback transformer")
                                com.example.jumpmeasure.core.geometry.PinholeCameraTransformer(
                                    frameWidthPixels      = logicalWidth,
                                    frameHeightPixels     = logicalHeight,
                                    cameraDistanceMeters  = cameraDistanceMeters.toDouble(),
                                    fovHorizontalDegrees  = 65.0,
                                    fovVerticalDegrees    = 45.0
                                )
                            }
                        }
                    } // end if (jumpType == "HORIZONTAL")

                    _cameraRotation.value = frame.rotationDegrees

                    if (_trackingState.value == TrackingState.TRACKING) {
                        val result = poseEstimator.estimate(frame)
                        _poseState.value = result

                        if (result.status == com.example.jumpmeasure.core.vision.PoseFrameStatus.SUCCESS) {
                            val newPhase = jumpTracker.processFrame(result, jumpType, coordinateTransformer)
                            _jumpPhase.value  = newPhase
                            _jumpMetrics.value = jumpTracker.currentMetrics

                            if (newPhase == JumpPhase.LANDING && !isJumpSaved) {
                                isJumpSaved = true
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

            val config = CameraConfiguration(
                surfaceProvider = surfaceProvider,
                lifecycleOwner  = lifecycleOwner
            )
            cameraFrameSource.start(config)
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
