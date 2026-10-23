package com.manspower.jumpmeasure.core.analytics

import com.manspower.jumpmeasure.core.vision.BodyLandmark
import com.manspower.jumpmeasure.core.vision.PoseFrameResult
import com.manspower.jumpmeasure.core.vision.PoseFrameStatus
import com.manspower.jumpmeasure.core.vision.PoseLandmarkSample
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JumpTrackerTest {

    private lateinit var tracker: JumpTracker

    @Before
    fun setup() {
        tracker = JumpTracker()
    }

    private fun createFrame(y: Double, presence: Double = 1.0, timestampMs: Long): PoseFrameResult {
        val landmarks = mapOf(
            BodyLandmark.LEFT_HEEL to createLandmark(BodyLandmark.LEFT_HEEL, y, presence),
            BodyLandmark.RIGHT_HEEL to createLandmark(BodyLandmark.RIGHT_HEEL, y, presence),
            BodyLandmark.LEFT_HIP to createLandmark(BodyLandmark.LEFT_HIP, y - 0.4, presence),
            BodyLandmark.RIGHT_HIP to createLandmark(BodyLandmark.RIGHT_HIP, y - 0.4, presence)
        )
        return PoseFrameResult(
            timestampNs = timestampMs * 1_000_000,
            landmarks = landmarks,
            poseConfidence = 1.0,
            inferenceDurationMs = 10.0,
            segmentationMask = null,
            status = PoseFrameStatus.SUCCESS
        )
    }

    private fun createLandmark(type: BodyLandmark, y: Double, presence: Double): PoseLandmarkSample {
        return PoseLandmarkSample(
            type = type,
            imageX = 500.0,
            imageY = y * 1000,
            normalizedX = 0.5,
            normalizedY = y,
            relativeZ = 0.0,
            visibility = 1.0,
            presence = presence
        )
    }

    @Test
    fun testJumpSequence() {
        // 1. Frame awal berdiri
        assertEquals(JumpPhase.IDLE, tracker.currentPhase)
        tracker.processFrame(createFrame(0.8, timestampMs = 1000))
        assertEquals(JumpPhase.STANDING, tracker.currentPhase)

        // 2. Preparation (Turun 10% Y)
        tracker.processFrame(createFrame(0.9, timestampMs = 1500))
        assertEquals(JumpPhase.PREPARATION, tracker.currentPhase)

        // 3. Airborne (Naik melebih batas ground, Y mengecil)
        tracker.processFrame(createFrame(0.7, timestampMs = 1600))
        assertEquals(JumpPhase.AIRBORNE, tracker.currentPhase)

        // 4. Mendarat setelah 500ms (Kembali ke 0.8)
        tracker.processFrame(createFrame(0.8, timestampMs = 2100)) // 2100 - 1600 = 500ms
        assertEquals(JumpPhase.LANDING, tracker.currentPhase)

        // Verifikasi hasil tinggi
        val metrics = tracker.currentMetrics
        assertNotNull(metrics)
        assertEquals(500L, metrics!!.flightTimeMs)
        // h = 1/8 * 9.81 * 0.5^2 = 0.3065 m = 30.65 cm, with empirical multiplier 1.25 = 38.31 cm
        assertEquals(30.65 * 1.25, metrics.jumpHeightCm, 0.1)
    }
}
