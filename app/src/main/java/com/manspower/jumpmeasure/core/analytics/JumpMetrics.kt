package com.manspower.jumpmeasure.core.analytics

data class JumpMetrics(
    val flightTimeMs: Long,
    val jumpHeightCm: Double,
    val horizontalDistanceCm: Double = 0.0,
    val jumpAngleDegrees: Double = 0.0,
    val takeoffPointX: Double = 0.0,
    val landingPointX: Double = 0.0,
    val takeoffNormalizedX: Double = 0.0,
    val takeoffNormalizedY: Double = 0.0,
    val landingNormalizedX: Double = 0.0,
    val landingNormalizedY: Double = 0.0,
    val confidenceScore: Double = 1.0
)

