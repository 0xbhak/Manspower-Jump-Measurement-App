package com.manspower.jumpmeasure.core.analytics

import com.manspower.jumpmeasure.core.vision.BodyLandmark
import com.manspower.jumpmeasure.core.vision.PoseFrameResult
import com.manspower.jumpmeasure.core.vision.PoseLandmarkSample
import com.manspower.jumpmeasure.core.geometry.CoordinateTransformer
import kotlin.math.abs
import kotlin.math.atan2

/**
 * JumpTracker — Robust state machine and physics-visual fusion engine for jump measurement.
 *
 * Kalibrasi & Presisi (v5):
 *  1. Ground-Contact Bottommost Tracking: Mengambil Y terbawah dari tumit (heel) dan jari kaki (toe/foot_index)
 *     sehingga saat atlet berjongkok/berjinjit ancang-ancang, sistem TIDAK falsely menganggap sudah melompat.
 *  2. Stable Baseline: Baseline ground & panggul dikunci ke median setelah berdiri stabil.
 *  3. Dynamic Peak Tracking: Selama AIRBORNE, melacak titik tertinggi panggul (minHipY) dan kaki (minFeetY).
 *  4. Physics-Visual Hybrid Calibration: Menggabungkan rumus waktu melayang (h = 1/8 * g * t²) dengan
 *     verifikasi perpindahan panggul visual, mencegah pembacaan ngaco (false spikes/micro-jitters).
 *  5. Min flight time dinaikkan ke 160ms untuk memfilter getaran kamera atau pergeseran kaki ringan.
 */
class JumpTracker {

    var currentPhase: JumpPhase = JumpPhase.IDLE
        private set

    var currentMetrics: JumpMetrics? = null
        private set

    var groundBaselineY: Double = 0.0
        private set
    private var standingHipBaselineY: Double = 0.0
    private var airborneStartTimeMs: Long = 0
    private var lastValidFrameTimeMs: Long = 0

    private var takeoffX: Double = 0.0
    var takeoffNormalizedX: Double = 0.0
        private set
    private var takeoffNormalizedY: Double = 0.0
    private var missingFrameCount: Int = 0
    private var totalProcessedFrames: Int = 0
    private var accumulatedPresence: Double = 0.0

    // Peak tracking during flight
    private var minHipYDuringFlight: Double = 1.0
    private var minFeetYDuringFlight: Double = 1.0

    // ── Stable Baseline Mechanism ─────────────────────────────────────────────
    private val recentYHistory = ArrayDeque<Double>()
    private val recentHipYHistory = ArrayDeque<Double>()
    private val HISTORY_MAX_SIZE = 18
    private val STABILITY_VARIANCE = 0.0008
    private var isBaselineFrozen = false

    // ── Configurable Thresholds ───────────────────────────────────────────────
    private val PRESENCE_THRESHOLD = 0.35

    private var crouchThresholdY        = 0.035  // hip drops 3.5% → PREPARATION
    private var takeoffThresholdY       = 0.025  // feet bottom rises 2.5% → AIRBORNE
    private var missingFrameToleranceMs = 400L
    private var minFlightTimeMs         = 160L   // 160ms minimum (eliminates micro-jitters)
    private val maxFlightTimeMs         = 1400L

    private var lastGroundedTimeMs: Long = 0
    private var landingTimeMs: Long = 0

    fun configure(jumpType: String) {
        if (jumpType == "HORIZONTAL") {
            crouchThresholdY        = 0.030
            takeoffThresholdY       = 0.020
            missingFrameToleranceMs = 700L
            minFlightTimeMs         = 150L
        } else {
            crouchThresholdY        = 0.035
            takeoffThresholdY       = 0.025
            missingFrameToleranceMs = 400L
            minFlightTimeMs         = 160L
        }
    }

    private fun getLowestPointOfFoot(
        heel: PoseLandmarkSample?,
        ankle: PoseLandmarkSample?,
        footIndex: PoseLandmarkSample?
    ): Pair<Double, PoseLandmarkSample?> {
        var maxY = 0.0
        var bestSample: PoseLandmarkSample? = null

        fun check(sample: PoseLandmarkSample?) {
            if (sample != null && (sample.presence ?: 0.0) > PRESENCE_THRESHOLD) {
                if (sample.normalizedY > maxY) {
                    maxY = sample.normalizedY
                    bestSample = sample
                }
            }
        }

        check(heel)
        check(footIndex)
        check(ankle)

        return Pair(maxY, bestSample)
    }

    fun processFrame(
        result: PoseFrameResult,
        jumpType: String = "VERTICAL",
        transformer: CoordinateTransformer? = null
    ): JumpPhase {
        val landmarks = result.landmarks

        val (leftFootBottomY, leftFootSample) = getLowestPointOfFoot(
            landmarks[BodyLandmark.LEFT_HEEL],
            landmarks[BodyLandmark.LEFT_ANKLE],
            landmarks[BodyLandmark.LEFT_FOOT_INDEX]
        )
        val (rightFootBottomY, rightFootSample) = getLowestPointOfFoot(
            landmarks[BodyLandmark.RIGHT_HEEL],
            landmarks[BodyLandmark.RIGHT_ANKLE],
            landmarks[BodyLandmark.RIGHT_FOOT_INDEX]
        )

        val leftHip  = landmarks[BodyLandmark.LEFT_HIP]
        val rightHip = landmarks[BodyLandmark.RIGHT_HIP]

        val leftFootValid  = leftFootSample != null
        val rightFootValid = rightFootSample != null
        val leftHipValid   = leftHip != null && (leftHip.presence ?: 0.0) > PRESENCE_THRESHOLD
        val rightHipValid  = rightHip != null && (rightHip.presence ?: 0.0) > PRESENCE_THRESHOLD

        val isFeetVisible = leftFootValid || rightFootValid
        val isBodyVisible = if (jumpType == "HORIZONTAL") {
            isFeetVisible
        } else {
            isFeetVisible && (leftHipValid || rightHipValid)
        }

        val currentTimeMs = result.timestampNs / 1_000_000

        if (!isBodyVisible) {
            missingFrameCount++
            if (currentPhase != JumpPhase.IDLE &&
                currentTimeMs - lastValidFrameTimeMs > missingFrameToleranceMs
            ) {
                reset()
            }
            return currentPhase
        }

        totalProcessedFrames++

        val visibleFeetCount = (if (leftFootValid) 1 else 0) + (if (rightFootValid) 1 else 0)
        val presenceSum = (leftFootSample?.presence ?: 0.0) + (rightFootSample?.presence ?: 0.0)
        accumulatedPresence += presenceSum / visibleFeetCount.coerceAtLeast(1)

        lastValidFrameTimeMs = currentTimeMs

        // Average ground-contact Y (larger Y = lower / closer to floor)
        val averageFeetBottomY = when {
            leftFootValid && rightFootValid -> (leftFootBottomY + rightFootBottomY) / 2.0
            leftFootValid                   -> leftFootBottomY
            else                            -> rightFootBottomY
        }

        val averageFeetImageX = when {
            leftFootValid && rightFootValid -> (leftFootSample!!.imageX + rightFootSample!!.imageX) / 2.0
            leftFootValid                   -> leftFootSample!!.imageX
            else                            -> rightFootSample!!.imageX
        }

        val averageFeetNormalizedX = when {
            leftFootValid && rightFootValid -> (leftFootSample!!.normalizedX + rightFootSample!!.normalizedX) / 2.0
            leftFootValid                   -> leftFootSample!!.normalizedX
            else                            -> rightFootSample!!.normalizedX
        }

        // Aggregate hip position
        val averageHipY = when {
            leftHipValid && rightHipValid -> (leftHip!!.normalizedY + rightHip!!.normalizedY) / 2.0
            leftHipValid                  -> leftHip!!.normalizedY
            rightHipValid                 -> rightHip!!.normalizedY
            else                          -> standingHipBaselineY
        }

        // ── State Machine ─────────────────────────────────────────────────────
        when (currentPhase) {

            JumpPhase.IDLE -> {
                groundBaselineY      = averageFeetBottomY
                standingHipBaselineY = averageHipY
                currentPhase         = JumpPhase.STANDING
                currentMetrics       = null
                missingFrameCount    = 0
                totalProcessedFrames = 0
                accumulatedPresence  = 0.0
                recentYHistory.clear()
                recentHipYHistory.clear()
                isBaselineFrozen     = false
                lastGroundedTimeMs   = currentTimeMs
                minHipYDuringFlight  = 1.0
                minFeetYDuringFlight = 1.0
            }

            JumpPhase.STANDING -> {
                lastGroundedTimeMs = currentTimeMs

                // Rolling Y history
                recentYHistory.addLast(averageFeetBottomY)
                recentHipYHistory.addLast(averageHipY)
                if (recentYHistory.size > HISTORY_MAX_SIZE) {
                    recentYHistory.removeFirst()
                    recentHipYHistory.removeFirst()
                }

                // Freeze baseline when stable
                if (recentYHistory.size >= HISTORY_MAX_SIZE && !isBaselineFrozen) {
                    val sorted   = recentYHistory.sorted()
                    val mean     = sorted.average()
                    val variance = sorted.map { d -> (d - mean) * (d - mean) }.average()
                    if (variance < STABILITY_VARIANCE) {
                        groundBaselineY      = sorted[sorted.size / 2]
                        standingHipBaselineY = recentHipYHistory.sorted()[recentHipYHistory.size / 2]
                        isBaselineFrozen     = true
                    }
                }

                if (!isBaselineFrozen) {
                    groundBaselineY      = (groundBaselineY * 0.85) + (averageFeetBottomY * 0.15)
                    standingHipBaselineY = (standingHipBaselineY * 0.85) + (averageHipY * 0.15)
                }

                when {
                    // ① Squat/crouch detected (hip descends) → PREPARATION
                    averageHipY > standingHipBaselineY + crouchThresholdY -> {
                        currentPhase = JumpPhase.PREPARATION
                    }

                    // ② Direct explosive takeoff without crouching
                    averageFeetBottomY < groundBaselineY - (takeoffThresholdY * 1.5) -> {
                        currentPhase         = JumpPhase.AIRBORNE
                        airborneStartTimeMs  = lastGroundedTimeMs
                        takeoffX             = averageFeetImageX
                        takeoffNormalizedX   = averageFeetNormalizedX
                        takeoffNormalizedY   = averageFeetBottomY
                        minHipYDuringFlight  = averageHipY
                        minFeetYDuringFlight = averageFeetBottomY
                    }
                }
            }

            JumpPhase.PREPARATION -> {
                lastGroundedTimeMs = currentTimeMs

                when {
                    // Both feet and toes leave the ground → AIRBORNE
                    averageFeetBottomY < groundBaselineY - takeoffThresholdY -> {
                        currentPhase         = JumpPhase.AIRBORNE
                        airborneStartTimeMs  = lastGroundedTimeMs
                        takeoffX             = averageFeetImageX
                        takeoffNormalizedX   = averageFeetNormalizedX
                        takeoffNormalizedY   = averageFeetBottomY
                        minHipYDuringFlight  = averageHipY
                        minFeetYDuringFlight = averageFeetBottomY
                    }

                    // Athlete stood back up without jumping → rollback
                    averageHipY < standingHipBaselineY + (crouchThresholdY * 0.5) &&
                    averageFeetBottomY >= groundBaselineY - takeoffThresholdY -> {
                        currentPhase = JumpPhase.STANDING
                    }
                }
            }

            JumpPhase.AIRBORNE -> {
                val flightTimeMs = currentTimeMs - airborneStartTimeMs

                // Track highest elevation reached in the air
                minHipYDuringFlight  = minOf(minHipYDuringFlight, averageHipY)
                minFeetYDuringFlight = minOf(minFeetYDuringFlight, averageFeetBottomY)

                when {
                    flightTimeMs > maxFlightTimeMs -> {
                        if (averageFeetBottomY >= groundBaselineY - (takeoffThresholdY * 2.0)) {
                            currentPhase         = JumpPhase.STANDING
                            groundBaselineY      = averageFeetBottomY
                            standingHipBaselineY = averageHipY
                            isBaselineFrozen     = false
                        }
                    }

                    // Feet touch back down to ground baseline → LANDING
                    averageFeetBottomY >= groundBaselineY - (takeoffThresholdY * 0.65) -> {
                        currentPhase = JumpPhase.LANDING

                        val landingX           = averageFeetImageX
                        val landingNormalizedX = averageFeetNormalizedX
                        val landingNormalizedY = averageFeetBottomY

                        if (flightTimeMs >= minFlightTimeMs) {
                            landingTimeMs = currentTimeMs
                            calculateMetrics(
                                flightTimeMs, takeoffX, landingX,
                                takeoffNormalizedX, landingNormalizedX,
                                takeoffNormalizedY, landingNormalizedY,
                                jumpType, transformer, result.imageHeight
                            )
                        } else {
                            // Rebound/jitter filter
                            currentPhase = JumpPhase.STANDING
                        }
                    }
                }
            }

            JumpPhase.LANDING -> {
                if (landingTimeMs > 0 && currentTimeMs - landingTimeMs > 1000) {
                    currentPhase     = JumpPhase.STANDING
                    isBaselineFrozen = false
                }
            }
        }

        return currentPhase
    }

    private fun calculateMetrics(
        flightTimeMs: Long,
        takeoffX: Double,
        landingX: Double,
        takeoffNormX: Double,
        landingNormX: Double,
        takeoffNormY: Double,
        landingNormY: Double,
        jumpType: String,
        transformer: CoordinateTransformer?,
        frameHeightPx: Int
    ) {
        val t = flightTimeMs / 1000.0
        val g = 9.81

        // Gold standard sports physics: flight time h = (1/8) * g * t²
        val flightHeightMeters = (1.0 / 8.0) * g * (t * t)

        // Visual hip displacement:
        val hipDeltaY = (standingHipBaselineY - minHipYDuringFlight).coerceAtLeast(0.0)
        val visualHipHeightMeters = if (transformer != null && transformer.metersPerPixelY > 0) {
            hipDeltaY * frameHeightPx * transformer.metersPerPixelY
        } else {
            hipDeltaY * 2.4 // default estimate for ~2.5m viewing frustum
        }

        // Calibrated Vertical Height:
        // Blend flight time (75%) with visual displacement (25%) when visual displacement is valid.
        // This stops exaggerated numbers when landing with bent knees while honoring physics!
        val jumpHeightCm = if (visualHipHeightMeters > 0.05) {
            val blendedMeters = (flightHeightMeters * 0.75) + (visualHipHeightMeters * 0.25)
            (blendedMeters * 100.0).coerceIn(5.0, 140.0)
        } else {
            (flightHeightMeters * 100.0).coerceIn(5.0, 140.0)
        }

        var horizontalDistanceCm = 0.0
        var jumpAngleDegrees     = 0.0

        if (jumpType == "HORIZONTAL" && transformer != null) {
            val pixelDistance  = abs(landingX - takeoffX)
            val distanceMeters = transformer.transformHorizontalDistance(pixelDistance)
            horizontalDistanceCm = (distanceMeters * 100.0).coerceIn(20.0, 420.0)

            if (distanceMeters > 0.01 && t > 0.0) {
                val vy = g * t / 2.0
                val vx = distanceMeters / t
                jumpAngleDegrees = Math.toDegrees(atan2(vy, vx)).coerceIn(15.0, 75.0)
            }
        }

        // Confidence scoring
        val averagePresence = if (totalProcessedFrames > 0)
            accumulatedPresence / totalProcessedFrames else 1.0
        val missingRatio = missingFrameCount.toDouble() /
            (totalProcessedFrames + missingFrameCount).coerceAtLeast(1)
        val confidence = (averagePresence - missingRatio * 0.4).coerceIn(0.60, 0.99)

        currentMetrics = JumpMetrics(
            flightTimeMs         = flightTimeMs,
            jumpHeightCm         = jumpHeightCm,
            horizontalDistanceCm = horizontalDistanceCm,
            jumpAngleDegrees     = jumpAngleDegrees,
            takeoffPointX        = takeoffX,
            landingPointX        = landingX,
            takeoffNormalizedX   = takeoffNormX,
            takeoffNormalizedY   = takeoffNormY,
            landingNormalizedX   = landingNormX,
            landingNormalizedY   = landingNormY,
            confidenceScore      = confidence
        )
    }

    fun reset() {
        currentPhase         = JumpPhase.IDLE
        groundBaselineY      = 0.0
        standingHipBaselineY = 0.0
        airborneStartTimeMs  = 0
        lastGroundedTimeMs   = 0
        landingTimeMs        = 0
        currentMetrics       = null
        missingFrameCount    = 0
        totalProcessedFrames = 0
        accumulatedPresence  = 0.0
        minHipYDuringFlight  = 1.0
        minFeetYDuringFlight = 1.0
        recentYHistory.clear()
        recentHipYHistory.clear()
        isBaselineFrozen     = false
    }
}
