package com.example.jumpmeasure.core.analytics

import com.example.jumpmeasure.core.vision.BodyLandmark
import com.example.jumpmeasure.core.vision.PoseFrameResult
import com.example.jumpmeasure.core.vision.PoseLandmarkSample
import com.example.jumpmeasure.core.geometry.CoordinateTransformer
import kotlin.math.abs
import kotlin.math.atan2

/**
 * JumpTracker — state machine utama untuk deteksi lompatan.
 *
 * PERBAIKAN KRITIS (v3):
 *  1. Stable baseline: baseline dikunci ke nilai median setelah atlet berdiri stabil ≥30 frame.
 *     Mencegah baseline "mengejar" squat atlet sehingga PREPARATION tidak pernah trigger.
 *  2. Direct STANDING → AIRBORNE: lompatan eksplosif tanpa fase persiapan yang terdeteksi.
 *  3. Multi-landmark fallback: HEEL → ANKLE → FOOT_INDEX untuk robustness tampak samping.
 *  4. Threshold per jumpType: HORIZONTAL menggunakan nilai yang lebih sensitif.
 *  5. PREPARATION → STANDING rollback: atlet kembali berdiri tanpa melompat.
 *  6. Missing frame tolerance 600ms untuk HORIZONTAL (mencegah reset saat kaki terokludasi di udara).
 *  7. Hip detection diperlonggar untuk tampak samping HORIZONTAL.
 *  8. [FIX v3] lastGroundedTimeMs: airborneStartTimeMs dihitung dari frame TERAKHIR di tanah,
 *     bukan frame PERTAMA di udara — mencegah under-counting flightTime.
 *  9. [FIX v3] PRESENCE_THRESHOLD diturunkan 0.4→0.35 untuk frame gerakan cepat.
 * 10. [FIX v3] Baseline stabil lebih cepat: HISTORY_MAX_SIZE 45→15.
 * 11. [FIX v4] Gunakan koordinat Hip (panggul) untuk mendeteksi ancang-ancang (crouch) karena telapak kaki tidak turun saat jongkok.
 * 12. [FIX v4] Terapkan multiplier empiris (1.25x) untuk perhitungan tinggi Vertical Jump agar lebih akurat dengan realita (flight-time physics selalu lebih rendah dari ukuran capaian tangan).
 */
class JumpTracker {

    var currentPhase: JumpPhase = JumpPhase.IDLE
        private set

    var currentMetrics: JumpMetrics? = null
        private set

    private var groundBaselineY: Double = 0.0
    private var standingHipBaselineY: Double = 0.0
    private var airborneStartTimeMs: Long = 0
    private var lastValidFrameTimeMs: Long = 0

    private var takeoffX: Double = 0.0
    private var takeoffNormalizedX: Double = 0.0
    private var takeoffNormalizedY: Double = 0.0
    private var missingFrameCount: Int = 0
    private var totalProcessedFrames: Int = 0
    private var accumulatedPresence: Double = 0.0

    // ── Stable Baseline Mechanism ─────────────────────────────────────────────
    // Track feet-Y and hip-Y history while STANDING. Once stable (low variance), freeze
    // baseline to the median. This prevents the moving average from "chasing"
    // the squat, which caused the PREPARATION phase to never trigger.
    private val recentYHistory = ArrayDeque<Double>()
    private val recentHipYHistory = ArrayDeque<Double>()
    // [FIX v4] 15 frame (~0.5 detik di 30fps) cukup — lebih cepat terkunci daripada 30.
    private val HISTORY_MAX_SIZE = 15
    // [FIX v3] Dinaikkan sedikit (0.0005→0.001) agar standing tidak perlu sempurna diam.
    private val STABILITY_VARIANCE = 0.001
    private var isBaselineFrozen = false

    // ── Configurable Thresholds (tuned per jumpType via configure()) ──────────
    /**
     * [FIX v3] Diturunkan dari 0.4 → 0.35 agar landmark tetap valid pada frame
     * gerakan cepat (lompatan), di mana MediaPipe memberikan confidence lebih rendah.
     */
    private val PRESENCE_THRESHOLD = 0.35

    // [FIX v3] Threshold default lebih sensitif untuk menangkap lompatan atlet putri
    // yang rata-rata memiliki lompatan lebih rendah dari atlet putra.
    private var crouchThresholdY        = 0.02   // hip drops this fraction → PREPARATION (diturunkan ke 0.02 agar responsif)
    private var takeoffThresholdY       = 0.015  // feet rise this fraction → AIRBORNE
    private var missingFrameToleranceMs = 300L
    private var minFlightTimeMs         = 80L    // [FIX v3] Turun dari 100ms → 80ms
    private val maxFlightTimeMs         = 1500L  // [FIX v3] Naik dari 1200ms → 1500ms

    // [FIX v3] Timestamp frame TERAKHIR saat kaki masih di tanah.
    // Digunakan sebagai airborneStartTimeMs yang akurat, mencegah under-counting
    // flightTime akibat pose detector yang mendeteksi takeoff terlambat beberapa frame.
    private var lastGroundedTimeMs: Long = 0

    // Timestamp saat kaki pertama kali kembali ke tanah (awal fase LANDING).
    // Digunakan sebagai referensi post-landing delay yang akurat (bukan airborneStartTimeMs).
    private var landingTimeMs: Long = 0

    /**
     * Configure detection thresholds for the specific jump type.
     * Must be called ONCE before the first [processFrame].
     */
    fun configure(jumpType: String) {
        if (jumpType == "HORIZONTAL") {
            // [FIX v3] Threshold lebih sensitif untuk side-view long jump
            crouchThresholdY        = 0.025  // 2.5% drop → preparation (lebih sensitif)
            takeoffThresholdY       = 0.012  // 1.2% rise → airborne (lebih sensitif)
            missingFrameToleranceMs = 800L   // 800ms: toleransi lebih lama saat kaki ter-okludasi
            minFlightTimeMs         = 60L    // 60ms minimum
        }
        // VERTICAL uses the defaults above — no changes needed
    }

    // ── Multi-Landmark Fallback ───────────────────────────────────────────────
    /**
     * Returns the best available foot landmark using a priority fallback chain:
     *   1. HEEL      — most accurate for ground-contact Y position
     *   2. ANKLE     — good side-view visibility
     *   3. FOOT_INDEX — toe, visible even when heel & ankle are occluded
     *
     * Returns null only if ALL three are below the presence threshold.
     */
    private fun getBestFootLandmark(
        heel:      PoseLandmarkSample?,
        ankle:     PoseLandmarkSample?,
        footIndex: PoseLandmarkSample?
    ): PoseLandmarkSample? {
        if (heel      != null && (heel.presence      ?: 0.0) > PRESENCE_THRESHOLD) return heel
        if (ankle     != null && (ankle.presence     ?: 0.0) > PRESENCE_THRESHOLD) return ankle
        if (footIndex != null && (footIndex.presence ?: 0.0) > PRESENCE_THRESHOLD) return footIndex
        return null
    }

    fun processFrame(
        result:      PoseFrameResult,
        jumpType:    String = "VERTICAL",
        transformer: CoordinateTransformer? = null
    ): JumpPhase {
        val landmarks = result.landmarks

        // ── Resolve best foot landmark for each side ──
        val leftFoot = getBestFootLandmark(
            landmarks[BodyLandmark.LEFT_HEEL],
            landmarks[BodyLandmark.LEFT_ANKLE],
            landmarks[BodyLandmark.LEFT_FOOT_INDEX]
        )
        val rightFoot = getBestFootLandmark(
            landmarks[BodyLandmark.RIGHT_HEEL],
            landmarks[BodyLandmark.RIGHT_ANKLE],
            landmarks[BodyLandmark.RIGHT_FOOT_INDEX]
        )

        val leftHip  = landmarks[BodyLandmark.LEFT_HIP]
        val rightHip = landmarks[BodyLandmark.RIGHT_HIP]

        val leftFootValid  = leftFoot  != null
        val rightFootValid = rightFoot != null
        val leftHipValid   = leftHip  != null && (leftHip.presence  ?: 0.0) > PRESENCE_THRESHOLD
        val rightHipValid  = rightHip != null && (rightHip.presence ?: 0.0) > PRESENCE_THRESHOLD

        val isFeetVisible = leftFootValid || rightFootValid

        // For HORIZONTAL (side-view): hip occlusion is common — only feet are strictly required.
        // For VERTICAL (front-view): require both feet AND hips for full-body context.
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

        // Accumulate presence for confidence scoring
        val visibleFeetCount = (if (leftFootValid) 1 else 0) + (if (rightFootValid) 1 else 0)
        val presenceSum = (leftFoot?.presence ?: 0.0) + (rightFoot?.presence ?: 0.0)
        accumulatedPresence += presenceSum / visibleFeetCount.coerceAtLeast(1)

        lastValidFrameTimeMs = currentTimeMs

        // ── Compute aggregate foot position ──
        val averageFeetY = when {
            leftFootValid && rightFootValid -> (leftFoot!!.normalizedY  + rightFoot!!.normalizedY)  / 2.0
            leftFootValid                  -> leftFoot!!.normalizedY
            else                           -> rightFoot!!.normalizedY
        }
        val averageFeetImageX = when {
            leftFootValid && rightFootValid -> (leftFoot!!.imageX  + rightFoot!!.imageX)  / 2.0
            leftFootValid                  -> leftFoot!!.imageX
            else                           -> rightFoot!!.imageX
        }
        val averageFeetNormalizedX = when {
            leftFootValid && rightFootValid -> (leftFoot!!.normalizedX  + rightFoot!!.normalizedX)  / 2.0
            leftFootValid                  -> leftFoot!!.normalizedX
            else                           -> rightFoot!!.normalizedX
        }

        // ── Compute aggregate hip position ──
        val averageHipY = when {
            leftHipValid && rightHipValid -> (leftHip!!.normalizedY + rightHip!!.normalizedY) / 2.0
            leftHipValid                  -> leftHip!!.normalizedY
            rightHipValid                 -> rightHip!!.normalizedY
            else                          -> standingHipBaselineY // fallback jika tidak terlihat sementara
        }

        // ── State Machine ─────────────────────────────────────────────────────
        when (currentPhase) {

            JumpPhase.IDLE -> {
                groundBaselineY = averageFeetY
                standingHipBaselineY = averageHipY
                currentPhase    = JumpPhase.STANDING
                currentMetrics  = null
                missingFrameCount    = 0
                totalProcessedFrames = 0
                accumulatedPresence  = 0.0
                recentYHistory.clear()
                recentHipYHistory.clear()
                isBaselineFrozen     = false
                lastGroundedTimeMs   = currentTimeMs
            }

            JumpPhase.STANDING -> {
                // [FIX v3] Selalu update lastGroundedTimeMs saat masih di tanah.
                // Ini digunakan sebagai airborneStartTimeMs yang akurat.
                lastGroundedTimeMs = currentTimeMs

                // ── Rolling Y history ──
                recentYHistory.addLast(averageFeetY)
                recentHipYHistory.addLast(averageHipY)
                if (recentYHistory.size > HISTORY_MAX_SIZE) {
                    recentYHistory.removeFirst()
                    recentHipYHistory.removeFirst()
                }

                // ── Stable baseline freeze ──
                // Setelah 15 frame (≈0.5 detik) dengan variance rendah, kunci baseline ke median.
                // Mencegah moving average mengikuti squat atlet.
                if (recentYHistory.size >= HISTORY_MAX_SIZE && !isBaselineFrozen) {
                    val sorted   = recentYHistory.sorted()
                    val mean     = sorted.average()
                    val variance = sorted.map { d -> (d - mean) * (d - mean) }.average()
                    if (variance < STABILITY_VARIANCE) {
                        groundBaselineY  = sorted[sorted.size / 2]   // robust median
                        standingHipBaselineY = recentHipYHistory.sorted()[recentHipYHistory.size / 2]
                        isBaselineFrozen = true
                    }
                }

                // ── Slow adaptive update (only when not frozen) ──
                if (!isBaselineFrozen) {
                    groundBaselineY = (groundBaselineY * 0.85) + (averageFeetY * 0.15)
                    standingHipBaselineY = (standingHipBaselineY * 0.85) + (averageHipY * 0.15)
                }

                when {
                    // ① Squat/crouch detected → PREPARATION (menggunakan Panggul / Hip)
                    averageHipY > standingHipBaselineY + crouchThresholdY -> {
                        currentPhase = JumpPhase.PREPARATION
                    }

                    // ② Direct explosive takeoff without detectable preparation
                    // Threshold is 2× the normal takeoff threshold to avoid false positives
                    averageFeetY < groundBaselineY - takeoffThresholdY * 2.0 -> {
                        // [FIX v3] Gunakan lastGroundedTimeMs bukan currentTimeMs agar
                        // flight time dihitung dari kaki meninggalkan tanah, bukan dari
                        // frame pertama kaki terdeteksi di udara (bisa terlambat).
                        currentPhase        = JumpPhase.AIRBORNE
                        airborneStartTimeMs = lastGroundedTimeMs
                        takeoffX            = averageFeetImageX
                        takeoffNormalizedX  = averageFeetNormalizedX
                        takeoffNormalizedY  = averageFeetY
                    }
                }
            }

            JumpPhase.PREPARATION -> {
                // [FIX v3] Update lastGroundedTimeMs di sini juga (masih di tanah)
                lastGroundedTimeMs = currentTimeMs

                when {
                    // Feet rose above the frozen baseline → AIRBORNE
                    averageFeetY < groundBaselineY - takeoffThresholdY -> {
                        // [FIX v3] Gunakan lastGroundedTimeMs untuk akurasi flightTime
                        currentPhase        = JumpPhase.AIRBORNE
                        airborneStartTimeMs = lastGroundedTimeMs
                        takeoffX            = averageFeetImageX
                        takeoffNormalizedX  = averageFeetNormalizedX
                        takeoffNormalizedY  = averageFeetY
                    }

                    // Athlete returned to standing without jumping → rollback (cek dari panggul kembali naik)
                    averageHipY < standingHipBaselineY + (crouchThresholdY / 2.0) && averageFeetY >= groundBaselineY - takeoffThresholdY -> {
                        currentPhase = JumpPhase.STANDING
                    }
                }
            }

            JumpPhase.AIRBORNE -> {
                val flightTimeMs = currentTimeMs - airborneStartTimeMs

                when {
                    // [FIX v3] Sanity cap ditingkatkan ke 1500ms. Jika melebihi, reset baseline
                    // tapi hanya jika kaki sudah terdeteksi kembali di dekat tanah, bukan langsung.
                    flightTimeMs > maxFlightTimeMs -> {
                        // Tidak langsung reset — tunggu kaki turun dulu
                        if (averageFeetY >= groundBaselineY - takeoffThresholdY * 3.0) {
                            currentPhase     = JumpPhase.STANDING
                            groundBaselineY  = averageFeetY
                            standingHipBaselineY = averageHipY
                            isBaselineFrozen = false
                        }
                        // Jika kaki masih tinggi, biarkan terus AIRBORNE
                    }

                    // Feet returned to ground level → LANDING
                    averageFeetY >= groundBaselineY - takeoffThresholdY -> {
                        currentPhase = JumpPhase.LANDING

                        val landingX           = averageFeetImageX
                        val landingNormalizedX = averageFeetNormalizedX
                        val landingNormalizedY = averageFeetY

                        if (flightTimeMs > minFlightTimeMs) {
                            landingTimeMs = currentTimeMs   // catat waktu landing untuk post-landing delay
                            calculateMetrics(
                                flightTimeMs, takeoffX, landingX,
                                takeoffNormalizedX, landingNormalizedX,
                                takeoffNormalizedY, landingNormalizedY,
                                jumpType, transformer
                            )
                        } else {
                            // Too short — noise/false positive, revert
                            currentPhase = JumpPhase.STANDING
                        }
                    }
                }
            }

            JumpPhase.LANDING -> {
                // Tunggu 1 detik penuh setelah landing sebelum reset untuk attempt berikutnya.
                // Gunakan landingTimeMs bukan airborneStartTimeMs agar delay konsisten
                // terlepas dari durasi lompatan (lompatan panjang tidak memperpendek delay).
                if (landingTimeMs > 0 && currentTimeMs - landingTimeMs > 1000) {
                    currentPhase     = JumpPhase.STANDING
                    isBaselineFrozen = false   // Let baseline re-adapt after landing
                }
            }
        }

        return currentPhase
    }

    // ── Metrics Calculation ───────────────────────────────────────────────────
    private fun calculateMetrics(
        flightTimeMs:      Long,
        takeoffX:          Double,
        landingX:          Double,
        takeoffNormX:      Double,
        landingNormX:      Double,
        takeoffNormY:      Double,
        landingNormY:      Double,
        jumpType:          String,
        transformer:       CoordinateTransformer?
    ) {
        val t = flightTimeMs / 1000.0
        val g = 9.81

        // Vertical height from flight time: h = (1/8) * g * t²
        // Terapkan empirical multiplier 1.25x agar hasil tinggi setara dengan lompatan jangkauan nyata.
        val heightMeters = (1.0 / 8.0) * g * (t * t) * 1.25

        var horizontalDistanceCm = 0.0
        var jumpAngleDegrees     = 0.0

        if (jumpType == "HORIZONTAL" && transformer != null) {
            val pixelDistance  = abs(landingX - takeoffX)
            val distanceMeters = transformer.transformHorizontalDistance(pixelDistance)
            horizontalDistanceCm = distanceMeters * 100.0

            // Projectile kinematics:
            //   Vy_takeoff = g * t / 2  (half time ascending)
            //   Vx_takeoff = distance / t
            //   θ = atan2(Vy, Vx)
            if (distanceMeters > 0.01 && t > 0.0) {
                val vy = g * t / 2.0
                val vx = distanceMeters / t
                jumpAngleDegrees = Math.toDegrees(atan2(vy, vx))
            }
        }

        // Confidence: penalise missing frames
        val averagePresence = if (totalProcessedFrames > 0)
            accumulatedPresence / totalProcessedFrames else 1.0
        val missingRatio = missingFrameCount.toDouble() /
            (totalProcessedFrames + missingFrameCount).coerceAtLeast(1)
        val confidence = (averagePresence - missingRatio * 0.5).coerceIn(0.0, 1.0)

        currentMetrics = JumpMetrics(
            flightTimeMs         = flightTimeMs,
            jumpHeightCm         = heightMeters * 100.0,
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

    // ── Reset ─────────────────────────────────────────────────────────────────
    fun reset() {
        currentPhase         = JumpPhase.IDLE
        groundBaselineY      = 0.0
        standingHipBaselineY = 0.0
        airborneStartTimeMs  = 0
        lastGroundedTimeMs   = 0  // [FIX v3]
        landingTimeMs        = 0
        currentMetrics       = null
        missingFrameCount    = 0
        totalProcessedFrames = 0
        accumulatedPresence  = 0.0
        recentYHistory.clear()
        recentHipYHistory.clear()
        isBaselineFrozen     = false
        // Note: thresholds configured via configure() are intentionally NOT reset
    }
}
