package com.manspower.jumpmeasure.domain.model

import com.manspower.jumpmeasure.core.vision.PoseFrameResult

@JvmInline
value class Meters(val value: Double)

data class ImagePoint(val xPx: Double, val yPx: Double)
data class PreviewPoint(val xPx: Double, val yPx: Double)
data class WorldPoint(val xMeters: Double, val yMeters: Double)

enum class MeasurementState {
    IDLE,
    CALIBRATING,
    READY,
    ARMED,
    PREPARING,
    TAKEOFF_CANDIDATE,
    AIRBORNE,
    LANDING_CANDIDATE,
    PROCESSING,
    COMPLETED,
    NEEDS_REVIEW,
    INVALID,
    CANCELLED
}

sealed interface MeasurementError {
    data object CameraPermissionDenied : MeasurementError
    data object CameraUnavailable : MeasurementError
    data object PoseModelInitializationFailed : MeasurementError
    data object CalibrationMissing : MeasurementError
    data object CalibrationInvalid : MeasurementError
    data object CameraMoved : MeasurementError
    data object AthleteOutsideMeasurementZone : MeasurementError
    data object FeetNotVisible : MeasurementError
    data object MultiplePeopleDetected : MeasurementError
    data object LowPoseConfidence : MeasurementError
    data object TakeoffNotDetected : MeasurementError
    data object LandingNotDetected : MeasurementError
    data object AttemptTimedOut : MeasurementError
    data class Unknown(val cause: Throwable) : MeasurementError
}

sealed interface MeasurementEvent {
    data object CalibrationConfirmed : MeasurementEvent
    data object ArmRequested : MeasurementEvent
    data class PoseUpdated(val frame: PoseFrameResult) : MeasurementEvent
    data object CancelRequested : MeasurementEvent
    data object TimeoutReached : MeasurementEvent
    // Placeholder for AttemptResult until we define it
    // data class ProcessingCompleted(val result: AttemptResult) : MeasurementEvent
}
